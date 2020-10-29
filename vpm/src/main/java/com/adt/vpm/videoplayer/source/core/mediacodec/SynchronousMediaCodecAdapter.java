/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */

package com.adt.vpm.videoplayer.source.core.mediacodec;

import android.media.MediaCodec;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.view.Surface;
import androidx.annotation.Nullable;
import com.adt.vpm.videoplayer.source.common.decoder.CryptoInfo;

/**
 * A {@link MediaCodecAdapter} that operates the underlying {@link MediaCodec} in synchronous mode.
 */
/* package */ final class SynchronousMediaCodecAdapter implements MediaCodecAdapter {

  private final MediaCodec codec;

  public SynchronousMediaCodecAdapter(MediaCodec mediaCodec) {
    this.codec = mediaCodec;
  }

  @Override
  public void configure(
      @Nullable MediaFormat mediaFormat,
      @Nullable Surface surface,
      @Nullable MediaCrypto crypto,
      int flags) {
    codec.configure(mediaFormat, surface, crypto, flags);
  }

  @Override
  public void start() {
    codec.start();
  }

  @Override
  public int dequeueInputBufferIndex() {
    return codec.dequeueInputBuffer(0);
  }

  @Override
  public int dequeueOutputBufferIndex(MediaCodec.BufferInfo bufferInfo) {
    return codec.dequeueOutputBuffer(bufferInfo, 0);
  }

  @Override
  public MediaFormat getOutputFormat() {
    return codec.getOutputFormat();
  }

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
  public void flush() {
    codec.flush();
  }

  @Override
  public void shutdown() {}

  @Override
  public MediaCodec getCodec() {
    return codec;
  }
}
