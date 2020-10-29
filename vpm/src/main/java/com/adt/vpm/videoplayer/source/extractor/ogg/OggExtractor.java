/*
 * Created by ADT author on 9/29/20 5:06 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 5:06 PM
 */
package com.adt.vpm.videoplayer.source.extractor.ogg;

import com.adt.vpm.videoplayer.source.extractor.Extractor;
import com.adt.vpm.videoplayer.source.extractor.ExtractorInput;
import com.adt.vpm.videoplayer.source.extractor.ExtractorOutput;
import com.adt.vpm.videoplayer.source.extractor.ExtractorsFactory;
import com.adt.vpm.videoplayer.source.extractor.PositionHolder;
import com.adt.vpm.videoplayer.source.extractor.TrackOutput;
import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.ParserException;
import com.adt.vpm.videoplayer.source.common.util.Assertions;
import com.adt.vpm.videoplayer.source.common.util.ParsableByteArray;

import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.io.IOException;

import static java.lang.Math.min;

/**
 * Extracts data from the Ogg container format.
 */
public class OggExtractor implements Extractor {

  /** Factory for {@link OggExtractor} instances. */
  public static final ExtractorsFactory FACTORY = () -> new Extractor[] {new OggExtractor()};

  private static final int MAX_VERIFICATION_BYTES = 8;

  private @MonotonicNonNull ExtractorOutput output;
  private @MonotonicNonNull StreamReader streamReader;
  private boolean streamReaderInitialized;

  @Override
  public boolean sniff(ExtractorInput input) throws IOException {
    try {
      return sniffInternal(input);
    } catch (ParserException e) {
      return false;
    }
  }

  @Override
  public void init(ExtractorOutput output) {
    this.output = output;
  }

  @Override
  public void seek(long position, long timeUs) {
    if (streamReader != null) {
      streamReader.seek(position, timeUs);
    }
  }

  @Override
  public void release() {
    // Do nothing
  }

  @Override
  public int read(ExtractorInput input, PositionHolder seekPosition) throws IOException {
    Assertions.checkStateNotNull(output); // Asserts that init has been called.
    if (streamReader == null) {
      if (!sniffInternal(input)) {
        throw new ParserException("Failed to determine bitstream type");
      }
      input.resetPeekPosition();
    }
    if (!streamReaderInitialized) {
      TrackOutput trackOutput = output.track(0, C.TRACK_TYPE_AUDIO);
      output.endTracks();
      streamReader.init(output, trackOutput);
      streamReaderInitialized = true;
    }
    return streamReader.read(input, seekPosition);
  }

  @EnsuresNonNullIf(expression = "streamReader", result = true)
  private boolean sniffInternal(ExtractorInput input) throws IOException {
    OggPageHeader header = new OggPageHeader();
    if (!header.populate(input, true) || (header.type & 0x02) != 0x02) {
      return false;
    }

    int length = min(header.bodySize, MAX_VERIFICATION_BYTES);
    ParsableByteArray scratch = new ParsableByteArray(length);
    input.peekFully(scratch.getData(), 0, length);

    if (FlacReader.verifyBitstreamType(resetPosition(scratch))) {
      streamReader = new FlacReader();
    } else if (VorbisReader.verifyBitstreamType(resetPosition(scratch))) {
      streamReader = new VorbisReader();
    } else if (OpusReader.verifyBitstreamType(resetPosition(scratch))) {
      streamReader = new OpusReader();
    } else {
      return false;
    }
    return true;
  }

  private static ParsableByteArray resetPosition(ParsableByteArray scratch) {
    scratch.setPosition(0);
    return scratch;
  }

}
