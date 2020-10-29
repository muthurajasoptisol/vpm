/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.source.chunk;

import com.adt.vpm.videoplayer.source.common.util.Log;
import com.adt.vpm.videoplayer.source.core.source.SampleQueue;
import com.adt.vpm.videoplayer.source.extractor.DummyTrackOutput;
import com.adt.vpm.videoplayer.source.extractor.TrackOutput;
import com.adt.vpm.videoplayer.source.core.source.chunk.ChunkExtractor.TrackOutputProvider;

/**
 * A {@link TrackOutputProvider} that provides {@link TrackOutput TrackOutputs} based on a
 * predefined mapping from track type to output.
 */
public final class BaseMediaChunkOutput implements TrackOutputProvider {

  private static final String TAG = "BaseMediaChunkOutput";

  private final int[] trackTypes;
  private final SampleQueue[] sampleQueues;

  /**
   * @param trackTypes The track types of the individual track outputs.
   * @param sampleQueues The individual sample queues.
   */
  public BaseMediaChunkOutput(int[] trackTypes, SampleQueue[] sampleQueues) {
    this.trackTypes = trackTypes;
    this.sampleQueues = sampleQueues;
  }

  @Override
  public TrackOutput track(int id, int type) {
    for (int i = 0; i < trackTypes.length; i++) {
      if (type == trackTypes[i]) {
        return sampleQueues[i];
      }
    }
    Log.e(TAG, "Unmatched track of type: " + type);
    return new DummyTrackOutput();
  }

  /**
   * Returns the current absolute write indices of the individual sample queues.
   */
  public int[] getWriteIndices() {
    int[] writeIndices = new int[sampleQueues.length];
    for (int i = 0; i < sampleQueues.length; i++) {
      writeIndices[i] = sampleQueues[i].getWriteIndex();
    }
    return writeIndices;
  }

  /**
   * Sets an offset that will be added to the timestamps (and sub-sample timestamps) of samples
   * subsequently written to the sample queues.
   */
  public void setSampleOffsetUs(long sampleOffsetUs) {
    for (SampleQueue sampleQueue : sampleQueues) {
      sampleQueue.setSampleOffsetUs(sampleOffsetUs);
    }
  }

}
