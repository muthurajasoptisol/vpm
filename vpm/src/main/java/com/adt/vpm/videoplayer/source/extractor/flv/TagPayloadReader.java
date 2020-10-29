/*
 * Created by ADT author on 9/29/20 5:06 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 5:06 PM
 */
package com.adt.vpm.videoplayer.source.extractor.flv;

import com.adt.vpm.videoplayer.source.extractor.TrackOutput;
import com.adt.vpm.videoplayer.source.common.ParserException;
import com.adt.vpm.videoplayer.source.common.util.ParsableByteArray;

/**
 * Extracts individual samples from FLV tags, preserving original order.
 */
/* package */ abstract class TagPayloadReader {

  /**
   * Thrown when the format is not supported.
   */
  public static final class UnsupportedFormatException extends ParserException {

    public UnsupportedFormatException(String msg) {
      super(msg);
    }

  }

  protected final TrackOutput output;

  /**
   * @param output A {@link TrackOutput} to which samples should be written.
   */
  protected TagPayloadReader(TrackOutput output) {
    this.output = output;
  }

  /**
   * Notifies the reader that a seek has occurred.
   * <p>
   * Following a call to this method, the data passed to the next invocation of
   * {@link #consume(ParsableByteArray, long)} will not be a continuation of the data that
   * was previously passed. Hence the reader should reset any internal state.
   */
  public abstract void seek();

  /**
   * Consumes payload data.
   *
   * @param data The payload data to consume.
   * @param timeUs The timestamp associated with the payload.
   * @return Whether a sample was output.
   * @throws ParserException If an error occurs parsing the data.
   */
  public final boolean consume(ParsableByteArray data, long timeUs) throws ParserException {
    return parseHeader(data) && parsePayload(data, timeUs);
  }

  /**
   * Parses tag header.
   *
   * @param data Buffer where the tag header is stored.
   * @return Whether the header was parsed successfully.
   * @throws ParserException If an error occurs parsing the header.
   */
  protected abstract boolean parseHeader(ParsableByteArray data) throws ParserException;

  /**
   * Parses tag payload.
   *
   * @param data Buffer where tag payload is stored.
   * @param timeUs Time position of the frame.
   * @return Whether a sample was output.
   * @throws ParserException If an error occurs parsing the payload.
   */
  protected abstract boolean parsePayload(ParsableByteArray data, long timeUs)
      throws ParserException;
}
