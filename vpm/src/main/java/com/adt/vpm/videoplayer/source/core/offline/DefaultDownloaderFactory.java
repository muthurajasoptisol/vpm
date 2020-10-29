/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.offline;

import android.util.SparseArray;
import androidx.annotation.Nullable;
import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.MediaItem;
import com.adt.vpm.videoplayer.source.core.upstream.cache.CacheDataSource;
import com.adt.vpm.videoplayer.source.common.util.Assertions;
import com.adt.vpm.videoplayer.source.common.util.Util;
import java.lang.reflect.Constructor;
import java.util.concurrent.Executor;

/**
 * Default {@link DownloaderFactory}, supporting creation of progressive, DASH, HLS and
 * SmoothStreaming downloaders. Note that for the latter three, the corresponding library module
 * must be built into the application.
 */
public class DefaultDownloaderFactory implements DownloaderFactory {

  private static final SparseArray<Constructor<? extends Downloader>> CONSTRUCTORS =
      createDownloaderConstructors();

  private final CacheDataSource.Factory cacheDataSourceFactory;
  private final Executor executor;

  /**
   * Creates an instance.
   *
   * @param cacheDataSourceFactory A {@link CacheDataSource.Factory} for the cache into which
   *     downloads will be written.
   * @deprecated Use {@link #DefaultDownloaderFactory(CacheDataSource.Factory, Executor)}.
   */
  @Deprecated
  public DefaultDownloaderFactory(CacheDataSource.Factory cacheDataSourceFactory) {
    this(cacheDataSourceFactory, /* executor= */ Runnable::run);
  }

  /**
   * Creates an instance.
   *
   * @param cacheDataSourceFactory A {@link CacheDataSource.Factory} for the cache into which
   *     downloads will be written.
   * @param executor An {@link Executor} used to download data. Passing {@code Runnable::run} will
   *     cause each download task to download data on its own thread. Passing an {@link Executor}
   *     that uses multiple threads will speed up download tasks that can be split into smaller
   *     parts for parallel execution.
   */
  public DefaultDownloaderFactory(
      CacheDataSource.Factory cacheDataSourceFactory, Executor executor) {
    this.cacheDataSourceFactory = Assertions.checkNotNull(cacheDataSourceFactory);
    this.executor = Assertions.checkNotNull(executor);
  }

  @Override
  public Downloader createDownloader(DownloadRequest request) {
    @C.ContentType
    int contentType = Util.inferContentTypeForUriAndMimeType(request.uri, request.mimeType);
    switch (contentType) {
      case C.TYPE_DASH:
      case C.TYPE_HLS:
      case C.TYPE_SS:
        return createDownloader(request, contentType);
      case C.TYPE_OTHER:
        return new ProgressiveDownloader(
            new MediaItem.Builder()
                .setUri(request.uri)
                .setCustomCacheKey(request.customCacheKey)
                .build(),
            cacheDataSourceFactory,
            executor);
      default:
        throw new IllegalArgumentException("Unsupported type: " + contentType);
    }
  }

  private Downloader createDownloader(DownloadRequest request, @C.ContentType int contentType) {
    @Nullable Constructor<? extends Downloader> constructor = CONSTRUCTORS.get(contentType);
    if (constructor == null) {
      throw new IllegalStateException("Module missing for content type " + contentType);
    }
    MediaItem mediaItem =
        new MediaItem.Builder()
            .setUri(request.uri)
            .setStreamKeys(request.streamKeys)
            .setCustomCacheKey(request.customCacheKey)
            .setDrmKeySetId(request.keySetId)
            .build();
    try {
      return constructor.newInstance(mediaItem, cacheDataSourceFactory, executor);
    } catch (Exception e) {
      throw new IllegalStateException(
          "Failed to instantiate downloader for content type " + contentType);
    }
  }

  // LINT.IfChange
  private static SparseArray<Constructor<? extends Downloader>> createDownloaderConstructors() {
    SparseArray<Constructor<? extends Downloader>> array = new SparseArray<>();
    try {
      array.put(
          C.TYPE_DASH,
          getDownloaderConstructor(
              Class.forName("com.adt.vpm.videoplayer.source.dash.offline.DashDownloader")));
    } catch (ClassNotFoundException e) {
      // Expected if the app was built without the DASH module.
    }

    try {
      array.put(
          C.TYPE_HLS,
          getDownloaderConstructor(
              Class.forName("com.adt.vpm.videoplayer.source.hls.offline.HlsDownloader")));
    } catch (ClassNotFoundException e) {
      // Expected if the app was built without the HLS module.
    }
    try {
      array.put(
          C.TYPE_SS,
          getDownloaderConstructor(
              Class.forName(
                  "com.adt.vpm.videoplayer.source.smoothstreaming.offline.SsDownloader")));
    } catch (ClassNotFoundException e) {
      // Expected if the app was built without the SmoothStreaming module.
    }
    return array;
  }

  private static Constructor<? extends Downloader> getDownloaderConstructor(Class<?> clazz) {
    try {
      return clazz
          .asSubclass(Downloader.class)
          .getConstructor(MediaItem.class, CacheDataSource.Factory.class, Executor.class);
    } catch (NoSuchMethodException e) {
      // The downloader is present, but the expected constructor is missing.
      throw new IllegalStateException("Downloader constructor missing", e);
    }
  }
  // LINT.ThenChange(../../../../../../../../proguard-rules.txt)
}
