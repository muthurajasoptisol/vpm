/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.util;

import com.adt.vpm.videoplayer.source.common.util.Assertions;
import com.adt.vpm.videoplayer.source.common.util.Util;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This is a subclass of {@link BufferedOutputStream} with a {@link #reset(OutputStream)} method
 * that allows an instance to be re-used with another underlying output stream.
 */
public final class ReusableBufferedOutputStream extends BufferedOutputStream {

  private boolean closed;

  public ReusableBufferedOutputStream(OutputStream out) {
    super(out);
  }

  public ReusableBufferedOutputStream(OutputStream out, int size) {
    super(out, size);
  }

  @Override
  public void close() throws IOException {
    closed = true;

    Throwable thrown = null;
    try {
      flush();
    } catch (Throwable e) {
      thrown = e;
    }
    try {
      out.close();
    } catch (Throwable e) {
      if (thrown == null) {
        thrown = e;
      }
    }
    if (thrown != null) {
      Util.sneakyThrow(thrown);
    }
  }

  /**
   * Resets this stream and uses the given output stream for writing. This stream must be closed
   * before resetting.
   *
   * @param out New output stream to be used for writing.
   * @throws IllegalStateException If the stream isn't closed.
   */
  public void reset(OutputStream out) {
    Assertions.checkState(closed);
    this.out = out;
    count = 0;
    closed = false;
  }
}
