/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.source;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.core.Timeline;
import com.adt.vpm.videoplayer.source.common.MediaItem;
import com.adt.vpm.videoplayer.source.core.upstream.Allocator;
import com.adt.vpm.videoplayer.source.common.upstream.TransferListener;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * Merges multiple {@link MediaSource}s.
 *
 * <p>The {@link Timeline}s of the sources being merged must have the same number of periods.
 */
public final class MergingMediaSource extends CompositeMediaSource<Integer> {

  /**
   * Thrown when a {@link MergingMediaSource} cannot merge its sources.
   */
  public static final class IllegalMergeException extends IOException {

    /** The reason the merge failed. One of {@link #REASON_PERIOD_COUNT_MISMATCH}. */
    @Documented
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({REASON_PERIOD_COUNT_MISMATCH})
    public @interface Reason {}
    /**
     * The sources have different period counts.
     */
    public static final int REASON_PERIOD_COUNT_MISMATCH = 0;

    /**
     * The reason the merge failed.
     */
    @Reason public final int reason;

    /**
     * @param reason The reason the merge failed.
     */
    public IllegalMergeException(@Reason int reason) {
      this.reason = reason;
    }

  }

  private static final int PERIOD_COUNT_UNSET = -1;
  private static final MediaItem EMPTY_MEDIA_ITEM =
      new MediaItem.Builder().setMediaId("MergingMediaSource").build();

  private final boolean adjustPeriodTimeOffsets;
  private final MediaSource[] mediaSources;
  private final Timeline[] timelines;
  private final ArrayList<MediaSource> pendingTimelineSources;
  private final CompositeSequenceableLoaderFactory compositeSequenceableLoaderFactory;

  private int periodCount;
  private long[][] periodTimeOffsetsUs;
  @Nullable private IllegalMergeException mergeError;

  /**
   * Creates a merging media source.
   *
   * <p>Offsets between the timestamps in the media sources will not be adjusted.
   *
   * @param mediaSources The {@link MediaSource}s to merge.
   */
  public MergingMediaSource(MediaSource... mediaSources) {
    this(/* adjustPeriodTimeOffsets= */ false, mediaSources);
  }

  /**
   * Creates a merging media source.
   *
   * @param adjustPeriodTimeOffsets Whether to adjust timestamps of the merged media sources to all
   *     start at the same time.
   * @param mediaSources The {@link MediaSource}s to merge.
   */
  public MergingMediaSource(boolean adjustPeriodTimeOffsets, MediaSource... mediaSources) {
    this(adjustPeriodTimeOffsets, new DefaultCompositeSequenceableLoaderFactory(), mediaSources);
  }

  /**
   * Creates a merging media source.
   *
   * @param adjustPeriodTimeOffsets Whether to adjust timestamps of the merged media sources to all
   *     start at the same time.
   * @param compositeSequenceableLoaderFactory A factory to create composite {@link
   *     SequenceableLoader}s for when this media source loads data from multiple streams (video,
   *     audio etc...).
   * @param mediaSources The {@link MediaSource}s to merge.
   */
  public MergingMediaSource(
      boolean adjustPeriodTimeOffsets,
      CompositeSequenceableLoaderFactory compositeSequenceableLoaderFactory,
      MediaSource... mediaSources) {
    this.adjustPeriodTimeOffsets = adjustPeriodTimeOffsets;
    this.mediaSources = mediaSources;
    this.compositeSequenceableLoaderFactory = compositeSequenceableLoaderFactory;
    pendingTimelineSources = new ArrayList<>(Arrays.asList(mediaSources));
    periodCount = PERIOD_COUNT_UNSET;
    timelines = new Timeline[mediaSources.length];
    periodTimeOffsetsUs = new long[0][];
  }

  /**
   * @deprecated Use {@link #getMediaItem()} and {@link MediaItem.PlaybackProperties#tag} instead.
   */
  @SuppressWarnings("deprecation")
  @Deprecated
  @Override
  @Nullable
  public Object getTag() {
    return mediaSources.length > 0 ? mediaSources[0].getTag() : null;
  }

  @Override
  public MediaItem getMediaItem() {
    return mediaSources.length > 0 ? mediaSources[0].getMediaItem() : EMPTY_MEDIA_ITEM;
  }

  @Override
  protected void prepareSourceInternal(@Nullable TransferListener mediaTransferListener) {
    super.prepareSourceInternal(mediaTransferListener);
    for (int i = 0; i < mediaSources.length; i++) {
      prepareChildSource(i, mediaSources[i]);
    }
  }

  @Override
  public void maybeThrowSourceInfoRefreshError() throws IOException {
    if (mergeError != null) {
      throw mergeError;
    }
    super.maybeThrowSourceInfoRefreshError();
  }

  @Override
  public MediaPeriod createPeriod(MediaPeriodId id, Allocator allocator, long startPositionUs) {
    MediaPeriod[] periods = new MediaPeriod[mediaSources.length];
    int periodIndex = timelines[0].getIndexOfPeriod(id.periodUid);
    for (int i = 0; i < periods.length; i++) {
      MediaPeriodId childMediaPeriodId =
          id.copyWithPeriodUid(timelines[i].getUidOfPeriod(periodIndex));
      periods[i] =
          mediaSources[i].createPeriod(
              childMediaPeriodId, allocator, startPositionUs - periodTimeOffsetsUs[periodIndex][i]);
    }
    return new MergingMediaPeriod(
        compositeSequenceableLoaderFactory, periodTimeOffsetsUs[periodIndex], periods);
  }

  @Override
  public void releasePeriod(MediaPeriod mediaPeriod) {
    MergingMediaPeriod mergingPeriod = (MergingMediaPeriod) mediaPeriod;
    for (int i = 0; i < mediaSources.length; i++) {
      mediaSources[i].releasePeriod(mergingPeriod.getChildPeriod(i));
    }
  }

  @Override
  protected void releaseSourceInternal() {
    super.releaseSourceInternal();
    Arrays.fill(timelines, null);
    periodCount = PERIOD_COUNT_UNSET;
    mergeError = null;
    pendingTimelineSources.clear();
    Collections.addAll(pendingTimelineSources, mediaSources);
  }

  @Override
  protected void onChildSourceInfoRefreshed(
      Integer id, MediaSource mediaSource, Timeline timeline) {
    if (mergeError != null) {
      return;
    }
    if (periodCount == PERIOD_COUNT_UNSET) {
      periodCount = timeline.getPeriodCount();
    } else if (timeline.getPeriodCount() != periodCount) {
      mergeError = new IllegalMergeException(IllegalMergeException.REASON_PERIOD_COUNT_MISMATCH);
      return;
    }
    if (periodTimeOffsetsUs.length == 0) {
      periodTimeOffsetsUs = new long[periodCount][timelines.length];
    }
    pendingTimelineSources.remove(mediaSource);
    timelines[id] = timeline;
    if (pendingTimelineSources.isEmpty()) {
      if (adjustPeriodTimeOffsets) {
        computePeriodTimeOffsets();
      }
      refreshSourceInfo(timelines[0]);
    }
  }

  @Override
  @Nullable
  protected MediaPeriodId getMediaPeriodIdForChildMediaPeriodId(
      Integer id, MediaPeriodId mediaPeriodId) {
    return id == 0 ? mediaPeriodId : null;
  }

  private void computePeriodTimeOffsets() {
    Timeline.Period period = new Timeline.Period();
    for (int periodIndex = 0; periodIndex < periodCount; periodIndex++) {
      long primaryWindowOffsetUs =
          -timelines[0].getPeriod(periodIndex, period).getPositionInWindowUs();
      for (int timelineIndex = 1; timelineIndex < timelines.length; timelineIndex++) {
        long secondaryWindowOffsetUs =
            -timelines[timelineIndex].getPeriod(periodIndex, period).getPositionInWindowUs();
        periodTimeOffsetsUs[periodIndex][timelineIndex] =
            primaryWindowOffsetUs - secondaryWindowOffsetUs;
      }
    }
  }
}
