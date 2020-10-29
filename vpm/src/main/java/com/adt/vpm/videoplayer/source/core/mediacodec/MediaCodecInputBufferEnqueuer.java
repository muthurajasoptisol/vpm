/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */

package com.adt.vpm.videoplayer.source.core.mediacodec;

import android.media.MediaCodec;
import com.adt.vpm.videoplayer.source.common.decoder.CryptoInfo;

/** Abstracts operations to enqueue input buffer on a {@link android.media.MediaCodec}. */
interface MediaCodecInputBufferEnqueuer {

  /**
   * Starts this instance.
   *
   * <p>Call this method after creating an instance.
   */
  void start();

  /**
   * Submits an input buffer for decoding.
   *
   * @see android.media.MediaCodec#queueInputBuffer
   */
  void queueInputBuffer(int index, int offset, int size, long presentationTimeUs, int flags);

  /**
   * Submits an input buffer that potentially contains encrypted data for decoding.
   *
   * <p>Note: This method behaves as {@link MediaCodec#queueSecureInputBuffer} with the difference
   * that {@code info} is of type {@link CryptoInfo} and not {@link
   * android.media.MediaCodec.CryptoInfo}.
   *
   * @see android.media.MediaCodec#queueSecureInputBuffer
   */
  void queueSecureInputBuffer(
      int index, int offset, CryptoInfo info, long presentationTimeUs, int flags);

  /** Flushes the instance. */
  void flush();

  /** Shut down the instance. Make sure to call this method to release its internal resources. */
  void shutdown();
}
