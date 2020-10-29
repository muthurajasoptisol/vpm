/*
 * Created by ADT author on 10/1/20 11:19 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/22/20 6:23 PM
 */
package com.adt.vpm.videoplayer.source.hls;

import androidx.annotation.Nullable;

import java.io.IOException;

/** Thrown when it is not possible to map a {@link TrackGroup} to a {@link SampleQueue}. */
public final class SampleQueueMappingException extends IOException {

  /** @param mimeType The mime type of the track group whose mapping failed. */
  public SampleQueueMappingException(@Nullable String mimeType) {
    super("Unable to bind a sample queue to TrackGroup with mime type " + mimeType + ".");
  }
}
