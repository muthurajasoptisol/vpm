/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.source;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.core.Timeline;
import com.adt.vpm.videoplayer.source.core.drm.DrmSessionEventListener;
import com.adt.vpm.videoplayer.source.common.upstream.TransferListener;
import com.adt.vpm.videoplayer.source.common.util.Assertions;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Base {@link MediaSource} implementation to handle parallel reuse and to keep a list of {@link
 * MediaSourceEventListener}s.
 *
 * <p>Whenever an implementing subclass needs to provide a new timeline, it must call {@link
 * #refreshSourceInfo(Timeline)} to notify all listeners.
 */
public abstract class BaseMediaSource implements MediaSource {

  private final ArrayList<MediaSourceCaller> mediaSourceCallers;
  private final HashSet<MediaSourceCaller> enabledMediaSourceCallers;
  private final MediaSourceEventListener.EventDispatcher eventDispatcher;
  private final DrmSessionEventListener.EventDispatcher drmEventDispatcher;

  @Nullable private Looper looper;
  @Nullable private Timeline timeline;

  public BaseMediaSource() {
    mediaSourceCallers = new ArrayList<>(/* initialCapacity= */ 1);
    enabledMediaSourceCallers = new HashSet<>(/* initialCapacity= */ 1);
    eventDispatcher = new MediaSourceEventListener.EventDispatcher();
    drmEventDispatcher = new DrmSessionEventListener.EventDispatcher();
  }

  /**
   * Starts source preparation and enables the source, see {@link #prepareSource(MediaSourceCaller,
   * TransferListener)}. This method is called at most once until the next call to {@link
   * #releaseSourceInternal()}.
   *
   * @param mediaTransferListener The transfer listener which should be informed of any media data
   *     transfers. May be null if no listener is available. Note that this listener should usually
   *     be only informed of transfers related to the media loads and not of auxiliary loads for
   *     manifests and other data.
   */
  protected abstract void prepareSourceInternal(@Nullable TransferListener mediaTransferListener);

  /** Enables the source, see {@link #enable(MediaSourceCaller)}. */
  protected void enableInternal() {}

  /** Disables the source, see {@link #disable(MediaSourceCaller)}. */
  protected void disableInternal() {}

  /**
   * Releases the source, see {@link #releaseSource(MediaSourceCaller)}. This method is called
   * exactly once after each call to {@link #prepareSourceInternal(TransferListener)}.
   */
  protected abstract void releaseSourceInternal();

  /**
   * Updates timeline and manifest and notifies all listeners of the update.
   *
   * @param timeline The new {@link Timeline}.
   */
  protected final void refreshSourceInfo(Timeline timeline) {
    this.timeline = timeline;
    for (MediaSourceCaller caller : mediaSourceCallers) {
      caller.onSourceInfoRefreshed(/* source= */ this, timeline);
    }
  }

  /**
   * Returns a {@link MediaSourceEventListener.EventDispatcher} which dispatches all events to the
   * registered listeners with the specified media period id.
   *
   * @param mediaPeriodId The {@link MediaPeriodId} to be reported with the events. May be null, if
   *     the events do not belong to a specific media period.
   * @return An event dispatcher with pre-configured media period id.
   */
  protected final MediaSourceEventListener.EventDispatcher createEventDispatcher(
      @Nullable MediaPeriodId mediaPeriodId) {
    return eventDispatcher.withParameters(
        /* windowIndex= */ 0, mediaPeriodId, /* mediaTimeOffsetMs= */ 0);
  }

  /**
   * Returns a {@link MediaSourceEventListener.EventDispatcher} which dispatches all events to the
   * registered listeners with the specified media period id and time offset.
   *
   * @param mediaPeriodId The {@link MediaPeriodId} to be reported with the events.
   * @param mediaTimeOffsetMs The offset to be added to all media times, in milliseconds.
   * @return An event dispatcher with pre-configured media period id and time offset.
   */
  protected final MediaSourceEventListener.EventDispatcher createEventDispatcher(
      MediaPeriodId mediaPeriodId, long mediaTimeOffsetMs) {
    Assertions.checkNotNull(mediaPeriodId);
    return eventDispatcher.withParameters(/* windowIndex= */ 0, mediaPeriodId, mediaTimeOffsetMs);
  }

  /**
   * Returns a {@link MediaSourceEventListener.EventDispatcher} which dispatches all events to the
   * registered listeners with the specified window index, media period id and time offset.
   *
   * @param windowIndex The timeline window index to be reported with the events.
   * @param mediaPeriodId The {@link MediaPeriodId} to be reported with the events. May be null, if
   *     the events do not belong to a specific media period.
   * @param mediaTimeOffsetMs The offset to be added to all media times, in milliseconds.
   * @return An event dispatcher with pre-configured media period id and time offset.
   */
  protected final MediaSourceEventListener.EventDispatcher createEventDispatcher(
      int windowIndex, @Nullable MediaPeriodId mediaPeriodId, long mediaTimeOffsetMs) {
    return eventDispatcher.withParameters(windowIndex, mediaPeriodId, mediaTimeOffsetMs);
  }

  /**
   * Returns a {@link DrmSessionEventListener.EventDispatcher} which dispatches all events to the
   * registered listeners with the specified media period id.
   *
   * @param mediaPeriodId The {@link MediaPeriodId} to be reported with the events. May be null, if
   *     the events do not belong to a specific media period.
   * @return An event dispatcher with pre-configured media period id.
   */
  protected final DrmSessionEventListener.EventDispatcher createDrmEventDispatcher(
      @Nullable MediaPeriodId mediaPeriodId) {
    return drmEventDispatcher.withParameters(/* windowIndex= */ 0, mediaPeriodId);
  }

  /**
   * Returns a {@link DrmSessionEventListener.EventDispatcher} which dispatches all events to the
   * registered listeners with the specified window index and media period id.
   *
   * @param windowIndex The timeline window index to be reported with the events.
   * @param mediaPeriodId The {@link MediaPeriodId} to be reported with the events. May be null, if
   *     the events do not belong to a specific media period.
   * @return An event dispatcher with pre-configured media period id and time offset.
   */
  protected final DrmSessionEventListener.EventDispatcher createDrmEventDispatcher(
      int windowIndex, @Nullable MediaPeriodId mediaPeriodId) {
    return drmEventDispatcher.withParameters(windowIndex, mediaPeriodId);
  }

  /** Returns whether the source is enabled. */
  protected final boolean isEnabled() {
    return !enabledMediaSourceCallers.isEmpty();
  }

  @Override
  public final void addEventListener(Handler handler, MediaSourceEventListener eventListener) {
    Assertions.checkNotNull(handler);
    Assertions.checkNotNull(eventListener);
    eventDispatcher.addEventListener(handler, eventListener);
  }

  @Override
  public final void removeEventListener(MediaSourceEventListener eventListener) {
    eventDispatcher.removeEventListener(eventListener);
  }

  @Override
  public final void addDrmEventListener(Handler handler, DrmSessionEventListener eventListener) {
    Assertions.checkNotNull(handler);
    Assertions.checkNotNull(eventListener);
    drmEventDispatcher.addEventListener(handler, eventListener);
  }

  @Override
  public final void removeDrmEventListener(DrmSessionEventListener eventListener) {
    drmEventDispatcher.removeEventListener(eventListener);
  }

  @Override
  public final void prepareSource(
      MediaSourceCaller caller, @Nullable TransferListener mediaTransferListener) {
    Looper looper = Looper.myLooper();
    Assertions.checkArgument(this.looper == null || this.looper == looper);
    @Nullable Timeline timeline = this.timeline;
    mediaSourceCallers.add(caller);
    if (this.looper == null) {
      this.looper = looper;
      enabledMediaSourceCallers.add(caller);
      prepareSourceInternal(mediaTransferListener);
    } else if (timeline != null) {
      enable(caller);
      caller.onSourceInfoRefreshed(/* source= */ this, timeline);
    }
  }

  @Override
  public final void enable(MediaSourceCaller caller) {
    Assertions.checkNotNull(looper);
    boolean wasDisabled = enabledMediaSourceCallers.isEmpty();
    enabledMediaSourceCallers.add(caller);
    if (wasDisabled) {
      enableInternal();
    }
  }

  @Override
  public final void disable(MediaSourceCaller caller) {
    boolean wasEnabled = !enabledMediaSourceCallers.isEmpty();
    enabledMediaSourceCallers.remove(caller);
    if (wasEnabled && enabledMediaSourceCallers.isEmpty()) {
      disableInternal();
    }
  }

  @Override
  public final void releaseSource(MediaSourceCaller caller) {
    mediaSourceCallers.remove(caller);
    if (mediaSourceCallers.isEmpty()) {
      looper = null;
      timeline = null;
      enabledMediaSourceCallers.clear();
      releaseSourceInternal();
    } else {
      disable(caller);
    }
  }
}
