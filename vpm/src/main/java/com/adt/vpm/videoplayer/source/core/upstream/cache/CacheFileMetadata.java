/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.upstream.cache;

/** Metadata associated with a cache file. */
/* package */ final class CacheFileMetadata {

  public final long length;
  public final long lastTouchTimestamp;

  public CacheFileMetadata(long length, long lastTouchTimestamp) {
    this.length = length;
    this.lastTouchTimestamp = lastTouchTimestamp;
  }
}
