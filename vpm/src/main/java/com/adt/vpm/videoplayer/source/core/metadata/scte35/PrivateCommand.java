/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.metadata.scte35;

import android.os.Parcel;
import android.os.Parcelable;
import com.adt.vpm.videoplayer.source.common.util.ParsableByteArray;
import com.adt.vpm.videoplayer.source.common.util.Util;

/**
 * Represents a private command as defined in SCTE35, Section 9.3.6.
 */
public final class PrivateCommand extends SpliceCommand {

  /**
   * The {@code pts_adjustment} as defined in SCTE35, Section 9.2.
   */
  public final long ptsAdjustment;
  /**
   * The identifier as defined in SCTE35, Section 9.3.6.
   */
  public final long identifier;
  /**
   * The private bytes as defined in SCTE35, Section 9.3.6.
   */
  public final byte[] commandBytes;

  private PrivateCommand(long identifier, byte[] commandBytes, long ptsAdjustment) {
    this.ptsAdjustment = ptsAdjustment;
    this.identifier = identifier;
    this.commandBytes = commandBytes;
  }

  private PrivateCommand(Parcel in) {
    ptsAdjustment = in.readLong();
    identifier = in.readLong();
    commandBytes = Util.castNonNull(in.createByteArray());
  }

  /* package */ static PrivateCommand parseFromSection(ParsableByteArray sectionData,
      int commandLength, long ptsAdjustment) {
    long identifier = sectionData.readUnsignedInt();
    byte[] privateBytes = new byte[commandLength - 4 /* identifier size */];
    sectionData.readBytes(privateBytes, 0, privateBytes.length);
    return new PrivateCommand(identifier, privateBytes, ptsAdjustment);
  }

  // Parcelable implementation.

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeLong(ptsAdjustment);
    dest.writeLong(identifier);
    dest.writeByteArray(commandBytes);
  }

  public static final Parcelable.Creator<PrivateCommand> CREATOR =
      new Parcelable.Creator<PrivateCommand>() {

    @Override
    public PrivateCommand createFromParcel(Parcel in) {
      return new PrivateCommand(in);
    }

    @Override
    public PrivateCommand[] newArray(int size) {
      return new PrivateCommand[size];
    }

  };

}
