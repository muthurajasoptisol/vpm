/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.upstream.cache;

import com.adt.vpm.videoplayer.source.core.upstream.DataSink;

/** @deprecated Use {@link CacheDataSink.Factory}. */
@Deprecated
public final class CacheDataSinkFactory implements DataSink.Factory {

  private final Cache cache;
  private final long fragmentSize;
  private final int bufferSize;

  /** @see CacheDataSink#CacheDataSink(Cache, long) */
  public CacheDataSinkFactory(Cache cache, long fragmentSize) {
    this(cache, fragmentSize, CacheDataSink.DEFAULT_BUFFER_SIZE);
  }

  /** @see CacheDataSink#CacheDataSink(Cache, long, int) */
  public CacheDataSinkFactory(Cache cache, long fragmentSize, int bufferSize) {
    this.cache = cache;
    this.fragmentSize = fragmentSize;
    this.bufferSize = bufferSize;
  }

  @Override
  public DataSink createDataSink() {
    return new CacheDataSink(cache, fragmentSize, bufferSize);
  }
}
