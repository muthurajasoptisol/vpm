/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.decoder;

import com.adt.vpm.videoplayer.source.common.decoder.Buffer;

/**
 * Output buffer decoded by a {@link Decoder}.
 */
public abstract class OutputBuffer extends Buffer {

  /** Buffer owner. */
  public interface Owner<S extends OutputBuffer> {

    /**
     * Releases the buffer.
     *
     * @param outputBuffer Output buffer.
     */
    void releaseOutputBuffer(S outputBuffer);
  }

  /**
   * The presentation timestamp for the buffer, in microseconds.
   */
  public long timeUs;

  /**
   * The number of buffers immediately prior to this one that were skipped in the {@link Decoder}.
   */
  public int skippedOutputBufferCount;

  /**
   * Releases the output buffer for reuse. Must be called when the buffer is no longer needed.
   */
  public abstract void release();
}
