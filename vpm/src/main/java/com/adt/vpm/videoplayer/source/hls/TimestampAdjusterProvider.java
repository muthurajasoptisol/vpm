/*
 * Created by ADT author on 10/1/20 11:19 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/22/20 6:23 PM
 */
package com.adt.vpm.videoplayer.source.hls;

import android.util.SparseArray;

import com.adt.vpm.videoplayer.source.common.util.TimestampAdjuster;

/**
 * Provides {@link TimestampAdjuster} instances for use during HLS playbacks.
 */
public final class TimestampAdjusterProvider {

  // TODO: Prevent this array from growing indefinitely large by removing adjusters that are no
  // longer required.
  private final SparseArray<TimestampAdjuster> timestampAdjusters;

  public TimestampAdjusterProvider() {
    timestampAdjusters = new SparseArray<>();
  }

  /**
   * Returns a {@link TimestampAdjuster} suitable for adjusting the pts timestamps contained in
   * a chunk with a given discontinuity sequence.
   *
   * @param discontinuitySequence The chunk's discontinuity sequence.
   * @return A {@link TimestampAdjuster}.
   */
  public TimestampAdjuster getAdjuster(int discontinuitySequence) {
    TimestampAdjuster adjuster = timestampAdjusters.get(discontinuitySequence);
    if (adjuster == null) {
      adjuster = new TimestampAdjuster(TimestampAdjuster.DO_NOT_OFFSET);
      timestampAdjusters.put(discontinuitySequence, adjuster);
    }
    return adjuster;
  }

  /**
   * Resets the provider.
   */
  public void reset() {
    timestampAdjusters.clear();
  }

}
