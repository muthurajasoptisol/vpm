/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.decoder;

import androidx.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Buffer for {@link SimpleDecoder} output.
 */
public class SimpleOutputBuffer extends OutputBuffer {

  private final Owner<SimpleOutputBuffer> owner;

  @Nullable public ByteBuffer data;

  public SimpleOutputBuffer(Owner<SimpleOutputBuffer> owner) {
    this.owner = owner;
  }

  /**
   * Initializes the buffer.
   *
   * @param timeUs The presentation timestamp for the buffer, in microseconds.
   * @param size An upper bound on the size of the data that will be written to the buffer.
   * @return The {@link #data} buffer, for convenience.
   */
  public ByteBuffer init(long timeUs, int size) {
    this.timeUs = timeUs;
    if (data == null || data.capacity() < size) {
      data = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
    }
    data.position(0);
    data.limit(size);
    return data;
  }

  @Override
  public void clear() {
    super.clear();
    if (data != null) {
      data.clear();
    }
  }

  @Override
  public void release() {
    owner.releaseOutputBuffer(this);
  }

}
