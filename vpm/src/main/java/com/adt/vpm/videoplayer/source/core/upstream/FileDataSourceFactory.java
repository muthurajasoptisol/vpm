/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.upstream;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.common.upstream.DataSource;
import com.adt.vpm.videoplayer.source.common.upstream.TransferListener;

/** @deprecated Use {@link FileDataSource.Factory}. */
@Deprecated
public final class FileDataSourceFactory implements DataSource.Factory {

  private final FileDataSource.Factory wrappedFactory;

  public FileDataSourceFactory() {
    this(/* listener= */ null);
  }

  public FileDataSourceFactory(@Nullable TransferListener listener) {
    wrappedFactory = new FileDataSource.Factory().setListener(listener);
  }

  @Override
  public FileDataSource createDataSource() {
    return wrappedFactory.createDataSource();
  }
}
