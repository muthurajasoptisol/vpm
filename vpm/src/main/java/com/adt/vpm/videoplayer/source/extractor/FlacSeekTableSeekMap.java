/*
 * Created by ADT author on 9/29/20 5:06 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 5:06 PM
 */
package com.adt.vpm.videoplayer.source.extractor;

import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.util.Assertions;
import com.adt.vpm.videoplayer.source.common.util.Util;

/**
 * A {@link SeekMap} implementation for FLAC streams that contain a <a
 * href="https://xiph.org/flac/format.html#metadata_block_seektable">seek table</a>.
 */
public final class FlacSeekTableSeekMap implements SeekMap {

  private final FlacStreamMetadata flacStreamMetadata;
  private final long firstFrameOffset;

  /**
   * Creates a seek map from the FLAC stream seek table.
   *
   * @param flacStreamMetadata The stream metadata.
   * @param firstFrameOffset The byte offset of the first frame in the stream.
   */
  public FlacSeekTableSeekMap(FlacStreamMetadata flacStreamMetadata, long firstFrameOffset) {
    this.flacStreamMetadata = flacStreamMetadata;
    this.firstFrameOffset = firstFrameOffset;
  }

  @Override
  public boolean isSeekable() {
    return true;
  }

  @Override
  public long getDurationUs() {
    return flacStreamMetadata.getDurationUs();
  }

  @Override
  public SeekPoints getSeekPoints(long timeUs) {
    Assertions.checkStateNotNull(flacStreamMetadata.seekTable);
    long[] pointSampleNumbers = flacStreamMetadata.seekTable.pointSampleNumbers;
    long[] pointOffsets = flacStreamMetadata.seekTable.pointOffsets;

    long targetSampleNumber = flacStreamMetadata.getSampleNumber(timeUs);
    int index =
        Util.binarySearchFloor(
            pointSampleNumbers,
            targetSampleNumber,
            /* inclusive= */ true,
            /* stayInBounds= */ false);

    long seekPointSampleNumber = index == -1 ? 0 : pointSampleNumbers[index];
    long seekPointOffsetFromFirstFrame = index == -1 ? 0 : pointOffsets[index];
    SeekPoint seekPoint = getSeekPoint(seekPointSampleNumber, seekPointOffsetFromFirstFrame);
    if (seekPoint.timeUs == timeUs || index == pointSampleNumbers.length - 1) {
      return new SeekPoints(seekPoint);
    } else {
      SeekPoint secondSeekPoint =
          getSeekPoint(pointSampleNumbers[index + 1], pointOffsets[index + 1]);
      return new SeekPoints(seekPoint, secondSeekPoint);
    }
  }

  private SeekPoint getSeekPoint(long sampleNumber, long offsetFromFirstFrame) {
    long seekTimeUs = sampleNumber * C.MICROS_PER_SECOND / flacStreamMetadata.sampleRate;
    long seekPosition = firstFrameOffset + offsetFromFirstFrame;
    return new SeekPoint(seekTimeUs, seekPosition);
  }
}
