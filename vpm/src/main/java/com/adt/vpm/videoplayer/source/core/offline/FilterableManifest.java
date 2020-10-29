/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.offline;

import com.adt.vpm.videoplayer.source.common.offline.StreamKey;

import java.util.List;

/**
 * A manifest that can generate copies of itself including only the streams specified by the given
 * keys.
 *
 * @param <T> The manifest type.
 */
public interface FilterableManifest<T> {

  /**
   * Returns a copy of the manifest including only the streams specified by the given keys. If the
   * manifest is unchanged then the instance may return itself.
   *
   * @param streamKeys A non-empty list of stream keys.
   * @return The filtered manifest.
   */
  T copy(List<StreamKey> streamKeys);
}
