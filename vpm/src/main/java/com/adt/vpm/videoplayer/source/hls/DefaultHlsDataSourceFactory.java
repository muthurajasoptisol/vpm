/*
 * Created by ADT author on 10/1/20 11:19 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/22/20 6:23 PM
 */
package com.adt.vpm.videoplayer.source.hls;

import com.adt.vpm.videoplayer.source.common.upstream.DataSource;

/**
 * Default implementation of {@link HlsDataSourceFactory}.
 */
public final class DefaultHlsDataSourceFactory implements HlsDataSourceFactory {

  private final DataSource.Factory dataSourceFactory;

  /**
   * @param dataSourceFactory The {@link DataSource.Factory} to use for all data types.
   */
  public DefaultHlsDataSourceFactory(DataSource.Factory dataSourceFactory) {
    this.dataSourceFactory = dataSourceFactory;
  }

  @Override
  public DataSource createDataSource(int dataType) {
    return dataSourceFactory.createDataSource();
  }

}
