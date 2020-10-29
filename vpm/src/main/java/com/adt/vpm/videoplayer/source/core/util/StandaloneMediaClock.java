/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.util;

import com.adt.vpm.videoplayer.source.core.PlaybackParameters;
import com.adt.vpm.videoplayer.source.common.C;

/**
 * A {@link MediaClock} whose position advances with real time based on the playback parameters when
 * started.
 */
public final class StandaloneMediaClock implements MediaClock {

  private final Clock clock;

  private boolean started;
  private long baseUs;
  private long baseElapsedMs;
  private PlaybackParameters playbackParameters;

  /**
   * Creates a new standalone media clock using the given {@link Clock} implementation.
   *
   * @param clock A {@link Clock}.
   */
  public StandaloneMediaClock(Clock clock) {
    this.clock = clock;
    playbackParameters = PlaybackParameters.DEFAULT;
  }

  /**
   * Starts the clock. Does nothing if the clock is already started.
   */
  public void start() {
    if (!started) {
      baseElapsedMs = clock.elapsedRealtime();
      started = true;
    }
  }

  /**
   * Stops the clock. Does nothing if the clock is already stopped.
   */
  public void stop() {
    if (started) {
      resetPosition(getPositionUs());
      started = false;
    }
  }

  /**
   * Resets the clock's position.
   *
   * @param positionUs The position to set in microseconds.
   */
  public void resetPosition(long positionUs) {
    baseUs = positionUs;
    if (started) {
      baseElapsedMs = clock.elapsedRealtime();
    }
  }

  @Override
  public long getPositionUs() {
    long positionUs = baseUs;
    if (started) {
      long elapsedSinceBaseMs = clock.elapsedRealtime() - baseElapsedMs;
      if (playbackParameters.speed == 1f) {
        positionUs += C.msToUs(elapsedSinceBaseMs);
      } else {
        // Add the media time in microseconds that will elapse in elapsedSinceBaseMs milliseconds of
        // wallclock time
        positionUs += playbackParameters.getMediaTimeUsForPlayoutTimeMs(elapsedSinceBaseMs);
      }
    }
    return positionUs;
  }

  @Override
  public void setPlaybackParameters(PlaybackParameters playbackParameters) {
    // Store the current position as the new base, in case the playback speed has changed.
    if (started) {
      resetPosition(getPositionUs());
    }
    this.playbackParameters = playbackParameters;
  }

  @Override
  public PlaybackParameters getPlaybackParameters() {
    return playbackParameters;
  }

}
