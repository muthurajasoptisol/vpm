/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.upstream.cache;

import com.adt.vpm.videoplayer.source.common.upstream.DataSpec;

/** Factory for cache keys. */
public interface CacheKeyFactory {

  /** Default {@link CacheKeyFactory}. */
  CacheKeyFactory DEFAULT =
      (dataSpec) -> dataSpec.key != null ? dataSpec.key : dataSpec.uri.toString();

  /**
   * Returns the cache key of the resource containing the data defined by a {@link DataSpec}.
   *
   * <p>Note that since the returned cache key corresponds to the whole resource, implementations
   * must not return different cache keys for {@link DataSpec DataSpecs} that define different
   * ranges of the same resource. As a result, implementations should not use fields such as {@link
   * DataSpec#position} and {@link DataSpec#length}.
   *
   * @param dataSpec The {@link DataSpec}.
   * @return The cache key of the resource.
   */
  String buildCacheKey(DataSpec dataSpec);
}
