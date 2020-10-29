/*
 * Created by ADT author on 10/1/20 11:19 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/22/20 6:23 PM
 */
package com.adt.vpm.videoplayer.source.hls;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.common.metadata.Metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Holds metadata associated to an HLS media track. */
public final class HlsTrackMetadataEntry implements Metadata.Entry {

  /** Holds attributes defined in an EXT-X-STREAM-INF tag. */
  public static final class VariantInfo implements Parcelable {

    /**
     * The average bitrate as declared by the AVERAGE-BANDWIDTH attribute of the EXT-X-STREAM-INF
     * tag, or {@link Format#NO_VALUE} if the attribute is not declared.
     */
    public final int averageBitrate;

    /** The peak bitrate as declared by the BANDWIDTH attribute of the EXT-X-STREAM-INF tag. */
    public final int peakBitrate;

    /**
     * The VIDEO value as defined in the EXT-X-STREAM-INF tag, or null if the VIDEO attribute is not
     * present.
     */
    @Nullable public final String videoGroupId;

    /**
     * The AUDIO value as defined in the EXT-X-STREAM-INF tag, or null if the AUDIO attribute is not
     * present.
     */
    @Nullable public final String audioGroupId;

    /**
     * The SUBTITLES value as defined in the EXT-X-STREAM-INF tag, or null if the SUBTITLES
     * attribute is not present.
     */
    @Nullable public final String subtitleGroupId;

    /**
     * The CLOSED-CAPTIONS value as defined in the EXT-X-STREAM-INF tag, or null if the
     * CLOSED-CAPTIONS attribute is not present.
     */
    @Nullable public final String captionGroupId;

    /**
     * Creates an instance.
     *
     * @param averageBitrate See {@link #averageBitrate}.
     * @param peakBitrate See {@link #peakBitrate}.
     * @param videoGroupId See {@link #videoGroupId}.
     * @param audioGroupId See {@link #audioGroupId}.
     * @param subtitleGroupId See {@link #subtitleGroupId}.
     * @param captionGroupId See {@link #captionGroupId}.
     */
    public VariantInfo(
        int averageBitrate,
        int peakBitrate,
        @Nullable String videoGroupId,
        @Nullable String audioGroupId,
        @Nullable String subtitleGroupId,
        @Nullable String captionGroupId) {
      this.averageBitrate = averageBitrate;
      this.peakBitrate = peakBitrate;
      this.videoGroupId = videoGroupId;
      this.audioGroupId = audioGroupId;
      this.subtitleGroupId = subtitleGroupId;
      this.captionGroupId = captionGroupId;
    }

    /* package */ VariantInfo(Parcel in) {
      averageBitrate = in.readInt();
      peakBitrate = in.readInt();
      videoGroupId = in.readString();
      audioGroupId = in.readString();
      subtitleGroupId = in.readString();
      captionGroupId = in.readString();
    }

    @Override
    public boolean equals(@Nullable Object other) {
      if (this == other) {
        return true;
      }
      if (other == null || getClass() != other.getClass()) {
        return false;
      }
      VariantInfo that = (VariantInfo) other;
      return averageBitrate == that.averageBitrate
          && peakBitrate == that.peakBitrate
          && TextUtils.equals(videoGroupId, that.videoGroupId)
          && TextUtils.equals(audioGroupId, that.audioGroupId)
          && TextUtils.equals(subtitleGroupId, that.subtitleGroupId)
          && TextUtils.equals(captionGroupId, that.captionGroupId);
    }

    @Override
    public int hashCode() {
      int result = averageBitrate;
      result = 31 * result + peakBitrate;
      result = 31 * result + (videoGroupId != null ? videoGroupId.hashCode() : 0);
      result = 31 * result + (audioGroupId != null ? audioGroupId.hashCode() : 0);
      result = 31 * result + (subtitleGroupId != null ? subtitleGroupId.hashCode() : 0);
      result = 31 * result + (captionGroupId != null ? captionGroupId.hashCode() : 0);
      return result;
    }

    // Parcelable implementation.

    @Override
    public int describeContents() {
      return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
      dest.writeInt(averageBitrate);
      dest.writeInt(peakBitrate);
      dest.writeString(videoGroupId);
      dest.writeString(audioGroupId);
      dest.writeString(subtitleGroupId);
      dest.writeString(captionGroupId);
    }

    public static final Creator<VariantInfo> CREATOR =
        new Creator<VariantInfo>() {
          @Override
          public VariantInfo createFromParcel(Parcel in) {
            return new VariantInfo(in);
          }

          @Override
          public VariantInfo[] newArray(int size) {
            return new VariantInfo[size];
          }
        };
  }

  /**
   * The GROUP-ID value of this track, if the track is derived from an EXT-X-MEDIA tag. Null if the
   * track is not derived from an EXT-X-MEDIA TAG.
   */
  @Nullable public final String groupId;
  /**
   * The NAME value of this track, if the track is derived from an EXT-X-MEDIA tag. Null if the
   * track is not derived from an EXT-X-MEDIA TAG.
   */
  @Nullable public final String name;
  /**
   * The EXT-X-STREAM-INF tags attributes associated with this track. This field is non-applicable
   * (and therefore empty) if this track is derived from an EXT-X-MEDIA tag.
   */
  public final List<VariantInfo> variantInfos;

  /**
   * Creates an instance.
   *
   * @param groupId See {@link #groupId}.
   * @param name See {@link #name}.
   * @param variantInfos See {@link #variantInfos}.
   */
  public HlsTrackMetadataEntry(
      @Nullable String groupId, @Nullable String name, List<VariantInfo> variantInfos) {
    this.groupId = groupId;
    this.name = name;
    this.variantInfos = Collections.unmodifiableList(new ArrayList<>(variantInfos));
  }

  /* package */ HlsTrackMetadataEntry(Parcel in) {
    groupId = in.readString();
    name = in.readString();
    int variantInfoSize = in.readInt();
    ArrayList<VariantInfo> variantInfos = new ArrayList<>(variantInfoSize);
    for (int i = 0; i < variantInfoSize; i++) {
      variantInfos.add(in.readParcelable(VariantInfo.class.getClassLoader()));
    }
    this.variantInfos = Collections.unmodifiableList(variantInfos);
  }

  @Override
  public String toString() {
    return "HlsTrackMetadataEntry" + (groupId != null ? (" [" + groupId + ", " + name + "]") : "");
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }

    HlsTrackMetadataEntry that = (HlsTrackMetadataEntry) other;
    return TextUtils.equals(groupId, that.groupId)
        && TextUtils.equals(name, that.name)
        && variantInfos.equals(that.variantInfos);
  }

  @Override
  public int hashCode() {
    int result = groupId != null ? groupId.hashCode() : 0;
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + variantInfos.hashCode();
    return result;
  }

  // Parcelable implementation.

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(groupId);
    dest.writeString(name);
    int variantInfosSize = variantInfos.size();
    dest.writeInt(variantInfosSize);
    for (int i = 0; i < variantInfosSize; i++) {
      dest.writeParcelable(variantInfos.get(i), /* parcelableFlags= */ 0);
    }
  }

  public static final Creator<HlsTrackMetadataEntry> CREATOR =
      new Creator<HlsTrackMetadataEntry>() {
        @Override
        public HlsTrackMetadataEntry createFromParcel(Parcel in) {
          return new HlsTrackMetadataEntry(in);
        }

        @Override
        public HlsTrackMetadataEntry[] newArray(int size) {
          return new HlsTrackMetadataEntry[size];
        }
      };
}
