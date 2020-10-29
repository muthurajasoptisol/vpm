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

import androidx.annotation.NonNull;

import com.adt.vpm.videoplayer.source.common.ParserException;
import com.adt.vpm.videoplayer.source.extractor.ExtractorOutput;
import com.adt.vpm.videoplayer.source.extractor.TrackOutput;
import com.adt.vpm.videoplayer.source.rtp.format.RtpPayloadFormat;
import com.adt.vpm.videoplayer.source.common.util.ParsableByteArray;
import com.adt.vpm.videoplayer.source.core.util.TrackIdGenerator;

/**
 * Parses RTP packet payload data
 */
/* package */ interface RtpPayloadReader {

    /**
     * Factory of {@link RtpPayloadReader} instances.
     */
    interface Factory {

        /**
         * Returns a {@link RtpPayloadReader} for a given payload format type.
         * May return null if the payload format type is not supported.
         *
         * @param format Rtp payload format associated to the stream.
         * @return A {@link RtpPayloadReader} for the packet stream carried by the provided pid.
         *     {@code null} if the stream is not supported.
         */
        @NonNull
        RtpPayloadReader createPayloadReader(RtpPayloadFormat format);

    }

    /**
     * Initializes the reader by providing outputs and ids for the tracks.
     *
     * @param extractorOutput The {@link ExtractorOutput} that receives the extracted data.
     * @param idGenerator A {@link TrackIdGenerator} that generates unique track ids for the
     *     {@link TrackOutput}s.
     */
    void createTracks(ExtractorOutput extractorOutput, TrackIdGenerator idGenerator);

    /**
     * Notifies the reader that a seek has occurred.
     *
     * @param position The byte offset in the stream from which data will be provided.
     * @param timeUs The seek time in microseconds.
     */
    void seek(long position, long timeUs);

    /**
     * Called when a packet starts.
     *
     * @param timestamp The RTP timestamp associated with the RTP payload.
     * @param dataAlignmentIndicator The data alignment indicator associated with the packet.
     */
    boolean packetStarted(long timestamp, boolean dataAlignmentIndicator, int sequence);

    /**
     * Consumes data from the current payload.
     *
     * @param data The data to consume.
     * @throws ParserException If the data could not be parsed.
     */
    void consume(ParsableByteArray data) throws ParserException;
}
