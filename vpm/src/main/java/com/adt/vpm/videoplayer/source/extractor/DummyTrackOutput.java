/*
 * Created by ADT author on 9/29/20 5:06 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 5:06 PM
 */
package com.adt.vpm.videoplayer.source.extractor;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.Format;
import com.adt.vpm.videoplayer.source.common.upstream.DataReader;
import com.adt.vpm.videoplayer.source.common.util.ParsableByteArray;

import java.io.EOFException;
import java.io.IOException;

import static java.lang.Math.min;

/** A fake {@link TrackOutput} implementation. */
public final class DummyTrackOutput implements TrackOutput {

  // Even though read data is discarded, data source implementations could be making use of the
  // buffer contents. For example, caches. So we cannot use a static field for this which could be
  // shared between different threads.
  private final byte[] readBuffer;

  public DummyTrackOutput() {
    readBuffer = new byte[4096];
  }

  @Override
  public void format(Format format) {
    // Do nothing.
  }

  @Override
  public int sampleData(
      DataReader input, int length, boolean allowEndOfInput, @SampleDataPart int sampleDataPart)
      throws IOException {
    int bytesToSkipByReading = min(readBuffer.length, length);
    int bytesSkipped = input.read(readBuffer, /* offset= */ 0, bytesToSkipByReading);
    if (bytesSkipped == C.RESULT_END_OF_INPUT) {
      if (allowEndOfInput) {
        return C.RESULT_END_OF_INPUT;
      }
      throw new EOFException();
    }
    return bytesSkipped;
  }

  @Override
  public void sampleData(ParsableByteArray data, int length, @SampleDataPart int sampleDataPart) {
    data.skipBytes(length);
  }

  @Override
  public void sampleMetadata(
      long timeUs,
      @C.BufferFlags int flags,
      int size,
      int offset,
      @Nullable CryptoData cryptoData) {
    // Do nothing.
  }
}
