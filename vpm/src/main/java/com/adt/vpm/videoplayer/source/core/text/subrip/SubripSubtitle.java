/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.text.subrip;

import com.adt.vpm.videoplayer.source.core.text.Cue;
import com.adt.vpm.videoplayer.source.core.text.Subtitle;
import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.util.Assertions;
import com.adt.vpm.videoplayer.source.common.util.Util;
import java.util.Collections;
import java.util.List;

/**
 * A representation of a SubRip subtitle.
 */
/* package */ final class SubripSubtitle implements Subtitle {

  private final Cue[] cues;
  private final long[] cueTimesUs;

  /**
   * @param cues The cues in the subtitle.
   * @param cueTimesUs The cue times, in microseconds.
   */
  public SubripSubtitle(Cue[] cues, long[] cueTimesUs) {
    this.cues = cues;
    this.cueTimesUs = cueTimesUs;
  }

  @Override
  public int getNextEventTimeIndex(long timeUs) {
    int index = Util.binarySearchCeil(cueTimesUs, timeUs, false, false);
    return index < cueTimesUs.length ? index : C.INDEX_UNSET;
  }

  @Override
  public int getEventTimeCount() {
    return cueTimesUs.length;
  }

  @Override
  public long getEventTime(int index) {
    Assertions.checkArgument(index >= 0);
    Assertions.checkArgument(index < cueTimesUs.length);
    return cueTimesUs[index];
  }

  @Override
  public List<Cue> getCues(long timeUs) {
    int index = Util.binarySearchFloor(cueTimesUs, timeUs, true, false);
    if (index == -1 || cues[index] == Cue.EMPTY) {
      // timeUs is earlier than the start of the first cue, or we have an empty cue.
      return Collections.emptyList();
    } else {
      return Collections.singletonList(cues[index]);
    }
  }

}
