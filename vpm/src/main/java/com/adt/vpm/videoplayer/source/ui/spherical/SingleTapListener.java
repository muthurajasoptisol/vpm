/*
 * Created by ADT author on 9/29/20 5:37 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/22/20 6:23 PM
 */
package com.adt.vpm.videoplayer.source.ui.spherical;

import android.view.MotionEvent;

/** Listens tap events on a {@link android.view.View}. */
public interface SingleTapListener {
  /**
   * Notified when a tap occurs with the up {@link MotionEvent} that triggered it.
   *
   * @param e The up motion event that completed the first tap.
   * @return Whether the event is consumed.
   */
  boolean onSingleTapUp(MotionEvent e);
}
