/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.upstream.cache;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.core.upstream.DataSink;
import com.adt.vpm.videoplayer.source.core.upstream.FileDataSource;
import com.adt.vpm.videoplayer.source.common.upstream.DataSource;

/** @deprecated Use {@link CacheDataSource.Factory}. */
@Deprecated
public final class CacheDataSourceFactory implements DataSource.Factory {

  private final Cache cache;
  private final DataSource.Factory upstreamFactory;
  private final DataSource.Factory cacheReadDataSourceFactory;
  @CacheDataSource.Flags private final int flags;
  @Nullable private final DataSink.Factory cacheWriteDataSinkFactory;
  @Nullable private final CacheDataSource.EventListener eventListener;
  @Nullable private final CacheKeyFactory cacheKeyFactory;

  /**
   * Constructs a factory which creates {@link CacheDataSource} instances with default {@link
   * DataSource} and {@link DataSink} instances for reading and writing the cache.
   *
   * @param cache The cache.
   * @param upstreamFactory A {@link DataSource.Factory} for creating upstream {@link DataSource}s
   *     for reading data not in the cache.
   */
  public CacheDataSourceFactory(Cache cache, DataSource.Factory upstreamFactory) {
    this(cache, upstreamFactory, /* flags= */ 0);
  }

  /** @see CacheDataSource#CacheDataSource(Cache, DataSource, int) */
  public CacheDataSourceFactory(
      Cache cache, DataSource.Factory upstreamFactory, @CacheDataSource.Flags int flags) {
    this(
        cache,
        upstreamFactory,
        new FileDataSource.Factory(),
        new CacheDataSink.Factory().setCache(cache),
        flags,
        /* eventListener= */ null);
  }

  /**
   * @see CacheDataSource#CacheDataSource(Cache, DataSource, DataSource, DataSink, int,
   *     CacheDataSource.EventListener)
   */
  public CacheDataSourceFactory(
      Cache cache,
      DataSource.Factory upstreamFactory,
      DataSource.Factory cacheReadDataSourceFactory,
      @Nullable DataSink.Factory cacheWriteDataSinkFactory,
      @CacheDataSource.Flags int flags,
      @Nullable CacheDataSource.EventListener eventListener) {
    this(
        cache,
        upstreamFactory,
        cacheReadDataSourceFactory,
        cacheWriteDataSinkFactory,
        flags,
        eventListener,
        /* cacheKeyFactory= */ null);
  }

  /**
   * @see CacheDataSource#CacheDataSource(Cache, DataSource, DataSource, DataSink, int,
   *     CacheDataSource.EventListener, CacheKeyFactory)
   */
  public CacheDataSourceFactory(
      Cache cache,
      DataSource.Factory upstreamFactory,
      DataSource.Factory cacheReadDataSourceFactory,
      @Nullable DataSink.Factory cacheWriteDataSinkFactory,
      @CacheDataSource.Flags int flags,
      @Nullable CacheDataSource.EventListener eventListener,
      @Nullable CacheKeyFactory cacheKeyFactory) {
    this.cache = cache;
    this.upstreamFactory = upstreamFactory;
    this.cacheReadDataSourceFactory = cacheReadDataSourceFactory;
    this.cacheWriteDataSinkFactory = cacheWriteDataSinkFactory;
    this.flags = flags;
    this.eventListener = eventListener;
    this.cacheKeyFactory = cacheKeyFactory;
  }

  @Override
  public CacheDataSource createDataSource() {
    return new CacheDataSource(
        cache,
        upstreamFactory.createDataSource(),
        cacheReadDataSourceFactory.createDataSource(),
        cacheWriteDataSinkFactory == null ? null : cacheWriteDataSinkFactory.createDataSink(),
        flags,
        eventListener,
        cacheKeyFactory);
  }

}
