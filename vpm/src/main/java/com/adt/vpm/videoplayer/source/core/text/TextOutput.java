/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.text;

import java.util.List;

/**
 * Receives text output.
 */
public interface TextOutput {

  /**
   * Called when there is a change in the {@link Cue Cues}.
   *
   * @param cues The {@link Cue Cues}. May be empty.
   */
  void onCues(List<Cue> cues);
}
