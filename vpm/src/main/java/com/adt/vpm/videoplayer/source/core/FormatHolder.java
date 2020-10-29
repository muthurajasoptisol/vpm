/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.common.Format;
import com.adt.vpm.videoplayer.source.core.drm.DrmSession;

/**
 * Holds a {@link Format}.
 */
public final class FormatHolder {

  /** An accompanying context for decrypting samples in the format. */
  @Nullable public DrmSession drmSession;

  /** The held {@link Format}. */
  @Nullable public Format format;

  /** Clears the holder. */
  public void clear() {
    drmSession = null;
    format = null;
  }
}
