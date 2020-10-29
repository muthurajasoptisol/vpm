/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.upstream;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.upstream.DataSpec;
import com.adt.vpm.videoplayer.source.common.util.Assertions;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static com.adt.vpm.videoplayer.source.common.util.Util.castNonNull;

/**
 * A {@link DataSink} for writing to a byte array.
 */
public final class ByteArrayDataSink implements DataSink {

  private @MonotonicNonNull ByteArrayOutputStream stream;

  @Override
  public void open(DataSpec dataSpec) {
    if (dataSpec.length == C.LENGTH_UNSET) {
      stream = new ByteArrayOutputStream();
    } else {
      Assertions.checkArgument(dataSpec.length <= Integer.MAX_VALUE);
      stream = new ByteArrayOutputStream((int) dataSpec.length);
    }
  }

  @Override
  public void close() throws IOException {
    castNonNull(stream).close();
  }

  @Override
  public void write(byte[] buffer, int offset, int length) {
    castNonNull(stream).write(buffer, offset, length);
  }

  /**
   * Returns the data written to the sink since the last call to {@link #open(DataSpec)}, or null if
   * {@link #open(DataSpec)} has never been called.
   */
  @Nullable
  public byte[] getData() {
    return stream == null ? null : stream.toByteArray();
  }

}
