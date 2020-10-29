/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.source;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.core.SeekParameters;
import com.adt.vpm.videoplayer.source.core.source.MediaSource.MediaPeriodId;
import com.adt.vpm.videoplayer.source.core.trackselection.TrackSelection;
import com.adt.vpm.videoplayer.source.core.upstream.Allocator;

import org.checkerframework.checker.nullness.compatqual.NullableType;

import java.io.IOException;

import static com.adt.vpm.videoplayer.source.common.util.Util.castNonNull;

/**
 * Media period that wraps a media source and defers calling its {@link
 * MediaSource#createPeriod(MediaPeriodId, Allocator, long)} method until {@link
 * #createPeriod(MediaPeriodId)} has been called. This is useful if you need to return a media
 * period immediately but the media source that should create it is not yet prepared.
 */
public final class MaskingMediaPeriod implements MediaPeriod, MediaPeriod.Callback {

  /** Listener for preparation events. */
  public interface PrepareListener {

    /** Called when preparing the media period completes. */
    void onPrepareComplete(MediaPeriodId mediaPeriodId);

    /**
     * Called the first time an error occurs while refreshing source info or preparing the period.
     */
    void onPrepareError(MediaPeriodId mediaPeriodId, IOException exception);
  }

  /** The {@link MediaSource} which will create the actual media period. */
  public final MediaSource mediaSource;
  /** The {@link MediaPeriodId} used to create the masking media period. */
  public final MediaPeriodId id;

  private final Allocator allocator;

  @Nullable private MediaPeriod mediaPeriod;
  @Nullable private Callback callback;
  private long preparePositionUs;
  @Nullable private PrepareListener listener;
  private boolean notifiedPrepareError;
  private long preparePositionOverrideUs;

  /**
   * Creates a new masking media period.
   *
   * @param mediaSource The media source to wrap.
   * @param id The identifier used to create the masking media period.
   * @param allocator The allocator used to create the media period.
   * @param preparePositionUs The expected start position, in microseconds.
   */
  public MaskingMediaPeriod(
      MediaSource mediaSource, MediaPeriodId id, Allocator allocator, long preparePositionUs) {
    this.id = id;
    this.allocator = allocator;
    this.mediaSource = mediaSource;
    this.preparePositionUs = preparePositionUs;
    preparePositionOverrideUs = C.TIME_UNSET;
  }

  /**
   * Sets a listener for preparation events.
   *
   * @param listener An listener to be notified of media period preparation events. If a listener is
   *     set, {@link #maybeThrowPrepareError()} will not throw but will instead pass the first
   *     preparation error (if any) to the listener.
   */
  public void setPrepareListener(PrepareListener listener) {
    this.listener = listener;
  }

  /** Returns the position at which the masking media period was prepared, in microseconds. */
  public long getPreparePositionUs() {
    return preparePositionUs;
  }

  /**
   * Overrides the default prepare position at which to prepare the media period. This method must
   * be called before {@link #createPeriod(MediaPeriodId)}.
   *
   * @param preparePositionUs The default prepare position to use, in microseconds.
   */
  public void overridePreparePositionUs(long preparePositionUs) {
    preparePositionOverrideUs = preparePositionUs;
  }

  /** Returns the prepare position override set by {@link #overridePreparePositionUs(long)}. */
  public long getPreparePositionOverrideUs() {
    return preparePositionOverrideUs;
  }

  /**
   * Calls {@link MediaSource#createPeriod(MediaPeriodId, Allocator, long)} on the wrapped source
   * then prepares it if {@link #prepare(Callback, long)} has been called. Call {@link
   * #releasePeriod()} to release the period.
   *
   * @param id The identifier that should be used to create the media period from the media source.
   */
  public void createPeriod(MediaPeriodId id) {
    long preparePositionUs = getPreparePositionWithOverride(this.preparePositionUs);
    mediaPeriod = mediaSource.createPeriod(id, allocator, preparePositionUs);
    if (callback != null) {
      mediaPeriod.prepare(this, preparePositionUs);
    }
  }

  /**
   * Releases the period.
   */
  public void releasePeriod() {
    if (mediaPeriod != null) {
      mediaSource.releasePeriod(mediaPeriod);
    }
  }

  /**
   * Returns whether the media source is a live.
   */
  public boolean isSourceLive() {
    return mediaSource.isLive();
  }

  /**
   * Returns whether the media source is on TCP.
   */
  public boolean isSourceOnTcp() {
    return mediaSource.isOnTcp();
  }

  @Override
  public void prepare(Callback callback, long preparePositionUs) {
    this.callback = callback;
    if (mediaPeriod != null) {
      mediaPeriod.prepare(this, getPreparePositionWithOverride(this.preparePositionUs));
    }
  }

  @Override
  public void maybeThrowPrepareError() throws IOException {
    try {
      if (mediaPeriod != null) {
        mediaPeriod.maybeThrowPrepareError();
      } else {
        mediaSource.maybeThrowSourceInfoRefreshError();
      }
    } catch (final IOException e) {
      if (listener == null) {
        throw e;
      }
      if (!notifiedPrepareError) {
        notifiedPrepareError = true;
        listener.onPrepareError(id, e);
      }
    }
  }

  @Override
  public TrackGroupArray getTrackGroups() {
    return castNonNull(mediaPeriod).getTrackGroups();
  }

  @Override
  public long selectTracks(
      @NullableType TrackSelection[] selections,
      boolean[] mayRetainStreamFlags,
      @NullableType SampleStream[] streams,
      boolean[] streamResetFlags,
      long positionUs) {
    if (preparePositionOverrideUs != C.TIME_UNSET && positionUs == preparePositionUs) {
      positionUs = preparePositionOverrideUs;
      preparePositionOverrideUs = C.TIME_UNSET;
    }
    return castNonNull(mediaPeriod)
        .selectTracks(selections, mayRetainStreamFlags, streams, streamResetFlags, positionUs);
  }

  @Override
  public void discardBuffer(long positionUs, boolean toKeyframe) {
    castNonNull(mediaPeriod).discardBuffer(positionUs, toKeyframe);
  }

  @Override
  public long readDiscontinuity() {
    return castNonNull(mediaPeriod).readDiscontinuity();
  }

  @Override
  public long getBufferedPositionUs() {
    return castNonNull(mediaPeriod).getBufferedPositionUs();
  }

  @Override
  public long seekToUs(long positionUs) {
    return castNonNull(mediaPeriod).seekToUs(positionUs);
  }

  @Override
  public long getAdjustedSeekPositionUs(long positionUs, SeekParameters seekParameters) {
    return castNonNull(mediaPeriod).getAdjustedSeekPositionUs(positionUs, seekParameters);
  }

  @Override
  public long getNextLoadPositionUs() {
    return castNonNull(mediaPeriod).getNextLoadPositionUs();
  }

  @Override
  public void reevaluateBuffer(long positionUs) {
    castNonNull(mediaPeriod).reevaluateBuffer(positionUs);
  }

  @Override
  public boolean continueLoading(long positionUs) {
    return mediaPeriod != null && mediaPeriod.continueLoading(positionUs);
  }

  @Override
  public boolean isLoading() {
    return mediaPeriod != null && mediaPeriod.isLoading();
  }

  @Override
  public void onContinueLoadingRequested(MediaPeriod source) {
    castNonNull(callback).onContinueLoadingRequested(this);
  }

  // MediaPeriod.Callback implementation

  @Override
  public void onPrepared(MediaPeriod mediaPeriod) {
    castNonNull(callback).onPrepared(this);
    if (listener != null) {
      listener.onPrepareComplete(id);
    }
  }

  private long getPreparePositionWithOverride(long preparePositionUs) {
    return preparePositionOverrideUs != C.TIME_UNSET
        ? preparePositionOverrideUs
        : preparePositionUs;
  }
}
