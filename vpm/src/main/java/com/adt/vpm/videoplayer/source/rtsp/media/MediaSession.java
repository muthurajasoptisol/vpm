/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.adt.vpm.videoplayer.source.rtsp.media;

import android.net.Uri;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.IntDef;

import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.core.Player;
import com.adt.vpm.videoplayer.source.rtsp.RtspSampleStreamWrapper;
import com.adt.vpm.videoplayer.source.rtsp.core.Client;
import com.adt.vpm.videoplayer.source.rtsp.message.InterleavedFrame;
import com.adt.vpm.videoplayer.source.rtsp.message.Range;
import com.adt.vpm.videoplayer.source.rtsp.message.Transport;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.adt.vpm.videoplayer.source.core.Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST;
import static com.adt.vpm.videoplayer.source.rtsp.core.Client.FLAG_ENABLE_RTCP_SUPPORT;
import static com.adt.vpm.videoplayer.source.rtsp.core.Client.FLAG_FORCE_RTCP_MUXED;
import static com.adt.vpm.videoplayer.source.rtsp.core.Client.RTSP_NAT_DUMMY;

public final class MediaSession implements Player.EventListener {

    public interface EventListener {
        void onPausePlayback();
        void onResumePlayback();
        void onSeekPlayback();
        void onStopPlayback();
    }
    /**
     * Flags to indicate the media session state.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = true, value = {IDLE, PREPARING, PREPARED, PLAYING, PAUSED, STOPPED})
    @interface SessionState {}
    final static int IDLE = 0;
    final static int PREPARING = 1;
    final static int PREPARED = 2;
    final static int PLAYING = 3;
    final static int PAUSED = 4;
    final static int STOPPED = 5;

    /**
     * Flags to indicate the delivery mode.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = true, value = {UNICAST, MULTICAST, INTERLEAVED, HTTP_TUNNELED})
    @interface DeliveryMode {}
    final static int UNICAST = 0;
    final static int MULTICAST = 1;
    final static int INTERLEAVED = 2;
    final static int HTTP_TUNNELED = 3;

    private boolean isVideoTrackEnable;

    private final static int DEFAULT_TIMEOUT_MILLIS = 60000;

    private int cSeq;

    private String id;
    private String name;
    private String description;

    private int timeout;
    private long duration;

    private String language;
    private String server;

    private final List<MediaTrack> tracks;
    private final ConcurrentLinkedQueue<RtspSampleStreamWrapper> preparing;
    private final ConcurrentLinkedQueue<RtspSampleStreamWrapper> prepared;

    private final CopyOnWriteArraySet<EventListener> listeners;
    private final Map<Integer, RtspSampleStreamWrapper> interleavedListeners;

    private Uri uri;
    private Uri baseUri;
    private final String username;
    private final String password;
    private final Client client;

    private final KeepAliveMonitor keepAliveMonitor;

    private boolean isInFallback;
    private @SessionState int state;
    private @DeliveryMode int deliveryMode;

    private int[] tcpChannels;
    private long pendingResetPosition;

    MediaSession(Builder builder) {
        this.client = builder.client;
        this.uri = builder.uri;
        this.username = builder.username;
        this.password = builder.password;

        duration = C.TIME_UNSET;

        listeners = new CopyOnWriteArraySet<>();
        interleavedListeners = Collections.synchronizedMap(new LinkedHashMap<>());

        tracks = new ArrayList<>();
        preparing = new ConcurrentLinkedQueue<>();
        prepared = new ConcurrentLinkedQueue<>();

        state = IDLE;
        timeout = DEFAULT_TIMEOUT_MILLIS;
        deliveryMode = (client.isInterleavedMode() || client.isRetryTcpFirst()) ?
            INTERLEAVED :
            UNICAST;

        keepAliveMonitor = new KeepAliveMonitor();

        tcpChannels = new int[0];
        pendingResetPosition = C.TIME_UNSET;

        new Handler(Looper.getMainLooper()).post(() ->
            client.getPlayer().addListener(MediaSession.this)
        );
    }

    public Uri getUri() { return uri; }

    public String getUsername() { return username; }

    public String getPassword() { return password; }

    public int getNextCSeq() { return ++cSeq; }

    public int getCSeq() { return cSeq; }

    public int getNextTcpChannel() {
        return (tcpChannels.length == 0) ? 0 : tcpChannels[tcpChannels.length - 1] + 1;
    }

    public Uri getBaseUri() { return baseUri; }

    public void setBaseUri(Uri baseUri) { this.baseUri = baseUri; }

    public void setTimeout(int timeout) { this.timeout = timeout; }

    public int getTimeout() { return timeout; }

    public void setId(String id) { this.id = id; }

    public String getId() { return id; }

    public void setName(String name) { this.name = name; }

    public String getName() { return name; }

    public void setDescription(String description) { this.description = description; }

    public String getDescription() { return description; }

    public void setLanguage(String language) { this.language = language; }

    public String getLanguage() { return language; }

    public String getServer() { return server; }

    public void setServer(String server) { this.server = server; }

    public long getDuration() { return duration; }

    public void setDuration(long duration) { this.duration = duration; }

    public boolean isInterleaved() { return client.isInterleavedMode() || tcpChannels.length > 0; }

    public boolean isPaused() {
        return state == PAUSED;
    }

    public boolean isInFallback() { return isInFallback; }

    public List<MediaTrack> getMediaTracks() {
        return Collections.unmodifiableList(tracks);
    }

    public List<MediaTrack> getMediaAudioTracks() {
        List<MediaTrack> audioTracks = new ArrayList<>();
        for (MediaTrack track : tracks) {
            if (track.format().type() == MediaFormat.AUDIO) {
                audioTracks.add(track);
            }
        }

        return Collections.unmodifiableList(audioTracks);
    }

    public List<MediaTrack> getMediaTextTracks() {
        List<MediaTrack> textTracks = new ArrayList<>();
        for (MediaTrack track : tracks) {
            if (track.format().type() == MediaFormat.TEXT) {
                textTracks.add(track);
            }
        }

        return Collections.unmodifiableList(textTracks);
    }

    public List<MediaTrack> getMediaVideoTracks() {
        List<MediaTrack> videoTracks = new ArrayList<>();
        for (MediaTrack track : tracks) {
            if (track.format().type() == MediaFormat.VIDEO) {
                videoTracks.add(track);
            }
        }

        return Collections.unmodifiableList(videoTracks);
    }

    public List<RtspSampleStreamWrapper> getSampleStreamsPrepared() {
        return Collections.unmodifiableList(Arrays.asList(prepared.toArray(
            new RtspSampleStreamWrapper[0])));
    }

    public void addMediaTrack(MediaTrack track) {
        tracks.add(track);
    }

    /**
     * Register a listener to receive events from the media session.
     *
     * @param listener The listener to register.
     */
    public void addListener(EventListener listener) {
        listeners.add(listener);
    }

    public void onPauseSuccess() {
        if (pendingResetPosition == C.TIME_UNSET) {
            if (state != PAUSED) {
                state = PAUSED;

                for (EventListener listener : listeners) {
                    listener.onPausePlayback();
                }
            }
        } else {
            if (state == PLAYING) {
                client.sendPlayRequest(Range.parse("npt=" + pendingResetPosition + "-end"), 1);
            }
        }
    }

    public void onPlaySuccess() {
        if (pendingResetPosition == C.TIME_UNSET) {
            if (state == PREPARED) {
                keepAliveMonitor.start();
            }

            if (state == PAUSED) {
                for (EventListener listener : listeners) {
                    listener.onResumePlayback();
                }
            }

            state = PLAYING;

        } else {
            for (EventListener listener : listeners) {
                listener.onSeekPlayback();
            }

            pendingResetPosition = C.TIME_UNSET;
        }
    }

    public void seekTo(long position) {
        if (state == PLAYING || state == PAUSED) {
            pendingResetPosition = position;
            if (state == PLAYING) {
                client.sendPauseRequest();
            } else {
                client.sendPlayRequest(Range.parse("npt=" + position + "-end"));
            }
        }
    }

    public void close() {
        if (state > PREPARING && state < STOPPED) {
            client.sendTeardownRequest();
        }
        client.close();
    }

    public void release() {
        if (state > PREPARED && state < STOPPED) {
            keepAliveMonitor.cancel();
            state = STOPPED;
        }

        for (EventListener listener : listeners) {
            listener.onStopPlayback();
        }

        new Handler(Looper.getMainLooper()).post(() -> {
            client.getPlayer().removeListener(MediaSession.this);
        });

        tracks.clear();
        prepared.clear();
        preparing.clear();

        listeners.clear();
        interleavedListeners.clear();

        tcpChannels = new int[0];

        state = IDLE;
        duration = C.TIME_UNSET;
        timeout = DEFAULT_TIMEOUT_MILLIS;
        pendingResetPosition = C.TIME_UNSET;
    }

    public final boolean isRtcpSupported() {
        return client.isFlagSet(FLAG_ENABLE_RTCP_SUPPORT);
    }

    public final boolean isRtcpMuxed() {
        return client.isFlagSet(FLAG_FORCE_RTCP_MUXED);
    }

    public final boolean isNatRequired() {
        return client.isNatSet(RTSP_NAT_DUMMY);
    }

    public void prepareStreams(RtspSampleStreamWrapper... sampleStreamWrappers) {
        if (state == IDLE) {
            Collections.addAll(preparing, sampleStreamWrappers);
            if (preparing.size() > 0) {
                RtspSampleStreamWrapper sampleStreamWrapper = preparing.poll();
                sampleStreamWrapper.prepare();

                prepared.add(sampleStreamWrapper);

                state = PREPARING;
            }
        }
    }

    public void continuePrepareStream(RtspSampleStreamWrapper sampleStreamWrapper) {
        MediaTrack track = sampleStreamWrapper.getMediaTrack();
        int localPort = sampleStreamWrapper.getLocalPort();

        if (deliveryMode == INTERLEAVED) {
            if (prepared.size() > 0) {
                Transport transport = Transport.parse("RTP/AVP/TCP;interleaved=" + getNextTcpChannel());
                client.sendSetupRequest(track.url(), transport);
            }

        } else {
            client.sendSetupRequest(track, localPort);
        }
    }

    public synchronized void configureTransport(Transport transport) {
        if (prepared.size() > 0) {
            RtspSampleStreamWrapper[] preparedSamples = new RtspSampleStreamWrapper[prepared.size()];
            prepared.toArray(preparedSamples);

            int currentSample = prepared.size() - 1;
            RtspSampleStreamWrapper sampleStreamWrapper = preparedSamples[currentSample];
            sampleStreamWrapper.getMediaTrack().format().transport(transport);

            if (Transport.TCP.equals(transport.getLowerTransport())) {
                int channelsCount = tcpChannels.length;
                tcpChannels = Arrays.copyOf(tcpChannels,
                    channelsCount + transport.getChannels().length);

                System.arraycopy(transport.getChannels(), 0, tcpChannels, channelsCount,
                    transport.getChannels().length);

                sampleStreamWrapper.setInterleavedChannels(transport.getChannels());

                for (int channel : transport.getChannels()) {
                    interleavedListeners.put(channel, sampleStreamWrapper);
                }

                if (!client.isInterleavedMode()) {
                    deliveryMode = INTERLEAVED;
                    isInFallback = true;
                    sampleStreamWrapper.prepare();
                }

            } else {
                if (client.isRetryTcpFirst()) {
                    deliveryMode = UNICAST;
                    isInFallback = true;
                    sampleStreamWrapper.prepare();
                }
            }
        }
    }

    public synchronized void continuePreparing() {
        if (state == PREPARING) {
            if (preparing.size() > 0) {
                RtspSampleStreamWrapper sampleStreamWrapper = preparing.poll();
                sampleStreamWrapper.prepare();

                prepared.add(sampleStreamWrapper);

            } else {
                if (prepared.size() > 0) {
                    state = PREPARED;

                    while (prepared.size() > 0) {
                        RtspSampleStreamWrapper sampleStreamWrapper = prepared.poll();
                        sampleStreamWrapper.playback();
                    }

                    client.sendPlayRequest(Range.parse("npt=0.000-"), 1);
                }
            }
        }
    }

    public void onSelectTracks(int[] types, boolean[] enabledStates) {
        isVideoTrackEnable = false;
        for (int i=0; i < enabledStates.length; i++) {
            if (enabledStates[i]) {
                isVideoTrackEnable |= types[i] == C.TRACK_TYPE_VIDEO;
            }
        }
    }

    public void onIncomingInterleavedFrame(InterleavedFrame interleavedFrame) {
        for(Map.Entry<Integer, RtspSampleStreamWrapper> entry : interleavedListeners.entrySet()) {
            Integer channel = entry.getKey();
            if (channel == interleavedFrame.getChannel()) {
                RtspSampleStreamWrapper sampleStreamWrapper = entry.getValue();
                sampleStreamWrapper.onInterleavedFrame(interleavedFrame);
                break;
            }
        }
    }

    public void onOutgoingInterleavedFrame(InterleavedFrame interleavedFrame) {
        client.dispatch(interleavedFrame);
    }

    // Player.EventListener
    public void onPlayWhenReadyChanged(
        boolean playWhenReady, @Player.PlayWhenReadyChangeReason int reason) {
        if (reason == PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) {
            if (!playWhenReady && state == PLAYING) {
                client.sendPauseRequest();

            } else if (playWhenReady && state == PAUSED) {
                client.sendPlayRequest(Range.parse("npt=-end"), 1);
            }
        }
    }


    /**
     * Monitor the keep alive message.
     */
    /* package */ final class KeepAliveMonitor {
        private ExecutorService executor = Executors.newSingleThreadExecutor();

        private volatile boolean enabled;
        private final Runnable keepAliveRunnable;

        KeepAliveMonitor() {
            keepAliveRunnable = () -> {
                try {

                    while (enabled) {

                        try {

                            while (!Thread.currentThread().isInterrupted() && enabled) {
                                Thread.sleep(timeout - 3000);
                                client.sendKeepAlive();
                            }

                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }

                } finally {
                    if (!executor.isShutdown()) {
                        executor.shutdown();
                    }
                }
            };
        }

        void start() {
            if (!enabled) {
                enabled = true;
                executor.execute(keepAliveRunnable);
            }
        }

        void cancel() {
            if (enabled) {
                enabled = false;

                if (!executor.isShutdown()) {
                    executor.shutdown();
                }
            }
        }
    }


    public static class Builder {
        Uri uri;
        Client client;
        String username;
        String password;

        public Builder(Client client) {
            if (client == null) throw new NullPointerException("client is null");
            this.client = client;
        }

        public final MediaSession build() {
            buildCredentialsByUri(client.getUri());

            return new MediaSession(this);
        }

        private void buildCredentialsByUri(Uri uri) {
            if (uri == null) throw new NullPointerException("uri is null");

            this.uri = uri;

            if (uri.getUserInfo() != null) {
                String[] values = uri.getUserInfo().split(":");

                this.username = values[0];
                this.password = values[1];

                if (username == null) throw new IllegalStateException("username is null");
                if (password == null) throw new IllegalStateException("password is null");
            }
        }
    }
}
