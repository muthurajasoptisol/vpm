/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.metadata.scte35;

import android.os.Parcel;

/**
 * Represents a splice null command as defined in SCTE35, Section 9.3.1.
 */
public final class SpliceNullCommand extends SpliceCommand {

  // Parcelable implementation.

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    // Do nothing.
  }

  public static final Creator<SpliceNullCommand> CREATOR =
      new Creator<SpliceNullCommand>() {

    @Override
    public SpliceNullCommand createFromParcel(Parcel in) {
      return new SpliceNullCommand();
    }

    @Override
    public SpliceNullCommand[] newArray(int size) {
      return new SpliceNullCommand[size];
    }

  };

}
