/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.mediacodec;

import android.media.MediaCodec;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.adt.vpm.videoplayer.source.core.decoder.DecoderException;
import com.adt.vpm.videoplayer.source.common.util.Util;

/** Thrown when a failure occurs in a {@link MediaCodec} decoder. */
public class MediaCodecDecoderException extends DecoderException {

  /** The {@link MediaCodecInfo} of the decoder that failed. Null if unknown. */
  @Nullable public final MediaCodecInfo codecInfo;

  /** An optional developer-readable diagnostic information string. May be null. */
  @Nullable public final String diagnosticInfo;

  public MediaCodecDecoderException(Throwable cause, @Nullable MediaCodecInfo codecInfo) {
    super("Decoder failed: " + (codecInfo == null ? null : codecInfo.name), cause);
    this.codecInfo = codecInfo;
    diagnosticInfo = Util.SDK_INT >= 21 ? getDiagnosticInfoV21(cause) : null;
  }

  @RequiresApi(21)
  @Nullable
  private static String getDiagnosticInfoV21(Throwable cause) {
    if (cause instanceof MediaCodec.CodecException) {
      return ((MediaCodec.CodecException) cause).getDiagnosticInfo();
    }
    return null;
  }
}
