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
package com.adt.vpm.videoplayer.source.rtp.extractor;

import android.util.Log;

import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.util.ParsableByteArray;
import com.adt.vpm.videoplayer.source.core.util.TrackIdGenerator;
import com.adt.vpm.videoplayer.source.extractor.Extractor;
import com.adt.vpm.videoplayer.source.extractor.ExtractorInput;
import com.adt.vpm.videoplayer.source.extractor.ExtractorOutput;
import com.adt.vpm.videoplayer.source.extractor.PositionHolder;
import com.adt.vpm.videoplayer.source.extractor.SeekMap;
import com.adt.vpm.videoplayer.source.extractor.UnsupportedFormatException;
import com.adt.vpm.videoplayer.source.rtp.RtpPacket;
import com.adt.vpm.videoplayer.source.rtp.format.RtpPayloadFormat;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Facilitates the extraction of sample from stream formatted as RTP payload
 */
public final class DefaultRtpExtractor implements Extractor {
    private ParsableByteArray sampleData;
    private final byte[] packetBuffer;

    private final RtpPayloadReader payloadReader;
    private final TrackIdGenerator trackIdGenerator;

    public DefaultRtpExtractor(RtpPayloadFormat payloadFormat,
                               TrackIdGenerator trackIdGenerator) throws UnsupportedFormatException {
        this.trackIdGenerator = trackIdGenerator;

        sampleData = new ParsableByteArray();
        packetBuffer = new byte[RtpPacket.MAX_PACKET_SIZE];

        payloadReader = new DefaultRtpPayloadReaderFactory().createPayloadReader(payloadFormat);

        if (payloadReader == null) {
            throw new UnsupportedFormatException("Payload reader not found for media type=[" +
                    payloadFormat.getSampleMimeType() + "]");
        }
    }

    @Override
    public boolean sniff(@NotNull ExtractorInput input) throws IOException {
        if (RtpPacket.sniffHeader(input) > 0) {
            return true;
        }

        return false;
    }

    @Override
    public void init(@NotNull ExtractorOutput output) {
        payloadReader.createTracks(output, trackIdGenerator);
        output.endTracks();
        output.seekMap(new SeekMap.Unseekable(C.TIME_UNSET));
    }

    @Override
    public int read(ExtractorInput input, PositionHolder seekPosition) throws IOException {
        int bytesRead = input.read(packetBuffer, 0, RtpPacket.MAX_PACKET_SIZE);

        if (bytesRead == C.RESULT_END_OF_INPUT) {
            return RESULT_END_OF_INPUT;

        } else if (bytesRead > 0) {
            try {

                RtpPacket packet = RtpPacket.parse(packetBuffer, bytesRead);
                if (payloadReader.packetStarted(packet.getTimestamp(), packet.getMarker(), packet.getSequenceNumber())) {
                    byte[] payload = packet.getPayload();
                    sampleData.reset(payload, payload.length);
                    payloadReader.consume(sampleData);
                }

            } catch (NullPointerException e) {
                throw new IOException(e);
            }
        }

        return RESULT_CONTINUE;
    }

    @Override
    public void seek(long position, long timeUs) {
        Log.v("DefaultRtpExtractor", "position=[" + position + "], timeUs=[" + timeUs + "]");
        payloadReader.seek(position, timeUs);
    }

    @Override
    public void release() {
        // Do nothing
    }
}
