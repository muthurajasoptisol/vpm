/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.text.ttml;

import androidx.annotation.VisibleForTesting;

import com.adt.vpm.videoplayer.source.core.text.Cue;
import com.adt.vpm.videoplayer.source.core.text.Subtitle;
import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.util.Util;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A representation of a TTML subtitle.
 */
/* package */ final class TtmlSubtitle implements Subtitle {

  private final TtmlNode root;
  private final long[] eventTimesUs;
  private final Map<String, TtmlStyle> globalStyles;
  private final Map<String, TtmlRegion> regionMap;
  private final Map<String, String> imageMap;

  public TtmlSubtitle(
      TtmlNode root,
      Map<String, TtmlStyle> globalStyles,
      Map<String, TtmlRegion> regionMap,
      Map<String, String> imageMap) {
    this.root = root;
    this.regionMap = regionMap;
    this.imageMap = imageMap;
    this.globalStyles =
        globalStyles != null ? Collections.unmodifiableMap(globalStyles) : Collections.emptyMap();
    this.eventTimesUs = root.getEventTimesUs();
  }

  @Override
  public int getNextEventTimeIndex(long timeUs) {
    int index = Util.binarySearchCeil(eventTimesUs, timeUs, false, false);
    return index < eventTimesUs.length ? index : C.INDEX_UNSET;
  }

  @Override
  public int getEventTimeCount() {
    return eventTimesUs.length;
  }

  @Override
  public long getEventTime(int index) {
    return eventTimesUs[index];
  }

  @VisibleForTesting
  /* package */ TtmlNode getRoot() {
    return root;
  }

  @Override
  public List<Cue> getCues(long timeUs) {
    return root.getCues(timeUs, globalStyles, regionMap, imageMap);
  }

  @VisibleForTesting
  /* package */ Map<String, TtmlStyle> getGlobalStyles() {
    return globalStyles;
  }
}
