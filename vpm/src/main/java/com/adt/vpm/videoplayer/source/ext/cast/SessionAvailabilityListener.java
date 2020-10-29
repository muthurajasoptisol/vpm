/*
 * Created by ADT author on 10/19/20 4:50 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/22/20 6:23 PM
 */
package com.adt.vpm.videoplayer.source.ext.cast;

/** Listener of changes in the cast session availability. */
public interface SessionAvailabilityListener {

  /** Called when a cast session becomes available to the player. */
  void onCastSessionAvailable();

  /** Called when the cast session becomes unavailable. */
  void onCastSessionUnavailable();
}
