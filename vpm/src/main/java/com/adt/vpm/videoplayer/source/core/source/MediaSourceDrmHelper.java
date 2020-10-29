/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.source;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.common.MediaItem;
import com.adt.vpm.videoplayer.source.common.upstream.HttpDataSource;
import com.adt.vpm.videoplayer.source.common.util.Assertions;
import com.adt.vpm.videoplayer.source.common.util.Util;
import com.adt.vpm.videoplayer.source.core.drm.DefaultDrmSessionManager;
import com.adt.vpm.videoplayer.source.core.drm.DrmSessionManager;
import com.adt.vpm.videoplayer.source.core.drm.FrameworkMediaDrm;
import com.adt.vpm.videoplayer.source.core.drm.HttpMediaDrmCallback;
import com.adt.vpm.videoplayer.source.core.upstream.DefaultHttpDataSourceFactory;
import com.google.common.primitives.Ints;

import java.util.Map;

import static com.adt.vpm.videoplayer.source.common.ExoPlayerLibraryInfo.DEFAULT_USER_AGENT;
import static com.adt.vpm.videoplayer.source.common.util.Util.castNonNull;

/** A helper to create a {@link DrmSessionManager} from a {@link MediaItem}. */
public final class MediaSourceDrmHelper {

  @Nullable private HttpDataSource.Factory drmHttpDataSourceFactory;
  @Nullable private String userAgent;

  /**
   * Sets the {@link HttpDataSource.Factory} to be used for creating {@link HttpMediaDrmCallback
   * HttpMediaDrmCallbacks} which executes key and provisioning requests over HTTP. If {@code null}
   * is passed the {@link DefaultHttpDataSourceFactory} is used.
   *
   * @param drmHttpDataSourceFactory The HTTP data source factory or {@code null} to use {@link
   *     DefaultHttpDataSourceFactory}.
   */
  public void setDrmHttpDataSourceFactory(
      @Nullable HttpDataSource.Factory drmHttpDataSourceFactory) {
    this.drmHttpDataSourceFactory = drmHttpDataSourceFactory;
  }

  /**
   * Sets the optional user agent to be used for DRM requests.
   *
   * <p>In case a factory has been set by {@link
   * #setDrmHttpDataSourceFactory(HttpDataSource.Factory)}, this user agent is ignored.
   *
   * @param userAgent The user agent to be used for DRM requests.
   */
  public void setDrmUserAgent(@Nullable String userAgent) {
    this.userAgent = userAgent;
  }

  /** Creates a {@link DrmSessionManager} for the given media item. */
  public DrmSessionManager create(MediaItem mediaItem) {
    Assertions.checkNotNull(mediaItem.playbackProperties);
    @Nullable
    MediaItem.DrmConfiguration drmConfiguration = mediaItem.playbackProperties.drmConfiguration;
    if (drmConfiguration == null || drmConfiguration.licenseUri == null || Util.SDK_INT < 18) {
      return DrmSessionManager.getDummyDrmSessionManager();
    }
    HttpDataSource.Factory dataSourceFactory =
        drmHttpDataSourceFactory != null
            ? drmHttpDataSourceFactory
            : new DefaultHttpDataSourceFactory(userAgent != null ? userAgent : DEFAULT_USER_AGENT);
    HttpMediaDrmCallback httpDrmCallback =
        new HttpMediaDrmCallback(
            castNonNull(drmConfiguration.licenseUri).toString(),
            drmConfiguration.forceDefaultLicenseUri,
            dataSourceFactory);
    for (Map.Entry<String, String> entry : drmConfiguration.requestHeaders.entrySet()) {
      httpDrmCallback.setKeyRequestProperty(entry.getKey(), entry.getValue());
    }
    DefaultDrmSessionManager drmSessionManager =
        new DefaultDrmSessionManager.Builder()
            .setUuidAndExoMediaDrmProvider(
                drmConfiguration.uuid, FrameworkMediaDrm.DEFAULT_PROVIDER)
            .setMultiSession(drmConfiguration.multiSession)
            .setPlayClearSamplesWithoutKeys(drmConfiguration.playClearContentWithoutKey)
            .setUseDrmSessionsForClearContent(Ints.toArray(drmConfiguration.sessionForClearTypes))
            .build(httpDrmCallback);
    drmSessionManager.setMode(DefaultDrmSessionManager.MODE_PLAYBACK, drmConfiguration.getKeySetId());
    return drmSessionManager;
  }
}
