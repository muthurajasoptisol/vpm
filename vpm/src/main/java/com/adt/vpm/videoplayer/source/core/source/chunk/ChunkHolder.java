/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.source.chunk;

import androidx.annotation.Nullable;

/**
 * Holds a chunk or an indication that the end of the stream has been reached.
 */
public final class ChunkHolder {

  /** The chunk. */
  @Nullable public Chunk chunk;

  /**
   * Indicates that the end of the stream has been reached.
   */
  public boolean endOfStream;

  /**
   * Clears the holder.
   */
  public void clear() {
    chunk = null;
    endOfStream = false;
  }

}
