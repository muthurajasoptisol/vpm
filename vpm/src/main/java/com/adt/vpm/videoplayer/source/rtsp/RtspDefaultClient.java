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

import com.adt.vpm.videoplayer.source.core.ExoPlayer;
import com.adt.vpm.videoplayer.source.rtsp.core.Client;
import com.adt.vpm.videoplayer.source.rtsp.message.Header;
import com.adt.vpm.videoplayer.source.rtsp.media.MediaType;
import com.adt.vpm.videoplayer.source.rtsp.message.Range;
import com.adt.vpm.videoplayer.source.rtsp.message.Request;
import com.adt.vpm.videoplayer.source.rtsp.message.Transport;
import com.adt.vpm.videoplayer.source.rtsp.media.MediaTrack;

public final class RtspDefaultClient extends Client {

    public static class Factory extends Client.Factory<RtspDefaultClient> {

        Factory(ExoPlayer player) {
            super(player);
        }

        public RtspDefaultClient create(Builder builder) {
            return new RtspDefaultClient(builder);
        }
    }

    public static Factory factory(ExoPlayer player) {
        return new Factory(player);
    }


    RtspDefaultClient(Builder builder) {
        super(builder);
    }

    @Override
    protected void sendOptionsRequest() {
        Request.Builder builder = new Request.Builder().options().setUrl(getSession().getUri().toString());
        builder.setHeader(Header.CSeq, getSession().getNextCSeq());
        builder.setHeader(Header.UserAgent, getUserAgent());

        if (getSession().getId() != null) {
            builder.setHeader(Header.Session, getSession().getId());
        }

        dispatch(builder.build());
    }

    @Override
    protected void sendDescribeRequest() {
        Request.Builder builder = new Request.Builder().describe().setUrl(getSession().getUri().toString());
        builder.setHeader(Header.CSeq, getSession().getNextCSeq());
        builder.setHeader(Header.UserAgent, getUserAgent());
        builder.setHeader(Header.Accept, MediaType.APPLICATION_SDP);

        dispatch(builder.build());
    }

    @Override
    public void sendSetupRequest(MediaTrack track, int localPort) {
        Request.Builder builder = new Request.Builder().setup().setUrl(track.url());
        builder.setHeader(Header.CSeq, getSession().getNextCSeq());
        builder.setHeader(Header.UserAgent, getUserAgent());

        if (getSession().getId() != null) {
            builder.setHeader(Header.Session, getSession().getId());
        }

        Transport transport = track.format().transport();

        if (isFlagSet(FLAG_ENABLE_RTCP_SUPPORT)) {
            if (isFlagSet(FLAG_FORCE_RTCP_MUXED)) {
                builder.setHeader(Header.Transport, transport + ";client_port=" + localPort +
                        "-" + localPort);
            } else {
                builder.setHeader(Header.Transport, transport + ";client_port=" + localPort +
                        "-" + (localPort + 1));
            }
        } else {
            builder.setHeader(Header.Transport, transport + ";client_port=" + localPort);
        }

        dispatch(builder.build());
    }

    @Override
    public void sendSetupRequest(String trackId, Transport transport) {
        Request.Builder builder = new Request.Builder().setup().setUrl(trackId);
        builder.setHeader(Header.CSeq, getSession().getNextCSeq());
        builder.setHeader(Header.UserAgent, getUserAgent());

        if (getSession().getId() != null) {
            builder.setHeader(Header.Session, getSession().getId());
        }

        builder.setHeader(Header.Transport, transport);

        dispatch(builder.build());
    }

    @Override
    public void sendPlayRequest(Range range) {
        Request.Builder builder = new Request.Builder().play().setUrl(getPlayUrl());
        builder.setHeader(Header.CSeq, getSession().getNextCSeq());
        builder.setHeader(Header.UserAgent, getUserAgent());
        builder.setHeader(Header.Session, getSession().getId());
        builder.setHeader(Header.Range, range);

        dispatch(builder.build());
    }

    @Override
    public void sendPlayRequest(Range range, float scale) {
        Request.Builder builder = new Request.Builder().play().setUrl(getPlayUrl());
        builder.setHeader(Header.CSeq, getSession().getNextCSeq());
        builder.setHeader(Header.UserAgent, getUserAgent());
        builder.setHeader(Header.Session, getSession().getId());
        builder.setHeader(Header.Range, range);
        builder.setHeader(Header.Scale, scale);

        dispatch(builder.build());
    }

    @Override
    public void sendPauseRequest() {
        Request.Builder builder = new Request.Builder().pause().setUrl(getSession().getUri().toString());
        builder.setHeader(Header.CSeq, getSession().getNextCSeq());
        builder.setHeader(Header.UserAgent, getUserAgent());
        builder.setHeader(Header.Session, getSession().getId());

        dispatch(builder.build());
    }

    @Override
    protected void sendRecordRequest() {
        // Not Implemented
    }

    @Override
    protected void sendGetParameterRequest() {
        Request.Builder builder = new Request.Builder().get_parameter().setUrl(getSession().getUri().toString());
        builder.setHeader(Header.CSeq, getSession().getNextCSeq());
        builder.setHeader(Header.UserAgent, getUserAgent());
        builder.setHeader(Header.Session, getSession().getId());

        dispatch(builder.build());
    }

    @Override
    protected void sendSetParameterRequest(String name, String value) {
        // Not Implemented
    }

    @Override
    public void sendTeardownRequest() {
        Request.Builder builder = new Request.Builder().teardown().setUrl(getSession().getUri().toString());
        builder.setHeader(Header.CSeq, getSession().getNextCSeq());
        builder.setHeader(Header.UserAgent, getUserAgent());
        builder.setHeader(Header.Session, getSession().getId());

        dispatch(builder.build());
    }
}
