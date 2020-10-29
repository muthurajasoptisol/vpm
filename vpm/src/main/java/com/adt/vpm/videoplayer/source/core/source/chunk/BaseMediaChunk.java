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
import com.adt.vpm.videoplayer.source.common.util.Assertions;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

/**
 * A base implementation of {@link MediaChunk} that outputs to a {@link BaseMediaChunkOutput}.
 */
public abstract class BaseMediaChunk extends MediaChunk {

  /**
   * The time from which output will begin, or {@link C#TIME_UNSET} if output will begin from the
   * start of the chunk.
   */
  public final long clippedStartTimeUs;
  /**
   * The time from which output will end, or {@link C#TIME_UNSET} if output will end at the end of
   * the chunk.
   */
  public final long clippedEndTimeUs;

  private @MonotonicNonNull BaseMediaChunkOutput output;
  private int @MonotonicNonNull [] firstSampleIndices;

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
   */
  public BaseMediaChunk(
      DataSource dataSource,
      DataSpec dataSpec,
      Format trackFormat,
      int trackSelectionReason,
      @Nullable Object trackSelectionData,
      long startTimeUs,
      long endTimeUs,
      long clippedStartTimeUs,
      long clippedEndTimeUs,
      long chunkIndex) {
    super(dataSource, dataSpec, trackFormat, trackSelectionReason, trackSelectionData, startTimeUs,
        endTimeUs, chunkIndex);
    this.clippedStartTimeUs = clippedStartTimeUs;
    this.clippedEndTimeUs = clippedEndTimeUs;
  }

  /**
   * Initializes the chunk for loading, setting the {@link BaseMediaChunkOutput} that will receive
   * samples as they are loaded.
   *
   * @param output The output that will receive the loaded media samples.
   */
  public void init(BaseMediaChunkOutput output) {
    this.output = output;
    firstSampleIndices = output.getWriteIndices();
  }

  /**
   * Returns the index of the first sample in the specified track of the output that will originate
   * from this chunk.
   */
  public final int getFirstSampleIndex(int trackIndex) {
    return Assertions.checkStateNotNull(firstSampleIndices)[trackIndex];
  }

  /**
   * Returns the output most recently passed to {@link #init(BaseMediaChunkOutput)}.
   */
  protected final BaseMediaChunkOutput getOutput() {
    return Assertions.checkStateNotNull(output);
  }

}
