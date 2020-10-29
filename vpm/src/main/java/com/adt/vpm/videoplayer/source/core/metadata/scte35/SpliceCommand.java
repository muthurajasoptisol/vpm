/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.metadata.scte35;

import com.adt.vpm.videoplayer.source.common.metadata.Metadata;

/**
 * Superclass for SCTE35 splice commands.
 */
public abstract class SpliceCommand implements Metadata.Entry {

  @Override
  public String toString() {
    return "SCTE-35 splice command: type=" + getClass().getSimpleName();
  }

  // Parcelable implementation.

  @Override
  public int describeContents() {
    return 0;
  }

}
