/*
 * Created by ADT author on 10/1/20 11:19 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/22/20 6:23 PM
 */
package com.adt.vpm.videoplayer.source.hls;

import com.adt.vpm.videoplayer.source.common.upstream.DataSource;

/**
 * Creates {@link DataSource}s for HLS playlists, encryption and media chunks.
 */
public interface HlsDataSourceFactory {

  /**
   * Creates a {@link DataSource} for the given data type.
   *
   * @param dataType The data type for which the {@link DataSource} will be used. One of {@link C}
   *     {@code .DATA_TYPE_*} constants.
   * @return A {@link DataSource} for the given data type.
   */
  DataSource createDataSource(int dataType);

}
