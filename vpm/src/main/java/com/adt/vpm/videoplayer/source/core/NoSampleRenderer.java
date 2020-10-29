/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.Format;
import com.adt.vpm.videoplayer.source.common.util.Assertions;
import com.adt.vpm.videoplayer.source.core.source.SampleStream;
import com.adt.vpm.videoplayer.source.core.util.MediaClock;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.io.IOException;

/**
 * A {@link Renderer} implementation whose track type is {@link C#TRACK_TYPE_NONE} and does not
 * consume data from its {@link SampleStream}.
 */
public abstract class NoSampleRenderer implements Renderer, RendererCapabilities {

  private @MonotonicNonNull RendererConfiguration configuration;
  private int index;
  private int state;
  @Nullable private SampleStream stream;
  private boolean streamIsFinal;

  @Override
  public final int getTrackType() {
    return C.TRACK_TYPE_NONE;
  }

  @Override
  public final RendererCapabilities getCapabilities() {
    return this;
  }

  @Override
  public final void setIndex(int index) {
    this.index = index;
  }

  @Override
  @Nullable
  public MediaClock getMediaClock() {
    return null;
  }

  @Override
  public final int getState() {
    return state;
  }

  @Override
  public final void enable(
      RendererConfiguration configuration,
      Format[] formats,
      SampleStream stream,
      long positionUs,
      boolean joining,
      boolean mayRenderStartOfStream,
      long startPositionUs,
      long offsetUs)
      throws ExoPlaybackException {
    Assertions.checkState(state == STATE_DISABLED);
    this.configuration = configuration;
    state = STATE_ENABLED;
    onEnabled(joining);
    replaceStream(formats, stream, startPositionUs, offsetUs);
    onPositionReset(positionUs, joining);
  }

  @Override
  public final void start() throws ExoPlaybackException {
    Assertions.checkState(state == STATE_ENABLED);
    state = STATE_STARTED;
    onStarted();
  }

  @Override
  public final void replaceStream(
      Format[] formats, SampleStream stream, long startPositionUs, long offsetUs)
      throws ExoPlaybackException {
    Assertions.checkState(!streamIsFinal);
    this.stream = stream;
    onRendererOffsetChanged(offsetUs);
  }

  @Override
  @Nullable
  public final SampleStream getStream() {
    return stream;
  }

  @Override
  public final boolean hasReadStreamToEnd() {
    return true;
  }

  @Override
  public long getReadingPositionUs() {
    return C.TIME_END_OF_SOURCE;
  }

  @Override
  public final void setCurrentStreamFinal() {
    streamIsFinal = true;
  }

  @Override
  public final boolean isCurrentStreamFinal() {
    return streamIsFinal;
  }

  @Override
  public final void maybeThrowStreamError() throws IOException {
  }

  @Override
  public final void resetPosition(long positionUs) throws ExoPlaybackException {
    streamIsFinal = false;
    onPositionReset(positionUs, false);
  }

  @Override
  public final void stop() {
    Assertions.checkState(state == STATE_STARTED);
    state = STATE_ENABLED;
    onStopped();
  }

  @Override
  public final void disable() {
    Assertions.checkState(state == STATE_ENABLED);
    state = STATE_DISABLED;
    stream = null;
    streamIsFinal = false;
    onDisabled();
  }

  @Override
  public final void reset() {
    Assertions.checkState(state == STATE_DISABLED);
    onReset();
  }

  @Override
  public boolean isReady() {
    return true;
  }

  @Override
  public boolean isEnded() {
    return true;
  }

  // RendererCapabilities implementation.

  @Override
  @Capabilities
  public int supportsFormat(Format format) throws ExoPlaybackException {
    return RendererCapabilities.create(FORMAT_UNSUPPORTED_TYPE);
  }

  @Override
  @AdaptiveSupport
  public int supportsMixedMimeTypeAdaptation() throws ExoPlaybackException {
    return ADAPTIVE_NOT_SUPPORTED;
  }

  // PlayerMessage.Target implementation.

  @Override
  public void handleMessage(int what, @Nullable Object object) throws ExoPlaybackException {
    // Do nothing.
  }

  // Methods to be overridden by subclasses.

  /**
   * Called when the renderer is enabled.
   * <p>
   * The default implementation is a no-op.
   *
   * @param joining Whether this renderer is being enabled to join an ongoing playback.
   * @throws ExoPlaybackException If an error occurs.
   */
  protected void onEnabled(boolean joining) throws ExoPlaybackException {
    // Do nothing.
  }

  /**
   * Called when the renderer's offset has been changed.
   * <p>
   * The default implementation is a no-op.
   *
   * @param offsetUs The offset that should be subtracted from {@code positionUs} in
   *     {@link #render(long, long)} to get the playback position with respect to the media.
   * @throws ExoPlaybackException If an error occurs.
   */
  protected void onRendererOffsetChanged(long offsetUs) throws ExoPlaybackException {
    // Do nothing.
  }

  /**
   * Called when the position is reset. This occurs when the renderer is enabled after
   * {@link #onRendererOffsetChanged(long)} has been called, and also when a position
   * discontinuity is encountered.
   * <p>
   * The default implementation is a no-op.
   *
   * @param positionUs The new playback position in microseconds.
   * @param joining Whether this renderer is being enabled to join an ongoing playback.
   * @throws ExoPlaybackException If an error occurs.
   */
  protected void onPositionReset(long positionUs, boolean joining) throws ExoPlaybackException {
    // Do nothing.
  }

  /**
   * Called when the renderer is started.
   * <p>
   * The default implementation is a no-op.
   *
   * @throws ExoPlaybackException If an error occurs.
   */
  protected void onStarted() throws ExoPlaybackException {
    // Do nothing.
  }

  /**
   * Called when the renderer is stopped.
   *
   * <p>The default implementation is a no-op.
   */
  protected void onStopped() {
    // Do nothing.
  }

  /**
   * Called when the renderer is disabled.
   * <p>
   * The default implementation is a no-op.
   */
  protected void onDisabled() {
    // Do nothing.
  }

  /**
   * Called when the renderer is reset.
   *
   * <p>The default implementation is a no-op.
   */
  protected void onReset() {
    // Do nothing.
  }

  // Methods to be called by subclasses.

  /**
   * Returns the configuration set when the renderer was most recently enabled, or {@code null} if
   * the renderer has never been enabled.
   */
  @Nullable
  protected final RendererConfiguration getConfiguration() {
    return configuration;
  }

  /**
   * Returns the index of the renderer within the player.
   */
  protected final int getIndex() {
    return index;
  }

}
