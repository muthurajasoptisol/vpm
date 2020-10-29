/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.upstream;

import android.net.Uri;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.common.upstream.DataSource;
import com.adt.vpm.videoplayer.source.common.upstream.DataSpec;
import com.adt.vpm.videoplayer.source.common.upstream.TransferListener;

import java.io.IOException;

/** A DataSource which provides no data. {@link #open(DataSpec)} throws {@link IOException}. */
public final class DummyDataSource implements DataSource {

  public static final DummyDataSource INSTANCE = new DummyDataSource();

  /** A factory that produces {@link DummyDataSource}. */
  public static final Factory FACTORY = DummyDataSource::new;

  private DummyDataSource() {}

  @Override
  public void addTransferListener(TransferListener transferListener) {
    // Do nothing.
  }

  @Override
  public long open(DataSpec dataSpec) throws IOException {
    throw new IOException("DummyDataSource cannot be opened");
  }

  @Override
  public int read(byte[] buffer, int offset, int readLength) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Nullable
  public Uri getUri() {
    return null;
  }

  @Override
  public void close() {
    // do nothing.
  }
}
