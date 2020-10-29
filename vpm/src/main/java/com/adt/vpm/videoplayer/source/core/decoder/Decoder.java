/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.decoder;

import androidx.annotation.Nullable;

/**
 * A media decoder.
 *
 * @param <I> The type of buffer input to the decoder.
 * @param <O> The type of buffer output from the decoder.
 * @param <E> The type of exception thrown from the decoder.
 */
public interface Decoder<I, O, E extends DecoderException> {

  /**
   * Returns the name of the decoder.
   *
   * @return The name of the decoder.
   */
  String getName();

  /**
   * Dequeues the next input buffer to be filled and queued to the decoder.
   *
   * @return The input buffer, which will have been cleared, or null if a buffer isn't available.
   * @throws E If a decoder error has occurred.
   */
  @Nullable
  I dequeueInputBuffer() throws E;

  /**
   * Queues an input buffer to the decoder.
   *
   * @param inputBuffer The input buffer.
   * @throws E If a decoder error has occurred.
   */
  void queueInputBuffer(I inputBuffer) throws E;

  /**
   * Dequeues the next output buffer from the decoder.
   *
   * @return The output buffer, or null if an output buffer isn't available.
   * @throws E If a decoder error has occurred.
   */
  @Nullable
  O dequeueOutputBuffer() throws E;

  /**
   * Flushes the decoder. Ownership of dequeued input buffers is returned to the decoder. The caller
   * is still responsible for releasing any dequeued output buffers.
   */
  void flush();

  /**
   * Releases the decoder. Must be called when the decoder is no longer needed.
   */
  void release();

}
