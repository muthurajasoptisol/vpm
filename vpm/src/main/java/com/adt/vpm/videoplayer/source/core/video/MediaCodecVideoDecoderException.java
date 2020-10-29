/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.video;

import android.media.MediaCodec;
import android.view.Surface;
import androidx.annotation.Nullable;
import com.adt.vpm.videoplayer.source.core.mediacodec.MediaCodecDecoderException;
import com.adt.vpm.videoplayer.source.core.mediacodec.MediaCodecInfo;

/** Thrown when a failure occurs in a {@link MediaCodec} video decoder. */
public class MediaCodecVideoDecoderException extends MediaCodecDecoderException {

  /** The {@link System#identityHashCode(Object)} of the surface when the exception occurred. */
  public final int surfaceIdentityHashCode;

  /** Whether the surface was valid when the exception occurred. */
  public final boolean isSurfaceValid;

  public MediaCodecVideoDecoderException(
      Throwable cause, @Nullable MediaCodecInfo codecInfo, @Nullable Surface surface) {
    super(cause, codecInfo);
    surfaceIdentityHashCode = System.identityHashCode(surface);
    isSurfaceValid = surface == null || surface.isValid();
  }
}
