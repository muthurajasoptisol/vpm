/*
 * Created by ADT author on 9/29/20 5:06 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 5:06 PM
 */
package com.adt.vpm.videoplayer.source.extractor.wav;

/** Header for a WAV file. */
/* package */ final class WavHeader {

  /**
   * The format type. Standard format types are the "WAVE form Registration Number" constants
   * defined in RFC 2361 Appendix A.
   */
  public final int formatType;
  /** The number of channels. */
  public final int numChannels;
  /** The sample rate in Hertz. */
  public final int frameRateHz;
  /** The average bytes per second for the sample data. */
  public final int averageBytesPerSecond;
  /** The block size in bytes. */
  public final int blockSize;
  /** Bits per sample for a single channel. */
  public final int bitsPerSample;
  /** Extra data appended to the format chunk of the header. */
  public final byte[] extraData;

  public WavHeader(
      int formatType,
      int numChannels,
      int frameRateHz,
      int averageBytesPerSecond,
      int blockSize,
      int bitsPerSample,
      byte[] extraData) {
    this.formatType = formatType;
    this.numChannels = numChannels;
    this.frameRateHz = frameRateHz;
    this.averageBytesPerSecond = averageBytesPerSecond;
    this.blockSize = blockSize;
    this.bitsPerSample = bitsPerSample;
    this.extraData = extraData;
  }
}
