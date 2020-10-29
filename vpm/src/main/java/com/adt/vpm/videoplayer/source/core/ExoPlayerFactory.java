/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core;

import android.content.Context;
import android.os.Looper;
import com.adt.vpm.videoplayer.source.core.analytics.AnalyticsCollector;
import com.adt.vpm.videoplayer.source.core.source.DefaultMediaSourceFactory;
import com.adt.vpm.videoplayer.source.core.trackselection.DefaultTrackSelector;
import com.adt.vpm.videoplayer.source.core.trackselection.TrackSelector;
import com.adt.vpm.videoplayer.source.core.upstream.BandwidthMeter;
import com.adt.vpm.videoplayer.source.core.upstream.DefaultBandwidthMeter;
import com.adt.vpm.videoplayer.source.core.util.Clock;
import com.adt.vpm.videoplayer.source.common.util.Util;

/** @deprecated Use {@link SimpleExoPlayer.Builder} or {@link ExoPlayer.Builder} instead. */
@Deprecated
public final class ExoPlayerFactory {

  private ExoPlayerFactory() {}

  /** @deprecated Use {@link SimpleExoPlayer.Builder} instead. */
  @Deprecated
  @SuppressWarnings("deprecation")
  public static SimpleExoPlayer newSimpleInstance(
      Context context,
      TrackSelector trackSelector,
      LoadControl loadControl,
      @DefaultRenderersFactory.ExtensionRendererMode int extensionRendererMode) {
    RenderersFactory renderersFactory =
        new DefaultRenderersFactory(context).setExtensionRendererMode(extensionRendererMode);
    return newSimpleInstance(context, renderersFactory, trackSelector, loadControl);
  }

  /** @deprecated Use {@link SimpleExoPlayer.Builder} instead. */
  @Deprecated
  @SuppressWarnings("deprecation")
  public static SimpleExoPlayer newSimpleInstance(
      Context context,
      TrackSelector trackSelector,
      LoadControl loadControl,
      @DefaultRenderersFactory.ExtensionRendererMode int extensionRendererMode,
      long allowedVideoJoiningTimeMs) {
    RenderersFactory renderersFactory =
        new DefaultRenderersFactory(context)
            .setExtensionRendererMode(extensionRendererMode)
            .setAllowedVideoJoiningTimeMs(allowedVideoJoiningTimeMs);
    return newSimpleInstance(context, renderersFactory, trackSelector, loadControl);
  }

  /** @deprecated Use {@link SimpleExoPlayer.Builder} instead. */
  @Deprecated
  @SuppressWarnings("deprecation")
  public static SimpleExoPlayer newSimpleInstance(Context context) {
    return newSimpleInstance(context, new DefaultTrackSelector(context));
  }

  /** @deprecated Use {@link SimpleExoPlayer.Builder} instead. */
  @Deprecated
  @SuppressWarnings("deprecation")
  public static SimpleExoPlayer newSimpleInstance(Context context, TrackSelector trackSelector) {
    return newSimpleInstance(context, new DefaultRenderersFactory(context), trackSelector);
  }

  /** @deprecated Use {@link SimpleExoPlayer.Builder} instead. */
  @Deprecated
  @SuppressWarnings("deprecation")
  public static SimpleExoPlayer newSimpleInstance(
      Context context, RenderersFactory renderersFactory, TrackSelector trackSelector) {
    return newSimpleInstance(context, renderersFactory, trackSelector, new DefaultLoadControl());
  }

  /** @deprecated Use {@link SimpleExoPlayer.Builder} instead. */
  @Deprecated
  @SuppressWarnings("deprecation")
  public static SimpleExoPlayer newSimpleInstance(
      Context context, TrackSelector trackSelector, LoadControl loadControl) {
    RenderersFactory renderersFactory = new DefaultRenderersFactory(context);
    return newSimpleInstance(context, renderersFactory, trackSelector, loadControl);
  }

  /** @deprecated Use {@link SimpleExoPlayer.Builder} instead. */
  @Deprecated
  @SuppressWarnings("deprecation")
  public static SimpleExoPlayer newSimpleInstance(
      Context context,
      RenderersFactory renderersFactory,
      TrackSelector trackSelector,
      LoadControl loadControl) {
    return newSimpleInstance(
        context, renderersFactory, trackSelector, loadControl, Util.getCurrentOrMainLooper());
  }

  /** @deprecated Use {@link SimpleExoPlayer.Builder} instead. */
  @Deprecated
  @SuppressWarnings("deprecation")
  public static SimpleExoPlayer newSimpleInstance(
      Context context,
      RenderersFactory renderersFactory,
      TrackSelector trackSelector,
      LoadControl loadControl,
      BandwidthMeter bandwidthMeter) {
    return newSimpleInstance(
        context,
        renderersFactory,
        trackSelector,
        loadControl,
        bandwidthMeter,
        new AnalyticsCollector(Clock.DEFAULT),
        Util.getCurrentOrMainLooper());
  }

  /** @deprecated Use {@link SimpleExoPlayer.Builder} instead. */
  @Deprecated
  @SuppressWarnings("deprecation")
  public static SimpleExoPlayer newSimpleInstance(
      Context context,
      RenderersFactory renderersFactory,
      TrackSelector trackSelector,
      LoadControl loadControl,
      AnalyticsCollector analyticsCollector) {
    return newSimpleInstance(
        context,
        renderersFactory,
        trackSelector,
        loadControl,
        analyticsCollector,
        Util.getCurrentOrMainLooper());
  }

  /** @deprecated Use {@link SimpleExoPlayer.Builder} instead. */
  @Deprecated
  @SuppressWarnings("deprecation")
  public static SimpleExoPlayer newSimpleInstance(
      Context context,
      RenderersFactory renderersFactory,
      TrackSelector trackSelector,
      LoadControl loadControl,
      Looper applicationLooper) {
    return newSimpleInstance(
        context,
        renderersFactory,
        trackSelector,
        loadControl,
        new AnalyticsCollector(Clock.DEFAULT),
        applicationLooper);
  }

  /** @deprecated Use {@link SimpleExoPlayer.Builder} instead. */
  @Deprecated
  @SuppressWarnings("deprecation")
  public static SimpleExoPlayer newSimpleInstance(
      Context context,
      RenderersFactory renderersFactory,
      TrackSelector trackSelector,
      LoadControl loadControl,
      AnalyticsCollector analyticsCollector,
      Looper applicationLooper) {
    return newSimpleInstance(
        context,
        renderersFactory,
        trackSelector,
        loadControl,
        DefaultBandwidthMeter.getSingletonInstance(context),
        analyticsCollector,
        applicationLooper);
  }

  /** @deprecated Use {@link SimpleExoPlayer.Builder} instead. */
  @SuppressWarnings("deprecation")
  @Deprecated
  public static SimpleExoPlayer newSimpleInstance(
      Context context,
      RenderersFactory renderersFactory,
      TrackSelector trackSelector,
      LoadControl loadControl,
      BandwidthMeter bandwidthMeter,
      AnalyticsCollector analyticsCollector,
      Looper applicationLooper) {
    return new SimpleExoPlayer(
        context,
        renderersFactory,
        trackSelector,
        new DefaultMediaSourceFactory(context),
        loadControl,
        bandwidthMeter,
        analyticsCollector,
        /* useLazyPreparation= */ true,
        Clock.DEFAULT,
        applicationLooper);
  }

  /** @deprecated Use {@link ExoPlayer.Builder} instead. */
  @Deprecated
  @SuppressWarnings("deprecation")
  public static ExoPlayer newInstance(
      Context context, Renderer[] renderers, TrackSelector trackSelector) {
    return newInstance(context, renderers, trackSelector, new DefaultLoadControl());
  }

  /** @deprecated Use {@link ExoPlayer.Builder} instead. */
  @Deprecated
  @SuppressWarnings("deprecation")
  public static ExoPlayer newInstance(
      Context context, Renderer[] renderers, TrackSelector trackSelector, LoadControl loadControl) {
    return newInstance(
        context, renderers, trackSelector, loadControl, Util.getCurrentOrMainLooper());
  }

  /** @deprecated Use {@link ExoPlayer.Builder} instead. */
  @Deprecated
  @SuppressWarnings("deprecation")
  public static ExoPlayer newInstance(
      Context context,
      Renderer[] renderers,
      TrackSelector trackSelector,
      LoadControl loadControl,
      Looper applicationLooper) {
    return newInstance(
        context,
        renderers,
        trackSelector,
        loadControl,
        DefaultBandwidthMeter.getSingletonInstance(context),
        applicationLooper);
  }

  /** @deprecated Use {@link ExoPlayer.Builder} instead. */
  @Deprecated
  public static ExoPlayer newInstance(
      Context context,
      Renderer[] renderers,
      TrackSelector trackSelector,
      LoadControl loadControl,
      BandwidthMeter bandwidthMeter,
      Looper applicationLooper) {
    return new ExoPlayerImpl(
        renderers,
        trackSelector,
        new DefaultMediaSourceFactory(context),
        loadControl,
        bandwidthMeter,
        /* analyticsCollector= */ null,
        /* useLazyPreparation= */ true,
        SeekParameters.DEFAULT,
        /* pauseAtEndOfMediaItems= */ false,
        Clock.DEFAULT,
        applicationLooper);
  }
}
