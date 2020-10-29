/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.mediacodec;

import android.media.MediaFormat;
import androidx.annotation.Nullable;
import com.adt.vpm.videoplayer.source.common.Format;
import com.adt.vpm.videoplayer.source.common.video.ColorInfo;
import java.nio.ByteBuffer;
import java.util.List;

/** Helper class for configuring {@link MediaFormat} instances. */
public final class MediaFormatUtil {

  private MediaFormatUtil() {}

  /**
   * Sets a {@link MediaFormat} {@link String} value.
   *
   * @param format The {@link MediaFormat} being configured.
   * @param key The key to set.
   * @param value The value to set.
   */
  public static void setString(MediaFormat format, String key, String value) {
    format.setString(key, value);
  }

  /**
   * Sets a {@link MediaFormat}'s codec specific data buffers.
   *
   * @param format The {@link MediaFormat} being configured.
   * @param csdBuffers The csd buffers to set.
   */
  public static void setCsdBuffers(MediaFormat format, List<byte[]> csdBuffers) {
    for (int i = 0; i < csdBuffers.size(); i++) {
      format.setByteBuffer("csd-" + i, ByteBuffer.wrap(csdBuffers.get(i)));
    }
  }

  /**
   * Sets a {@link MediaFormat} integer value. Does nothing if {@code value} is {@link
   * Format#NO_VALUE}.
   *
   * @param format The {@link MediaFormat} being configured.
   * @param key The key to set.
   * @param value The value to set.
   */
  public static void maybeSetInteger(MediaFormat format, String key, int value) {
    if (value != Format.NO_VALUE) {
      format.setInteger(key, value);
    }
  }

  /**
   * Sets a {@link MediaFormat} float value. Does nothing if {@code value} is {@link
   * Format#NO_VALUE}.
   *
   * @param format The {@link MediaFormat} being configured.
   * @param key The key to set.
   * @param value The value to set.
   */
  public static void maybeSetFloat(MediaFormat format, String key, float value) {
    if (value != Format.NO_VALUE) {
      format.setFloat(key, value);
    }
  }

  /**
   * Sets a {@link MediaFormat} {@link ByteBuffer} value. Does nothing if {@code value} is null.
   *
   * @param format The {@link MediaFormat} being configured.
   * @param key The key to set.
   * @param value The byte array that will be wrapped to obtain the value.
   */
  public static void maybeSetByteBuffer(MediaFormat format, String key, @Nullable byte[] value) {
    if (value != null) {
      format.setByteBuffer(key, ByteBuffer.wrap(value));
    }
  }

  /**
   * Sets a {@link MediaFormat}'s color information. Does nothing if {@code colorInfo} is null.
   *
   * @param format The {@link MediaFormat} being configured.
   * @param colorInfo The color info to set.
   */
  @SuppressWarnings("InlinedApi")
  public static void maybeSetColorInfo(MediaFormat format, @Nullable ColorInfo colorInfo) {
    if (colorInfo != null) {
      maybeSetInteger(format, MediaFormat.KEY_COLOR_TRANSFER, colorInfo.colorTransfer);
      maybeSetInteger(format, MediaFormat.KEY_COLOR_STANDARD, colorInfo.colorSpace);
      maybeSetInteger(format, MediaFormat.KEY_COLOR_RANGE, colorInfo.colorRange);
      maybeSetByteBuffer(format, MediaFormat.KEY_HDR_STATIC_INFO, colorInfo.hdrStaticInfo);
    }
  }
}
