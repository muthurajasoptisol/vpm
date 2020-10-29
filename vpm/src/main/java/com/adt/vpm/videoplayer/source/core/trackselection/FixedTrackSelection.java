/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.trackselection;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.core.source.TrackGroup;
import com.adt.vpm.videoplayer.source.core.source.chunk.MediaChunk;
import com.adt.vpm.videoplayer.source.core.source.chunk.MediaChunkIterator;
import com.adt.vpm.videoplayer.source.core.upstream.BandwidthMeter;
import com.adt.vpm.videoplayer.source.common.C;

import java.util.List;
import org.checkerframework.checker.nullness.compatqual.NullableType;

/**
 * A {@link TrackSelection} consisting of a single track.
 */
public final class FixedTrackSelection extends BaseTrackSelection {

  /**
   * @deprecated Don't use as adaptive track selection factory as it will throw when multiple tracks
   *     are selected. If you would like to disable adaptive selection in {@link
   *     DefaultTrackSelector}, enable the {@link
   *     DefaultTrackSelector.Parameters#forceHighestSupportedBitrate} flag instead.
   */
  @Deprecated
  public static final class Factory implements TrackSelection.Factory {

    private final int reason;
    @Nullable private final Object data;

    public Factory() {
      this.reason = C.SELECTION_REASON_UNKNOWN;
      this.data = null;
    }

    /**
     * @param reason A reason for the track selection.
     * @param data Optional data associated with the track selection.
     */
    public Factory(int reason, @Nullable Object data) {
      this.reason = reason;
      this.data = data;
    }

    @Override
    public @NullableType TrackSelection[] createTrackSelections(
        @NullableType Definition[] definitions, BandwidthMeter bandwidthMeter) {
      return TrackSelectionUtil.createTrackSelectionsForDefinitions(
          definitions,
          definition ->
              new FixedTrackSelection(definition.group, definition.tracks[0], reason, data));
    }
  }

  private final int reason;
  @Nullable private final Object data;

  /**
   * @param group The {@link TrackGroup}. Must not be null.
   * @param track The index of the selected track within the {@link TrackGroup}.
   */
  public FixedTrackSelection(TrackGroup group, int track) {
    this(group, track, C.SELECTION_REASON_UNKNOWN, null);
  }

  /**
   * @param group The {@link TrackGroup}. Must not be null.
   * @param track The index of the selected track within the {@link TrackGroup}.
   * @param reason A reason for the track selection.
   * @param data Optional data associated with the track selection.
   */
  public FixedTrackSelection(TrackGroup group, int track, int reason, @Nullable Object data) {
    super(group, track);
    this.reason = reason;
    this.data = data;
  }

  @Override
  public void updateSelectedTrack(
      long playbackPositionUs,
      long bufferedDurationUs,
      long availableDurationUs,
      List<? extends MediaChunk> queue,
      MediaChunkIterator[] mediaChunkIterators) {
    // Do nothing.
  }

  @Override
  public int getSelectedIndex() {
    return 0;
  }

  @Override
  public int getSelectionReason() {
    return reason;
  }

  @Override
  @Nullable
  public Object getSelectionData() {
    return data;
  }

}
