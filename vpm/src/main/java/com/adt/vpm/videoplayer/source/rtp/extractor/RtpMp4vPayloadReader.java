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

import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.Format;
import com.adt.vpm.videoplayer.source.common.ParserException;
import com.adt.vpm.videoplayer.source.extractor.ExtractorOutput;
import com.adt.vpm.videoplayer.source.extractor.TrackOutput;
import com.adt.vpm.videoplayer.source.rtp.format.RtpVideoPayload;
import com.adt.vpm.videoplayer.source.common.util.ParsableByteArray;
import com.adt.vpm.videoplayer.source.core.util.TrackIdGenerator;

import java.util.List;

/**
 * Extracts individual video frames from MPEG-4 Visual Streams RTP payload
 */
/*package*/ final class RtpMp4vPayloadReader implements RtpPayloadReader {

    private final RtpTimestampAdjuster timestampAdjuster;
    private final RtpVideoPayload payloadFormat;

    private int trackId;
    private String formatId;
    private TrackOutput output;

    private int sampleLength;

    private boolean completeFrameIndicator;

    public RtpMp4vPayloadReader(RtpVideoPayload payloadFormat) {
        this.payloadFormat = payloadFormat;

        timestampAdjuster = new RtpTimestampAdjuster(payloadFormat.getClockrate());
    }

    @Override
    public void seek(long position, long timeUs) {
        timestampAdjuster.reset();
        timestampAdjuster.adjustSampleTimestamp(timeUs);
    }

    @Override
    public void createTracks(ExtractorOutput extractorOutput, TrackIdGenerator trackIdGenerator) {
        trackIdGenerator.generateNewId();

        trackId = trackIdGenerator.getTrackId();
        formatId = trackIdGenerator.getFormatId();

        output = extractorOutput.track(trackId, C.TRACK_TYPE_VIDEO);

        List<byte[]> codecSpecificData = payloadFormat.buildCodecSpecificData();

        if (codecSpecificData != null) {
            Format format = Format.createVideoSampleFormat(formatId, payloadFormat.getSampleMimeType(),
                    payloadFormat.getCodecs(), payloadFormat.getBitrate(), Format.NO_VALUE,
                    payloadFormat.getWidth() > 0 ? payloadFormat.getWidth() : Format.NO_VALUE,
                    payloadFormat.getHeight() > 0 ? payloadFormat.getHeight() : Format.NO_VALUE,
                    payloadFormat.getFramerate(), codecSpecificData, Format.NO_VALUE,
                    payloadFormat.getPixelWidthAspectRatio(),null);

            output.format(format);
        }
    }

    @Override
    public boolean packetStarted(long sampleTimeStamp, boolean completeFrameIndicator,
                                 int sequenceNumber) {
        this.completeFrameIndicator = completeFrameIndicator;

        if (completeFrameIndicator) {
            timestampAdjuster.adjustSampleTimestamp(sampleTimeStamp);
        }

        return true;
    }

    @Override
    public void consume(ParsableByteArray packet) throws ParserException {
        int limit = packet.bytesLeft();
        sampleLength += limit;

        // Write the video sample
        output.sampleData(packet, limit);

        // Marker (M) bit: The marker bit is set to 1 to indicate the last RTP
        // packet(or only RTP packet) of a VOP. When multiple VOPs are carried
        // in the same RTP packet, the marker bit is set to 1.
        if (completeFrameIndicator) {
            @C.BufferFlags int flags = C.BUFFER_FLAG_KEY_FRAME;
            output.sampleMetadata(timestampAdjuster.getSampleTimeUs(), flags, sampleLength,
                    0, null);
            sampleLength = 0;
        }
    }
}
