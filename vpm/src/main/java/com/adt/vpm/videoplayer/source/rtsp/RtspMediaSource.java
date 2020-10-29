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
package com.adt.vpm.videoplayer.source.rtsp;

import android.net.Uri;
import android.os.Handler;

import androidx.annotation.Nullable;

import com.adt.vpm.util.LogMsg;
import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.ExoPlayerLibraryInfo;
import com.adt.vpm.videoplayer.source.common.MediaItem;
import com.adt.vpm.videoplayer.source.common.MediaMetadata;
import com.adt.vpm.videoplayer.source.common.upstream.DataSpec;
import com.adt.vpm.videoplayer.source.common.upstream.TransferListener;
import com.adt.vpm.videoplayer.source.common.util.Assertions;
import com.adt.vpm.videoplayer.source.common.util.Util;
import com.adt.vpm.videoplayer.source.core.Timeline;
import com.adt.vpm.videoplayer.source.core.drm.DrmSessionManager;
import com.adt.vpm.videoplayer.source.core.source.BaseMediaSource;
import com.adt.vpm.videoplayer.source.core.source.LoadEventInfo;
import com.adt.vpm.videoplayer.source.core.source.MediaPeriod;
import com.adt.vpm.videoplayer.source.core.source.MediaSourceEventListener;
import com.adt.vpm.videoplayer.source.core.source.MediaSourceEventListener.EventDispatcher;
import com.adt.vpm.videoplayer.source.core.source.SinglePeriodTimeline;
import com.adt.vpm.videoplayer.source.core.upstream.Allocator;
import com.adt.vpm.videoplayer.source.rtsp.core.Client;
import com.adt.vpm.videoplayer.source.rtsp.media.MediaType;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static com.adt.vpm.videoplayer.source.common.C.TCP;

public final class RtspMediaSource extends BaseMediaSource implements Client.EventListener {

    static {
        ExoPlayerLibraryInfo.registerModule("goog.exo.rtsp");
    }

    /**
     * Factory for {@link RtspMediaSource}.
     */
    public static final class Factory {
        private boolean isLive;
        private boolean isCreateCalled;
        private DrmSessionManager drmSessionManager;

        private final Client.Factory<? extends Client> factory;

        /**
         * Creates a factory for {@link RtspMediaSource}s.
         *
         * @param factory The factory from which read the media will
         *                be obtained.
         */
        public Factory(Client.Factory<? extends Client> factory) {
            this.factory = Assertions.checkNotNull(factory);
            drmSessionManager = DrmSessionManager.getDummyDrmSessionManager();
        }

        public Factory setIsLive(boolean isLive) {
            Assertions.checkState(!isCreateCalled);
            this.isLive = isLive;
            return this;
        }

        /**
         * Sets the {@link DrmSessionManager} to use for acquiring {@link DrmSession DrmSessions}. The
         * default value is {@link DrmSessionManager#DUMMY}.
         *
         * @param drmSessionManager The {@link DrmSessionManager}.
         * @return This factory, for convenience.
         * @throws IllegalStateException If one of the {@code create} methods has already been called.
         */
        public Factory setDrmSessionManager(DrmSessionManager drmSessionManager) {
            Assertions.checkState(!isCreateCalled);
            this.drmSessionManager = drmSessionManager;
            return this;
        }

        /**
         * Returns a new {@link RtspMediaSource} using the current parameters. Media source events
         * will not be delivered.
         *
         * @param uri The {@link Uri}.
         * @return The new {@link RtspMediaSource}.
         */
        public RtspMediaSource createMediaSource(Uri uri) {
            isCreateCalled = true;
            return new RtspMediaSource(uri, factory, isLive, drmSessionManager, getMediaItem(uri));
        }

        /**
         * This method is used to get media item from Uri
         *
         * @param uri Uri of the file
         */
        private MediaItem getMediaItem(Uri uri) {
            String adaptiveMimeType =
                    Util.getAdaptiveMimeTypeForContentType(Util.inferContentType(uri,
                            ""));
            return new MediaItem.Builder()
                    .setUri(uri)
                    .setMediaMetadata(new MediaMetadata.Builder().setTitle(LogMsg.app_name).build())
                    .setMimeType(adaptiveMimeType)
                    .build();
        }

        /**
         * Returns a new {@link RtspMediaSource} using the current parameters. Media source events
         * will not be delivered.
         *
         * @param uri           The {@link Uri}.
         * @param eventHandler  A handler for events.
         * @param eventListener A listener of events.
         * @return The new {@link RtspMediaSource}.
         */
        public RtspMediaSource createMediaSource(Uri uri,
                                                 @Nullable Handler eventHandler,
                                                 @Nullable MediaSourceEventListener eventListener) {
            RtspMediaSource mediaSource = createMediaSource(uri);
            if (eventHandler != null && eventListener != null) {
                mediaSource.addEventListener(eventHandler, eventListener);
            }
            return mediaSource;
        }

    }

    private final Uri uri;
    private final Client.Factory factory;
    private EventDispatcher eventDispatcher;

    private Client client;
    private boolean isLive;

    private @C.TransportProtocol
    int transportProtocol;

    private DrmSessionManager drmSessionManager;
    private final RtspFallbackPolicy fallbackPolicy;
    private @Nullable
    TransferListener transferListener;
    private MediaItem mediaItem;
    private LoadEventInfo loadEventInfo;
    private Timeline timeline = null;

    private RtspMediaSource(Uri uri, Client.Factory factory, boolean isLive,
                            DrmSessionManager drmSessionManager, MediaItem mediaItem) {
        this.uri = uri;
        this.isLive = isLive;
        this.factory = factory;
        this.drmSessionManager = drmSessionManager;
        this.mediaItem = mediaItem;
        this.loadEventInfo = new LoadEventInfo(LoadEventInfo.getNewId(), new DataSpec(uri), 0);
        fallbackPolicy = new RtspFallbackPolicy(this, factory);
    }

    @Override
    public boolean isLive() {
        return isLive;
    }

    /**
     * Returns the initial placeholder timeline that is returned immediately when the real timeline is
     * not yet known, or null to let the player create an initial timeline.
     *
     * <p>The initial timeline must use the same uids for windows and periods that the real timeline
     * will use. It also must provide windows which are marked as dynamic to indicate that the window
     * is expected to change when the real timeline arrives.
     *
     * <p>Any media source which has multiple windows should typically provide such an initial
     * timeline to make sure the player reports the correct number of windows immediately.
     */
    @Nullable
    @Override
    public Timeline getInitialTimeline() {
        return timeline;
    }

    /**
     * Returns true if the media source is guaranteed to never have zero or more than one window.
     *
     * <p>The default implementation returns {@code true}.
     *
     * @return true if the source has exactly one window.
     */
    @Override
    public boolean isSingleWindow() {
        return false;
    }

    /**
     * @deprecated Use {@link #getMediaItem()} and {@link MediaItem.PlaybackProperties#tag} instead.
     */
    @Nullable
    @Override
    public Object getTag() {
        return mediaItem.playbackProperties.tag;
    }

    /**
     * Returns the {@link MediaItem} whose media is provided by the source.
     */
    @NotNull
    @Override
    public MediaItem getMediaItem() {
        return mediaItem;
    }

    @Override
    public boolean isOnTcp() {
        return transportProtocol == TCP;
    }

    // MediaTrackSource implementation
    @Override
    public void maybeThrowSourceInfoRefreshError() throws IOException {
        if (client == null) {
            throw new IOException();
        }
    }

    @Override
    public MediaPeriod createPeriod(MediaPeriodId id, Allocator allocator, long startPositionUs) {
        eventDispatcher = createEventDispatcher(id);
        return new RtspMediaPeriod(client, fallbackPolicy, transferListener, eventDispatcher,
                allocator, drmSessionManager);
    }

    @Override
    public void releasePeriod(MediaPeriod mediaPeriod) {
        ((RtspMediaPeriod) mediaPeriod).release();
    }

    @Override
    protected void prepareSourceInternal(@Nullable TransferListener transferListener) {
        this.transferListener = transferListener;

        drmSessionManager.prepare();
        eventDispatcher = createEventDispatcher(null);

        try {

            client = new Client.Builder(factory)
                    .setUri(uri)
                    .setListener(this)
                    .build();

            client.open();

        } catch (IOException e) {
            eventDispatcher.loadError(loadEventInfo, C.DATA_TYPE_MEDIA_INITIALIZATION, e, false);
        }
    }

    @Override
    public void releaseSourceInternal() {
        drmSessionManager.release();
        if (client != null) {
            client.release();
            client = null;
        }
    }

    // Client.EventListener implementation
    @Override
    public void onMediaDescriptionInfoRefreshed(long durationUs) {
        this.timeline = new SinglePeriodTimeline(durationUs,
                durationUs != C.TIME_UNSET, false, isLive, null, mediaItem);
        refreshSourceInfo(timeline);
    }

    @Override
    public void onMediaDescriptionTypeUnSupported(MediaType mediaType) {
        if (eventDispatcher != null) {
            eventDispatcher.loadError(loadEventInfo, C.DATA_TYPE_MANIFEST,
                    new IOException("Media Description Type [" + mediaType + "] is not supported"),
                    false);
        }
    }

    @Override
    public void onTransportProtocolNegotiated(@C.TransportProtocol int protocol) {
        transportProtocol = protocol;
    }

    @Override
    public void onClientError(Throwable throwable, int type) {
        if (eventDispatcher != null) {
            eventDispatcher.loadError(loadEventInfo, type, (IOException) throwable, false);
        }
    }

    public Client getClient() {
        return client;
    }

}
