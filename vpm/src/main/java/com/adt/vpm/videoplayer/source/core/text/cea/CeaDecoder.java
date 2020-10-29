/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.text.cea;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.core.text.Subtitle;
import com.adt.vpm.videoplayer.source.core.text.SubtitleDecoder;
import com.adt.vpm.videoplayer.source.core.text.SubtitleDecoderException;
import com.adt.vpm.videoplayer.source.core.text.SubtitleInputBuffer;
import com.adt.vpm.videoplayer.source.core.text.SubtitleOutputBuffer;
import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.Format;
import com.adt.vpm.videoplayer.source.common.util.Assertions;
import com.adt.vpm.videoplayer.source.common.util.Util;
import java.util.ArrayDeque;
import java.util.PriorityQueue;

/**
 * Base class for subtitle parsers for CEA captions.
 */
/* package */ abstract class CeaDecoder implements SubtitleDecoder {

  private static final int NUM_INPUT_BUFFERS = 10;
  private static final int NUM_OUTPUT_BUFFERS = 2;

  private final ArrayDeque<CeaInputBuffer> availableInputBuffers;
  private final ArrayDeque<SubtitleOutputBuffer> availableOutputBuffers;
  private final PriorityQueue<CeaInputBuffer> queuedInputBuffers;

  @Nullable private CeaInputBuffer dequeuedInputBuffer;
  private long playbackPositionUs;
  private long queuedInputBufferCount;

  @SuppressWarnings("nullness:methodref.receiver.bound.invalid")
  public CeaDecoder() {
    availableInputBuffers = new ArrayDeque<>();
    for (int i = 0; i < NUM_INPUT_BUFFERS; i++) {
      availableInputBuffers.add(new CeaInputBuffer());
    }
    availableOutputBuffers = new ArrayDeque<>();
    for (int i = 0; i < NUM_OUTPUT_BUFFERS; i++) {
      availableOutputBuffers.add(new CeaOutputBuffer(this::releaseOutputBuffer));
    }
    queuedInputBuffers = new PriorityQueue<>();
  }

  @Override
  public abstract String getName();

  @Override
  public void setPositionUs(long positionUs) {
    playbackPositionUs = positionUs;
  }

  @Override
  @Nullable
  public SubtitleInputBuffer dequeueInputBuffer() throws SubtitleDecoderException {
    Assertions.checkState(dequeuedInputBuffer == null);
    if (availableInputBuffers.isEmpty()) {
      return null;
    }
    dequeuedInputBuffer = availableInputBuffers.pollFirst();
    return dequeuedInputBuffer;
  }

  @Override
  public void queueInputBuffer(SubtitleInputBuffer inputBuffer) throws SubtitleDecoderException {
    Assertions.checkArgument(inputBuffer == dequeuedInputBuffer);
    CeaInputBuffer ceaInputBuffer = (CeaInputBuffer) inputBuffer;
    if (ceaInputBuffer.isDecodeOnly()) {
      // We can start decoding anywhere in CEA formats, so discarding on the input side is fine.
      releaseInputBuffer(ceaInputBuffer);
    } else {
      ceaInputBuffer.queuedInputBufferCount = queuedInputBufferCount++;
      queuedInputBuffers.add(ceaInputBuffer);
    }
    dequeuedInputBuffer = null;
  }

  @Override
  @Nullable
  public SubtitleOutputBuffer dequeueOutputBuffer() throws SubtitleDecoderException {
    if (availableOutputBuffers.isEmpty()) {
      return null;
    }
    // Process input buffers up to the current playback position. Processing of input buffers for
    // future content is deferred.
    while (!queuedInputBuffers.isEmpty()
        && Util.castNonNull(queuedInputBuffers.peek()).timeUs <= playbackPositionUs) {
      CeaInputBuffer inputBuffer = Util.castNonNull(queuedInputBuffers.poll());

      if (inputBuffer.isEndOfStream()) {
        // availableOutputBuffers.isEmpty() is checked at the top of the method, so this is safe.
        SubtitleOutputBuffer outputBuffer = Util.castNonNull(availableOutputBuffers.pollFirst());
        outputBuffer.addFlag(C.BUFFER_FLAG_END_OF_STREAM);
        releaseInputBuffer(inputBuffer);
        return outputBuffer;
      }

      decode(inputBuffer);

      if (isNewSubtitleDataAvailable()) {
        Subtitle subtitle = createSubtitle();
        // availableOutputBuffers.isEmpty() is checked at the top of the method, so this is safe.
        SubtitleOutputBuffer outputBuffer = Util.castNonNull(availableOutputBuffers.pollFirst());
        outputBuffer.setContent(inputBuffer.timeUs, subtitle, Format.OFFSET_SAMPLE_RELATIVE);
        releaseInputBuffer(inputBuffer);
        return outputBuffer;
      }

      releaseInputBuffer(inputBuffer);
    }
    return null;
  }

  private void releaseInputBuffer(CeaInputBuffer inputBuffer) {
    inputBuffer.clear();
    availableInputBuffers.add(inputBuffer);
  }

  protected void releaseOutputBuffer(SubtitleOutputBuffer outputBuffer) {
    outputBuffer.clear();
    availableOutputBuffers.add(outputBuffer);
  }

  @Override
  public void flush() {
    queuedInputBufferCount = 0;
    playbackPositionUs = 0;
    while (!queuedInputBuffers.isEmpty()) {
      releaseInputBuffer(Util.castNonNull(queuedInputBuffers.poll()));
    }
    if (dequeuedInputBuffer != null) {
      releaseInputBuffer(dequeuedInputBuffer);
      dequeuedInputBuffer = null;
    }
  }

  @Override
  public void release() {
    // Do nothing.
  }

  /**
   * Returns whether there is data available to create a new {@link Subtitle}.
   */
  protected abstract boolean isNewSubtitleDataAvailable();

  /**
   * Creates a {@link Subtitle} from the available data.
   */
  protected abstract Subtitle createSubtitle();

  /**
   * Filters and processes the raw data, providing {@link Subtitle}s via {@link #createSubtitle()}
   * when sufficient data has been processed.
   */
  protected abstract void decode(SubtitleInputBuffer inputBuffer);

  @Nullable
  protected final SubtitleOutputBuffer getAvailableOutputBuffer() {
    return availableOutputBuffers.pollFirst();
  }

  protected final long getPositionUs() {
    return playbackPositionUs;
  }

  private static final class CeaInputBuffer extends SubtitleInputBuffer
      implements Comparable<CeaInputBuffer> {

    private long queuedInputBufferCount;

    @Override
    public int compareTo(CeaInputBuffer other) {
      if (isEndOfStream() != other.isEndOfStream()) {
        return isEndOfStream() ? 1 : -1;
      }
      long delta = timeUs - other.timeUs;
      if (delta == 0) {
        delta = queuedInputBufferCount - other.queuedInputBufferCount;
        if (delta == 0) {
          return 0;
        }
      }
      return delta > 0 ? 1 : -1;
    }
  }

  private static final class CeaOutputBuffer extends SubtitleOutputBuffer {

    private Owner<CeaOutputBuffer> owner;

    public CeaOutputBuffer(Owner<CeaOutputBuffer> owner) {
      this.owner = owner;
    }

    @Override
    public final void release() {
      owner.releaseOutputBuffer(this);
    }
  }
}
