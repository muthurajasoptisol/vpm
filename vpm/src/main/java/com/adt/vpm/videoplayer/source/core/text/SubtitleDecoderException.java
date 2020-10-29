/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.text;

import androidx.annotation.Nullable;
import com.adt.vpm.videoplayer.source.core.decoder.DecoderException;

/** Thrown when an error occurs decoding subtitle data. */
public class SubtitleDecoderException extends DecoderException {

  /**
   * @param message The detail message for this exception.
   */
  public SubtitleDecoderException(String message) {
    super(message);
  }

  /** @param cause The cause of this exception, or {@code null}. */
  public SubtitleDecoderException(@Nullable Throwable cause) {
    super(cause);
  }

  /**
   * @param message The detail message for this exception.
   * @param cause The cause of this exception, or {@code null}.
   */
  public SubtitleDecoderException(String message, @Nullable Throwable cause) {
    super(message, cause);
  }
}
