/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.audio;

import androidx.annotation.CallSuper;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Base class for audio processors that keep an output buffer and an internal buffer that is reused
 * whenever input is queued. Subclasses should override {@link #onConfigure(AudioFormat)} to return
 * the output audio format for the processor if it's active.
 */
public abstract class BaseAudioProcessor implements AudioProcessor {

  /** The current input audio format. */
  protected AudioFormat inputAudioFormat;
  /** The current output audio format. */
  protected AudioFormat outputAudioFormat;

  private AudioFormat pendingInputAudioFormat;
  private AudioFormat pendingOutputAudioFormat;
  private ByteBuffer buffer;
  private ByteBuffer outputBuffer;
  private boolean inputEnded;

  public BaseAudioProcessor() {
    buffer = EMPTY_BUFFER;
    outputBuffer = EMPTY_BUFFER;
    pendingInputAudioFormat = AudioFormat.NOT_SET;
    pendingOutputAudioFormat = AudioFormat.NOT_SET;
    inputAudioFormat = AudioFormat.NOT_SET;
    outputAudioFormat = AudioFormat.NOT_SET;
  }

  @Override
  public final AudioFormat configure(AudioFormat inputAudioFormat)
      throws UnhandledAudioFormatException {
    pendingInputAudioFormat = inputAudioFormat;
    pendingOutputAudioFormat = onConfigure(inputAudioFormat);
    return isActive() ? pendingOutputAudioFormat : AudioFormat.NOT_SET;
  }

  @Override
  public boolean isActive() {
    return pendingOutputAudioFormat != AudioFormat.NOT_SET;
  }

  @Override
  public final void queueEndOfStream() {
    inputEnded = true;
    onQueueEndOfStream();
  }

  @CallSuper
  @Override
  public ByteBuffer getOutput() {
    ByteBuffer outputBuffer = this.outputBuffer;
    this.outputBuffer = EMPTY_BUFFER;
    return outputBuffer;
  }

  @CallSuper
  @SuppressWarnings("ReferenceEquality")
  @Override
  public boolean isEnded() {
    return inputEnded && outputBuffer == EMPTY_BUFFER;
  }

  @Override
  public final void flush() {
    outputBuffer = EMPTY_BUFFER;
    inputEnded = false;
    inputAudioFormat = pendingInputAudioFormat;
    outputAudioFormat = pendingOutputAudioFormat;
    onFlush();
  }

  @Override
  public final void reset() {
    flush();
    buffer = EMPTY_BUFFER;
    pendingInputAudioFormat = AudioFormat.NOT_SET;
    pendingOutputAudioFormat = AudioFormat.NOT_SET;
    inputAudioFormat = AudioFormat.NOT_SET;
    outputAudioFormat = AudioFormat.NOT_SET;
    onReset();
  }

  /**
   * Replaces the current output buffer with a buffer of at least {@code count} bytes and returns
   * it. Callers should write to the returned buffer then {@link ByteBuffer#flip()} it so it can be
   * read via {@link #getOutput()}.
   */
  protected final ByteBuffer replaceOutputBuffer(int count) {
    if (buffer.capacity() < count) {
      buffer = ByteBuffer.allocateDirect(count).order(ByteOrder.nativeOrder());
    } else {
      buffer.clear();
    }
    outputBuffer = buffer;
    return buffer;
  }

  /** Returns whether the current output buffer has any data remaining. */
  protected final boolean hasPendingOutput() {
    return outputBuffer.hasRemaining();
  }

  /** Called when the processor is configured for a new input format. */
  protected AudioFormat onConfigure(AudioFormat inputAudioFormat)
      throws UnhandledAudioFormatException {
    return AudioFormat.NOT_SET;
  }

  /** Called when the end-of-stream is queued to the processor. */
  protected void onQueueEndOfStream() {
    // Do nothing.
  }

  /** Called when the processor is flushed, directly or as part of resetting. */
  protected void onFlush() {
    // Do nothing.
  }

  /** Called when the processor is reset. */
  protected void onReset() {
    // Do nothing.
  }
}
