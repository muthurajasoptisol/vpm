/*
 * Created by ADT author on 9/29/20 5:06 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 5:06 PM
 */
package com.adt.vpm.videoplayer.source.extractor.ts;

import com.adt.vpm.videoplayer.source.extractor.ExtractorOutput;
import com.adt.vpm.videoplayer.source.extractor.TrackOutput;
import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.Format;
import com.adt.vpm.videoplayer.source.common.util.Assertions;
import com.adt.vpm.videoplayer.source.common.util.ParsableByteArray;
import com.adt.vpm.videoplayer.source.common.util.TimestampAdjuster;
import com.adt.vpm.videoplayer.source.common.util.Util;

import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

/**
 * A {@link SectionPayloadReader} that directly outputs the section bytes as sample data.
 *
 * <p>Timestamp adjustment is provided through {@link Format#subsampleOffsetUs}.
 */
public final class PassthroughSectionPayloadReader implements SectionPayloadReader {

  private Format format;
  private @MonotonicNonNull TimestampAdjuster timestampAdjuster;
  private @MonotonicNonNull TrackOutput output;

  /**
   * Create a new PassthroughSectionPayloadReader.
   *
   * @param mimeType The MIME type set as {@link Format#sampleMimeType} on the created output track.
   */
  public PassthroughSectionPayloadReader(String mimeType) {
    this.format = new Format.Builder().setSampleMimeType(mimeType).build();
  }

  @Override
  public void init(
      TimestampAdjuster timestampAdjuster,
      ExtractorOutput extractorOutput,
      TsPayloadReader.TrackIdGenerator idGenerator) {
    this.timestampAdjuster = timestampAdjuster;
    idGenerator.generateNewId();
    output = extractorOutput.track(idGenerator.getTrackId(), C.TRACK_TYPE_METADATA);
    // Eagerly output an incomplete format (missing timestamp offset) to ensure source preparation
    // is not blocked waiting for potentially sparse metadata.
    output.format(format);
  }

  @Override
  public void consume(ParsableByteArray sectionData) {
    assertInitialized();
    long subsampleOffsetUs = timestampAdjuster.getTimestampOffsetUs();
    if (subsampleOffsetUs == C.TIME_UNSET) {
      // Don't output samples without a known subsample offset.
      return;
    }
    if (subsampleOffsetUs != format.subsampleOffsetUs) {
      format = format.buildUpon().setSubsampleOffsetUs(subsampleOffsetUs).build();
      output.format(format);
    }
    int sampleSize = sectionData.bytesLeft();
    output.sampleData(sectionData, sampleSize);
    output.sampleMetadata(
        timestampAdjuster.getLastAdjustedTimestampUs(),
        C.BUFFER_FLAG_KEY_FRAME,
        sampleSize,
        0,
        null);
  }

  @EnsuresNonNull({"timestampAdjuster", "output"})
  private void assertInitialized() {
    Assertions.checkStateNotNull(timestampAdjuster);
    Util.castNonNull(output);
  }
}
