/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.source;

import androidx.annotation.Nullable;
import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.Format;

/** Descriptor for data being loaded or selected by a media source. */
public final class MediaLoadData {

  /** One of the {@link C} {@code DATA_TYPE_*} constants defining the type of data. */
  public final int dataType;
  /**
   * One of the {@link C} {@code TRACK_TYPE_*} constants if the data corresponds to media of a
   * specific type. {@link C#TRACK_TYPE_UNKNOWN} otherwise.
   */
  public final int trackType;
  /**
   * The format of the track to which the data belongs. Null if the data does not belong to a
   * specific track.
   */
  @Nullable public final Format trackFormat;
  /**
   * One of the {@link C} {@code SELECTION_REASON_*} constants if the data belongs to a track.
   * {@link C#SELECTION_REASON_UNKNOWN} otherwise.
   */
  public final int trackSelectionReason;
  /**
   * Optional data associated with the selection of the track to which the data belongs. Null if the
   * data does not belong to a track.
   */
  @Nullable public final Object trackSelectionData;
  /**
   * The start time of the media, or {@link C#TIME_UNSET} if the data does not belong to a specific
   * media period.
   */
  public final long mediaStartTimeMs;
  /**
   * The end time of the media, or {@link C#TIME_UNSET} if the data does not belong to a specific
   * media period or the end time is unknown.
   */
  public final long mediaEndTimeMs;

  /** Creates an instance with the given {@link #dataType}. */
  public MediaLoadData(int dataType) {
    this(
        dataType,
        /* trackType= */ C.TRACK_TYPE_UNKNOWN,
        /* trackFormat= */ null,
        /* trackSelectionReason= */ C.SELECTION_REASON_UNKNOWN,
        /* trackSelectionData= */ null,
        /* mediaStartTimeMs= */ C.TIME_UNSET,
        /* mediaEndTimeMs= */ C.TIME_UNSET);
  }

  /**
   * Creates media load data.
   *
   * @param dataType See {@link #dataType}.
   * @param trackType See {@link #trackType}.
   * @param trackFormat See {@link #trackFormat}.
   * @param trackSelectionReason See {@link #trackSelectionReason}.
   * @param trackSelectionData See {@link #trackSelectionData}.
   * @param mediaStartTimeMs See {@link #mediaStartTimeMs}.
   * @param mediaEndTimeMs See {@link #mediaEndTimeMs}.
   */
  public MediaLoadData(
      int dataType,
      int trackType,
      @Nullable Format trackFormat,
      int trackSelectionReason,
      @Nullable Object trackSelectionData,
      long mediaStartTimeMs,
      long mediaEndTimeMs) {
    this.dataType = dataType;
    this.trackType = trackType;
    this.trackFormat = trackFormat;
    this.trackSelectionReason = trackSelectionReason;
    this.trackSelectionData = trackSelectionData;
    this.mediaStartTimeMs = mediaStartTimeMs;
    this.mediaEndTimeMs = mediaEndTimeMs;
  }
}
