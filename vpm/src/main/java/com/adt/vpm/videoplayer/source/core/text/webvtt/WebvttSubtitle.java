/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.text.webvtt;

import com.adt.vpm.videoplayer.source.core.text.Cue;
import com.adt.vpm.videoplayer.source.core.text.Subtitle;
import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.util.Assertions;
import com.adt.vpm.videoplayer.source.common.util.Util;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A representation of a WebVTT subtitle.
 */
/* package */ final class WebvttSubtitle implements Subtitle {

  private final List<WebvttCueInfo> cueInfos;
  private final long[] cueTimesUs;
  private final long[] sortedCueTimesUs;

  /** Constructs a new WebvttSubtitle from a list of {@link WebvttCueInfo}s. */
  public WebvttSubtitle(List<WebvttCueInfo> cueInfos) {
    this.cueInfos = Collections.unmodifiableList(new ArrayList<>(cueInfos));
    cueTimesUs = new long[2 * cueInfos.size()];
    for (int cueIndex = 0; cueIndex < cueInfos.size(); cueIndex++) {
      WebvttCueInfo cueInfo = cueInfos.get(cueIndex);
      int arrayIndex = cueIndex * 2;
      cueTimesUs[arrayIndex] = cueInfo.startTimeUs;
      cueTimesUs[arrayIndex + 1] = cueInfo.endTimeUs;
    }
    sortedCueTimesUs = Arrays.copyOf(cueTimesUs, cueTimesUs.length);
    Arrays.sort(sortedCueTimesUs);
  }

  @Override
  public int getNextEventTimeIndex(long timeUs) {
    int index = Util.binarySearchCeil(sortedCueTimesUs, timeUs, false, false);
    return index < sortedCueTimesUs.length ? index : C.INDEX_UNSET;
  }

  @Override
  public int getEventTimeCount() {
    return sortedCueTimesUs.length;
  }

  @Override
  public long getEventTime(int index) {
    Assertions.checkArgument(index >= 0);
    Assertions.checkArgument(index < sortedCueTimesUs.length);
    return sortedCueTimesUs[index];
  }

  @Override
  public List<Cue> getCues(long timeUs) {
    List<Cue> currentCues = new ArrayList<>();
    List<WebvttCueInfo> cuesWithUnsetLine = new ArrayList<>();
    for (int i = 0; i < cueInfos.size(); i++) {
      if ((cueTimesUs[i * 2] <= timeUs) && (timeUs < cueTimesUs[i * 2 + 1])) {
        WebvttCueInfo cueInfo = cueInfos.get(i);
        if (cueInfo.cue.line == Cue.DIMEN_UNSET) {
          cuesWithUnsetLine.add(cueInfo);
        } else {
          currentCues.add(cueInfo.cue);
        }
      }
    }
    // Steps 4 - 10 of https://www.w3.org/TR/webvtt1/#cue-computed-line
    // (steps 1 - 3 are handled by WebvttCueParser#computeLine(float, int))
    Collections.sort(cuesWithUnsetLine, (c1, c2) -> Long.compare(c1.startTimeUs, c2.startTimeUs));
    for (int i = 0; i < cuesWithUnsetLine.size(); i++) {
      Cue cue = cuesWithUnsetLine.get(i).cue;
      currentCues.add(cue.buildUpon().setLine((float) (-1 - i), Cue.LINE_TYPE_NUMBER).build());
    }
    return currentCues;
  }
}