/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.text.ssa;

import com.adt.vpm.videoplayer.source.core.text.Cue;
import com.adt.vpm.videoplayer.source.core.text.Subtitle;
import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.util.Assertions;
import com.adt.vpm.videoplayer.source.common.util.Util;
import java.util.Collections;
import java.util.List;

/**
 * A representation of an SSA/ASS subtitle.
 */
/* package */ final class SsaSubtitle implements Subtitle {

  private final List<List<Cue>> cues;
  private final List<Long> cueTimesUs;

  /**
   * @param cues The cues in the subtitle.
   * @param cueTimesUs The cue times, in microseconds.
   */
  public SsaSubtitle(List<List<Cue>> cues, List<Long> cueTimesUs) {
    this.cues = cues;
    this.cueTimesUs = cueTimesUs;
  }

  @Override
  public int getNextEventTimeIndex(long timeUs) {
    int index = Util.binarySearchCeil(cueTimesUs, timeUs, false, false);
    return index < cueTimesUs.size() ? index : C.INDEX_UNSET;
  }

  @Override
  public int getEventTimeCount() {
    return cueTimesUs.size();
  }

  @Override
  public long getEventTime(int index) {
    Assertions.checkArgument(index >= 0);
    Assertions.checkArgument(index < cueTimesUs.size());
    return cueTimesUs.get(index);
  }

  @Override
  public List<Cue> getCues(long timeUs) {
    int index = Util.binarySearchFloor(cueTimesUs, timeUs, true, false);
    if (index == -1) {
      // timeUs is earlier than the start of the first cue.
      return Collections.emptyList();
    } else {
      return cues.get(index);
    }
  }
}
