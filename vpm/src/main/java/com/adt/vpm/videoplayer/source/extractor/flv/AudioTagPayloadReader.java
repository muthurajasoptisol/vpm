/*
 * Created by ADT author on 9/29/20 5:06 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 5:06 PM
 */
package com.adt.vpm.videoplayer.source.extractor.flv;

import com.adt.vpm.videoplayer.source.extractor.TrackOutput;
import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.Format;
import com.adt.vpm.videoplayer.source.common.ParserException;
import com.adt.vpm.videoplayer.source.common.audio.AacUtil;
import com.adt.vpm.videoplayer.source.common.util.MimeTypes;
import com.adt.vpm.videoplayer.source.common.util.ParsableByteArray;

import java.util.Collections;

/**
 * Parses audio tags from an FLV stream and extracts AAC frames.
 */
/* package */ final class AudioTagPayloadReader extends TagPayloadReader {

  private static final int AUDIO_FORMAT_MP3 = 2;
  private static final int AUDIO_FORMAT_ALAW = 7;
  private static final int AUDIO_FORMAT_ULAW = 8;
  private static final int AUDIO_FORMAT_AAC = 10;

  private static final int AAC_PACKET_TYPE_SEQUENCE_HEADER = 0;
  private static final int AAC_PACKET_TYPE_AAC_RAW = 1;

  private static final int[] AUDIO_SAMPLING_RATE_TABLE = new int[] {5512, 11025, 22050, 44100};

  // State variables
  private boolean hasParsedAudioDataHeader;
  private boolean hasOutputFormat;
  private int audioFormat;

  public AudioTagPayloadReader(TrackOutput output) {
    super(output);
  }

  @Override
  public void seek() {
    // Do nothing.
  }

  @Override
  protected boolean parseHeader(ParsableByteArray data) throws UnsupportedFormatException {
    if (!hasParsedAudioDataHeader) {
      int header = data.readUnsignedByte();
      audioFormat = (header >> 4) & 0x0F;
      if (audioFormat == AUDIO_FORMAT_MP3) {
        int sampleRateIndex = (header >> 2) & 0x03;
        int sampleRate = AUDIO_SAMPLING_RATE_TABLE[sampleRateIndex];
        Format format =
            new Format.Builder()
                .setSampleMimeType(MimeTypes.AUDIO_MPEG)
                .setChannelCount(1)
                .setSampleRate(sampleRate)
                .build();
        output.format(format);
        hasOutputFormat = true;
      } else if (audioFormat == AUDIO_FORMAT_ALAW || audioFormat == AUDIO_FORMAT_ULAW) {
        String mimeType =
            audioFormat == AUDIO_FORMAT_ALAW ? MimeTypes.AUDIO_ALAW : MimeTypes.AUDIO_MLAW;
        Format format =
            new Format.Builder()
                .setSampleMimeType(mimeType)
                .setChannelCount(1)
                .setSampleRate(8000)
                .build();
        output.format(format);
        hasOutputFormat = true;
      } else if (audioFormat != AUDIO_FORMAT_AAC) {
        throw new UnsupportedFormatException("Audio format not supported: " + audioFormat);
      }
      hasParsedAudioDataHeader = true;
    } else {
      // Skip header if it was parsed previously.
      data.skipBytes(1);
    }
    return true;
  }

  @Override
  protected boolean parsePayload(ParsableByteArray data, long timeUs) throws ParserException {
    if (audioFormat == AUDIO_FORMAT_MP3) {
      int sampleSize = data.bytesLeft();
      output.sampleData(data, sampleSize);
      output.sampleMetadata(timeUs, C.BUFFER_FLAG_KEY_FRAME, sampleSize, 0, null);
      return true;
    } else {
      int packetType = data.readUnsignedByte();
      if (packetType == AAC_PACKET_TYPE_SEQUENCE_HEADER && !hasOutputFormat) {
        // Parse the sequence header.
        byte[] audioSpecificConfig = new byte[data.bytesLeft()];
        data.readBytes(audioSpecificConfig, 0, audioSpecificConfig.length);
        AacUtil.Config aacConfig = AacUtil.parseAudioSpecificConfig(audioSpecificConfig);
        Format format =
            new Format.Builder()
                .setSampleMimeType(MimeTypes.AUDIO_AAC)
                .setCodecs(aacConfig.codecs)
                .setChannelCount(aacConfig.channelCount)
                .setSampleRate(aacConfig.sampleRateHz)
                .setInitializationData(Collections.singletonList(audioSpecificConfig))
                .build();
        output.format(format);
        hasOutputFormat = true;
        return false;
      } else if (audioFormat != AUDIO_FORMAT_AAC || packetType == AAC_PACKET_TYPE_AAC_RAW) {
        int sampleSize = data.bytesLeft();
        output.sampleData(data, sampleSize);
        output.sampleMetadata(timeUs, C.BUFFER_FLAG_KEY_FRAME, sampleSize, 0, null);
        return true;
      } else {
        return false;
      }
    }
  }
}
