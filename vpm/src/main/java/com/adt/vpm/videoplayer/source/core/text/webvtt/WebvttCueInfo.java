/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.text.webvtt;


import com.adt.vpm.videoplayer.source.core.text.Cue;

/** A representation of a WebVTT cue. */
public final class WebvttCueInfo {

  public final Cue cue;
  public final long startTimeUs;
  public final long endTimeUs;

  public WebvttCueInfo(Cue cue, long startTimeUs, long endTimeUs) {
    this.cue = cue;
    this.startTimeUs = startTimeUs;
    this.endTimeUs = endTimeUs;
  }
}
