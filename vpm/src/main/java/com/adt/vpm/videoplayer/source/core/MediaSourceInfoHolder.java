/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core;

import com.adt.vpm.videoplayer.source.core.source.MediaSource;

/** A holder of information about a {@link MediaSource}. */
/* package */ interface MediaSourceInfoHolder {

  /** Returns the uid of the {@link MediaSourceList.MediaSourceHolder}. */
  Object getUid();

  /** Returns the timeline. */
  Timeline getTimeline();
}