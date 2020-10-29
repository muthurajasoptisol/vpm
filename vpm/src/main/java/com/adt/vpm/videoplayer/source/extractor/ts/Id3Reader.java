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
import com.adt.vpm.videoplayer.source.common.util.Log;
import com.adt.vpm.videoplayer.source.common.util.MimeTypes;
import com.adt.vpm.videoplayer.source.common.util.ParsableByteArray;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import static com.adt.vpm.videoplayer.source.common.metadata.id3.Id3Decoder.ID3_HEADER_LENGTH;
import static java.lang.Math.min;

/**
 * Parses ID3 data and extracts individual text information frames.
 */
public final class Id3Reader implements ElementaryStreamReader {

  private static final String TAG = "Id3Reader";

  private final ParsableByteArray id3Header;

  private @MonotonicNonNull TrackOutput output;

  // State that should be reset on seek.
  private boolean writingSample;

  // Per sample state that gets reset at the start of each sample.
  private long sampleTimeUs;
  private int sampleSize;
  private int sampleBytesRead;

  public Id3Reader() {
    id3Header = new ParsableByteArray(ID3_HEADER_LENGTH);
  }

  @Override
  public void seek() {
    writingSample = false;
  }

  @Override
  public void createTracks(ExtractorOutput extractorOutput, TsPayloadReader.TrackIdGenerator idGenerator) {
    idGenerator.generateNewId();
    output = extractorOutput.track(idGenerator.getTrackId(), C.TRACK_TYPE_METADATA);
    output.format(
        new Format.Builder()
            .setId(idGenerator.getFormatId())
            .setSampleMimeType(MimeTypes.APPLICATION_ID3)
            .build());
  }

  @Override
  public void packetStarted(long pesTimeUs, @TsPayloadReader.Flags int flags) {
    if ((flags & TsPayloadReader.FLAG_DATA_ALIGNMENT_INDICATOR) == 0) {
      return;
    }
    writingSample = true;
    sampleTimeUs = pesTimeUs;
    sampleSize = 0;
    sampleBytesRead = 0;
  }

  @Override
  public void consume(ParsableByteArray data) {
    Assertions.checkStateNotNull(output); // Asserts that createTracks has been called.
    if (!writingSample) {
      return;
    }
    int bytesAvailable = data.bytesLeft();
    if (sampleBytesRead < ID3_HEADER_LENGTH) {
      // We're still reading the ID3 header.
      int headerBytesAvailable = min(bytesAvailable, ID3_HEADER_LENGTH - sampleBytesRead);
      System.arraycopy(
          data.getData(),
          data.getPosition(),
          id3Header.getData(),
          sampleBytesRead,
          headerBytesAvailable);
      if (sampleBytesRead + headerBytesAvailable == ID3_HEADER_LENGTH) {
        // We've finished reading the ID3 header. Extract the sample size.
        id3Header.setPosition(0);
        if ('I' != id3Header.readUnsignedByte() || 'D' != id3Header.readUnsignedByte()
            || '3' != id3Header.readUnsignedByte()) {
          Log.w(TAG, "Discarding invalid ID3 tag");
          writingSample = false;
          return;
        }
        id3Header.skipBytes(3); // version (2) + flags (1)
        sampleSize = ID3_HEADER_LENGTH + id3Header.readSynchSafeInt();
      }
    }
    // Write data to the output.
    int bytesToWrite = min(bytesAvailable, sampleSize - sampleBytesRead);
    output.sampleData(data, bytesToWrite);
    sampleBytesRead += bytesToWrite;
  }

  @Override
  public void packetFinished() {
    Assertions.checkStateNotNull(output); // Asserts that createTracks has been called.
    if (!writingSample || sampleSize == 0 || sampleBytesRead != sampleSize) {
      return;
    }
    output.sampleMetadata(sampleTimeUs, C.BUFFER_FLAG_KEY_FRAME, sampleSize, 0, null);
    writingSample = false;
  }

}
