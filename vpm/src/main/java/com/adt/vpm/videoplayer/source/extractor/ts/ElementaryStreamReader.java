/*
 * Created by ADT author on 9/29/20 5:06 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 5:06 PM
 */
package com.adt.vpm.videoplayer.source.extractor.ts;

import com.adt.vpm.videoplayer.source.extractor.ExtractorOutput;
import com.adt.vpm.videoplayer.source.extractor.TrackOutput;
import com.adt.vpm.videoplayer.source.common.ParserException;
import com.adt.vpm.videoplayer.source.common.util.ParsableByteArray;

/**
 * Extracts individual samples from an elementary media stream, preserving original order.
 */
public interface ElementaryStreamReader {

  /**
   * Notifies the reader that a seek has occurred.
   */
  void seek();

  /**
   * Initializes the reader by providing outputs and ids for the tracks.
   *
   * @param extractorOutput The {@link ExtractorOutput} that receives the extracted data.
   * @param idGenerator A {@link PesReader.TrackIdGenerator} that generates unique track ids for the
   *     {@link TrackOutput}s.
   */
  void createTracks(ExtractorOutput extractorOutput, PesReader.TrackIdGenerator idGenerator);

  /**
   * Called when a packet starts.
   *
   * @param pesTimeUs The timestamp associated with the packet.
   * @param flags See {@link TsPayloadReader.Flags}.
   */
  void packetStarted(long pesTimeUs, @TsPayloadReader.Flags int flags);

  /**
   * Consumes (possibly partial) data from the current packet.
   *
   * @param data The data to consume.
   * @throws ParserException If the data could not be parsed.
   */
  void consume(ParsableByteArray data) throws ParserException;

  /**
   * Called when a packet ends.
   */
  void packetFinished();

}
