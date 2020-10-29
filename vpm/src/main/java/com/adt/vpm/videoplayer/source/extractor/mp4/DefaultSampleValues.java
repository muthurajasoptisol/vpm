/*
 * Created by ADT author on 9/29/20 5:06 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 5:06 PM
 */
package com.adt.vpm.videoplayer.source.extractor.mp4;

/* package */ final class DefaultSampleValues {

  public final int sampleDescriptionIndex;
  public final int duration;
  public final int size;
  public final int flags;

  public DefaultSampleValues(int sampleDescriptionIndex, int duration, int size, int flags) {
    this.sampleDescriptionIndex = sampleDescriptionIndex;
    this.duration = duration;
    this.size = size;
    this.flags = flags;
  }

}
