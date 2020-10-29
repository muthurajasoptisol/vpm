/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.offline;

import android.net.Uri;
import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.core.util.PriorityTaskManager;
import com.adt.vpm.videoplayer.source.core.util.RunnableFutureTask;
import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.MediaItem;
import com.adt.vpm.videoplayer.source.common.upstream.DataSpec;
import com.adt.vpm.videoplayer.source.core.upstream.cache.CacheDataSource;
import com.adt.vpm.videoplayer.source.core.upstream.cache.CacheWriter;
import com.adt.vpm.videoplayer.source.common.util.Assertions;
import com.adt.vpm.videoplayer.source.common.util.Util;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

/** A downloader for progressive media streams. */
public final class ProgressiveDownloader implements Downloader {

  private final Executor executor;
  private final DataSpec dataSpec;
  private final CacheDataSource dataSource;
  @Nullable private final PriorityTaskManager priorityTaskManager;

  @Nullable private ProgressListener progressListener;
  private volatile @MonotonicNonNull
  RunnableFutureTask<Void, IOException> downloadRunnable;
  private volatile boolean isCanceled;

  /** @deprecated Use {@link #ProgressiveDownloader(MediaItem, CacheDataSource.Factory)} instead. */
  @SuppressWarnings("deprecation")
  @Deprecated
  public ProgressiveDownloader(
      Uri uri, @Nullable String customCacheKey, CacheDataSource.Factory cacheDataSourceFactory) {
    this(uri, customCacheKey, cacheDataSourceFactory, Runnable::run);
  }

  /**
   * Creates a new instance.
   *
   * @param mediaItem The media item with a uri to the stream to be downloaded.
   * @param cacheDataSourceFactory A {@link CacheDataSource.Factory} for the cache into which the
   *     download will be written.
   */
  public ProgressiveDownloader(
      MediaItem mediaItem, CacheDataSource.Factory cacheDataSourceFactory) {
    this(mediaItem, cacheDataSourceFactory, Runnable::run);
  }

  /**
   * @deprecated Use {@link #ProgressiveDownloader(MediaItem, CacheDataSource.Factory, Executor)}
   *     instead.
   */
  @Deprecated
  public ProgressiveDownloader(
      Uri uri,
      @Nullable String customCacheKey,
      CacheDataSource.Factory cacheDataSourceFactory,
      Executor executor) {
    this(
        new MediaItem.Builder().setUri(uri).setCustomCacheKey(customCacheKey).build(),
        cacheDataSourceFactory,
        executor);
  }

  /**
   * Creates a new instance.
   *
   * @param mediaItem The media item with a uri to the stream to be downloaded.
   * @param cacheDataSourceFactory A {@link CacheDataSource.Factory} for the cache into which the
   *     download will be written.
   * @param executor An {@link Executor} used to make requests for the media being downloaded. In
   *     the future, providing an {@link Executor} that uses multiple threads may speed up the
   *     download by allowing parts of it to be executed in parallel.
   */
  public ProgressiveDownloader(
      MediaItem mediaItem, CacheDataSource.Factory cacheDataSourceFactory, Executor executor) {
    this.executor = Assertions.checkNotNull(executor);
    Assertions.checkNotNull(mediaItem.playbackProperties);
    dataSpec =
        new DataSpec.Builder()
            .setUri(mediaItem.playbackProperties.uri)
            .setKey(mediaItem.playbackProperties.customCacheKey)
            .setFlags(DataSpec.FLAG_ALLOW_CACHE_FRAGMENTATION)
            .build();
    dataSource = cacheDataSourceFactory.createDataSourceForDownloading();
    priorityTaskManager = cacheDataSourceFactory.getUpstreamPriorityTaskManager();
  }

  @Override
  public void download(@Nullable ProgressListener progressListener)
      throws IOException, InterruptedException {
    this.progressListener = progressListener;
    if (downloadRunnable == null) {
      CacheWriter cacheWriter =
          new CacheWriter(
              dataSource,
              dataSpec,
              /* allowShortContent= */ false,
              /* temporaryBuffer= */ null,
              this::onProgress);
      downloadRunnable =
          new RunnableFutureTask<Void, IOException>() {
            @Override
            protected Void doWork() throws IOException {
              cacheWriter.cache();
              return null;
            }

            @Override
            protected void cancelWork() {
              cacheWriter.cancel();
            }
          };
    }

    if (priorityTaskManager != null) {
      priorityTaskManager.add(C.PRIORITY_DOWNLOAD);
    }
    try {
      boolean finished = false;
      while (!finished && !isCanceled) {
        if (priorityTaskManager != null) {
          priorityTaskManager.proceed(C.PRIORITY_DOWNLOAD);
        }
        executor.execute(downloadRunnable);
        try {
          downloadRunnable.get();
          finished = true;
        } catch (ExecutionException e) {
          Throwable cause = Assertions.checkNotNull(e.getCause());
          if (cause instanceof PriorityTaskManager.PriorityTooLowException) {
            // The next loop iteration will block until the task is able to proceed.
          } else if (cause instanceof IOException) {
            throw (IOException) cause;
          } else {
            // The cause must be an uncaught Throwable type.
            Util.sneakyThrow(cause);
          }
        }
      }
    } finally {
      // If the main download thread was interrupted as part of cancelation, then it's possible that
      // the runnable is still doing work. We need to wait until it's finished before returning.
      downloadRunnable.blockUntilFinished();
      if (priorityTaskManager != null) {
        priorityTaskManager.remove(C.PRIORITY_DOWNLOAD);
      }
    }
  }

  @Override
  public void cancel() {
    isCanceled = true;
    RunnableFutureTask<Void, IOException> downloadRunnable = this.downloadRunnable;
    if (downloadRunnable != null) {
      downloadRunnable.cancel(/* interruptIfRunning= */ true);
    }
  }

  @Override
  public void remove() {
    dataSource.getCache().removeResource(dataSource.getCacheKeyFactory().buildCacheKey(dataSpec));
  }

  private void onProgress(long contentLength, long bytesCached, long newBytesCached) {
    if (progressListener == null) {
      return;
    }
    float percentDownloaded =
        contentLength == C.LENGTH_UNSET || contentLength == 0
            ? C.PERCENTAGE_UNSET
            : ((bytesCached * 100f) / contentLength);
    progressListener.onProgress(contentLength, bytesCached, percentDownloaded);
  }
}
