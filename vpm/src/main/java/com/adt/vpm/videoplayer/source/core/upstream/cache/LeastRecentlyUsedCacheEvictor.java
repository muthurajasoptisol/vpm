/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.upstream.cache;

import com.adt.vpm.videoplayer.source.common.C;
import java.util.TreeSet;

/** Evicts least recently used cache files first. */
public final class LeastRecentlyUsedCacheEvictor implements CacheEvictor {

  private final long maxBytes;
  private final TreeSet<CacheSpan> leastRecentlyUsed;

  private long currentSize;

  public LeastRecentlyUsedCacheEvictor(long maxBytes) {
    this.maxBytes = maxBytes;
    this.leastRecentlyUsed = new TreeSet<>(LeastRecentlyUsedCacheEvictor::compare);
  }

  @Override
  public boolean requiresCacheSpanTouches() {
    return true;
  }

  @Override
  public void onCacheInitialized() {
    // Do nothing.
  }

  @Override
  public void onStartFile(Cache cache, String key, long position, long length) {
    if (length != C.LENGTH_UNSET) {
      evictCache(cache, length);
    }
  }

  @Override
  public void onSpanAdded(Cache cache, CacheSpan span) {
    leastRecentlyUsed.add(span);
    currentSize += span.length;
    evictCache(cache, 0);
  }

  @Override
  public void onSpanRemoved(Cache cache, CacheSpan span) {
    leastRecentlyUsed.remove(span);
    currentSize -= span.length;
  }

  @Override
  public void onSpanTouched(Cache cache, CacheSpan oldSpan, CacheSpan newSpan) {
    onSpanRemoved(cache, oldSpan);
    onSpanAdded(cache, newSpan);
  }

  private void evictCache(Cache cache, long requiredSpace) {
    while (currentSize + requiredSpace > maxBytes && !leastRecentlyUsed.isEmpty()) {
      cache.removeSpan(leastRecentlyUsed.first());
    }
  }

  private static int compare(CacheSpan lhs, CacheSpan rhs) {
    long lastTouchTimestampDelta = lhs.lastTouchTimestamp - rhs.lastTouchTimestamp;
    if (lastTouchTimestampDelta == 0) {
      // Use the standard compareTo method as a tie-break.
      return lhs.compareTo(rhs);
    }
    return lhs.lastTouchTimestamp < rhs.lastTouchTimestamp ? -1 : 1;
  }
}
