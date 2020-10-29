/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core;

/**
 * Thrown when an attempt is made to seek to a position that does not exist in the player's
 * {@link Timeline}.
 */
public final class IllegalSeekPositionException extends IllegalStateException {

  /**
   * The {@link Timeline} in which the seek was attempted.
   */
  public final Timeline timeline;
  /**
   * The index of the window being seeked to.
   */
  public final int windowIndex;
  /**
   * The seek position in the specified window.
   */
  public final long positionMs;

  /**
   * @param timeline The {@link Timeline} in which the seek was attempted.
   * @param windowIndex The index of the window being seeked to.
   * @param positionMs The seek position in the specified window.
   */
  public IllegalSeekPositionException(Timeline timeline, int windowIndex, long positionMs) {
    this.timeline = timeline;
    this.windowIndex = windowIndex;
    this.positionMs = positionMs;
  }

}
