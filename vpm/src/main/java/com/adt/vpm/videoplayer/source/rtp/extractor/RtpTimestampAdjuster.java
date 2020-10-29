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
import com.adt.vpm.videoplayer.source.common.util.Assertions;

/*package*/ final class RtpTimestampAdjuster {
  private long firstSampleTimestamp;
  private long timestampOffset;

  private long sampleTimestampUs;

  private final int clockrate;

  /**
   * @param clockrate The kHz clock.
   */
  public RtpTimestampAdjuster(int clockrate) {
    this.clockrate = clockrate;
    firstSampleTimestamp = C.TIME_UNSET;
  }

  /** Returns the kHz clock */
  public int getClockrate() { return clockrate; }

  /**
   * Offsets a timestamp in microseconds.
   *
   * @param timeUs The timestamp to adjust in microseconds.
   * @return The adjusted timestamp in microseconds.
   */
  public void adjustSampleTimestamp(long timeUs) {
    if (firstSampleTimestamp == C.TIME_UNSET) {
      firstSampleTimestamp = timeUs;
    } else {
      timestampOffset = timeUs - firstSampleTimestamp;
    }

    sampleTimestampUs = (timestampOffset * C.MICROS_PER_SECOND) / clockrate;
  }

  /** Returns the last value passed to {@link #adjustSampleTimestamp(long)}. */
  public long getSampleTimeUs() {
    return sampleTimestampUs;
  }

  /**
   * Resets the instance to its initial state.
   */
  public void reset() {
    sampleTimestampUs = C.TIME_UNSET;
  }

  /**
   * Sets the desired result of the first call to {@link #adjustSampleTimestamp(long)}.
   *
   * @param firstSampleTimestampUs The first adjusted sample timestamp in microseconds. Can only be
   * called before any timestamps have been adjusted.
   */
  public synchronized void setFirstSampleTimestampUs(long firstSampleTimestampUs) {
    Assertions.checkState(sampleTimestampUs == C.TIME_UNSET);
    this.firstSampleTimestamp = firstSampleTimestampUs;
  }
}
