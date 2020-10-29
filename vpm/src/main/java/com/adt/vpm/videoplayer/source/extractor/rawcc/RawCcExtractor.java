/*
 * Created by ADT author on 9/29/20 5:06 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 5:06 PM
 */
package com.adt.vpm.videoplayer.source.extractor.rawcc;

import com.adt.vpm.videoplayer.source.extractor.Extractor;
import com.adt.vpm.videoplayer.source.extractor.ExtractorInput;
import com.adt.vpm.videoplayer.source.extractor.ExtractorOutput;
import com.adt.vpm.videoplayer.source.extractor.PositionHolder;
import com.adt.vpm.videoplayer.source.extractor.SeekMap;
import com.adt.vpm.videoplayer.source.extractor.TrackOutput;
import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.Format;
import com.adt.vpm.videoplayer.source.common.ParserException;
import com.adt.vpm.videoplayer.source.common.util.Assertions;
import com.adt.vpm.videoplayer.source.common.util.ParsableByteArray;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

import java.io.IOException;

/**
 * Extracts data from the RawCC container format.
 */
public final class RawCcExtractor implements Extractor {

  private static final int SCRATCH_SIZE = 9;
  private static final int HEADER_SIZE = 8;
  private static final int HEADER_ID = 0x52434301;
  private static final int TIMESTAMP_SIZE_V0 = 4;
  private static final int TIMESTAMP_SIZE_V1 = 8;

  // Parser states.
  private static final int STATE_READING_HEADER = 0;
  private static final int STATE_READING_TIMESTAMP_AND_COUNT = 1;
  private static final int STATE_READING_SAMPLES = 2;

  private final Format format;
  private final ParsableByteArray dataScratch;

  private @MonotonicNonNull TrackOutput trackOutput;
  private int parserState;
  private int version;
  private long timestampUs;
  private int remainingSampleCount;
  private int sampleBytesWritten;

  public RawCcExtractor(Format format) {
    this.format = format;
    dataScratch = new ParsableByteArray(SCRATCH_SIZE);
    parserState = STATE_READING_HEADER;
  }

  @Override
  public void init(ExtractorOutput output) {
    output.seekMap(new SeekMap.Unseekable(C.TIME_UNSET));
    trackOutput = output.track(0, C.TRACK_TYPE_TEXT);
    trackOutput.format(format);
    output.endTracks();
  }

  @Override
  public boolean sniff(ExtractorInput input) throws IOException {
    dataScratch.reset(/* limit= */ HEADER_SIZE);
    input.peekFully(dataScratch.getData(), 0, HEADER_SIZE);
    return dataScratch.readInt() == HEADER_ID;
  }

  @Override
  public int read(ExtractorInput input, PositionHolder seekPosition) throws IOException {
    Assertions.checkStateNotNull(trackOutput); // Asserts that init has been called.
    while (true) {
      switch (parserState) {
        case STATE_READING_HEADER:
          if (parseHeader(input)) {
            parserState = STATE_READING_TIMESTAMP_AND_COUNT;
          } else {
            return RESULT_END_OF_INPUT;
          }
          break;
        case STATE_READING_TIMESTAMP_AND_COUNT:
          if (parseTimestampAndSampleCount(input)) {
            parserState = STATE_READING_SAMPLES;
          } else {
            parserState = STATE_READING_HEADER;
            return RESULT_END_OF_INPUT;
          }
          break;
        case STATE_READING_SAMPLES:
          parseSamples(input);
          parserState = STATE_READING_TIMESTAMP_AND_COUNT;
          return RESULT_CONTINUE;
        default:
          throw new IllegalStateException();
      }
    }
  }

  @Override
  public void seek(long position, long timeUs) {
    parserState = STATE_READING_HEADER;
  }

  @Override
  public void release() {
    // Do nothing
  }

  private boolean parseHeader(ExtractorInput input) throws IOException {
    dataScratch.reset(/* limit= */ HEADER_SIZE);
    if (input.readFully(dataScratch.getData(), 0, HEADER_SIZE, true)) {
      if (dataScratch.readInt() != HEADER_ID) {
        throw new IOException("Input not RawCC");
      }
      version = dataScratch.readUnsignedByte();
      // no versions use the flag fields yet
      return true;
    } else {
      return false;
    }
  }

  private boolean parseTimestampAndSampleCount(ExtractorInput input) throws IOException {
    if (version == 0) {
      dataScratch.reset(/* limit= */ TIMESTAMP_SIZE_V0 + 1);
      if (!input.readFully(dataScratch.getData(), 0, TIMESTAMP_SIZE_V0 + 1, true)) {
        return false;
      }
      // version 0 timestamps are 45kHz, so we need to convert them into us
      timestampUs = dataScratch.readUnsignedInt() * 1000 / 45;
    } else if (version == 1) {
      dataScratch.reset(/* limit= */ TIMESTAMP_SIZE_V1 + 1);
      if (!input.readFully(dataScratch.getData(), 0, TIMESTAMP_SIZE_V1 + 1, true)) {
        return false;
      }
      timestampUs = dataScratch.readLong();
    } else {
      throw new ParserException("Unsupported version number: " + version);
    }

    remainingSampleCount = dataScratch.readUnsignedByte();
    sampleBytesWritten = 0;
    return true;
  }

  @RequiresNonNull("trackOutput")
  private void parseSamples(ExtractorInput input) throws IOException {
    for (; remainingSampleCount > 0; remainingSampleCount--) {
      dataScratch.reset(/* limit= */ 3);
      input.readFully(dataScratch.getData(), 0, 3);

      trackOutput.sampleData(dataScratch, 3);
      sampleBytesWritten += 3;
    }

    if (sampleBytesWritten > 0) {
      trackOutput.sampleMetadata(timestampUs, C.BUFFER_FLAG_KEY_FRAME, sampleBytesWritten, 0, null);
    }
  }
}
