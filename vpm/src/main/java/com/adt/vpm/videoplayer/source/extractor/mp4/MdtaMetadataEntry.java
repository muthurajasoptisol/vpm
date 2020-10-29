/*
 * Created by ADT author on 9/29/20 5:06 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 5:06 PM
 */
package com.adt.vpm.videoplayer.source.extractor.mp4;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.common.metadata.Metadata;
import com.adt.vpm.videoplayer.source.common.util.Util;

import java.util.Arrays;

/**
 * Stores extensible metadata with handler type 'mdta'. See also the QuickTime File Format
 * Specification.
 */
public final class MdtaMetadataEntry implements Metadata.Entry {

  /** The metadata key name. */
  public final String key;
  /** The payload. The interpretation of the value depends on {@link #typeIndicator}. */
  public final byte[] value;
  /** The four byte locale indicator. */
  public final int localeIndicator;
  /** The four byte type indicator. */
  public final int typeIndicator;

  /** Creates a new metadata entry for the specified metadata key/value. */
  public MdtaMetadataEntry(String key, byte[] value, int localeIndicator, int typeIndicator) {
    this.key = key;
    this.value = value;
    this.localeIndicator = localeIndicator;
    this.typeIndicator = typeIndicator;
  }

  private MdtaMetadataEntry(Parcel in) {
    key = Util.castNonNull(in.readString());
    value = Util.castNonNull(in.createByteArray());
    localeIndicator = in.readInt();
    typeIndicator = in.readInt();
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    MdtaMetadataEntry other = (MdtaMetadataEntry) obj;
    return key.equals(other.key)
        && Arrays.equals(value, other.value)
        && localeIndicator == other.localeIndicator
        && typeIndicator == other.typeIndicator;
  }

  @Override
  public int hashCode() {
    int result = 17;
    result = 31 * result + key.hashCode();
    result = 31 * result + Arrays.hashCode(value);
    result = 31 * result + localeIndicator;
    result = 31 * result + typeIndicator;
    return result;
  }

  @Override
  public String toString() {
    return "mdta: key=" + key;
  }

  // Parcelable implementation.

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(key);
    dest.writeByteArray(value);
    dest.writeInt(localeIndicator);
    dest.writeInt(typeIndicator);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public static final Parcelable.Creator<MdtaMetadataEntry> CREATOR =
      new Parcelable.Creator<MdtaMetadataEntry>() {

        @Override
        public MdtaMetadataEntry createFromParcel(Parcel in) {
          return new MdtaMetadataEntry(in);
        }

        @Override
        public MdtaMetadataEntry[] newArray(int size) {
          return new MdtaMetadataEntry[size];
        }
      };
}
