/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.util;

import com.adt.vpm.videoplayer.source.core.PlaybackParameters;

/**
 * Tracks the progression of media time.
 */
public interface MediaClock {

  /**
   * Returns the current media position in microseconds.
   */
  long getPositionUs();

  /**
   * Attempts to set the playback parameters. The media clock may override the speed if changing the
   * playback parameters is not supported.
   *
   * @param playbackParameters The playback parameters to attempt to set.
   */
  void setPlaybackParameters(PlaybackParameters playbackParameters);

  /** Returns the active playback parameters. */
  PlaybackParameters getPlaybackParameters();
}
