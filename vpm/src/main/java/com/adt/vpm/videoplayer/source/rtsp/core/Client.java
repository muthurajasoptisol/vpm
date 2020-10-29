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
package com.adt.vpm.videoplayer.source.rtsp.core;

import android.net.Uri;
import android.os.Handler;

import androidx.annotation.IntDef;

import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.ExoPlayerLibraryInfo;
import com.adt.vpm.videoplayer.source.common.util.Log;
import com.adt.vpm.videoplayer.source.core.ExoPlayer;
import com.adt.vpm.videoplayer.source.core.Player;
import com.adt.vpm.videoplayer.source.core.source.MediaSourceEventListener;
import com.adt.vpm.videoplayer.source.core.upstream.UdpDataSource;
import com.adt.vpm.videoplayer.source.rtp.format.FormatSpecificParameter;
import com.adt.vpm.videoplayer.source.rtp.format.RtpAudioPayload;
import com.adt.vpm.videoplayer.source.rtp.format.RtpPayloadFormat;
import com.adt.vpm.videoplayer.source.rtp.format.RtpVideoPayload;
import com.adt.vpm.videoplayer.source.rtp.upstream.RtpQueue;
import com.adt.vpm.videoplayer.source.rtsp.RtspSampleStreamWrapper;
import com.adt.vpm.videoplayer.source.rtsp.auth.AuthScheme;
import com.adt.vpm.videoplayer.source.rtsp.auth.BasicCredentials;
import com.adt.vpm.videoplayer.source.rtsp.auth.Credentials;
import com.adt.vpm.videoplayer.source.rtsp.auth.DigestCredentials;
import com.adt.vpm.videoplayer.source.rtsp.media.MediaFormat;
import com.adt.vpm.videoplayer.source.rtsp.media.MediaSession;
import com.adt.vpm.videoplayer.source.rtsp.media.MediaTrack;
import com.adt.vpm.videoplayer.source.rtsp.media.MediaType;
import com.adt.vpm.videoplayer.source.rtsp.message.Header;
import com.adt.vpm.videoplayer.source.rtsp.message.Headers;
import com.adt.vpm.videoplayer.source.rtsp.message.InterleavedFrame;
import com.adt.vpm.videoplayer.source.rtsp.message.MessageBody;
import com.adt.vpm.videoplayer.source.rtsp.message.Method;
import com.adt.vpm.videoplayer.source.rtsp.message.Range;
import com.adt.vpm.videoplayer.source.rtsp.message.Request;
import com.adt.vpm.videoplayer.source.rtsp.message.Response;
import com.adt.vpm.videoplayer.source.rtsp.message.Status;
import com.adt.vpm.videoplayer.source.rtsp.message.Transport;
import com.adt.vpm.videoplayer.source.sdp.MediaDescription;
import com.adt.vpm.videoplayer.source.sdp.SessionDescription;
import com.adt.vpm.videoplayer.source.sdp.core.Attribute;
import com.adt.vpm.videoplayer.source.sdp.core.Bandwidth;
import com.adt.vpm.videoplayer.source.sdp.core.Media;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Client implements Dispatcher.EventListener {

    private static final String USER_AGENT = ExoPlayerLibraryInfo.VERSION_SLASHY +
        " (Media Player for Android)";

    private static final String TAG = "RTSP Client";
    public static final int TYPE_RTSP_SOCKET_TIMEOUT = 6;
    public static final int TYPE_RTSP_NO_RESPONSE = 7;
    public static final int TYPE_RTSP_UNSUCCESS = 8;
    public static final int TYPE_RTSP_UNAUTHORIZE = 9;
    public static final int TYPE_RTSP_MALFUNCTION = 10;
    public static final int TYPE_RTSP_IO_ERROR = 11;
    public static final int TYPE_RTSP_REQUEST_TIMEOUT = 12;

    public static abstract class Factory<T extends Client> {
        long delayMs;
        int bufferSize;
        @Mode int mode;
        String userAgent;
        @Flags int flags;
        @AVOptions int avOptions;
        @NatMethod int natMethod;

        final ExoPlayer player;

        public Factory(ExoPlayer player) {
            this.player = player;
            userAgent = USER_AGENT;
            mode = RTSP_AUTO_DETECT;
            natMethod = RTSP_NAT_NONE;
            delayMs = RtpQueue.DELAY_REORDER_MS;
            bufferSize = UdpDataSource.DEFAULT_RECEIVE_BUFFER_SIZE;
        }

        public Factory<T> setFlags(@Flags int flags) {
            this.flags = flags;
            return this;
        }

        public Factory<T> setMode(@Mode int mode) {
            this.mode = mode;
            return this;
        }

        public Factory<T> setAvOptions(@AVOptions int avOptions) {
            this.avOptions = avOptions;
            return this;
        }

        public Factory<T> setUserAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Factory<T> setBufferSize(int bufferSize) {
            if (bufferSize < MIN_RECEIVE_BUFFER_SIZE || bufferSize > MAX_RECEIVE_BUFFER_SIZE) {
                throw new IllegalArgumentException("Invalid receive buffer size");
            }

            this.bufferSize = bufferSize;
            return this;
        }

        public Factory<T> setMaxDelay(long delayMs) {
            if (delayMs < 0) {
                throw new IllegalArgumentException("Invalid delay");
            }

            this.delayMs = delayMs;
            return this;
        }

        public Factory<T> setNatMethod(@NatMethod int natMethod) {
            this.natMethod = natMethod;
            return this;
        }

        public abstract T create(Builder builder);
    }

    public interface EventListener {
        /**
         * Called when the Rtsp media session is established and prepared.
         *
         */
        void onMediaDescriptionInfoRefreshed(long durationUs);

        /**
         * Called when the rtsp media description type is not supported.
         *
         */
        void onMediaDescriptionTypeUnSupported(MediaType mediaType);

        /**
         * Called when the transport protocol is negotiated.
         *
         */
        void onTransportProtocolNegotiated(@C.TransportProtocol int protocol);

        /**
         * Called when an error occurs on rtsp client.
         *
         */
        void onClientError(Throwable throwable, int type);
    }

    private static final Pattern regexRtpMap = Pattern.compile(
            "\\d+\\s+([a-zA-Z0-9-]*)/(\\d+){1}(/(\\d+))?",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern regexFrameSize = Pattern.compile("(\\d+)\\s+(\\d+)-(\\d+)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern regexXDimensions = Pattern.compile("(\\d+),\\s+(\\d+)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern regexFmtp = Pattern.compile("\\d+\\s+(.+)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern regexNumber = Pattern.compile("([\\d\\.]+)\\b");

    private static final Pattern regexAuth = Pattern.compile("(\\S+)\\s+(.+)",
            Pattern.CASE_INSENSITIVE);

    private static final int DEFAULT_PORT = 554;
    protected static final int MIN_RECEIVE_BUFFER_SIZE = UdpDataSource.DEFAULT_RECEIVE_BUFFER_SIZE / 2;
    protected static final int MAX_RECEIVE_BUFFER_SIZE = 500 * 1024;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = true, value = {AV_OPT_FLAG_DISABLE_AUDIO, AV_OPT_FLAG_DISABLE_VIDEO})
    public @interface AVOptions {}
    public static final int AV_OPT_FLAG_DISABLE_AUDIO = 1;
    public static final int AV_OPT_FLAG_DISABLE_VIDEO = 1 << 1;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = true, value = {FLAG_ENABLE_RTCP_SUPPORT, FLAG_FORCE_RTCP_MUXED, FLAG_TRY_TCP_FIRST})
    public @interface Flags {}
    public static final int FLAG_ENABLE_RTCP_SUPPORT = 1;
    public static final int FLAG_FORCE_RTCP_MUXED = 1 << 1;
    public static final int FLAG_TRY_TCP_FIRST = 1 << 2;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {RTSP_AUTO_DETECT, RTSP_INTERLEAVED})
    public @interface Mode {}
    protected static final int RTSP_AUTO_DETECT = 0;
    public static final int RTSP_INTERLEAVED = 1;
    //public static final int RTSP_TUNNELING = 2;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {RTSP_NAT_NONE, RTSP_NAT_DUMMY})
    public @interface NatMethod {}
    public static final int RTSP_NAT_NONE = 0;
    public static final int RTSP_NAT_DUMMY = 1;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = true, value = {IDLE, INIT, READY, PLAYING, RECORDING})
    @interface ClientState {}
    final static int IDLE = 0;
    final static int INIT = 1;
    final static int READY = 2;
    final static int PLAYING = 3;
    final static int RECORDING = 4;

    private String userAgent;
    private final MediaSession session;
    private final Dispatcher dispatcher;
    private final List<Method> serverMethods;

    private final Uri uri;
    private @Mode int mode;
    private @Flags int flags;
    private final long delayMs;
    private final Player player;
    private final int bufferSize;
    private final EventListener listener;
    private final @NatMethod int natMethod;
    private final @AVOptions int avOptions;

    private @ClientState int state;
    private Credentials credentials;

    private boolean opened;
    private boolean released;

    public Client(Builder builder) {
        uri = builder.uri;
        listener = builder.listener;

        mode = builder.factory.mode;
        flags = builder.factory.flags;
        player = builder.factory.player;
        delayMs = builder.factory.delayMs;
        avOptions = builder.factory.avOptions;
        natMethod = builder.factory.natMethod;
        userAgent = builder.factory.userAgent;
        bufferSize = builder.factory.bufferSize;

        serverMethods = new ArrayList<>();

        dispatcher = new Dispatcher.Builder(this)
                .setUri(uri)
                .setUserAgent(userAgent)
                .build();

        session = new MediaSession.Builder(this)
                .build();
    }

    public final Uri getUri() { return uri; }

    public final long getMaxDelay() { return delayMs; }

    public final Player getPlayer() { return player; }

    public final int getBufferSize() { return bufferSize; }

    public final String getUserAgent() { return userAgent; }

    public final MediaSession getSession() { return session; }

    protected final @ClientState int getState() { return state; }

    public final boolean isFlagSet(@Flags int flag) {
        return (flags & flag) == flag;
    }

    private final boolean isAVOptionSet(@AVOptions int option) {
        return (avOptions & option) == option;
    }

    public final boolean isInterleavedMode() {
        return RTSP_INTERLEAVED == mode;
    }

    public final boolean isRetryTcpFirst() {
        return isFlagSet(FLAG_TRY_TCP_FIRST);
    }

    public final boolean isNatSet(@NatMethod int method) {
        return (natMethod & method) != 0;
    }

    public final void open() throws IOException, NullPointerException {
        if (!opened) {
            dispatcher.connect();
            sendOptionsRequest();

            opened = true;
        }
    }

    public final void close() {
        if (opened || !released) {
            opened = false;
            released = true;

            session.release();
            dispatcher.close();
            serverMethods.clear();

            state = IDLE;
        }
    }

    public final void release() {
        close();
    }

    public final void dispatch(InterleavedFrame interleavedFrame) {
        dispatcher.execute(interleavedFrame);
    }

    public final void dispatch(Request request) {
        if (credentials != null) {
            if (!request.getHeaders().contains(Header.Authorization)) {
                credentials.applyToRequest(request);
            }
        }

        switch (state) {
            case IDLE:
                if (Method.DESCRIBE.equals(request.getMethod())||
                    Method.OPTIONS.equals(request.getMethod())) {
                    dispatcher.execute(request);

                } else if (Method.SETUP.equals(request.getMethod())) {
                    state = INIT;
                    dispatcher.execute(request);
                }
                break;

            case INIT:
                if (Method.SETUP.equals(request.getMethod()) ||
                    Method.TEARDOWN.equals(request.getMethod())) {
                    dispatcher.execute(request);
                }
                break;

            case READY:
                if (Method.PAUSE.equals(request.getMethod()) ||
                    Method.PLAY.equals(request.getMethod()) ||
                    Method.GET_PARAMETER.equals(request.getMethod()) ||
                    Method.RECORD.equals(request.getMethod()) ||
                    Method.SETUP.equals(request.getMethod()) ||
                    Method.TEARDOWN.equals(request.getMethod())) {
                    dispatcher.execute(request);
                }
                break;

            case PLAYING:
            case RECORDING:
                if (Method.ANNOUNCE.equals(request.getMethod()) ||
                    Method.GET_PARAMETER.equals(request.getMethod()) ||
                    Method.OPTIONS.equals(request.getMethod()) ||
                    Method.PLAY.equals(request.getMethod()) ||
                    Method.PAUSE.equals(request.getMethod()) ||
                    Method.SETUP.equals(request.getMethod()) ||
                    Method.TEARDOWN.equals(request.getMethod())) {
                    dispatcher.execute(request);
                }
        }
    }

    // Dispatcher.EventListener implementation
    @Override
    public final void onAnnounceRequest(Request request) {
        Response.Builder builder = new Response.Builder().setStatus(Status.OK);
        builder.setHeader(Header.CSeq, request.getHeaders().getValue(Header.CSeq));
        builder.setHeader(Header.UserAgent, userAgent);

        dispatcher.execute(builder.build());
    }

    @Override
    public final void onRedirectRequest(Request request) {
        Response.Builder builder = new Response.Builder().setStatus(Status.MethodNotAllowed);
        builder.setHeader(Header.CSeq, request.getHeaders().getValue(Header.CSeq));
        builder.setHeader(Header.UserAgent, userAgent);

        dispatcher.execute(builder.build());
    }

    @Override
    public final void onOptionsRequest(Request request) {
        Response.Builder builder = new Response.Builder().setStatus(Status.MethodNotAllowed);
        builder.setHeader(Header.CSeq, request.getHeaders().getValue(Header.CSeq));
        builder.setHeader(Header.UserAgent, userAgent);

        dispatcher.execute(builder.build());
    }

    @Override
    public final void onGetParameterRequest(Request request) {
        Response.Builder builder = new Response.Builder().setStatus(Status.MethodNotAllowed);
        builder.setHeader(Header.CSeq, request.getHeaders().getValue(Header.CSeq));
        builder.setHeader(Header.UserAgent, userAgent);

        dispatcher.execute(builder.build());
    }

    @Override
    public final void onSetParameterRequest(Request request) {
        Response.Builder builder = new Response.Builder().setStatus(Status.MethodNotAllowed);
        builder.setHeader(Header.CSeq, request.getHeaders().getValue(Header.CSeq));
        builder.setHeader(Header.UserAgent, userAgent);

        dispatcher.execute(builder.build());
    }

    @Override
    public final void onAnnounceResponse(Response response) {
        // Not Applicable
    }

    @Override
    public final void onOptionsResponse(Response response) {
        if (serverMethods.size() == 0) {
            if (response.getHeaders().contains(Header.Public)) {
                String publicHeader = response.getHeaders().getValue(Header.Public);
                String[] names = publicHeader.split((publicHeader.indexOf(',') != -1) ? "," : " ");

                for (String name : names) {
                    serverMethods.add(Method.parse(name.trim()));
                }
            }

            if (state == IDLE) {
                sendDescribeRequest();
            }
        }

        if (response.getHeaders().contains(Header.Server)) {
            session.setServer(response.getHeaders().getValue(Header.Server));
        }
    }

    @Override
    public final void onDescribeResponse(Response response) {
        MessageBody body = response.getMessageBody();
        Headers headers = response.getHeaders();
        String baseUrl = session.getUri().toString();

        ////Log.d(TAG, "DESCRIBE RESPONSE: \n" + body.toString());

        if (headers.contains(Header.ContentBase)) {
            baseUrl = headers.getValue(Header.ContentBase);
        } else if (headers.contains(Header.ContentLocation)) {
            baseUrl = headers.getValue(Header.ContentLocation);
        }

        session.setBaseUri(Uri.parse(baseUrl));

        if (body != null) {
            MediaType mediaType = body.getContentType();

            if (MediaType.APPLICATION_SDP.equals(mediaType)) {
                String content = body.getContent();

                if ((content != null) && (content.length() > 0)) {
                    SessionDescription sessionDescription = SessionDescription.parse(content);

                    if (sessionDescription.getSessionName() != null) {
                        session.setName(sessionDescription.getSessionName().getName());
                    }

                    if (sessionDescription.getInformation() != null) {
                        session.setDescription(sessionDescription.getInformation().getInformation());
                    }

                    for (Attribute attribute : sessionDescription.getAttributes()) {
                        String attrName = attribute.getName();
                        if (Attribute.RANGE.equalsIgnoreCase(attrName)) {
                            session.setDuration(Range.parse(attribute.getValue()).getDuration());

                        } else if (Attribute.LENGTH.equalsIgnoreCase(attrName)) {
                            session.setDuration((long)Double.parseDouble(attribute.getValue()));

                        } else if (Attribute.SDPLANG.equalsIgnoreCase(attrName)) {
                            session.setLanguage(attribute.getValue());
                        }
                    }

                    // Only support permanent sessions
                    if (sessionDescription.getTime() == null || sessionDescription.getTime().isZero()) {

                        for (MediaDescription mediaDescription :
                                sessionDescription.getMediaDescriptions()) {

                            Media media = mediaDescription.getMedia();

                            // We only support audio o video
                            if ((Media.audio.equals(media.getType()) && !isAVOptionSet(
                                AV_OPT_FLAG_DISABLE_AUDIO)) ||
                                (Media.video.equals(media.getType())) & !isAVOptionSet(
                                    AV_OPT_FLAG_DISABLE_VIDEO)) {

                                RtpPayloadFormat.Builder payloadBuilder = null;
                                MediaTrack.Builder trackBuilder = new MediaTrack.Builder();

                                @MediaFormat.MediaType int type = media.getType().equals(Media.audio) ?
                                        MediaFormat.AUDIO : MediaFormat.VIDEO;

                                MediaFormat.Builder formatBuilder = new MediaFormat.Builder(type);

                                Transport transport = Transport.parse(media.getProto(), media.getFmt());
                                if (Transport.AVP_PROFILE.equals(transport.getProfile())) {
                                    payloadBuilder = MediaFormat.AUDIO == type ?
                                            new RtpAudioPayload.Builder() :
                                            new RtpVideoPayload.Builder();

                                    if (isNumeric(media.getFmt())) {
                                        payloadBuilder.setPayload(Integer.parseInt(media.getFmt()));
                                    }
                                }

                                formatBuilder.transport(transport);

                                Bandwidth bandwidth = mediaDescription.getBandwidth();
                                if (bandwidth != null && Bandwidth.AS.equals(bandwidth.getType())) {
                                    formatBuilder.bitrate(bandwidth.getWidth());
                                    payloadBuilder.setBitrate(bandwidth.getWidth());
                                }

                                for (Attribute attribute : mediaDescription.getAttributes()) {
                                    String attrName = attribute.getName();
                                    String attrValue = attribute.getValue();

                                    if (Attribute.RANGE.equalsIgnoreCase(attrName)) {
                                        session.setDuration(Range.parse(attrValue).getDuration());

                                    } else if (Attribute.CONTROL.equalsIgnoreCase(attrName)) {
                                        if (baseUrl != null && attrValue.startsWith(baseUrl)) {
                                            trackBuilder.url(attrValue);
                                        } else {
                                            if (attrValue.toLowerCase().startsWith("rtsp://")) {
                                                trackBuilder.url(attrValue);

                                            } else {
                                                Uri uri = session.getUri();
                                                String url = uri.getScheme() + "://" + uri.getHost()
                                                        + ((uri.getPort() > 0) ? ":" + uri.getPort()
                                                        : ":" + DEFAULT_PORT) + uri.getPath();

                                                if (baseUrl != null) {
                                                    Uri uriBaseUrl = Uri.parse(baseUrl);
                                                    String scheme = uriBaseUrl.getScheme();
                                                    if (scheme != null &&
                                                            "rtsp".equalsIgnoreCase(scheme)) {
                                                        url = baseUrl;
                                                    }
                                                }

                                                if (url.lastIndexOf('/') == url.length() - 1) {
                                                    trackBuilder.url(url + attrValue);
                                                } else {
                                                    trackBuilder.url(url + "/" + attrValue);
                                                }
                                            }
                                        }

                                    } else if (Attribute.RTCP_MUX.equalsIgnoreCase(attrName)) {
                                        trackBuilder.muxed(true);
                                        flags |= FLAG_FORCE_RTCP_MUXED;

                                    } else if (Attribute.SDPLANG.equalsIgnoreCase(attrName)) {
                                        trackBuilder.language(attrValue);

                                    } else if (payloadBuilder != null) {
                                        if (Attribute.RTPMAP.equalsIgnoreCase(attrName)) {
                                            Matcher matcher = regexRtpMap.matcher(attrValue);
                                            if (matcher.find()) {
                                                @RtpPayloadFormat.MediaCodec String encoding =
                                                        matcher.group(1).toUpperCase();

                                                payloadBuilder.setEncoding(encoding);

                                                if (matcher.group(2) != null) {
                                                    if (isNumeric(matcher.group(2))) {
                                                        payloadBuilder.setClockrate(
                                                                Integer.parseInt(matcher.group(2)));
                                                    }
                                                }

                                                if (matcher.group(3) != null) {
                                                    if (isNumeric(matcher.group(4))) {
                                                        ((RtpAudioPayload.Builder) payloadBuilder).
                                                            setChannels(Integer.parseInt(matcher.group(4)));
                                                    }
                                                }
                                            }
                                        /* NOTE: fmtp is only supported AFTER the 'a=rtpmap:xxx' tag */
                                        } else if (Attribute.FMTP.equalsIgnoreCase(attrName)) {
                                            Matcher matcher = regexFmtp.matcher(attrValue);
                                            if (matcher.find()) {
                                                String[] encodingParameters = matcher.group(1).
                                                        split(";");
                                                for (String parameter : encodingParameters) {
                                                    payloadBuilder.addEncodingParameter(
                                                            FormatSpecificParameter.parse(parameter));
                                                }
                                            }
                                        } else if (Attribute.FRAMERATE.equalsIgnoreCase(attrName)) {
                                            if (isNumeric(attrValue)) {
                                                ((RtpVideoPayload.Builder) payloadBuilder).getFramerate(
                                                        Float.parseFloat(attrValue));
                                            }

                                        } else if (Attribute.FRAMESIZE.equalsIgnoreCase(attrName)) {
                                            Matcher matcher = regexFrameSize.matcher(attrValue);
                                            if (matcher.find()) {
                                                if (isNumeric(matcher.group(2)) &&
                                                        isNumeric(matcher.group(3))) {
                                                    ((RtpVideoPayload.Builder) payloadBuilder).getWidth(
                                                            Integer.parseInt(matcher.group(2)));

                                                    ((RtpVideoPayload.Builder) payloadBuilder).getHeight(
                                                            Integer.parseInt(matcher.group(3)));
                                                }
                                            }
                                        } else if (Attribute.X_FRAMERATE.equalsIgnoreCase(attrName)) {
                                            if (isNumeric(attrValue)) {
                                                ((RtpVideoPayload.Builder) payloadBuilder).getFramerate(
                                                        Float.parseFloat(attrValue));
                                            }

                                        } else if (Attribute.X_DIMENSIONS.equalsIgnoreCase(attrName)) {
                                            Matcher matcher = regexXDimensions.matcher(attrValue);
                                            if (matcher.find()) {
                                                if (isNumeric(matcher.group(2)) &&
                                                        isNumeric(matcher.group(3))) {
                                                    ((RtpVideoPayload.Builder) payloadBuilder).getWidth(
                                                            Integer.parseInt(matcher.group(2)));

                                                    ((RtpVideoPayload.Builder) payloadBuilder).getHeight(
                                                            Integer.parseInt(matcher.group(3)));
                                                }
                                            }

                                        } else if (Attribute.PTIME.equalsIgnoreCase(attrName)) {
                                            if (isNumeric(attrValue)) {
                                                ((RtpAudioPayload.Builder) payloadBuilder).
                                                    setPtime(Long.parseLong(attrValue));
                                            }

                                        } else if (Attribute.MAXPTIME.equalsIgnoreCase(attrName)) {
                                            if (isNumeric(attrValue)) {
                                                ((RtpAudioPayload.Builder) payloadBuilder).
                                                    setMaxPtime(Long.parseLong(attrValue));
                                            }

                                        } else if (Attribute.QUALITY.equalsIgnoreCase(attrName)) {
                                            if (isNumeric(attrValue)) {
                                                ((RtpVideoPayload.Builder) payloadBuilder).
                                                    getQuality(Integer.parseInt(attrValue));
                                            }
                                        }
                                    }
                                }

                                if (payloadBuilder != null) {
                                    formatBuilder.format(payloadBuilder.build());
                                }

                                try {

                                    MediaFormat format = formatBuilder.build();
                                    MediaTrack track = trackBuilder.format(format).build();
                                    session.addMediaTrack(track);

                                } catch (IllegalStateException ex) {
                                    // Do nothing
                                }
                            }
                        }

                        listener.onMediaDescriptionInfoRefreshed(session.getDuration());
                    }
                }
            } else {

                Response.Builder builder = new Response.Builder().setStatus(Status.UnsupportedMediaType);
                builder.setHeader(Header.CSeq, Integer.toString(session.getNextCSeq()));
                builder.setHeader(Header.UserAgent, userAgent);
                builder.setHeader(Header.Unsupported, mediaType.toString());

                dispatcher.execute(builder.build());

                close();
                listener.onMediaDescriptionTypeUnSupported(mediaType);
            }
        }
    }

    @Override
    public final void onSetupResponse(Response response) {
        //Log.d(TAG, "SETUP RESPONSE: \n" + response.getMessageBody().toString());

        if (session.getId() == null) {
            Pattern rexegSession = Pattern.compile("(\\S+);timeout=(\\S+)|(\\S+)",
                    Pattern.CASE_INSENSITIVE);
            Matcher matcher = rexegSession.matcher(response.getHeaders().getValue(Header.Session));

            if (matcher.find()) {
                if (matcher.group(1) != null) {
                    session.setId(matcher.group(1));
                    // timeout in milliseconds
                    session.setTimeout(Integer.parseInt(matcher.group(2))*1000);

                } else {
                    session.setId(matcher.group(3));
                }
            }

            if (state == INIT) {
                state = READY;
            }
        }

        Transport transport = Transport.parse(response.getHeaders().getValue(Header.Transport));
        session.configureTransport(transport);
        session.continuePreparing();
    }

    @Override
    public final void onPlayResponse(Response response) {

        //Log.d(TAG, "PLAY RESPONSE: \n" + response);



        state = PLAYING;
        session.onPlaySuccess();

        if (session.isInterleaved()) {
            listener.onTransportProtocolNegotiated(C.TCP);
        } else {
            listener.onTransportProtocolNegotiated(C.UDP);
        }
    }

    @Override
    public final void onPauseResponse(Response response) {
        //Log.d(TAG, "PAUSE RESPONSE: \n" + response);

        state = READY;
        session.onPauseSuccess();
    }

    @Override
    public final void onGetParameterResponse(Response response) {
    }

    @Override
    public final void onRecordResponse(Response response) {
        state = RECORDING;
        // Not Supported
    }

    @Override
    public final void onSetParameterResponse(Response response) {
        // Not Supported
    }

    @Override
    public final void onTeardownResponse(Response response) {
        //Log.d(TAG, "TEARDOWN RESPONSE: \n" + response);

        state = IDLE;
    }

    @Override
    public final void onEmbeddedBinaryData(InterleavedFrame frame) {
        session.onIncomingInterleavedFrame(frame);
    }

    @Override
    public final void onUnauthorized(Request request, Response response) {
        List<String> w3AuthenticateList = response.getHeaders().getValues(Header.W3Authenticate);

        for (String w3Authenticate : w3AuthenticateList) {
            Matcher matcher = regexAuth.matcher(w3Authenticate);

            if (matcher.find()) {
                try {

                    switch (AuthScheme.parse(matcher.group(1))) {
                        case BASIC:
                            if (session.getUsername() != null) {
                                credentials = new BasicCredentials.Builder(matcher.group(2)).
                                        setUsername(session.getUsername()).
                                        setPassword(session.getPassword()).
                                        build();
                                credentials.applyToRequest(request);

                                request.getHeaders().add(Header.CSeq.toString(),
                                        String.valueOf(session.getNextCSeq()));

                                dispatcher.execute(request);
                            }

                            return;

                        case DIGEST:
                            if (session.getUsername() != null) {
                                credentials = new DigestCredentials.Builder(matcher.group(2)).
                                        setUsername(session.getUsername()).
                                        setPassword(session.getPassword()).
                                        setParam(DigestCredentials.URI, session.getUri().toString()).
                                        build();
                                credentials.applyToRequest(request);

                                request.getHeaders().add(Header.CSeq.toString(),
                                        String.valueOf(session.getNextCSeq()));

                                dispatcher.execute(request);
                            }
                            return;
                    }

                } catch (IOException ex) {
                    close();
                    Log.e(TAG, "onUnauthorized");
                    listener.onClientError(ex, TYPE_RTSP_UNAUTHORIZE);
                }
            }
        }
    }

    @Override
    public final void onUnSuccess(Request request, Response response) {
        // when options method isn't supported from server a describe method is sent
        if ((Method.OPTIONS.equals(request.getMethod())) &&
                (Status.NotImplemented.equals(response.getStatus()))) {

            if (state == IDLE) {
                sendDescribeRequest();
            }

        } else if (Method.SETUP.equals(request.getMethod()) &&
            Status.UnsupportedTransport.equals(response.getStatus())) {
            if (isRetryTcpFirst() && !session.isInterleaved()) {
                for (MediaTrack track : session.getMediaTracks()) {
                    if (request.getUrl().equals(track.url())) {
                        for (RtspSampleStreamWrapper sampleStream : session.getSampleStreamsPrepared()) {
                            if (request.getUrl().equals(sampleStream.getMediaTrack().url())) {
                                sendSetupRequest(track, sampleStream.getLocalPort());
                                return;
                            }
                        }
                    }
                }

                close();
                listener.onClientError(new IOException(), TYPE_RTSP_UNSUCCESS);
                Log.e(TAG, "onUnSuccess Method Setup");
            } else {
                sendSetupRequest(request.getUrl(), Transport.parse("RTP/AVP/TCP;interleaved=" +
                    session.getNextTcpChannel()));
            }

        } else {
            // any other unsuccessful response
            if (state >= READY) {
                if (serverMethods.contains(Method.TEARDOWN)) {
                    sendTeardownRequest();
                }
            }

            close();
            Log.e(TAG, "onUnSuccess");
            listener.onClientError(new IOException(), TYPE_RTSP_UNSUCCESS);
        }
    }

    @Override
    public final void onMalformedResponse(Response response) {
        //Log.d(TAG, "onMalformedResponse \n");

        close();
        Log.e(TAG, "onMalformedResponse");
        listener.onClientError(new IOException(), TYPE_RTSP_MALFUNCTION);
    }

    @Override
    public final void onIOError() {
        //Log.d(TAG, "onIOError \n");

        close();
        Log.e(TAG, "onIOError");
        listener.onClientError(new IOException(), TYPE_RTSP_IO_ERROR);
    }

    @Override
    public final void onRequestTimeOut() {
        Log.d(TAG, "onRequestTimeOut \n");

        close();
        listener.onClientError(new IOException(), TYPE_RTSP_REQUEST_TIMEOUT);
    }

    @Override
    public final void onNoResponse(Request request) {
        Method method = request.getMethod();
        if (Method.OPTIONS.equals(method) || Method.GET_PARAMETER.equals(method)) {
            if (session.isInterleaved()) {
                return;
            }
        }

        close();
        Log.e(TAG, "onNoResponse");
        listener.onClientError(new IOException(), TYPE_RTSP_NO_RESPONSE);
    }

    protected abstract void sendOptionsRequest();
    protected abstract void sendDescribeRequest();
    public abstract void sendSetupRequest(MediaTrack track, int localPort);
    public abstract void sendSetupRequest(String trackId, Transport transport);
    public abstract void sendPlayRequest(Range range);
    public abstract void sendPlayRequest(Range range, float scale);
    public abstract void sendPauseRequest();
    protected abstract void sendRecordRequest();
    protected abstract void sendGetParameterRequest();
    protected abstract void sendSetParameterRequest(String name, String value);
    public abstract void sendTeardownRequest();

    public void sendKeepAlive() {
        if (state >= READY) {
            if (serverMethods.contains(Method.GET_PARAMETER)) {
                sendGetParameterRequest();

            } else {
                sendOptionsRequest();
            }
        }
    }

    private boolean isNumeric(String number) {
        Matcher matcher = regexNumber.matcher(number);

        if (matcher.find()) {
            return true;
        }

        return false;
    }

    protected final String getPlayUrl() {
        // determine the URL to use for PLAY requests
        Uri baseUri = session.getBaseUri();
        if (baseUri != null) {
            return baseUri.toString();
        }

        // remove the user info from the URL if it is present
        Uri uri = session.getUri();
        String url = uri.toString();
        String uriUserInfo = uri.getUserInfo();
        if (uriUserInfo != null && !uriUserInfo.isEmpty()) {
            uriUserInfo += "@";
            url = url.replace(uriUserInfo, "");
        }

        return url;
    }

    public static final class Builder {
        private Uri uri;
        private Handler eventHandler;
        private EventListener listener;
        private MediaSourceEventListener eventListener;

        private final Factory factory;

        public Builder(Factory factory) {
            this.factory = factory;
        }

        public Builder setListener(EventListener listener) {
            if (listener == null) throw new IllegalArgumentException("listener == null");

            this.listener = listener;
            return this;
        }

        /**
         * Sets the listener to respond to adaptive {@link MediaSource} events and the handler to
         * deliver these events.
         *
         * @param eventHandler A handler for events.
         * @param eventListener A listener of events.
         * @return This builder.
         */
        public Builder setEventListener(Handler eventHandler, MediaSourceEventListener eventListener) {
            this.eventHandler = eventHandler;
            this.eventListener = eventListener;
            return this;
        }

        public Builder setUri(Uri uri) {
            if (uri == null) throw new NullPointerException("uri == null");

            if (uri.getPort() == C.PORT_UNSET) {
                this.uri = Uri.parse(uri.getScheme() + "://" + ((uri.getUserInfo() != null) ?
                        uri.getUserInfo() + "@" : "") + uri.getHost() +
                        ((uri.getPort() > 0) ? ":" + uri.getPort() : ":" + DEFAULT_PORT) +
                        uri.getPath() + ((uri.getQuery() != null) ? "?" + uri.getQuery() : ""));
            } else {
                this.uri = uri;
            }

            return this;
        }

        public Client build() {
            if (factory == null) throw new IllegalStateException("factory is null");
            if (listener == null) throw new IllegalStateException("listener is null");
            if (uri == null) throw new IllegalStateException("uri is null");

            return factory.create(this);
        }
    }
}
