/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.mediacodec;

import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.Format;
import com.adt.vpm.videoplayer.source.common.audio.MpegAudioUtil;
import com.adt.vpm.videoplayer.source.common.decoder.DecoderInputBuffer;
import com.adt.vpm.videoplayer.source.common.util.Assertions;
import com.adt.vpm.videoplayer.source.common.util.Log;

import java.nio.ByteBuffer;

/**
 * Tracks the number of processed samples to calculate an accurate current timestamp, matching the
 * calculations made in the Codec2 Mp3 decoder.
 */
/* package */ final class C2Mp3TimestampTracker {

  // Mirroring the actual codec, as can be found at
  // https://cs.android.com/android/platform/superproject/+/master:frameworks/av/media/codec2/components/mp3/C2SoftMp3Dec.h;l=55;drc=3665390c9d32a917398b240c5a46ced07a3b65eb
  private static final long DECODER_DELAY_SAMPLES = 529;
  private static final String TAG = "C2Mp3TimestampTracker";

  private long processedSamples;
  private long anchorTimestampUs;
  private boolean seenInvalidMpegAudioHeader;

  /**
   * Resets the timestamp tracker.
   *
   * <p>This should be done when the codec is flushed.
   */
  public void reset() {
    processedSamples = 0;
    anchorTimestampUs = 0;
    seenInvalidMpegAudioHeader = false;
  }

  /**
   * Updates the tracker with the given input buffer and returns the expected output timestamp.
   *
   * @param format The format associated with the buffer.
   * @param buffer The current input buffer.
   * @return The expected output presentation time, in microseconds.
   */
  public long updateAndGetPresentationTimeUs(Format format, DecoderInputBuffer buffer) {
    if (seenInvalidMpegAudioHeader) {
      return buffer.timeUs;
    }

    ByteBuffer data = Assertions.checkNotNull(buffer.data);
    int sampleHeaderData = 0;
    for (int i = 0; i < 4; i++) {
      sampleHeaderData <<= 8;
      sampleHeaderData |= data.get(i) & 0xFF;
    }

    int frameCount = MpegAudioUtil.parseMpegAudioFrameSampleCount(sampleHeaderData);
    if (frameCount == C.LENGTH_UNSET) {
      seenInvalidMpegAudioHeader = true;
      Log.w(TAG, "MPEG audio header is invalid.");
      return buffer.timeUs;
    }

    // These calculations mirror the timestamp calculations in the Codec2 Mp3 Decoder.
    // https://cs.android.com/android/platform/superproject/+/master:frameworks/av/media/codec2/components/mp3/C2SoftMp3Dec.cpp;l=464;drc=ed134640332fea70ca4b05694289d91a5265bb46
    if (processedSamples == 0) {
      anchorTimestampUs = buffer.timeUs;
      processedSamples = frameCount - DECODER_DELAY_SAMPLES;
      return anchorTimestampUs;
    }
    long processedDurationUs = getProcessedDurationUs(format);
    processedSamples += frameCount;
    return anchorTimestampUs + processedDurationUs;
  }

  private long getProcessedDurationUs(Format format) {
    return processedSamples * C.MICROS_PER_SECOND / format.sampleRate;
  }
}
