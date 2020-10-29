/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.analytics;

import android.view.Surface;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.Format;
import com.adt.vpm.videoplayer.source.common.MediaItem;
import com.adt.vpm.videoplayer.source.common.audio.AudioAttributes;
import com.adt.vpm.videoplayer.source.common.metadata.Metadata;
import com.adt.vpm.videoplayer.source.common.util.Assertions;
import com.adt.vpm.videoplayer.source.core.ExoPlaybackException;
import com.adt.vpm.videoplayer.source.core.PlaybackParameters;
import com.adt.vpm.videoplayer.source.core.Player;
import com.adt.vpm.videoplayer.source.core.Timeline;
import com.adt.vpm.videoplayer.source.core.audio.AudioListener;
import com.adt.vpm.videoplayer.source.core.audio.AudioRendererEventListener;
import com.adt.vpm.videoplayer.source.core.decoder.DecoderCounters;
import com.adt.vpm.videoplayer.source.core.drm.DrmSessionEventListener;
import com.adt.vpm.videoplayer.source.core.metadata.MetadataOutput;
import com.adt.vpm.videoplayer.source.core.source.LoadEventInfo;
import com.adt.vpm.videoplayer.source.core.source.MediaLoadData;
import com.adt.vpm.videoplayer.source.core.source.MediaSource;
import com.adt.vpm.videoplayer.source.core.source.MediaSourceEventListener;
import com.adt.vpm.videoplayer.source.core.source.TrackGroupArray;
import com.adt.vpm.videoplayer.source.core.trackselection.TrackSelectionArray;
import com.adt.vpm.videoplayer.source.core.upstream.BandwidthMeter;
import com.adt.vpm.videoplayer.source.core.util.Clock;
import com.adt.vpm.videoplayer.source.core.video.VideoListener;
import com.adt.vpm.videoplayer.source.core.video.VideoRendererEventListener;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.adt.vpm.videoplayer.source.common.util.Assertions.checkNotNull;

/**
 * Data collector which is able to forward analytics events to {@link AnalyticsListener}s by
 * listening to all available ExoPlayer listeners.
 */
public class AnalyticsCollector
    implements Player.EventListener,
        MetadataOutput,
        AudioRendererEventListener,
        VideoRendererEventListener,
        MediaSourceEventListener,
        BandwidthMeter.EventListener,
        DrmSessionEventListener,
        VideoListener,
        AudioListener {

  private final CopyOnWriteArraySet<AnalyticsListener> listeners;
  private final Clock clock;
  private final Timeline.Period period;
  private final Timeline.Window window;
  private final MediaPeriodQueueTracker mediaPeriodQueueTracker;

  private @MonotonicNonNull Player player;
  private boolean isSeeking;

  /**
   * Creates an analytics collector.
   *
   * @param clock A {@link Clock} used to generate timestamps.
   */
  public AnalyticsCollector(Clock clock) {
    this.clock = checkNotNull(clock);
    listeners = new CopyOnWriteArraySet<>();
    period = new Timeline.Period();
    window = new Timeline.Window();
    mediaPeriodQueueTracker = new MediaPeriodQueueTracker(period);
  }

  /**
   * Adds a listener for analytics events.
   *
   * @param listener The listener to add.
   */
  public void addListener(AnalyticsListener listener) {
    Assertions.checkNotNull(listener);
    listeners.add(listener);
  }

  /**
   * Removes a previously added analytics event listener.
   *
   * @param listener The listener to remove.
   */
  public void removeListener(AnalyticsListener listener) {
    listeners.remove(listener);
  }

  /**
   * Sets the player for which data will be collected. Must only be called if no player has been set
   * yet or the current player is idle.
   *
   * @param player The {@link Player} for which data will be collected.
   */
  public void setPlayer(Player player) {
    Assertions.checkState(
        this.player == null || mediaPeriodQueueTracker.mediaPeriodQueue.isEmpty());
    this.player = checkNotNull(player);
  }

  /**
   * Updates the playback queue information used for event association.
   *
   * <p>Should only be called by the player controlling the queue and not from app code.
   *
   * @param queue The playback queue of media periods identified by their {@link MediaSource.MediaPeriodId}.
   * @param readingPeriod The media period in the queue that is currently being read by renderers,
   *     or null if the queue is empty.
   */
  public void updateMediaPeriodQueueInfo(
          List<MediaSource.MediaPeriodId> queue, @Nullable MediaSource.MediaPeriodId readingPeriod) {
    mediaPeriodQueueTracker.onQueueUpdated(queue, readingPeriod, checkNotNull(player));
  }

  // External events.

  /**
   * Notify analytics collector that a seek operation will start. Should be called before the player
   * adjusts its state and position to the seek.
   */
  public final void notifySeekStarted() {
    if (!isSeeking) {
      AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
      isSeeking = true;
      for (AnalyticsListener listener : listeners) {
        listener.onSeekStarted(eventTime);
      }
    }
  }

  /** Resets the analytics collector for a new playlist. */
  public final void resetForNewPlaylist() {
    // TODO: remove method.
  }

  // MetadataOutput implementation.

  @Override
  public final void onMetadata(Metadata metadata) {
    AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
    for (AnalyticsListener listener : listeners) {
      listener.onMetadata(eventTime, metadata);
    }
  }

  // AudioRendererEventListener implementation.

  @SuppressWarnings("deprecation")
  @Override
  public final void onAudioEnabled(DecoderCounters counters) {
    AnalyticsListener.EventTime eventTime = generateReadingMediaPeriodEventTime();
    for (AnalyticsListener listener : listeners) {
      listener.onAudioEnabled(eventTime, counters);
      listener.onDecoderEnabled(eventTime, C.TRACK_TYPE_AUDIO, counters);
    }
  }

  @SuppressWarnings("deprecation")
  @Override
  public final void onAudioDecoderInitialized(
      String decoderName, long initializedTimestampMs, long initializationDurationMs) {
    AnalyticsListener.EventTime eventTime = generateReadingMediaPeriodEventTime();
    for (AnalyticsListener listener : listeners) {
      listener.onAudioDecoderInitialized(eventTime, decoderName, initializationDurationMs);
      listener.onDecoderInitialized(
          eventTime, C.TRACK_TYPE_AUDIO, decoderName, initializationDurationMs);
    }
  }

  @SuppressWarnings("deprecation")
  @Override
  public final void onAudioInputFormatChanged(Format format) {
    AnalyticsListener.EventTime eventTime = generateReadingMediaPeriodEventTime();
    for (AnalyticsListener listener : listeners) {
      listener.onAudioInputFormatChanged(eventTime, format);
      listener.onDecoderInputFormatChanged(eventTime, C.TRACK_TYPE_AUDIO, format);
    }
  }

  @Override
  public final void onAudioPositionAdvancing(long playoutStartSystemTimeMs) {
    AnalyticsListener.EventTime eventTime = generateReadingMediaPeriodEventTime();
    for (AnalyticsListener listener : listeners) {
      listener.onAudioPositionAdvancing(eventTime, playoutStartSystemTimeMs);
    }
  }

  @Override
  public final void onAudioUnderrun(
      int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {
    AnalyticsListener.EventTime eventTime = generateReadingMediaPeriodEventTime();
    for (AnalyticsListener listener : listeners) {
      listener.onAudioUnderrun(eventTime, bufferSize, bufferSizeMs, elapsedSinceLastFeedMs);
    }
  }

  @SuppressWarnings("deprecation")
  @Override
  public final void onAudioDisabled(DecoderCounters counters) {
    AnalyticsListener.EventTime eventTime = generatePlayingMediaPeriodEventTime();
    for (AnalyticsListener listener : listeners) {
      listener.onAudioDisabled(eventTime, counters);
      listener.onDecoderDisabled(eventTime, C.TRACK_TYPE_AUDIO, counters);
    }
  }

  // AudioListener implementation.

  @Override
  public final void onAudioSessionId(int audioSessionId) {
    AnalyticsListener.EventTime eventTime = generateReadingMediaPeriodEventTime();
    for (AnalyticsListener listener : listeners) {
      listener.onAudioSessionId(eventTime, audioSessionId);
    }
  }

  @Override
  public void onAudioAttributesChanged(AudioAttributes audioAttributes) {
    AnalyticsListener.EventTime eventTime = generateReadingMediaPeriodEventTime();
    for (AnalyticsListener listener : listeners) {
      listener.onAudioAttributesChanged(eventTime, audioAttributes);
    }
  }

  @Override
  public void onSkipSilenceEnabledChanged(boolean skipSilenceEnabled) {
    AnalyticsListener.EventTime eventTime = generateReadingMediaPeriodEventTime();
    for (AnalyticsListener listener : listeners) {
      listener.onSkipSilenceEnabledChanged(eventTime, skipSilenceEnabled);
    }
  }

  @Override
  public void onVolumeChanged(float audioVolume) {
    AnalyticsListener.EventTime eventTime = generateReadingMediaPeriodEventTime();
    for (AnalyticsListener listener : listeners) {
      listener.onVolumeChanged(eventTime, audioVolume);
    }
  }

  // VideoRendererEventListener implementation.

  @SuppressWarnings("deprecation")
  @Override
  public final void onVideoEnabled(DecoderCounters counters) {
    AnalyticsListener.EventTime eventTime = generateReadingMediaPeriodEventTime();
    for (AnalyticsListener listener : listeners) {
      listener.onVideoEnabled(eventTime, counters);
      listener.onDecoderEnabled(eventTime, C.TRACK_TYPE_VIDEO, counters);
    }
  }

  @SuppressWarnings("deprecation")
  @Override
  public final void onVideoDecoderInitialized(
      String decoderName, long initializedTimestampMs, long initializationDurationMs) {
    AnalyticsListener.EventTime eventTime = generateReadingMediaPeriodEventTime();
    for (AnalyticsListener listener : listeners) {
      listener.onVideoDecoderInitialized(eventTime, decoderName, initializationDurationMs);
      listener.onDecoderInitialized(
          eventTime, C.TRACK_TYPE_VIDEO, decoderName, initializationDurationMs);
    }
  }

  @SuppressWarnings("deprecation")
  @Override
  public final void onVideoInputFormatChanged(Format format) {
    AnalyticsListener.EventTime eventTime = generateReadingMediaPeriodEventTime();
    for (AnalyticsListener listener : listeners) {
      listener.onVideoInputFormatChanged(eventTime, format);
      listener.onDecoderInputFormatChanged(eventTime, C.TRACK_TYPE_VIDEO, format);
    }
  }

  @Override
  public final void onDroppedFrames(int count, long elapsedMs) {
    AnalyticsListener.EventTime eventTime = generatePlayingMediaPeriodEventTime();
    for (AnalyticsListener listener : listeners) {
      listener.onDroppedVideoFrames(eventTime, count, elapsedMs);
    }
  }

  @SuppressWarnings("deprecation")
  @Override
  public final void onVideoDisabled(DecoderCounters counters) {
    AnalyticsListener.EventTime eventTime = generatePlayingMediaPeriodEventTime();
    for (AnalyticsListener listener : listeners) {
      listener.onVideoDisabled(eventTime, counters);
      listener.onDecoderDisabled(eventTime, C.TRACK_TYPE_VIDEO, counters);
    }
  }

  @Override
  public final void onRenderedFirstFrame(@Nullable Surface surface) {
    AnalyticsListener.EventTime eventTime = generateReadingMediaPeriodEventTime();
    for (AnalyticsListener listener : listeners) {
      listener.onRenderedFirstFrame(eventTime, surface);
    }
  }

  @Override
  public final void onVideoFrameProcessingOffset(long totalProcessingOffsetUs, int frameCount) {
    AnalyticsListener.EventTime eventTime = generatePlayingMediaPeriodEventTime();
    for (AnalyticsListener listener : listeners) {
      listener.onVideoFrameProcessingOffset(eventTime, totalProcessingOffsetUs, frameCount);
    }
  }

  // VideoListener implementation.

  @Override
  public final void onRenderedFirstFrame() {
    // Do nothing. Already reported in VideoRendererEventListener.onRenderedFirstFrame.
  }

  @Override
  public final void onVideoSizeChanged(
      int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
    AnalyticsListener.EventTime eventTime = generateReadingMediaPeriodEventTime();
    for (AnalyticsListener listener : listeners) {
      listener.onVideoSizeChanged(
          eventTime, width, height, unappliedRotationDegrees, pixelWidthHeightRatio);
    }
  }

  @Override
  public void onSurfaceSizeChanged(int width, int height) {
    AnalyticsListener.EventTime eventTime = generateReadingMediaPeriodEventTime();
    for (AnalyticsListener listener : listeners) {
      listener.onSurfaceSizeChanged(eventTime, width, height);
    }
  }

  // MediaSourceEventListener implementation.

  @Override
  public final void onLoadStarted(
      int windowIndex,
      @Nullable MediaSource.MediaPeriodId mediaPeriodId,
      LoadEventInfo loadEventInfo,
      MediaLoadData mediaLoadData) {
    AnalyticsListener.EventTime eventTime = generateMediaPeriodEventTime(windowIndex, mediaPeriodId);
    for (AnalyticsListener listener : listeners) {
      listener.onLoadStarted(eventTime, loadEventInfo, mediaLoadData);
    }
  }

  @Override
  public final void onLoadCompleted(
      int windowIndex,
      @Nullable MediaSource.MediaPeriodId mediaPeriodId,
      LoadEventInfo loadEventInfo,
      MediaLoadData mediaLoadData) {
    AnalyticsListener.EventTime eventTime = generateMediaPeriodEventTime(windowIndex, mediaPeriodId);
    for (AnalyticsListener listener : listeners) {
      listener.onLoadCompleted(eventTime, loadEventInfo, mediaLoadData);
    }
  }

  @Override
  public final void onLoadCanceled(
      int windowIndex,
      @Nullable MediaSource.MediaPeriodId mediaPeriodId,
      LoadEventInfo loadEventInfo,
      MediaLoadData mediaLoadData) {
    AnalyticsListener.EventTime eventTime = generateMediaPeriodEventTime(windowIndex, mediaPeriodId);
    for (AnalyticsListener listener : listeners) {
      listener.onLoadCanceled(eventTime, loadEventInfo, mediaLoadData);
    }
  }

  @Override
  public final void onLoadError(
      int windowIndex,
      @Nullable MediaSource.MediaPeriodId mediaPeriodId,
      LoadEventInfo loadEventInfo,
      MediaLoadData mediaLoadData,
      IOException error,
      boolean wasCanceled) {
    AnalyticsListener.EventTime eventTime = generateMediaPeriodEventTime(windowIndex, mediaPeriodId);
    for (AnalyticsListener listener : listeners) {
      listener.onLoadError(eventTime, loadEventInfo, mediaLoadData, error, wasCanceled);
    }
  }

  @Override
  public final void onUpstreamDiscarded(
          int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, MediaLoadData mediaLoadData) {
    AnalyticsListener.EventTime eventTime = generateMediaPeriodEventTime(windowIndex, mediaPeriodId);
    for (AnalyticsListener listener : listeners) {
      listener.onUpstreamDiscarded(eventTime, mediaLoadData);
    }
  }

  @Override
  public final void onDownstreamFormatChanged(
          int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, MediaLoadData mediaLoadData) {
    AnalyticsListener.EventTime eventTime = generateMediaPeriodEventTime(windowIndex, mediaPeriodId);
    for (AnalyticsListener listener : listeners) {
      listener.onDownstreamFormatChanged(eventTime, mediaLoadData);
    }
  }

  // Player.EventListener implementation.

  // TODO: Add onFinishedReportingChanges to Player.EventListener to know when a set of simultaneous
  // callbacks finished. This helps to assign exactly the same EventTime to all of them instead of
  // having slightly different real times.

  @Override
  public final void onTimelineChanged(Timeline timeline, @Player.TimelineChangeReason int reason) {
    mediaPeriodQueueTracker.onTimelineChanged(checkNotNull(player));
    AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
    for (AnalyticsListener listener : listeners) {
      listener.onTimelineChanged(eventTime, reason);
    }
  }

  @Override
  public final void onMediaItemTransition(
      @Nullable MediaItem mediaItem, @Player.MediaItemTransitionReason int reason) {
    AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
    for (AnalyticsListener listener : listeners) {
      listener.onMediaItemTransition(eventTime, mediaItem, reason);
    }
  }

  @Override
  public final void onTracksChanged(
          TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
    AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
    for (AnalyticsListener listener : listeners) {
      listener.onTracksChanged(eventTime, trackGroups, trackSelections);
    }
  }

  @Override
  public final void onIsLoadingChanged(boolean isLoading) {
    AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
    for (AnalyticsListener listener : listeners) {
      listener.onIsLoadingChanged(eventTime, isLoading);
    }
  }

  @SuppressWarnings("deprecation")
  @Override
  public final void onPlayerStateChanged(boolean playWhenReady, @Player.State int playbackState) {
    AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
    for (AnalyticsListener listener : listeners) {
      listener.onPlayerStateChanged(eventTime, playWhenReady, playbackState);
    }
  }

  @Override
  public final void onPlaybackStateChanged(@Player.State int state) {
    AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
    for (AnalyticsListener listener : listeners) {
      listener.onPlaybackStateChanged(eventTime, state);
    }
  }

  @Override
  public final void onPlayWhenReadyChanged(
      boolean playWhenReady, @Player.PlayWhenReadyChangeReason int reason) {
    AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
    for (AnalyticsListener listener : listeners) {
      listener.onPlayWhenReadyChanged(eventTime, playWhenReady, reason);
    }
  }

  @Override
  public void onPlaybackSuppressionReasonChanged(
      @Player.PlaybackSuppressionReason int playbackSuppressionReason) {
    AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
    for (AnalyticsListener listener : listeners) {
      listener.onPlaybackSuppressionReasonChanged(eventTime, playbackSuppressionReason);
    }
  }

  @Override
  public void onIsPlayingChanged(boolean isPlaying) {
    AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
    for (AnalyticsListener listener : listeners) {
      listener.onIsPlayingChanged(eventTime, isPlaying);
    }
  }

  @Override
  public final void onRepeatModeChanged(@Player.RepeatMode int repeatMode) {
    AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
    for (AnalyticsListener listener : listeners) {
      listener.onRepeatModeChanged(eventTime, repeatMode);
    }
  }

  @Override
  public final void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
    AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
    for (AnalyticsListener listener : listeners) {
      listener.onShuffleModeChanged(eventTime, shuffleModeEnabled);
    }
  }

  @Override
  public final void onPlayerError(ExoPlaybackException error) {
    AnalyticsListener.EventTime eventTime =
        error.mediaPeriodId != null
            ? generateEventTime(error.mediaPeriodId)
            : generateCurrentPlayerMediaPeriodEventTime();
    for (AnalyticsListener listener : listeners) {
      listener.onPlayerError(eventTime, error);
    }
  }

  @Override
  public final void onPositionDiscontinuity(@Player.DiscontinuityReason int reason) {
    if (reason == Player.DISCONTINUITY_REASON_SEEK) {
      isSeeking = false;
    }
    mediaPeriodQueueTracker.onPositionDiscontinuity(checkNotNull(player));
    AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
    for (AnalyticsListener listener : listeners) {
      listener.onPositionDiscontinuity(eventTime, reason);
    }
  }

  @Override
  public final void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
    AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
    for (AnalyticsListener listener : listeners) {
      listener.onPlaybackParametersChanged(eventTime, playbackParameters);
    }
  }

  @SuppressWarnings("deprecation")
  @Override
  public final void onSeekProcessed() {
    AnalyticsListener.EventTime eventTime = generateCurrentPlayerMediaPeriodEventTime();
    for (AnalyticsListener listener : listeners) {
      listener.onSeekProcessed(eventTime);
    }
  }

  // BandwidthMeter.Listener implementation.

  @Override
  public final void onBandwidthSample(int elapsedMs, long bytes, long bitrate) {
    AnalyticsListener.EventTime eventTime = generateLoadingMediaPeriodEventTime();
    for (AnalyticsListener listener : listeners) {
      listener.onBandwidthEstimate(eventTime, elapsedMs, bytes, bitrate);
    }
  }

  // DefaultDrmSessionManager.EventListener implementation.

  @Override
  public final void onDrmSessionAcquired(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId) {
    AnalyticsListener.EventTime eventTime = generateMediaPeriodEventTime(windowIndex, mediaPeriodId);
    for (AnalyticsListener listener : listeners) {
      listener.onDrmSessionAcquired(eventTime);
    }
  }

  @Override
  public final void onDrmKeysLoaded(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId) {
    AnalyticsListener.EventTime eventTime = generateMediaPeriodEventTime(windowIndex, mediaPeriodId);
    for (AnalyticsListener listener : listeners) {
      listener.onDrmKeysLoaded(eventTime);
    }
  }

  @Override
  public final void onDrmSessionManagerError(
          int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId, Exception error) {
    AnalyticsListener.EventTime eventTime = generateMediaPeriodEventTime(windowIndex, mediaPeriodId);
    for (AnalyticsListener listener : listeners) {
      listener.onDrmSessionManagerError(eventTime, error);
    }
  }

  @Override
  public final void onDrmKeysRestored(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId) {
    AnalyticsListener.EventTime eventTime = generateMediaPeriodEventTime(windowIndex, mediaPeriodId);
    for (AnalyticsListener listener : listeners) {
      listener.onDrmKeysRestored(eventTime);
    }
  }

  @Override
  public final void onDrmKeysRemoved(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId) {
    AnalyticsListener.EventTime eventTime = generateMediaPeriodEventTime(windowIndex, mediaPeriodId);
    for (AnalyticsListener listener : listeners) {
      listener.onDrmKeysRemoved(eventTime);
    }
  }

  @Override
  public final void onDrmSessionReleased(int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId) {
    AnalyticsListener.EventTime eventTime = generateMediaPeriodEventTime(windowIndex, mediaPeriodId);
    for (AnalyticsListener listener : listeners) {
      listener.onDrmSessionReleased(eventTime);
    }
  }

  // Internal methods.


  /** Returns a new {@link AnalyticsListener.EventTime} for the specified timeline, window and media period id. */
  @RequiresNonNull("player")
  protected AnalyticsListener.EventTime generateEventTime(
      Timeline timeline, int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId) {
    if (timeline.isEmpty()) {
      // Ensure media period id is only reported together with a valid timeline.
      mediaPeriodId = null;
    }
    long realtimeMs = clock.elapsedRealtime();
    long eventPositionMs;
    boolean isInCurrentWindow =
        timeline.equals(player.getCurrentTimeline())
            && windowIndex == player.getCurrentWindowIndex();
    if (mediaPeriodId != null && mediaPeriodId.isAd()) {
      boolean isCurrentAd =
          isInCurrentWindow
              && player.getCurrentAdGroupIndex() == mediaPeriodId.adGroupIndex
              && player.getCurrentAdIndexInAdGroup() == mediaPeriodId.adIndexInAdGroup;
      // Assume start position of 0 for future ads.
      eventPositionMs = isCurrentAd ? player.getCurrentPosition() : 0;
    } else if (isInCurrentWindow) {
      eventPositionMs = player.getContentPosition();
    } else {
      // Assume default start position for future content windows. If timeline is not available yet,
      // assume start position of 0.
      eventPositionMs =
          timeline.isEmpty() ? 0 : timeline.getWindow(windowIndex, window).getDefaultPositionMs();
    }
    @Nullable
    MediaSource.MediaPeriodId currentMediaPeriodId = mediaPeriodQueueTracker.getCurrentPlayerMediaPeriod();
    return new AnalyticsListener.EventTime(
        realtimeMs,
        timeline,
        windowIndex,
        mediaPeriodId,
        eventPositionMs,
        player.getCurrentTimeline(),
        player.getCurrentWindowIndex(),
        currentMediaPeriodId,
        player.getCurrentPosition(),
        player.getTotalBufferedDuration());
  }

  private AnalyticsListener.EventTime generateEventTime(@Nullable MediaSource.MediaPeriodId mediaPeriodId) {
    checkNotNull(player);
    @Nullable
    Timeline knownTimeline =
        mediaPeriodId == null
            ? null
            : mediaPeriodQueueTracker.getMediaPeriodIdTimeline(mediaPeriodId);
    if (mediaPeriodId == null || knownTimeline == null) {
      int windowIndex = player.getCurrentWindowIndex();
      Timeline timeline = player.getCurrentTimeline();
      boolean windowIsInTimeline = windowIndex < timeline.getWindowCount();
      return generateEventTime(
          windowIsInTimeline ? timeline : Timeline.EMPTY, windowIndex, /* mediaPeriodId= */ null);
    }
    int windowIndex = knownTimeline.getPeriodByUid(mediaPeriodId.periodUid, period).windowIndex;
    return generateEventTime(knownTimeline, windowIndex, mediaPeriodId);
  }

  private AnalyticsListener.EventTime generateCurrentPlayerMediaPeriodEventTime() {
    return generateEventTime(mediaPeriodQueueTracker.getCurrentPlayerMediaPeriod());
  }

  private AnalyticsListener.EventTime generatePlayingMediaPeriodEventTime() {
    return generateEventTime(mediaPeriodQueueTracker.getPlayingMediaPeriod());
  }

  private AnalyticsListener.EventTime generateReadingMediaPeriodEventTime() {
    return generateEventTime(mediaPeriodQueueTracker.getReadingMediaPeriod());
  }

  private AnalyticsListener.EventTime generateLoadingMediaPeriodEventTime() {
    return generateEventTime(mediaPeriodQueueTracker.getLoadingMediaPeriod());
  }

  private AnalyticsListener.EventTime generateMediaPeriodEventTime(
      int windowIndex, @Nullable MediaSource.MediaPeriodId mediaPeriodId) {
    checkNotNull(player);
    if (mediaPeriodId != null) {
      boolean isInKnownTimeline =
          mediaPeriodQueueTracker.getMediaPeriodIdTimeline(mediaPeriodId) != null;
      return isInKnownTimeline
          ? generateEventTime(mediaPeriodId)
          : generateEventTime(Timeline.EMPTY, windowIndex, mediaPeriodId);
    }
    Timeline timeline = player.getCurrentTimeline();
    boolean windowIsInTimeline = windowIndex < timeline.getWindowCount();
    return generateEventTime(
        windowIsInTimeline ? timeline : Timeline.EMPTY, windowIndex, /* mediaPeriodId= */ null);
  }

  /** Keeps track of the active media periods and currently playing and reading media period. */
  private static final class MediaPeriodQueueTracker {

    // TODO: Investigate reporting MediaPeriodId in renderer events.

    private final Timeline.Period period;

    private ImmutableList<MediaSource.MediaPeriodId> mediaPeriodQueue;
    private ImmutableMap<MediaSource.MediaPeriodId, Timeline> mediaPeriodTimelines;
    @Nullable private MediaSource.MediaPeriodId currentPlayerMediaPeriod;
    private MediaSource.MediaPeriodId playingMediaPeriod;
    private MediaSource.MediaPeriodId readingMediaPeriod;

    public MediaPeriodQueueTracker(Timeline.Period period) {
      this.period = period;
      mediaPeriodQueue = ImmutableList.of();
      mediaPeriodTimelines = ImmutableMap.of();
    }

    /**
     * Returns the {@link MediaSource.MediaPeriodId} of the media period corresponding the current position of
     * the player.
     *
     * <p>May be null if no matching media period has been created yet.
     */
    @Nullable
    public MediaSource.MediaPeriodId getCurrentPlayerMediaPeriod() {
      return currentPlayerMediaPeriod;
    }

    /**
     * Returns the {@link MediaSource.MediaPeriodId} of the media period at the front of the queue. If the queue
     * is empty, this is the last media period which was at the front of the queue.
     *
     * <p>May be null, if no media period has been created yet.
     */
    @Nullable
    public MediaSource.MediaPeriodId getPlayingMediaPeriod() {
      return playingMediaPeriod;
    }

    /**
     * Returns the {@link MediaSource.MediaPeriodId} of the media period currently being read by the player. If
     * the queue is empty, this is the last media period which was read by the player.
     *
     * <p>May be null, if no media period has been created yet.
     */
    @Nullable
    public MediaSource.MediaPeriodId getReadingMediaPeriod() {
      return readingMediaPeriod;
    }

    /**
     * Returns the {@link MediaSource.MediaPeriodId} of the media period at the end of the queue which is
     * currently loading or will be the next one loading.
     *
     * <p>May be null, if no media period is active yet.
     */
    @Nullable
    public MediaSource.MediaPeriodId getLoadingMediaPeriod() {
      return mediaPeriodQueue.isEmpty() ? null : Iterables.getLast(mediaPeriodQueue);
    }

    /**
     * Returns the most recent {@link Timeline} for the given {@link MediaSource.MediaPeriodId}, or null if no
     * timeline is available.
     */
    @Nullable
    public Timeline getMediaPeriodIdTimeline(MediaSource.MediaPeriodId mediaPeriodId) {
      return mediaPeriodTimelines.get(mediaPeriodId);
    }

    /** Updates the queue tracker with a reported position discontinuity. */
    public void onPositionDiscontinuity(Player player) {
      currentPlayerMediaPeriod =
          findCurrentPlayerMediaPeriodInQueue(player, mediaPeriodQueue, playingMediaPeriod, period);
    }

    /** Updates the queue tracker with a reported timeline change. */
    public void onTimelineChanged(Player player) {
      currentPlayerMediaPeriod =
          findCurrentPlayerMediaPeriodInQueue(player, mediaPeriodQueue, playingMediaPeriod, period);
      updateMediaPeriodTimelines(/* preferredTimeline= */ player.getCurrentTimeline());
    }

    /** Updates the queue tracker to a new queue of media periods. */
    public void onQueueUpdated(
            List<MediaSource.MediaPeriodId> queue, @Nullable MediaSource.MediaPeriodId readingPeriod, Player player) {
      mediaPeriodQueue = ImmutableList.copyOf(queue);
      if (!queue.isEmpty()) {
        playingMediaPeriod = queue.get(0);
        readingMediaPeriod = checkNotNull(readingPeriod);
      }
      if (currentPlayerMediaPeriod == null) {
        currentPlayerMediaPeriod =
            findCurrentPlayerMediaPeriodInQueue(
                player, mediaPeriodQueue, playingMediaPeriod, period);
      }
      updateMediaPeriodTimelines(/* preferredTimeline= */ player.getCurrentTimeline());
    }

    private void updateMediaPeriodTimelines(Timeline preferredTimeline) {
      ImmutableMap.Builder<MediaSource.MediaPeriodId, Timeline> builder = ImmutableMap.builder();
      if (mediaPeriodQueue.isEmpty()) {
        addTimelineForMediaPeriodId(builder, playingMediaPeriod, preferredTimeline);
        if (!Objects.equal(readingMediaPeriod, playingMediaPeriod)) {
          addTimelineForMediaPeriodId(builder, readingMediaPeriod, preferredTimeline);
        }
        if (!Objects.equal(currentPlayerMediaPeriod, playingMediaPeriod)
            && !Objects.equal(currentPlayerMediaPeriod, readingMediaPeriod)) {
          addTimelineForMediaPeriodId(builder, currentPlayerMediaPeriod, preferredTimeline);
        }
      } else {
        for (int i = 0; i < mediaPeriodQueue.size(); i++) {
          addTimelineForMediaPeriodId(builder, mediaPeriodQueue.get(i), preferredTimeline);
        }
        if (!mediaPeriodQueue.contains(currentPlayerMediaPeriod)) {
          addTimelineForMediaPeriodId(builder, currentPlayerMediaPeriod, preferredTimeline);
        }
      }
      mediaPeriodTimelines = builder.build();
    }

    private void addTimelineForMediaPeriodId(
        ImmutableMap.Builder<MediaSource.MediaPeriodId, Timeline> mediaPeriodTimelinesBuilder,
        @Nullable MediaSource.MediaPeriodId mediaPeriodId,
        Timeline preferredTimeline) {
      if (mediaPeriodId == null) {
        return;
      }
      if (preferredTimeline.getIndexOfPeriod(mediaPeriodId.periodUid) != C.INDEX_UNSET) {
        mediaPeriodTimelinesBuilder.put(mediaPeriodId, preferredTimeline);
      } else {
        @Nullable Timeline existingTimeline = mediaPeriodTimelines.get(mediaPeriodId);
        if (existingTimeline != null) {
          mediaPeriodTimelinesBuilder.put(mediaPeriodId, existingTimeline);
        }
      }
    }

    @Nullable
    private static MediaSource.MediaPeriodId findCurrentPlayerMediaPeriodInQueue(
        Player player,
        ImmutableList<MediaSource.MediaPeriodId> mediaPeriodQueue,
        @Nullable MediaSource.MediaPeriodId playingMediaPeriod,
        Timeline.Period period) {
      Timeline playerTimeline = player.getCurrentTimeline();
      int playerPeriodIndex = player.getCurrentPeriodIndex();
      @Nullable
      Object playerPeriodUid =
          playerTimeline.isEmpty() ? null : playerTimeline.getUidOfPeriod(playerPeriodIndex);
      int playerNextAdGroupIndex =
          player.isPlayingAd() || playerTimeline.isEmpty()
              ? C.INDEX_UNSET
              : playerTimeline
                  .getPeriod(playerPeriodIndex, period)
                  .getAdGroupIndexAfterPositionUs(
                      C.msToUs(player.getCurrentPosition()) - period.getPositionInWindowUs());
      for (int i = 0; i < mediaPeriodQueue.size(); i++) {
        MediaSource.MediaPeriodId mediaPeriodId = mediaPeriodQueue.get(i);
        if (isMatchingMediaPeriod(
            mediaPeriodId,
            playerPeriodUid,
            player.isPlayingAd(),
            player.getCurrentAdGroupIndex(),
            player.getCurrentAdIndexInAdGroup(),
            playerNextAdGroupIndex)) {
          return mediaPeriodId;
        }
      }
      if (mediaPeriodQueue.isEmpty() && playingMediaPeriod != null) {
        if (isMatchingMediaPeriod(
            playingMediaPeriod,
            playerPeriodUid,
            player.isPlayingAd(),
            player.getCurrentAdGroupIndex(),
            player.getCurrentAdIndexInAdGroup(),
            playerNextAdGroupIndex)) {
          return playingMediaPeriod;
        }
      }
      return null;
    }

    private static boolean isMatchingMediaPeriod(
        MediaSource.MediaPeriodId mediaPeriodId,
        @Nullable Object playerPeriodUid,
        boolean isPlayingAd,
        int playerAdGroupIndex,
        int playerAdIndexInAdGroup,
        int playerNextAdGroupIndex) {
      if (!mediaPeriodId.periodUid.equals(playerPeriodUid)) {
        return false;
      }
      // Timeline period matches. Still need to check ad information.
      return (isPlayingAd
              && mediaPeriodId.adGroupIndex == playerAdGroupIndex
              && mediaPeriodId.adIndexInAdGroup == playerAdIndexInAdGroup)
          || (!isPlayingAd
              && mediaPeriodId.adGroupIndex == C.INDEX_UNSET
              && mediaPeriodId.nextAdGroupIndex == playerNextAdGroupIndex);
    }
  }
}
