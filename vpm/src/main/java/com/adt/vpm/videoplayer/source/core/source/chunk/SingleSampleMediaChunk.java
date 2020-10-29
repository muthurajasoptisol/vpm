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
import com.adt.vpm.videoplayer.source.extractor.TrackOutput;

import java.io.IOException;

/**
 * A {@link BaseMediaChunk} for chunks consisting of a single raw sample.
 */
public final class SingleSampleMediaChunk extends BaseMediaChunk {

  private final int trackType;
  private final Format sampleFormat;

  private long nextLoadPosition;
  private boolean loadCompleted;

  /**
   * @param dataSource The source from which the data should be loaded.
   * @param dataSpec Defines the data to be loaded.
   * @param trackFormat See {@link #trackFormat}.
   * @param trackSelectionReason See {@link #trackSelectionReason}.
   * @param trackSelectionData See {@link #trackSelectionData}.
   * @param startTimeUs The start time of the media contained by the chunk, in microseconds.
   * @param endTimeUs The end time of the media contained by the chunk, in microseconds.
   * @param chunkIndex The index of the chunk, or {@link C#INDEX_UNSET} if it is not known.
   * @param trackType The type of the chunk. Typically one of the {@link C} {@code TRACK_TYPE_*}
   *     constants.
   * @param sampleFormat The {@link Format} of the sample in the chunk.
   */
  public SingleSampleMediaChunk(
      DataSource dataSource,
      DataSpec dataSpec,
      Format trackFormat,
      int trackSelectionReason,
      @Nullable Object trackSelectionData,
      long startTimeUs,
      long endTimeUs,
      long chunkIndex,
      int trackType,
      Format sampleFormat) {
    super(
        dataSource,
        dataSpec,
        trackFormat,
        trackSelectionReason,
        trackSelectionData,
        startTimeUs,
        endTimeUs,
        /* clippedStartTimeUs= */ C.TIME_UNSET,
        /* clippedEndTimeUs= */ C.TIME_UNSET,
        chunkIndex);
    this.trackType = trackType;
    this.sampleFormat = sampleFormat;
  }


  @Override
  public boolean isLoadCompleted() {
    return loadCompleted;
  }

  // Loadable implementation.

  @Override
  public void cancelLoad() {
    // Do nothing.
  }

  @SuppressWarnings("NonAtomicVolatileUpdate")
  @Override
  public void load() throws IOException {
    BaseMediaChunkOutput output = getOutput();
    output.setSampleOffsetUs(0);
    TrackOutput trackOutput = output.track(0, trackType);
    trackOutput.format(sampleFormat);
    try {
      // Create and open the input.
      DataSpec loadDataSpec = dataSpec.subrange(nextLoadPosition);
      long length = dataSource.open(loadDataSpec);
      if (length != C.LENGTH_UNSET) {
        length += nextLoadPosition;
      }
      ExtractorInput extractorInput =
          new DefaultExtractorInput(dataSource, nextLoadPosition, length);
      // Load the sample data.
      int result = 0;
      while (result != C.RESULT_END_OF_INPUT) {
        nextLoadPosition += result;
        result = trackOutput.sampleData(extractorInput, Integer.MAX_VALUE, true);
      }
      int sampleSize = (int) nextLoadPosition;
      trackOutput.sampleMetadata(startTimeUs, C.BUFFER_FLAG_KEY_FRAME, sampleSize, 0, null);
    } finally {
      Util.closeQuietly(dataSource);
    }
    loadCompleted = true;
  }
}
