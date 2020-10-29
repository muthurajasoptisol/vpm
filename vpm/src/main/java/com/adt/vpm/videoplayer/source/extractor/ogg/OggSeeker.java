/*
 * Created by ADT author on 9/29/20 5:06 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 5:06 PM
 */
package com.adt.vpm.videoplayer.source.extractor.ogg;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.extractor.ExtractorInput;
import com.adt.vpm.videoplayer.source.extractor.SeekMap;

import java.io.IOException;

/**
 * Used to seek in an Ogg stream. OggSeeker implementation may do direct seeking or progressive
 * seeking. OggSeeker works together with a {@link SeekMap} instance to capture the queried position
 * and start the seeking with an initial estimated position.
 */
/* package */ interface OggSeeker {

  /**
   * Returns a {@link SeekMap} that returns an initial estimated position for progressive seeking or
   * the final position for direct seeking. Returns null if {@link #read} has yet to return -1.
   */
  @Nullable
  SeekMap createSeekMap();

  /**
   * Starts a seek operation.
   *
   * @param targetGranule The target granule position.
   */
  void startSeek(long targetGranule);

  /**
   * Reads data from the {@link ExtractorInput} to build the {@link SeekMap} or to continue a seek.
   *
   * <p>If more data is required or if the position of the input needs to be modified then a
   * position from which data should be provided is returned. Else a negative value is returned. If
   * a seek has been completed then the value returned is -(currentGranule + 2). Else it is -1.
   *
   * @param input The {@link ExtractorInput} to read from.
   * @return A non-negative position to seek the {@link ExtractorInput} to, or -(currentGranule + 2)
   *     if the progressive seek has completed, or -1 otherwise.
   * @throws IOException If reading from the {@link ExtractorInput} fails.
   */
  long read(ExtractorInput input) throws IOException;
}
