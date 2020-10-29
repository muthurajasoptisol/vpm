/*
 * Created by ADT author on 9/29/20 5:06 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 5:06 PM
 */
package com.adt.vpm.videoplayer.source.extractor;

import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.util.Util;

import static java.lang.Math.max;

/**
 * A {@link SeekMap} implementation that assumes the stream has a constant bitrate and consists of
 * multiple independent frames of the same size. Seek points are calculated to be at frame
 * boundaries.
 */
public class ConstantBitrateSeekMap implements SeekMap {

  private final long inputLength;
  private final long firstFrameBytePosition;
  private final int frameSize;
  private final long dataSize;
  private final int bitrate;
  private final long durationUs;

  /**
   * Constructs a new instance from a stream.
   *
   * @param inputLength The length of the stream in bytes, or {@link C#LENGTH_UNSET} if unknown.
   * @param firstFrameBytePosition The byte-position of the first frame in the stream.
   * @param bitrate The bitrate (which is assumed to be constant in the stream).
   * @param frameSize The size of each frame in the stream in bytes. May be {@link C#LENGTH_UNSET}
   *     if unknown.
   */
  public ConstantBitrateSeekMap(
      long inputLength, long firstFrameBytePosition, int bitrate, int frameSize) {
    this.inputLength = inputLength;
    this.firstFrameBytePosition = firstFrameBytePosition;
    this.frameSize = frameSize == C.LENGTH_UNSET ? 1 : frameSize;
    this.bitrate = bitrate;

    if (inputLength == C.LENGTH_UNSET) {
      dataSize = C.LENGTH_UNSET;
      durationUs = C.TIME_UNSET;
    } else {
      dataSize = inputLength - firstFrameBytePosition;
      durationUs = getTimeUsAtPosition(inputLength, firstFrameBytePosition, bitrate);
    }
  }

  @Override
  public boolean isSeekable() {
    return dataSize != C.LENGTH_UNSET;
  }

  @Override
  public SeekPoints getSeekPoints(long timeUs) {
    if (dataSize == C.LENGTH_UNSET) {
      return new SeekPoints(new SeekPoint(0, firstFrameBytePosition));
    }
    long seekFramePosition = getFramePositionForTimeUs(timeUs);
    long seekTimeUs = getTimeUsAtPosition(seekFramePosition);
    SeekPoint seekPoint = new SeekPoint(seekTimeUs, seekFramePosition);
    if (seekTimeUs >= timeUs || seekFramePosition + frameSize >= inputLength) {
      return new SeekPoints(seekPoint);
    } else {
      long secondSeekPosition = seekFramePosition + frameSize;
      long secondSeekTimeUs = getTimeUsAtPosition(secondSeekPosition);
      SeekPoint secondSeekPoint = new SeekPoint(secondSeekTimeUs, secondSeekPosition);
      return new SeekPoints(seekPoint, secondSeekPoint);
    }
  }

  @Override
  public long getDurationUs() {
    return durationUs;
  }

  /**
   * Returns the stream time in microseconds for a given position.
   *
   * @param position The stream byte-position.
   * @return The stream time in microseconds for the given position.
   */
  public long getTimeUsAtPosition(long position) {
    return getTimeUsAtPosition(position, firstFrameBytePosition, bitrate);
  }

  // Internal methods

  /**
   * Returns the stream time in microseconds for a given stream position.
   *
   * @param position The stream byte-position.
   * @param firstFrameBytePosition The position of the first frame in the stream.
   * @param bitrate The bitrate (which is assumed to be constant in the stream).
   * @return The stream time in microseconds for the given stream position.
   */
  private static long getTimeUsAtPosition(long position, long firstFrameBytePosition, int bitrate) {
    return max(0, position - firstFrameBytePosition)
        * C.BITS_PER_BYTE
        * C.MICROS_PER_SECOND
        / bitrate;
  }

  private long getFramePositionForTimeUs(long timeUs) {
    long positionOffset = (timeUs * bitrate) / (C.MICROS_PER_SECOND * C.BITS_PER_BYTE);
    // Constrain to nearest preceding frame offset.
    positionOffset = (positionOffset / frameSize) * frameSize;
    positionOffset =
        Util.constrainValue(positionOffset, /* min= */ 0, /* max= */ dataSize - frameSize);
    return firstFrameBytePosition + positionOffset;
  }
}
