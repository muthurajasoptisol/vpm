/*
 * Created by ADT author on 10/1/20 11:19 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/22/20 6:23 PM
 */
package com.adt.vpm.videoplayer.source.hls;

import com.adt.vpm.videoplayer.source.extractor.ExtractorInput;
import com.adt.vpm.videoplayer.source.extractor.ExtractorOutput;

import java.io.IOException;

/** Extracts samples and track {@link Format Formats} from {@link HlsMediaChunk HlsMediaChunks}. */
public interface HlsMediaChunkExtractor {

  /**
   * Initializes the extractor with an {@link ExtractorOutput}. Called at most once.
   *
   * @param extractorOutput An {@link ExtractorOutput} to receive extracted data.
   */
  void init(ExtractorOutput extractorOutput);

  /**
   * Extracts data read from a provided {@link ExtractorInput}. Must not be called before {@link
   * #init(ExtractorOutput)}.
   *
   * <p>A single call to this method will block until some progress has been made, but will not
   * block for longer than this. Hence each call will consume only a small amount of input data.
   *
   * <p>When this method throws an {@link IOException}, extraction may continue by providing an
   * {@link ExtractorInput} with an unchanged {@link ExtractorInput#getPosition() read position} to
   * a subsequent call to this method.
   *
   * @param extractorInput The input to read from.
   * @return Whether there is any data left to extract. Returns false if the end of input has been
   *     reached.
   * @throws IOException If an error occurred reading from or parsing the input.
   */
  boolean read(ExtractorInput extractorInput) throws IOException;

  /** Returns whether this is a packed audio extractor, as defined in RFC 8216, Section 3.4. */
  boolean isPackedAudioExtractor();

  /** Returns whether this instance can be used for extracting multiple continuous segments. */
  boolean isReusable();

  /**
   * Returns a new instance for extracting the same type of media as this one. Can only be called on
   * instances that are not {@link #isReusable() reusable}.
   */
  HlsMediaChunkExtractor recreate();
}
