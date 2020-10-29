/*
 * Created by ADT author on 10/1/20 11:19 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/22/20 6:23 PM
 */
package com.adt.vpm.videoplayer.source.hls;

import android.net.Uri;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.common.util.Assertions;

import java.util.LinkedHashMap;

/**
 * LRU cache that holds up to {@code maxSize} full-segment-encryption keys. Which each addition,
 * once the cache's size exceeds {@code maxSize}, the oldest item (according to insertion order) is
 * removed.
 */
/* package */ final class FullSegmentEncryptionKeyCache {

  private final LinkedHashMap<Uri, byte[]> backingMap;

  public FullSegmentEncryptionKeyCache(int maxSize) {
    backingMap =
        new LinkedHashMap<Uri, byte[]>(
            /* initialCapacity= */ maxSize + 1, /* loadFactor= */ 1, /* accessOrder= */ false) {
          @Override
          protected boolean removeEldestEntry(Entry<Uri, byte[]> eldest) {
            return size() > maxSize;
          }
        };
  }

  /**
   * Returns the {@code encryptionKey} cached against this {@code uri}, or null if {@code uri} is
   * null or not present in the cache.
   */
  @Nullable
  public byte[] get(@Nullable Uri uri) {
    if (uri == null) {
      return null;
    }
    return backingMap.get(uri);
  }

  /**
   * Inserts an entry into the cache.
   *
   * @throws NullPointerException if {@code uri} or {@code encryptionKey} are null.
   */
  @Nullable
  public byte[] put(Uri uri, byte[] encryptionKey) {
    return backingMap.put(Assertions.checkNotNull(uri), Assertions.checkNotNull(encryptionKey));
  }

  /**
   * Returns true if {@code uri} is present in the cache.
   *
   * @throws NullPointerException if {@code uri} is null.
   */
  public boolean containsUri(Uri uri) {
    return backingMap.containsKey(Assertions.checkNotNull(uri));
  }

  /**
   * Removes {@code uri} from the cache. If {@code uri} was present in the cahce, this returns the
   * corresponding {@code encryptionKey}, otherwise null.
   *
   * @throws NullPointerException if {@code uri} is null.
   */
  @Nullable
  public byte[] remove(Uri uri) {
    return backingMap.remove(Assertions.checkNotNull(uri));
  }
}
