/*
 * Created by ADT author on 9/29/20 5:06 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 5:06 PM
 */
package com.adt.vpm.videoplayer.source.extractor.mkv;

import com.adt.vpm.videoplayer.source.extractor.ExtractorInput;
import com.adt.vpm.videoplayer.source.common.ParserException;

import java.io.IOException;

/**
 * Event-driven EBML reader that delivers events to an {@link EbmlProcessor}.
 *
 * <p>EBML can be summarized as a binary XML format somewhat similar to Protocol Buffers. It was
 * originally designed for the Matroska container format. More information about EBML and Matroska
 * is available <a href="http://www.matroska.org/technical/specs/index.html">here</a>.
 */
/* package */ interface EbmlReader {

  /**
   * Initializes the extractor with an {@link EbmlProcessor}.
   *
   * @param processor An {@link EbmlProcessor} to process events.
   */
  void init(EbmlProcessor processor);

  /**
   * Resets the state of the reader.
   * <p>
   * Subsequent calls to {@link #read(ExtractorInput)} will start reading a new EBML structure
   * from scratch.
   */
  void reset();

  /**
   * Reads from an {@link ExtractorInput}, invoking an event callback if possible.
   *
   * @param input The {@link ExtractorInput} from which data should be read.
   * @return True if data can continue to be read. False if the end of the input was encountered.
   * @throws ParserException If parsing fails.
   * @throws IOException If an error occurs reading from the input.
   */
  boolean read(ExtractorInput input) throws IOException;
}
