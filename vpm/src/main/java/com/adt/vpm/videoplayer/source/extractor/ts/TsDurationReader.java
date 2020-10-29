/*
 * Created by ADT author on 9/29/20 5:06 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 5:06 PM
 */
package com.adt.vpm.videoplayer.source.extractor.ts;

import com.adt.vpm.videoplayer.source.extractor.Extractor;
import com.adt.vpm.videoplayer.source.extractor.ExtractorInput;
import com.adt.vpm.videoplayer.source.extractor.PositionHolder;
import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.util.ParsableByteArray;
import com.adt.vpm.videoplayer.source.common.util.TimestampAdjuster;
import com.adt.vpm.videoplayer.source.common.util.Util;

import java.io.IOException;

import static java.lang.Math.min;

/**
 * A reader that can extract the approximate duration from a given MPEG transport stream (TS).
 *
 * <p>This reader extracts the duration by reading PCR values of the PCR PID packets at the start
 * and at the end of the stream, calculating the difference, and converting that into stream
 * duration. This reader also handles the case when a single PCR wraparound takes place within the
 * stream, which can make PCR values at the beginning of the stream larger than PCR values at the
 * end. This class can only be used once to read duration from a given stream, and the usage of the
 * class is not thread-safe, so all calls should be made from the same thread.
 */
/* package */ final class TsDurationReader {

  private static final int TIMESTAMP_SEARCH_BYTES = 600 * TsExtractor.TS_PACKET_SIZE;

  private final TimestampAdjuster pcrTimestampAdjuster;
  private final ParsableByteArray packetBuffer;

  private boolean isDurationRead;
  private boolean isFirstPcrValueRead;
  private boolean isLastPcrValueRead;

  private long firstPcrValue;
  private long lastPcrValue;
  private long durationUs;

  /* package */ TsDurationReader() {
    pcrTimestampAdjuster = new TimestampAdjuster(/* firstSampleTimestampUs= */ 0);
    firstPcrValue = C.TIME_UNSET;
    lastPcrValue = C.TIME_UNSET;
    durationUs = C.TIME_UNSET;
    packetBuffer = new ParsableByteArray();
  }

  /** Returns true if a TS duration has been read. */
  public boolean isDurationReadFinished() {
    return isDurationRead;
  }

  /**
   * Reads a TS duration from the input, using the given PCR PID.
   *
   * <p>This reader reads the duration by reading PCR values of the PCR PID packets at the start and
   * at the end of the stream, calculating the difference, and converting that into stream duration.
   *
   * @param input The {@link ExtractorInput} from which data should be read.
   * @param seekPositionHolder If {@link Extractor#RESULT_SEEK} is returned, this holder is updated
   *     to hold the position of the required seek.
   * @param pcrPid The PID of the packet stream within this TS stream that contains PCR values.
   * @return One of the {@code RESULT_} values defined in {@link Extractor}.
   * @throws IOException If an error occurred reading from the input.
   */
  public @Extractor.ReadResult int readDuration(
      ExtractorInput input, PositionHolder seekPositionHolder, int pcrPid) throws IOException {
    if (pcrPid <= 0) {
      return finishReadDuration(input);
    }
    if (!isLastPcrValueRead) {
      return readLastPcrValue(input, seekPositionHolder, pcrPid);
    }
    if (lastPcrValue == C.TIME_UNSET) {
      return finishReadDuration(input);
    }
    if (!isFirstPcrValueRead) {
      return readFirstPcrValue(input, seekPositionHolder, pcrPid);
    }
    if (firstPcrValue == C.TIME_UNSET) {
      return finishReadDuration(input);
    }

    long minPcrPositionUs = pcrTimestampAdjuster.adjustTsTimestamp(firstPcrValue);
    long maxPcrPositionUs = pcrTimestampAdjuster.adjustTsTimestamp(lastPcrValue);
    durationUs = maxPcrPositionUs - minPcrPositionUs;
    return finishReadDuration(input);
  }

  /**
   * Returns the duration last read from {@link #readDuration(ExtractorInput, PositionHolder, int)}.
   */
  public long getDurationUs() {
    return durationUs;
  }

  /**
   * Returns the {@link TimestampAdjuster} that this class uses to adjust timestamps read from the
   * input TS stream.
   */
  public TimestampAdjuster getPcrTimestampAdjuster() {
    return pcrTimestampAdjuster;
  }

  private int finishReadDuration(ExtractorInput input) {
    packetBuffer.reset(Util.EMPTY_BYTE_ARRAY);
    isDurationRead = true;
    input.resetPeekPosition();
    return Extractor.RESULT_CONTINUE;
  }

  private int readFirstPcrValue(ExtractorInput input, PositionHolder seekPositionHolder, int pcrPid)
      throws IOException {
    int bytesToSearch = (int) min(TIMESTAMP_SEARCH_BYTES, input.getLength());
    int searchStartPosition = 0;
    if (input.getPosition() != searchStartPosition) {
      seekPositionHolder.position = searchStartPosition;
      return Extractor.RESULT_SEEK;
    }

    packetBuffer.reset(bytesToSearch);
    input.resetPeekPosition();
    input.peekFully(packetBuffer.getData(), /* offset= */ 0, bytesToSearch);

    firstPcrValue = readFirstPcrValueFromBuffer(packetBuffer, pcrPid);
    isFirstPcrValueRead = true;
    return Extractor.RESULT_CONTINUE;
  }

  private long readFirstPcrValueFromBuffer(ParsableByteArray packetBuffer, int pcrPid) {
    int searchStartPosition = packetBuffer.getPosition();
    int searchEndPosition = packetBuffer.limit();
    for (int searchPosition = searchStartPosition;
        searchPosition < searchEndPosition;
        searchPosition++) {
      if (packetBuffer.getData()[searchPosition] != TsExtractor.TS_SYNC_BYTE) {
        continue;
      }
      long pcrValue = TsUtil.readPcrFromPacket(packetBuffer, searchPosition, pcrPid);
      if (pcrValue != C.TIME_UNSET) {
        return pcrValue;
      }
    }
    return C.TIME_UNSET;
  }

  private int readLastPcrValue(ExtractorInput input, PositionHolder seekPositionHolder, int pcrPid)
      throws IOException {
    long inputLength = input.getLength();
    int bytesToSearch = (int) min(TIMESTAMP_SEARCH_BYTES, inputLength);
    long searchStartPosition = inputLength - bytesToSearch;
    if (input.getPosition() != searchStartPosition) {
      seekPositionHolder.position = searchStartPosition;
      return Extractor.RESULT_SEEK;
    }

    packetBuffer.reset(bytesToSearch);
    input.resetPeekPosition();
    input.peekFully(packetBuffer.getData(), /* offset= */ 0, bytesToSearch);

    lastPcrValue = readLastPcrValueFromBuffer(packetBuffer, pcrPid);
    isLastPcrValueRead = true;
    return Extractor.RESULT_CONTINUE;
  }

  private long readLastPcrValueFromBuffer(ParsableByteArray packetBuffer, int pcrPid) {
    int searchStartPosition = packetBuffer.getPosition();
    int searchEndPosition = packetBuffer.limit();
    for (int searchPosition = searchEndPosition - 1;
        searchPosition >= searchStartPosition;
        searchPosition--) {
      if (packetBuffer.getData()[searchPosition] != TsExtractor.TS_SYNC_BYTE) {
        continue;
      }
      long pcrValue = TsUtil.readPcrFromPacket(packetBuffer, searchPosition, pcrPid);
      if (pcrValue != C.TIME_UNSET) {
        return pcrValue;
      }
    }
    return C.TIME_UNSET;
  }

}
