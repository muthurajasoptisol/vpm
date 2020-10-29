/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.source.chunk;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.Format;
import com.adt.vpm.videoplayer.source.common.upstream.DataSource;
import com.adt.vpm.videoplayer.source.common.upstream.DataSpec;
import com.adt.vpm.videoplayer.source.common.util.Util;
import com.adt.vpm.videoplayer.source.extractor.DefaultExtractorInput;
import com.adt.vpm.videoplayer.source.extractor.ExtractorInput;

import java.io.IOException;

/**
 * A {@link Chunk} that uses an {@link Extractor} to decode initialization data for single track.
 */
public final class InitializationChunk extends Chunk {

  private final ChunkExtractor chunkExtractor;

  private ChunkExtractor.TrackOutputProvider trackOutputProvider;
  private long nextLoadPosition;
  private volatile boolean loadCanceled;

  /**
   * @param dataSource The source from which the data should be loaded.
   * @param dataSpec Defines the data to be loaded.
   * @param trackFormat See {@link #trackFormat}.
   * @param trackSelectionReason See {@link #trackSelectionReason}.
   * @param trackSelectionData See {@link #trackSelectionData}.
   * @param chunkExtractor A wrapped extractor to use for parsing the initialization data.
   */
  public InitializationChunk(
      DataSource dataSource,
      DataSpec dataSpec,
      Format trackFormat,
      int trackSelectionReason,
      @Nullable Object trackSelectionData,
      ChunkExtractor chunkExtractor) {
    super(dataSource, dataSpec, C.DATA_TYPE_MEDIA_INITIALIZATION, trackFormat, trackSelectionReason,
        trackSelectionData, C.TIME_UNSET, C.TIME_UNSET);
    this.chunkExtractor = chunkExtractor;
  }

  /**
   * Initializes the chunk for loading, setting a {@link ChunkExtractor.TrackOutputProvider} for track outputs to
   * which formats will be written as they are loaded.
   *
   * @param trackOutputProvider The {@link ChunkExtractor.TrackOutputProvider} for track outputs to which formats
   *     will be written as they are loaded.
   */
  public void init(ChunkExtractor.TrackOutputProvider trackOutputProvider) {
    this.trackOutputProvider = trackOutputProvider;
  }

  // Loadable implementation.

  @Override
  public void cancelLoad() {
    loadCanceled = true;
  }

  @SuppressWarnings("NonAtomicVolatileUpdate")
  @Override
  public void load() throws IOException {
    if (nextLoadPosition == 0) {
      chunkExtractor.init(
          trackOutputProvider, /* startTimeUs= */ C.TIME_UNSET, /* endTimeUs= */ C.TIME_UNSET);
    }
    try {
      // Create and open the input.
      DataSpec loadDataSpec = dataSpec.subrange(nextLoadPosition);
      ExtractorInput input =
          new DefaultExtractorInput(
              dataSource, loadDataSpec.position, dataSource.open(loadDataSpec));
      // Load and decode the initialization data.
      try {
        while (!loadCanceled && chunkExtractor.read(input)) {}
      } finally {
        nextLoadPosition = input.getPosition() - dataSpec.position;
      }
    } finally {
      Util.closeQuietly(dataSource);
    }
  }
}
