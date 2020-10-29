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
 * A {@link BaseMediaChunk} that uses an {@link Extractor} to decode sample data.
 */
public class ContainerMediaChunk extends BaseMediaChunk {

  private final int chunkCount;
  private final long sampleOffsetUs;
  private final ChunkExtractor chunkExtractor;

  private long nextLoadPosition;
  private volatile boolean loadCanceled;
  private boolean loadCompleted;

  /**
   * @param dataSource The source from which the data should be loaded.
   * @param dataSpec Defines the data to be loaded.
   * @param trackFormat See {@link #trackFormat}.
   * @param trackSelectionReason See {@link #trackSelectionReason}.
   * @param trackSelectionData See {@link #trackSelectionData}.
   * @param startTimeUs The start time of the media contained by the chunk, in microseconds.
   * @param endTimeUs The end time of the media contained by the chunk, in microseconds.
   * @param clippedStartTimeUs The time in the chunk from which output will begin, or {@link
   *     C#TIME_UNSET} to output from the start of the chunk.
   * @param clippedEndTimeUs The time in the chunk from which output will end, or {@link
   *     C#TIME_UNSET} to output to the end of the chunk.
   * @param chunkIndex The index of the chunk, or {@link C#INDEX_UNSET} if it is not known.
   * @param chunkCount The number of chunks in the underlying media that are spanned by this
   *     instance. Normally equal to one, but may be larger if multiple chunks as defined by the
   *     underlying media are being merged into a single load.
   * @param sampleOffsetUs An offset to add to the sample timestamps parsed by the extractor.
   * @param chunkExtractor A wrapped extractor to use for parsing the data.
   */
  public ContainerMediaChunk(
      DataSource dataSource,
      DataSpec dataSpec,
      Format trackFormat,
      int trackSelectionReason,
      @Nullable Object trackSelectionData,
      long startTimeUs,
      long endTimeUs,
      long clippedStartTimeUs,
      long clippedEndTimeUs,
      long chunkIndex,
      int chunkCount,
      long sampleOffsetUs,
      ChunkExtractor chunkExtractor) {
    super(
        dataSource,
        dataSpec,
        trackFormat,
        trackSelectionReason,
        trackSelectionData,
        startTimeUs,
        endTimeUs,
        clippedStartTimeUs,
        clippedEndTimeUs,
        chunkIndex);
    this.chunkCount = chunkCount;
    this.sampleOffsetUs = sampleOffsetUs;
    this.chunkExtractor = chunkExtractor;
  }

  @Override
  public long getNextChunkIndex() {
    return chunkIndex + chunkCount;
  }

  @Override
  public boolean isLoadCompleted() {
    return loadCompleted;
  }

  // Loadable implementation.

  @Override
  public final void cancelLoad() {
    loadCanceled = true;
  }

  @SuppressWarnings("NonAtomicVolatileUpdate")
  @Override
  public final void load() throws IOException {
    if (nextLoadPosition == 0) {
      // Configure the output and set it as the target for the extractor wrapper.
      BaseMediaChunkOutput output = getOutput();
      output.setSampleOffsetUs(sampleOffsetUs);
      chunkExtractor.init(
          getTrackOutputProvider(output),
          clippedStartTimeUs == C.TIME_UNSET ? C.TIME_UNSET : (clippedStartTimeUs - sampleOffsetUs),
          clippedEndTimeUs == C.TIME_UNSET ? C.TIME_UNSET : (clippedEndTimeUs - sampleOffsetUs));
    }
    try {
      // Create and open the input.
      DataSpec loadDataSpec = dataSpec.subrange(nextLoadPosition);
      ExtractorInput input =
          new DefaultExtractorInput(
              dataSource, loadDataSpec.position, dataSource.open(loadDataSpec));
      // Load and decode the sample data.
      try {
        while (!loadCanceled && chunkExtractor.read(input)) {}
      } finally {
        nextLoadPosition = input.getPosition() - dataSpec.position;
      }
    } finally {
      Util.closeQuietly(dataSource);
    }
    loadCompleted = !loadCanceled;
  }

  /**
   * Returns the {@link ChunkExtractor.TrackOutputProvider} to be used by the wrapped extractor.
   *
   * @param baseMediaChunkOutput The {@link BaseMediaChunkOutput} most recently passed to {@link
   *     #init(BaseMediaChunkOutput)}.
   * @return A {@link ChunkExtractor.TrackOutputProvider} to be used by the wrapped extractor.
   */
  protected ChunkExtractor.TrackOutputProvider getTrackOutputProvider(BaseMediaChunkOutput baseMediaChunkOutput) {
    return baseMediaChunkOutput;
  }
}
