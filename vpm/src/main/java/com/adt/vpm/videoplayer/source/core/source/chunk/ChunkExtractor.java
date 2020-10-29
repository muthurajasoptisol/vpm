/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.source.chunk;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.Format;
import com.adt.vpm.videoplayer.source.extractor.ChunkIndex;
import com.adt.vpm.videoplayer.source.extractor.ExtractorInput;
import com.adt.vpm.videoplayer.source.extractor.TrackOutput;

import java.io.IOException;

/**
 * Extracts samples and track {@link Format Formats} from chunks.
 *
 * <p>The {@link TrackOutputProvider} passed to {@link #init} provides the {@link TrackOutput
 * TrackOutputs} that receive the extracted data.
 */
public interface ChunkExtractor {

  /** Provides {@link TrackOutput} instances to be written to during extraction. */
  interface TrackOutputProvider {

    /**
     * Called to get the {@link TrackOutput} for a specific track.
     *
     * <p>The same {@link TrackOutput} is returned if multiple calls are made with the same {@code
     * id}.
     *
     * @param id A track identifier.
     * @param type The type of the track. Typically one of the {@link C} {@code TRACK_TYPE_*}
     *     constants.
     * @return The {@link TrackOutput} for the given track identifier.
     */
    TrackOutput track(int id, int type);
  }

  /**
   * Returns the {@link ChunkIndex} most recently obtained from the chunks, or null if a {@link
   * ChunkIndex} has not been obtained.
   */
  @Nullable
  ChunkIndex getChunkIndex();

  /**
   * Returns the sample {@link Format}s for the tracks identified by the extractor, or null if the
   * extractor has not finished identifying tracks.
   */
  @Nullable
  Format[] getSampleFormats();

  /**
   * Initializes the wrapper to output to {@link TrackOutput}s provided by the specified {@link
   * TrackOutputProvider}, and configures the extractor to receive data from a new chunk.
   *
   * @param trackOutputProvider The provider of {@link TrackOutput}s that will receive sample data.
   * @param startTimeUs The start position in the new chunk, or {@link C#TIME_UNSET} to output
   *     samples from the start of the chunk.
   * @param endTimeUs The end position in the new chunk, or {@link C#TIME_UNSET} to output samples
   *     to the end of the chunk.
   */
  void init(@Nullable TrackOutputProvider trackOutputProvider, long startTimeUs, long endTimeUs);

  /** Releases any held resources. */
  void release();

  /**
   * Reads from the given {@link ExtractorInput}.
   *
   * @param input The input to read from.
   * @return Whether there is any data left to extract. Returns false if the end of input has been
   *     reached.
   * @throws IOException If an error occurred reading from or parsing the input.
   */
  boolean read(ExtractorInput input) throws IOException;
}
