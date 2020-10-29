/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.source;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.core.AbstractConcatenatedTimeline;
import com.adt.vpm.videoplayer.source.core.ExoPlayer;
import com.adt.vpm.videoplayer.source.core.Player;
import com.adt.vpm.videoplayer.source.core.Timeline;
import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.MediaItem;
import com.adt.vpm.videoplayer.source.core.upstream.Allocator;
import com.adt.vpm.videoplayer.source.common.upstream.TransferListener;
import com.adt.vpm.videoplayer.source.common.util.Assertions;
import java.util.HashMap;
import java.util.Map;

/**
 * Loops a {@link MediaSource} a specified number of times.
 *
 * <p>Note: To loop a {@link MediaSource} indefinitely, it is usually better to use {@link
 * ExoPlayer#setRepeatMode(int)} instead of this class.
 */
public final class LoopingMediaSource extends CompositeMediaSource<Void> {

  private final MaskingMediaSource maskingMediaSource;
  private final int loopCount;
  private final Map<MediaPeriodId, MediaPeriodId> childMediaPeriodIdToMediaPeriodId;
  private final Map<MediaPeriod, MediaPeriodId> mediaPeriodToChildMediaPeriodId;

  /**
   * Loops the provided source indefinitely. Note that it is usually better to use
   * {@link ExoPlayer#setRepeatMode(int)}.
   *
   * @param childSource The {@link MediaSource} to loop.
   */
  public LoopingMediaSource(MediaSource childSource) {
    this(childSource, Integer.MAX_VALUE);
  }

  /**
   * Loops the provided source a specified number of times.
   *
   * @param childSource The {@link MediaSource} to loop.
   * @param loopCount The desired number of loops. Must be strictly positive.
   */
  public LoopingMediaSource(MediaSource childSource, int loopCount) {
    Assertions.checkArgument(loopCount > 0);
    this.maskingMediaSource = new MaskingMediaSource(childSource, /* useLazyPreparation= */ false);
    this.loopCount = loopCount;
    childMediaPeriodIdToMediaPeriodId = new HashMap<>();
    mediaPeriodToChildMediaPeriodId = new HashMap<>();
  }

  /**
   * @deprecated Use {@link #getMediaItem()} and {@link MediaItem.PlaybackProperties#tag} instead.
   */
  @SuppressWarnings("deprecation")
  @Deprecated
  @Override
  @Nullable
  public Object getTag() {
    return maskingMediaSource.getTag();
  }

  @Override
  public MediaItem getMediaItem() {
    return maskingMediaSource.getMediaItem();
  }

  @Override
  @Nullable
  public Timeline getInitialTimeline() {
    return loopCount != Integer.MAX_VALUE
        ? new LoopingTimeline(maskingMediaSource.getTimeline(), loopCount)
        : new InfinitelyLoopingTimeline(maskingMediaSource.getTimeline());
  }

  @Override
  public boolean isSingleWindow() {
    return false;
  }

  @Override
  protected void prepareSourceInternal(@Nullable TransferListener mediaTransferListener) {
    super.prepareSourceInternal(mediaTransferListener);
    prepareChildSource(/* id= */ null, maskingMediaSource);
  }

  @Override
  public MediaPeriod createPeriod(MediaPeriodId id, Allocator allocator, long startPositionUs) {
    if (loopCount == Integer.MAX_VALUE) {
      return maskingMediaSource.createPeriod(id, allocator, startPositionUs);
    }
    Object childPeriodUid = LoopingTimeline.getChildPeriodUidFromConcatenatedUid(id.periodUid);
    MediaPeriodId childMediaPeriodId = id.copyWithPeriodUid(childPeriodUid);
    childMediaPeriodIdToMediaPeriodId.put(childMediaPeriodId, id);
    MediaPeriod mediaPeriod =
        maskingMediaSource.createPeriod(childMediaPeriodId, allocator, startPositionUs);
    mediaPeriodToChildMediaPeriodId.put(mediaPeriod, childMediaPeriodId);
    return mediaPeriod;
  }

  @Override
  public void releasePeriod(MediaPeriod mediaPeriod) {
    maskingMediaSource.releasePeriod(mediaPeriod);
    @Nullable
    MediaPeriodId childMediaPeriodId = mediaPeriodToChildMediaPeriodId.remove(mediaPeriod);
    if (childMediaPeriodId != null) {
      childMediaPeriodIdToMediaPeriodId.remove(childMediaPeriodId);
    }
  }

  @Override
  protected void onChildSourceInfoRefreshed(Void id, MediaSource mediaSource, Timeline timeline) {
    Timeline loopingTimeline =
        loopCount != Integer.MAX_VALUE
            ? new LoopingTimeline(timeline, loopCount)
            : new InfinitelyLoopingTimeline(timeline);
    refreshSourceInfo(loopingTimeline);
  }

  @Override
  @Nullable
  protected MediaPeriodId getMediaPeriodIdForChildMediaPeriodId(
      Void id, MediaPeriodId mediaPeriodId) {
    return loopCount != Integer.MAX_VALUE
        ? childMediaPeriodIdToMediaPeriodId.get(mediaPeriodId)
        : mediaPeriodId;
  }

  private static final class LoopingTimeline extends AbstractConcatenatedTimeline {

    private final Timeline childTimeline;
    private final int childPeriodCount;
    private final int childWindowCount;
    private final int loopCount;

    public LoopingTimeline(Timeline childTimeline, int loopCount) {
      super(/* isAtomic= */ false, new ShuffleOrder.UnshuffledShuffleOrder(loopCount));
      this.childTimeline = childTimeline;
      childPeriodCount = childTimeline.getPeriodCount();
      childWindowCount = childTimeline.getWindowCount();
      this.loopCount = loopCount;
      if (childPeriodCount > 0) {
        Assertions.checkState(loopCount <= Integer.MAX_VALUE / childPeriodCount,
            "LoopingMediaSource contains too many periods");
      }
    }

    @Override
    public int getWindowCount() {
      return childWindowCount * loopCount;
    }

    @Override
    public int getPeriodCount() {
      return childPeriodCount * loopCount;
    }

    @Override
    protected int getChildIndexByPeriodIndex(int periodIndex) {
      return periodIndex / childPeriodCount;
    }

    @Override
    protected int getChildIndexByWindowIndex(int windowIndex) {
      return windowIndex / childWindowCount;
    }

    @Override
    protected int getChildIndexByChildUid(Object childUid) {
      if (!(childUid instanceof Integer)) {
        return C.INDEX_UNSET;
      }
      return (Integer) childUid;
    }

    @Override
    protected Timeline getTimelineByChildIndex(int childIndex) {
      return childTimeline;
    }

    @Override
    protected int getFirstPeriodIndexByChildIndex(int childIndex) {
      return childIndex * childPeriodCount;
    }

    @Override
    protected int getFirstWindowIndexByChildIndex(int childIndex) {
      return childIndex * childWindowCount;
    }

    @Override
    protected Object getChildUidByChildIndex(int childIndex) {
      return childIndex;
    }

  }

  private static final class InfinitelyLoopingTimeline extends ForwardingTimeline {

    public InfinitelyLoopingTimeline(Timeline timeline) {
      super(timeline);
    }

    @Override
    public int getNextWindowIndex(int windowIndex, @Player.RepeatMode int repeatMode,
        boolean shuffleModeEnabled) {
      int childNextWindowIndex = timeline.getNextWindowIndex(windowIndex, repeatMode,
          shuffleModeEnabled);
      return childNextWindowIndex == C.INDEX_UNSET ? getFirstWindowIndex(shuffleModeEnabled)
          : childNextWindowIndex;
    }

    @Override
    public int getPreviousWindowIndex(int windowIndex, @Player.RepeatMode int repeatMode,
        boolean shuffleModeEnabled) {
      int childPreviousWindowIndex = timeline.getPreviousWindowIndex(windowIndex, repeatMode,
          shuffleModeEnabled);
      return childPreviousWindowIndex == C.INDEX_UNSET ? getLastWindowIndex(shuffleModeEnabled)
          : childPreviousWindowIndex;
    }

  }

}
