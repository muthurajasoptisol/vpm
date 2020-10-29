/*
 * Created by ADT author on 9/29/20 5:37 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/22/20 6:23 PM
 */
package com.adt.vpm.videoplayer.source.ui;

import com.adt.vpm.videoplayer.source.common.Format;

/** Converts {@link Format}s to user readable track names. */
public interface TrackNameProvider {

  /** Returns a user readable track name for the given {@link Format}. */
  String getTrackName(Format format);
}
