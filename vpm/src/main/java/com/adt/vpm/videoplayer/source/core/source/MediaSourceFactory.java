/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.source;

import android.net.Uri;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.MediaItem;
import com.adt.vpm.videoplayer.source.common.offline.StreamKey;
import com.adt.vpm.videoplayer.source.common.upstream.HttpDataSource;
import com.adt.vpm.videoplayer.source.core.drm.DefaultDrmSessionManager;
import com.adt.vpm.videoplayer.source.core.drm.DrmSessionManager;
import com.adt.vpm.videoplayer.source.core.drm.HttpMediaDrmCallback;
import com.adt.vpm.videoplayer.source.core.upstream.DefaultHttpDataSourceFactory;
import com.adt.vpm.videoplayer.source.core.upstream.DefaultLoadErrorHandlingPolicy;
import com.adt.vpm.videoplayer.source.core.upstream.LoadErrorHandlingPolicy;

import java.util.List;

/**
 * Factory for creating {@link MediaSource}s from URIs.
 *
 * <h3>DrmSessionManager creation for protected content</h3>
 *
 * <p>In case a {@link DrmSessionManager} is passed to {@link
 * #setDrmSessionManager(DrmSessionManager)}, it will be used regardless of the drm configuration of
 * the media item.
 *
 * <p>For a media item with a {@link MediaItem.DrmConfiguration}, a {@link DefaultDrmSessionManager}
 * is created based on that configuration. The following setter can be used to optionally configure
 * the creation:
 *
 * <ul>
 *   <li>{@link #setDrmHttpDataSourceFactory(HttpDataSource.Factory)}: Sets the data source factory
 *       to be used by the {@link HttpMediaDrmCallback} for network requests (default: {@link
 *       DefaultHttpDataSourceFactory}).
 * </ul>
 */
public interface MediaSourceFactory {

  /** @deprecated Use {@link MediaItem.PlaybackProperties#streamKeys} instead. */
  @Deprecated
  default MediaSourceFactory setStreamKeys(@Nullable List<StreamKey> streamKeys) {
    return this;
  }

  /**
   * Sets the {@link DrmSessionManager} to use for all media items regardless of their {@link
   * MediaItem.DrmConfiguration}.
   *
   * @param drmSessionManager The {@link DrmSessionManager}, or {@code null} to use the {@link
   *     DefaultDrmSessionManager}.
   * @return This factory, for convenience.
   */
  MediaSourceFactory setDrmSessionManager(@Nullable DrmSessionManager drmSessionManager);

  /**
   * Sets the {@link HttpDataSource.Factory} to be used for creating {@link HttpMediaDrmCallback
   * HttpMediaDrmCallbacks} to execute key and provisioning requests over HTTP.
   *
   * <p>In case a {@link DrmSessionManager} has been set by {@link
   * #setDrmSessionManager(DrmSessionManager)}, this data source factory is ignored.
   *
   * @param drmHttpDataSourceFactory The HTTP data source factory, or {@code null} to use {@link
   *     DefaultHttpDataSourceFactory}.
   * @return This factory, for convenience.
   */
  MediaSourceFactory setDrmHttpDataSourceFactory(
      @Nullable HttpDataSource.Factory drmHttpDataSourceFactory);

  /**
   * Sets the optional user agent to be used for DRM requests.
   *
   * <p>In case a factory has been set by {@link
   * #setDrmHttpDataSourceFactory(HttpDataSource.Factory)} or a {@link DrmSessionManager} has been
   * set by {@link #setDrmSessionManager(DrmSessionManager)}, this user agent is ignored.
   *
   * @param userAgent The user agent to be used for DRM requests, or {@code null} to use the
   *     default.
   * @return This factory, for convenience.
   */
  MediaSourceFactory setDrmUserAgent(@Nullable String userAgent);

  /**
   * Sets an optional {@link LoadErrorHandlingPolicy}.
   *
   * @param loadErrorHandlingPolicy A {@link LoadErrorHandlingPolicy}, or {@code null} to use the
   *     {@link DefaultLoadErrorHandlingPolicy}.
   * @return This factory, for convenience.
   */
  MediaSourceFactory setLoadErrorHandlingPolicy(
      @Nullable LoadErrorHandlingPolicy loadErrorHandlingPolicy);

  /**
   * Returns the {@link C.ContentType content types} supported by media sources created by this
   * factory.
   */
  @C.ContentType
  int[] getSupportedTypes();

  /**
   * Creates a new {@link MediaSource} with the specified {@link MediaItem}.
   *
   * @param mediaItem The media item to play.
   * @return The new {@link MediaSource media source}.
   */
  MediaSource createMediaSource(MediaItem mediaItem);

  /** @deprecated Use {@link #createMediaSource(MediaItem)} instead. */
  @Deprecated
  default MediaSource createMediaSource(Uri uri) {
    return createMediaSource(MediaItem.fromUri(uri));
  }
}
