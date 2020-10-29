/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.upstream;

import android.net.Uri;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.upstream.DataSource;
import com.adt.vpm.videoplayer.source.common.upstream.DataSpec;
import com.adt.vpm.videoplayer.source.common.upstream.TransferListener;
import com.adt.vpm.videoplayer.source.common.util.Assertions;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Tees data into a {@link DataSink} as the data is read.
 */
public final class TeeDataSource implements DataSource {

  private final DataSource upstream;
  private final DataSink dataSink;

  private boolean dataSinkNeedsClosing;
  private long bytesRemaining;

  /**
   * @param upstream The upstream {@link DataSource}.
   * @param dataSink The {@link DataSink} into which data is written.
   */
  public TeeDataSource(DataSource upstream, DataSink dataSink) {
    this.upstream = Assertions.checkNotNull(upstream);
    this.dataSink = Assertions.checkNotNull(dataSink);
  }

  @Override
  public void addTransferListener(TransferListener transferListener) {
    Assertions.checkNotNull(transferListener);
    upstream.addTransferListener(transferListener);
  }

  @Override
  public long open(DataSpec dataSpec) throws IOException {
    bytesRemaining = upstream.open(dataSpec);
    if (bytesRemaining == 0) {
      return 0;
    }
    if (dataSpec.length == C.LENGTH_UNSET && bytesRemaining != C.LENGTH_UNSET) {
      // Reconstruct dataSpec in order to provide the resolved length to the sink.
      dataSpec = dataSpec.subrange(0, bytesRemaining);
    }
    dataSinkNeedsClosing = true;
    dataSink.open(dataSpec);
    return bytesRemaining;
  }

  @Override
  public int read(byte[] buffer, int offset, int max) throws IOException {
    if (bytesRemaining == 0) {
      return C.RESULT_END_OF_INPUT;
    }
    int bytesRead = upstream.read(buffer, offset, max);
    if (bytesRead > 0) {
      // TODO: Consider continuing even if writes to the sink fail.
      dataSink.write(buffer, offset, bytesRead);
      if (bytesRemaining != C.LENGTH_UNSET) {
        bytesRemaining -= bytesRead;
      }
    }
    return bytesRead;
  }

  @Override
  @Nullable
  public Uri getUri() {
    return upstream.getUri();
  }

  @Override
  public Map<String, List<String>> getResponseHeaders() {
    return upstream.getResponseHeaders();
  }

  @Override
  public void close() throws IOException {
    try {
      upstream.close();
    } finally {
      if (dataSinkNeedsClosing) {
        dataSinkNeedsClosing = false;
        dataSink.close();
      }
    }
  }

}
