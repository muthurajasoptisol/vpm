/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.source;

import com.adt.vpm.videoplayer.source.core.FormatHolder;
import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.decoder.DecoderInputBuffer;

/**
 * An empty {@link SampleStream}.
 */
public final class EmptySampleStream implements SampleStream {

  @Override
  public boolean isReady() {
    return true;
  }

  @Override
  public void maybeThrowError() {
    // Do nothing.
  }

  @Override
  public int readData(FormatHolder formatHolder, DecoderInputBuffer buffer,
                      boolean formatRequired) {
    buffer.setFlags(C.BUFFER_FLAG_END_OF_STREAM);
    return C.RESULT_BUFFER_READ;
  }

  @Override
  public int skipData(long positionUs) {
    return 0;
  }
}
