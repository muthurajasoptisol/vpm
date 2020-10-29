/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core;

import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.util.Util;
import com.adt.vpm.videoplayer.source.core.source.ShuffleOrder;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/** Timeline exposing concatenated timelines of playlist media sources. */
/* package */ final class PlaylistTimeline extends AbstractConcatenatedTimeline {

  private final int windowCount;
  private final int periodCount;
  private final int[] firstPeriodInChildIndices;
  private final int[] firstWindowInChildIndices;
  private final Timeline[] timelines;
  private final Object[] uids;
  private final HashMap<Object, Integer> childIndexByUid;

  /** Creates an instance. */
  public PlaylistTimeline(
      Collection<? extends MediaSourceInfoHolder> mediaSourceInfoHolders,
      ShuffleOrder shuffleOrder) {
    super(/* isAtomic= */ false, shuffleOrder);
    int childCount = mediaSourceInfoHolders.size();
    firstPeriodInChildIndices = new int[childCount];
    firstWindowInChildIndices = new int[childCount];
    timelines = new Timeline[childCount];
    uids = new Object[childCount];
    childIndexByUid = new HashMap<>();
    int index = 0;
    int windowCount = 0;
    int periodCount = 0;
    for (MediaSourceInfoHolder mediaSourceInfoHolder : mediaSourceInfoHolders) {
      timelines[index] = mediaSourceInfoHolder.getTimeline();
      firstWindowInChildIndices[index] = windowCount;
      firstPeriodInChildIndices[index] = periodCount;
      windowCount += timelines[index].getWindowCount();
      periodCount += timelines[index].getPeriodCount();
      uids[index] = mediaSourceInfoHolder.getUid();
      childIndexByUid.put(uids[index], index++);
    }
    this.windowCount = windowCount;
    this.periodCount = periodCount;
  }

  /** Returns the child timelines. */
  /* package */ List<Timeline> getChildTimelines() {
    return Arrays.asList(timelines);
  }

  @Override
  protected int getChildIndexByPeriodIndex(int periodIndex) {
    return Util.binarySearchFloor(firstPeriodInChildIndices, periodIndex + 1, false, false);
  }

  @Override
  protected int getChildIndexByWindowIndex(int windowIndex) {
    return Util.binarySearchFloor(firstWindowInChildIndices, windowIndex + 1, false, false);
  }

  @Override
  protected int getChildIndexByChildUid(Object childUid) {
    Integer index = childIndexByUid.get(childUid);
    return index == null ? C.INDEX_UNSET : index;
  }

  @Override
  protected Timeline getTimelineByChildIndex(int childIndex) {
    return timelines[childIndex];
  }

  @Override
  protected int getFirstPeriodIndexByChildIndex(int childIndex) {
    return firstPeriodInChildIndices[childIndex];
  }

  @Override
  protected int getFirstWindowIndexByChildIndex(int childIndex) {
    return firstWindowInChildIndices[childIndex];
  }

  @Override
  protected Object getChildUidByChildIndex(int childIndex) {
    return uids[childIndex];
  }

  @Override
  public int getWindowCount() {
    return windowCount;
  }

  @Override
  public int getPeriodCount() {
    return periodCount;
  }
}
