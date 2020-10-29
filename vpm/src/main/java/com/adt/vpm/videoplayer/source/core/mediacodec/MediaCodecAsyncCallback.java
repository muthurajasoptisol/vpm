/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.mediacodec;

import android.media.MediaCodec;
import android.media.MediaFormat;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;

import com.adt.vpm.videoplayer.source.core.util.IntArrayQueue;

import java.util.ArrayDeque;

/** Handles the asynchronous callbacks from {@link android.media.MediaCodec.Callback}. */
@RequiresApi(21)
/* package */ final class MediaCodecAsyncCallback extends MediaCodec.Callback {
  private final IntArrayQueue availableInputBuffers;
  private final IntArrayQueue availableOutputBuffers;
  private final ArrayDeque<MediaCodec.BufferInfo> bufferInfos;
  private final ArrayDeque<MediaFormat> formats;
  @Nullable private MediaFormat currentFormat;
  @Nullable private MediaFormat pendingOutputFormat;
  @Nullable private IllegalStateException mediaCodecException;

  /** Creates a new MediaCodecAsyncCallback. */
  public MediaCodecAsyncCallback() {
    availableInputBuffers = new IntArrayQueue();
    availableOutputBuffers = new IntArrayQueue();
    bufferInfos = new ArrayDeque<>();
    formats = new ArrayDeque<>();
  }

  /**
   * Returns the next available input buffer index or {@link MediaCodec#INFO_TRY_AGAIN_LATER} if no
   * such buffer exists.
   */
  public int dequeueInputBufferIndex() {
    return availableInputBuffers.isEmpty()
        ? MediaCodec.INFO_TRY_AGAIN_LATER
        : availableInputBuffers.remove();
  }

  /**
   * Returns the next available output buffer index. If the next available output is a MediaFormat
   * change, it will return {@link MediaCodec#INFO_OUTPUT_FORMAT_CHANGED} and you should call {@link
   * #getOutputFormat()} to get the format. If there is no available output, this method will return
   * {@link MediaCodec#INFO_TRY_AGAIN_LATER}.
   */
  public int dequeueOutputBufferIndex(MediaCodec.BufferInfo bufferInfo) {
    if (availableOutputBuffers.isEmpty()) {
      return MediaCodec.INFO_TRY_AGAIN_LATER;
    } else {
      int bufferIndex = availableOutputBuffers.remove();
      if (bufferIndex >= 0) {
        MediaCodec.BufferInfo nextBufferInfo = bufferInfos.remove();
        bufferInfo.set(
            nextBufferInfo.offset,
            nextBufferInfo.size,
            nextBufferInfo.presentationTimeUs,
            nextBufferInfo.flags);
      } else if (bufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
        currentFormat = formats.remove();
      }
      return bufferIndex;
    }
  }

  /**
   * Returns the {@link MediaFormat} signalled by the underlying {@link MediaCodec}.
   *
   * <p>Call this <b>after</b> {@link #dequeueOutputBufferIndex} returned {@link
   * MediaCodec#INFO_OUTPUT_FORMAT_CHANGED}.
   *
   * @throws IllegalStateException If called before {@link #dequeueOutputBufferIndex} has returned
   *     {@link MediaCodec#INFO_OUTPUT_FORMAT_CHANGED}.
   */
  public MediaFormat getOutputFormat() throws IllegalStateException {
    if (currentFormat == null) {
      throw new IllegalStateException();
    }
    return currentFormat;
  }

  /**
   * Checks and throws an {@link IllegalStateException} if an error was previously set on this
   * instance via {@link #onError}.
   */
  public void maybeThrowMediaCodecException() throws IllegalStateException {
    IllegalStateException exception = mediaCodecException;
    mediaCodecException = null;
    if (exception != null) {
      throw exception;
    }
  }

  /**
   * Flushes the MediaCodecAsyncCallback. This method removes all available input and output buffers
   * and any error that was previously set.
   */
  public void flush() {
    pendingOutputFormat = formats.isEmpty() ? null : formats.getLast();
    availableInputBuffers.clear();
    availableOutputBuffers.clear();
    bufferInfos.clear();
    formats.clear();
    mediaCodecException = null;
  }

  @Override
  public void onInputBufferAvailable(MediaCodec mediaCodec, int index) {
    availableInputBuffers.add(index);
  }

  @Override
  public void onOutputBufferAvailable(
      MediaCodec mediaCodec, int index, MediaCodec.BufferInfo bufferInfo) {
    if (pendingOutputFormat != null) {
      addOutputFormat(pendingOutputFormat);
      pendingOutputFormat = null;
    }
    availableOutputBuffers.add(index);
    bufferInfos.add(bufferInfo);
  }

  @Override
  public void onError(MediaCodec mediaCodec, MediaCodec.CodecException e) {
    onMediaCodecError(e);
  }

  @Override
  public void onOutputFormatChanged(MediaCodec mediaCodec, MediaFormat mediaFormat) {
    addOutputFormat(mediaFormat);
    pendingOutputFormat = null;
  }

  @VisibleForTesting()
  void onMediaCodecError(IllegalStateException e) {
    mediaCodecException = e;
  }

  private void addOutputFormat(MediaFormat mediaFormat) {
    availableOutputBuffers.add(MediaCodec.INFO_OUTPUT_FORMAT_CHANGED);
    formats.add(mediaFormat);
  }
}
