/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.trackselection;

import android.os.SystemClock;
import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.core.source.TrackGroup;
import com.adt.vpm.videoplayer.source.core.source.chunk.MediaChunk;
import com.adt.vpm.videoplayer.source.core.source.chunk.MediaChunkIterator;
import com.adt.vpm.videoplayer.source.core.upstream.BandwidthMeter;
import com.adt.vpm.videoplayer.source.common.C;

import java.util.List;
import java.util.Random;
import org.checkerframework.checker.nullness.compatqual.NullableType;

/**
 * A {@link TrackSelection} whose selected track is updated randomly.
 */
public final class RandomTrackSelection extends BaseTrackSelection {

  /**
   * Factory for {@link RandomTrackSelection} instances.
   */
  public static final class Factory implements TrackSelection.Factory {

    private final Random random;

    public Factory() {
      random = new Random();
    }

    /**
     * @param seed A seed for the {@link Random} instance used by the factory.
     */
    public Factory(int seed) {
      random = new Random(seed);
    }

    @Override
    public @NullableType TrackSelection[] createTrackSelections(
        @NullableType Definition[] definitions, BandwidthMeter bandwidthMeter) {
      return TrackSelectionUtil.createTrackSelectionsForDefinitions(
          definitions,
          definition -> new RandomTrackSelection(definition.group, definition.tracks, random));
    }
  }

  private final Random random;

  private int selectedIndex;

  /**
   * @param group The {@link TrackGroup}. Must not be null.
   * @param tracks The indices of the selected tracks within the {@link TrackGroup}. Must not be
   *     null or empty. May be in any order.
   */
  public RandomTrackSelection(TrackGroup group, int... tracks) {
    super(group, tracks);
    random = new Random();
    selectedIndex = random.nextInt(length);
  }

  /**
   * @param group The {@link TrackGroup}. Must not be null.
   * @param tracks The indices of the selected tracks within the {@link TrackGroup}. Must not be
   *     null or empty. May be in any order.
   * @param seed A seed for the {@link Random} instance used to update the selected track.
   */
  public RandomTrackSelection(TrackGroup group, int[] tracks, long seed) {
    this(group, tracks, new Random(seed));
  }

  /**
   * @param group The {@link TrackGroup}. Must not be null.
   * @param tracks The indices of the selected tracks within the {@link TrackGroup}. Must not be
   *     null or empty. May be in any order.
   * @param random A source of random numbers.
   */
  public RandomTrackSelection(TrackGroup group, int[] tracks, Random random) {
    super(group, tracks);
    this.random = random;
    selectedIndex = random.nextInt(length);
  }

  @Override
  public void updateSelectedTrack(
      long playbackPositionUs,
      long bufferedDurationUs,
      long availableDurationUs,
      List<? extends MediaChunk> queue,
      MediaChunkIterator[] mediaChunkIterators) {
    // Count the number of allowed formats.
    long nowMs = SystemClock.elapsedRealtime();
    int allowedFormatCount = 0;
    for (int i = 0; i < length; i++) {
      if (!isBlacklisted(i, nowMs)) {
        allowedFormatCount++;
      }
    }

    selectedIndex = random.nextInt(allowedFormatCount);
    if (allowedFormatCount != length) {
      // Adjust the format index to account for excluded formats.
      allowedFormatCount = 0;
      for (int i = 0; i < length; i++) {
        if (!isBlacklisted(i, nowMs) && selectedIndex == allowedFormatCount++) {
          selectedIndex = i;
          return;
        }
      }
    }
  }

  @Override
  public int getSelectedIndex() {
    return selectedIndex;
  }

  @Override
  public int getSelectionReason() {
    return C.SELECTION_REASON_ADAPTIVE;
  }

  @Override
  @Nullable
  public Object getSelectionData() {
    return null;
  }

}
