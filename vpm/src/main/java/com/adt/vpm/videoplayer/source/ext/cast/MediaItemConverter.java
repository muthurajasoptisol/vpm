/*
 * Created by ADT author on 10/19/20 4:50 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/22/20 6:23 PM
 */
package com.adt.vpm.videoplayer.source.ext.cast;

import com.adt.vpm.videoplayer.source.common.MediaItem;
import com.google.android.gms.cast.MediaQueueItem;

/** Converts between {@link MediaItem} and the Cast SDK's {@link MediaQueueItem}. */
public interface MediaItemConverter {

  /**
   * Converts a {@link MediaItem} to a {@link MediaQueueItem}.
   *
   * @param mediaItem The {@link MediaItem}.
   * @return An equivalent {@link MediaQueueItem}.
   */
  MediaQueueItem toMediaQueueItem(MediaItem mediaItem);

  /**
   * Converts a {@link MediaQueueItem} to a {@link MediaItem}.
   *
   * @param mediaQueueItem The {@link MediaQueueItem}.
   * @return The equivalent {@link MediaItem}.
   */
  MediaItem toMediaItem(MediaQueueItem mediaQueueItem);
}
