/*
 * Created by ADT author on 9/29/20 5:06 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 5:06 PM
 */
package com.adt.vpm.videoplayer.source.extractor.mp3;

import androidx.annotation.VisibleForTesting;

import com.adt.vpm.videoplayer.source.extractor.SeekPoint;
import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.util.LongArray;
import com.adt.vpm.videoplayer.source.common.util.Util;

/** MP3 seeker that builds a time-to-byte mapping as the stream is read. */
/* package */ final class IndexSeeker implements Seeker {

  @VisibleForTesting
  /* package */ static final long MIN_TIME_BETWEEN_POINTS_US = C.MICROS_PER_SECOND / 10;

  private final long dataEndPosition;
  private final LongArray timesUs;
  private final LongArray positions;

  private long durationUs;

  public IndexSeeker(long durationUs, long dataStartPosition, long dataEndPosition) {
    this.durationUs = durationUs;
    this.dataEndPosition = dataEndPosition;
    timesUs = new LongArray();
    positions = new LongArray();
    timesUs.add(0L);
    positions.add(dataStartPosition);
  }

  @Override
  public long getTimeUs(long position) {
    int targetIndex =
        Util.binarySearchFloor(
            positions, position, /* inclusive= */ true, /* stayInBounds= */ true);
    return timesUs.get(targetIndex);
  }

  @Override
  public long getDataEndPosition() {
    return dataEndPosition;
  }

  @Override
  public boolean isSeekable() {
    return true;
  }

  @Override
  public long getDurationUs() {
    return durationUs;
  }

  @Override
  public SeekPoints getSeekPoints(long timeUs) {
    int targetIndex =
        Util.binarySearchFloor(timesUs, timeUs, /* inclusive= */ true, /* stayInBounds= */ true);
    SeekPoint seekPoint = new SeekPoint(timesUs.get(targetIndex), positions.get(targetIndex));
    if (seekPoint.timeUs >= timeUs || targetIndex == timesUs.size() - 1) {
      return new SeekPoints(seekPoint);
    } else {
      SeekPoint nextSeekPoint =
          new SeekPoint(timesUs.get(targetIndex + 1), positions.get(targetIndex + 1));
      return new SeekPoints(seekPoint, nextSeekPoint);
    }
  }

  /**
   * Adds a seek point to the index if it is sufficiently distant from the other points.
   *
   * <p>Seek points must be added in order.
   *
   * @param timeUs The time corresponding to the seek point to add in microseconds.
   * @param position The position corresponding to the seek point to add in bytes.
   */
  public void maybeAddSeekPoint(long timeUs, long position) {
    if (isTimeUsInIndex(timeUs)) {
      return;
    }
    timesUs.add(timeUs);
    positions.add(position);
  }

  /**
   * Returns whether {@code timeUs} (in microseconds) is included in the index.
   *
   * <p>A point is included in the index if it is equal to another point, between 2 points, or
   * sufficiently close to the last point.
   */
  public boolean isTimeUsInIndex(long timeUs) {
    long lastIndexedTimeUs = timesUs.get(timesUs.size() - 1);
    return timeUs - lastIndexedTimeUs < MIN_TIME_BETWEEN_POINTS_US;
  }

  /* package */ void setDurationUs(long durationUs) {
    this.durationUs = durationUs;
  }
}
