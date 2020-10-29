/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.source.ads;

import androidx.annotation.VisibleForTesting;

import com.adt.vpm.videoplayer.source.core.Timeline;
import com.adt.vpm.videoplayer.source.core.source.ForwardingTimeline;
import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.util.Assertions;

/** A {@link Timeline} for sources that have ads. */
@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
public final class SinglePeriodAdTimeline extends ForwardingTimeline {

  private final AdPlaybackState adPlaybackState;

  /**
   * Creates a new timeline with a single period containing ads.
   *
   * @param contentTimeline The timeline of the content alongside which ads will be played. It must
   *     have one window and one period.
   * @param adPlaybackState The state of the period's ads.
   */
  public SinglePeriodAdTimeline(Timeline contentTimeline, AdPlaybackState adPlaybackState) {
    super(contentTimeline);
    Assertions.checkState(contentTimeline.getPeriodCount() == 1);
    Assertions.checkState(contentTimeline.getWindowCount() == 1);
    this.adPlaybackState = adPlaybackState;
  }

  @Override
  public Timeline.Period getPeriod(int periodIndex, Timeline.Period period, boolean setIds) {
    timeline.getPeriod(periodIndex, period, setIds);
    long durationUs =
        period.durationUs == C.TIME_UNSET ? adPlaybackState.contentDurationUs : period.durationUs;
    period.set(
        period.id,
        period.uid,
        period.windowIndex,
        durationUs,
        period.getPositionInWindowUs(),
        adPlaybackState);
    return period;
  }

}
