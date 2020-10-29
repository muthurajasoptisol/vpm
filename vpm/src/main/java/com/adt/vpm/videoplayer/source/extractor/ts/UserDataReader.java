/*
 * Created by ADT author on 9/29/20 5:06 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 5:06 PM
 */
package com.adt.vpm.videoplayer.source.extractor.ts;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.extractor.CeaUtil;
import com.adt.vpm.videoplayer.source.extractor.ExtractorOutput;
import com.adt.vpm.videoplayer.source.extractor.TrackOutput;
import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.Format;
import com.adt.vpm.videoplayer.source.common.util.Assertions;
import com.adt.vpm.videoplayer.source.common.util.MimeTypes;
import com.adt.vpm.videoplayer.source.common.util.ParsableByteArray;

import java.util.List;

/** Consumes user data, outputting contained CEA-608/708 messages to a {@link TrackOutput}. */
/* package */ final class UserDataReader {

  private static final int USER_DATA_START_CODE = 0x0001B2;

  private final List<Format> closedCaptionFormats;
  private final TrackOutput[] outputs;

  public UserDataReader(List<Format> closedCaptionFormats) {
    this.closedCaptionFormats = closedCaptionFormats;
    outputs = new TrackOutput[closedCaptionFormats.size()];
  }

  public void createTracks(
      ExtractorOutput extractorOutput, TsPayloadReader.TrackIdGenerator idGenerator) {
    for (int i = 0; i < outputs.length; i++) {
      idGenerator.generateNewId();
      TrackOutput output = extractorOutput.track(idGenerator.getTrackId(), C.TRACK_TYPE_TEXT);
      Format channelFormat = closedCaptionFormats.get(i);
      @Nullable String channelMimeType = channelFormat.sampleMimeType;
      Assertions.checkArgument(
          MimeTypes.APPLICATION_CEA608.equals(channelMimeType)
              || MimeTypes.APPLICATION_CEA708.equals(channelMimeType),
          "Invalid closed caption mime type provided: " + channelMimeType);
      output.format(
          new Format.Builder()
              .setId(idGenerator.getFormatId())
              .setSampleMimeType(channelMimeType)
              .setSelectionFlags(channelFormat.selectionFlags)
              .setLanguage(channelFormat.language)
              .setAccessibilityChannel(channelFormat.accessibilityChannel)
              .setInitializationData(channelFormat.initializationData)
              .build());
      outputs[i] = output;
    }
  }

  public void consume(long pesTimeUs, ParsableByteArray userDataPayload) {
    if (userDataPayload.bytesLeft() < 9) {
      return;
    }
    int userDataStartCode = userDataPayload.readInt();
    int userDataIdentifier = userDataPayload.readInt();
    int userDataTypeCode = userDataPayload.readUnsignedByte();
    if (userDataStartCode == USER_DATA_START_CODE
        && userDataIdentifier == CeaUtil.USER_DATA_IDENTIFIER_GA94
        && userDataTypeCode == CeaUtil.USER_DATA_TYPE_CODE_MPEG_CC) {
      CeaUtil.consumeCcData(pesTimeUs, userDataPayload, outputs);
    }
  }
}
