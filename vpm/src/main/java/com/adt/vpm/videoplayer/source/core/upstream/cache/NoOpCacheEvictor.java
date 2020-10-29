/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.upstream.cache;


/**
 * Evictor that doesn't ever evict cache files.
 *
 * Warning: Using this evictor might have unforeseeable consequences if cache
 * size is not managed elsewhere.
 */
public final class NoOpCacheEvictor implements CacheEvictor {

  @Override
  public boolean requiresCacheSpanTouches() {
    return false;
  }

  @Override
  public void onCacheInitialized() {
    // Do nothing.
  }

  @Override
  public void onStartFile(Cache cache, String key, long position, long maxLength) {
    // Do nothing.
  }

  @Override
  public void onSpanAdded(Cache cache, CacheSpan span) {
    // Do nothing.
  }

  @Override
  public void onSpanRemoved(Cache cache, CacheSpan span) {
    // Do nothing.
  }

  @Override
  public void onSpanTouched(Cache cache, CacheSpan oldSpan, CacheSpan newSpan) {
    // Do nothing.
  }

}
