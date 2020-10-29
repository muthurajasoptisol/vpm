/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.metadata;

import com.adt.vpm.videoplayer.source.common.metadata.Metadata;

/**
 * Receives metadata output.
 */
public interface MetadataOutput {

  /**
   * Called when there is metadata associated with current playback time.
   *
   * @param metadata The metadata.
   */
  void onMetadata(Metadata metadata);

}
