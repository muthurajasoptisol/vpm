/*
 * Created by ADT author on 10/1/20 11:19 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/22/20 6:23 PM
 */
package com.adt.vpm.videoplayer.source.hls;


import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.core.source.chunk.MediaChunk;

import java.io.IOException;

/**
 * Thrown when an attempt is made to write a sample to a {@link SampleQueue} whose timestamp is
 * inconsistent with the chunk from which it originates.
 */
/* package */ final class UnexpectedSampleTimestampException extends IOException {

  /** The {@link MediaChunk} that contained the rejected sample. */
  public final MediaChunk mediaChunk;

  /**
   * The timestamp of the last sample that was loaded from {@link #mediaChunk} and successfully
   * written to the {@link SampleQueue}, in microseconds. {@link C#TIME_UNSET} if the first sample
   * in the chunk was rejected.
   */
  public final long lastAcceptedSampleTimeUs;

  /** The timestamp of the rejected sample, in microseconds. */
  public final long rejectedSampleTimeUs;

  /**
   * Constructs an instance.
   *
   * @param mediaChunk The {@link MediaChunk} with the unexpected sample timestamp.
   * @param lastAcceptedSampleTimeUs The timestamp of the last sample that was loaded from the chunk
   *     and successfully written to the {@link SampleQueue}, in microseconds. {@link C#TIME_UNSET}
   *     if the first sample in the chunk was rejected.
   * @param rejectedSampleTimeUs The timestamp of the rejected sample, in microseconds.
   */
  public UnexpectedSampleTimestampException(
      MediaChunk mediaChunk, long lastAcceptedSampleTimeUs, long rejectedSampleTimeUs) {
    super(
        "Unexpected sample timestamp: "
            + C.usToMs(rejectedSampleTimeUs)
            + " in chunk ["
            + mediaChunk.startTimeUs
            + ", "
            + mediaChunk.endTimeUs
            + "]");
    this.mediaChunk = mediaChunk;
    this.lastAcceptedSampleTimeUs = lastAcceptedSampleTimeUs;
    this.rejectedSampleTimeUs = rejectedSampleTimeUs;
  }
}
