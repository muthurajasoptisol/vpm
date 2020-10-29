/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.text.cea;

import com.adt.vpm.videoplayer.source.core.text.Cue;
import com.adt.vpm.videoplayer.source.core.text.Subtitle;
import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.util.Assertions;
import java.util.Collections;
import java.util.List;

/**
 * A representation of a CEA subtitle.
 */
/* package */ final class CeaSubtitle implements Subtitle {

  private final List<Cue> cues;

  /**
   * @param cues The subtitle cues.
   */
  public CeaSubtitle(List<Cue> cues) {
    this.cues = cues;
  }

  @Override
  public int getNextEventTimeIndex(long timeUs) {
    return timeUs < 0 ? 0 : C.INDEX_UNSET;
  }

  @Override
  public int getEventTimeCount() {
    return 1;
  }

  @Override
  public long getEventTime(int index) {
    Assertions.checkArgument(index == 0);
    return 0;
  }

  @Override
  public List<Cue> getCues(long timeUs) {
    return timeUs >= 0 ? cues : Collections.emptyList();
  }

}
