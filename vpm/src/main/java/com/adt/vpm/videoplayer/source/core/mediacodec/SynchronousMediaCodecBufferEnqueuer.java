/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */

package com.adt.vpm.videoplayer.source.core.mediacodec;

import android.media.MediaCodec;
import com.adt.vpm.videoplayer.source.common.decoder.CryptoInfo;

/**
 * A {@link MediaCodecInputBufferEnqueuer} that forwards queueing methods directly to {@link
 * MediaCodec}.
 */
class SynchronousMediaCodecBufferEnqueuer implements MediaCodecInputBufferEnqueuer {
  private final MediaCodec codec;

  /**
   * Creates an instance that queues input buffers on the specified {@link MediaCodec}.
   *
   * @param codec The {@link MediaCodec} to submit input buffers to.
   */
  SynchronousMediaCodecBufferEnqueuer(MediaCodec codec) {
    this.codec = codec;
  }

  @Override
  public void start() {}

  @Override
  public void queueInputBuffer(
      int index, int offset, int size, long presentationTimeUs, int flags) {
    codec.queueInputBuffer(index, offset, size, presentationTimeUs, flags);
  }

  @Override
  public void queueSecureInputBuffer(
      int index, int offset, CryptoInfo info, long presentationTimeUs, int flags) {
    codec.queueSecureInputBuffer(
        index, offset, info.getFrameworkCryptoInfo(), presentationTimeUs, flags);
  }

  @Override
  public void flush() {}

  @Override
  public void shutdown() {}
}
