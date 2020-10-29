/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */

package com.adt.vpm.videoplayer.source.core;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.MediaItem;
import com.adt.vpm.videoplayer.source.common.util.Util;
import com.adt.vpm.videoplayer.source.core.source.DefaultMediaSourceFactory;
import com.adt.vpm.videoplayer.source.core.source.MediaPeriod;
import com.adt.vpm.videoplayer.source.core.source.MediaSource;
import com.adt.vpm.videoplayer.source.core.source.MediaSourceFactory;
import com.adt.vpm.videoplayer.source.core.source.TrackGroupArray;
import com.adt.vpm.videoplayer.source.core.upstream.Allocator;
import com.adt.vpm.videoplayer.source.core.upstream.DefaultAllocator;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import static com.adt.vpm.videoplayer.source.common.util.Assertions.checkNotNull;

// TODO(internal b/161127201): discard samples written to the sample queue.
/** Retrieves the static metadata of {@link MediaItem MediaItems}. */
public final class MetadataRetriever {

  private MetadataRetriever() {}

  /**
   * Retrieves the {@link TrackGroupArray} corresponding to a {@link MediaItem}.
   *
   * <p>This is equivalent to using {@code retrieveMetadata(new DefaultMediaSourceFactory(context),
   * mediaItem)}.
   *
   * @param context The {@link Context}.
   * @param mediaItem The {@link MediaItem} whose metadata should be retrieved.
   * @return A {@link ListenableFuture} of the result.
   */
  public static ListenableFuture<TrackGroupArray> retrieveMetadata(
      Context context, MediaItem mediaItem) {
    return retrieveMetadata(new DefaultMediaSourceFactory(context), mediaItem);
  }

  /**
   * Retrieves the {@link TrackGroupArray} corresponding to a {@link MediaItem}.
   *
   * <p>This method is thread-safe.
   *
   * @param mediaSourceFactory mediaSourceFactory The {@link MediaSourceFactory} to use to read the
   *     data.
   * @param mediaItem The {@link MediaItem} whose metadata should be retrieved.
   * @return A {@link ListenableFuture} of the result.
   */
  public static ListenableFuture<TrackGroupArray> retrieveMetadata(
      MediaSourceFactory mediaSourceFactory, MediaItem mediaItem) {
    // Recreate thread and handler every time this method is called so that it can be used
    // concurrently.
    return new MetadataRetrieverInternal(mediaSourceFactory).retrieveMetadata(mediaItem);
  }

  private static final class MetadataRetrieverInternal {

    private static final int MESSAGE_PREPARE_SOURCE = 0;
    private static final int MESSAGE_CHECK_FOR_FAILURE = 1;
    private static final int MESSAGE_CONTINUE_LOADING = 2;
    private static final int MESSAGE_RELEASE = 3;

    private final MediaSourceFactory mediaSourceFactory;
    private final HandlerThread mediaSourceThread;
    private final Handler mediaSourceHandler;
    private final SettableFuture<TrackGroupArray> trackGroupsFuture;

    public MetadataRetrieverInternal(MediaSourceFactory mediaSourceFactory) {
      this.mediaSourceFactory = mediaSourceFactory;
      mediaSourceThread = new HandlerThread("ExoPlayer:MetadataRetriever");
      mediaSourceThread.start();
      mediaSourceHandler =
          Util.createHandler(mediaSourceThread.getLooper(), new MediaSourceHandlerCallback());
      trackGroupsFuture = SettableFuture.create();
    }

    public ListenableFuture<TrackGroupArray> retrieveMetadata(MediaItem mediaItem) {
      mediaSourceHandler.obtainMessage(MESSAGE_PREPARE_SOURCE, mediaItem).sendToTarget();
      return trackGroupsFuture;
    }

    private final class MediaSourceHandlerCallback implements Handler.Callback {

      private static final int ERROR_POLL_INTERVAL_MS = 100;

      private final MediaSourceCaller mediaSourceCaller;

      private @MonotonicNonNull MediaSource mediaSource;
      private @MonotonicNonNull MediaPeriod mediaPeriod;

      public MediaSourceHandlerCallback() {
        mediaSourceCaller = new MediaSourceCaller();
      }

      @Override
      public boolean handleMessage(Message msg) {
        switch (msg.what) {
          case MESSAGE_PREPARE_SOURCE:
            MediaItem mediaItem = (MediaItem) msg.obj;
            mediaSource = mediaSourceFactory.createMediaSource(mediaItem);
            mediaSource.prepareSource(mediaSourceCaller, /* mediaTransferListener= */ null);
            mediaSourceHandler.sendEmptyMessage(MESSAGE_CHECK_FOR_FAILURE);
            return true;
          case MESSAGE_CHECK_FOR_FAILURE:
            try {
              if (mediaPeriod == null) {
                checkNotNull(mediaSource).maybeThrowSourceInfoRefreshError();
              } else {
                mediaPeriod.maybeThrowPrepareError();
              }
              mediaSourceHandler.sendEmptyMessageDelayed(
                  MESSAGE_CHECK_FOR_FAILURE, /* delayMillis= */ ERROR_POLL_INTERVAL_MS);
            } catch (Exception e) {
              trackGroupsFuture.setException(e);
              mediaSourceHandler.obtainMessage(MESSAGE_RELEASE).sendToTarget();
            }
            return true;
          case MESSAGE_CONTINUE_LOADING:
            checkNotNull(mediaPeriod).continueLoading(/* positionUs= */ 0);
            return true;
          case MESSAGE_RELEASE:
            if (mediaPeriod != null) {
              checkNotNull(mediaSource).releasePeriod(mediaPeriod);
            }
            checkNotNull(mediaSource).releaseSource(mediaSourceCaller);
            mediaSourceHandler.removeCallbacksAndMessages(/* token= */ null);
            mediaSourceThread.quit();
            return true;
          default:
            return false;
        }
      }

      private final class MediaSourceCaller implements MediaSource.MediaSourceCaller {

        private final MediaPeriodCallback mediaPeriodCallback;
        private final Allocator allocator;

        private boolean mediaPeriodCreated;

        public MediaSourceCaller() {
          mediaPeriodCallback = new MediaPeriodCallback();
          allocator =
              new DefaultAllocator(
                  /* trimOnReset= */ true,
                  /* individualAllocationSize= */ C.DEFAULT_BUFFER_SEGMENT_SIZE);
        }

        @Override
        public void onSourceInfoRefreshed(MediaSource source, Timeline timeline) {
          if (mediaPeriodCreated) {
            // Ignore dynamic updates.
            return;
          }
          mediaPeriodCreated = true;
          mediaPeriod =
              source.createPeriod(
                  new MediaSource.MediaPeriodId(timeline.getUidOfPeriod(/* periodIndex= */ 0)),
                  allocator,
                  /* startPositionUs= */ 0);
          mediaPeriod.prepare(mediaPeriodCallback, /* positionUs= */ 0);
        }

        private final class MediaPeriodCallback implements MediaPeriod.Callback {

          @Override
          public void onPrepared(MediaPeriod mediaPeriod) {
            trackGroupsFuture.set(mediaPeriod.getTrackGroups());
            mediaSourceHandler.obtainMessage(MESSAGE_RELEASE).sendToTarget();
          }

          @Override
          public void onContinueLoadingRequested(MediaPeriod mediaPeriod) {
            mediaSourceHandler.obtainMessage(MESSAGE_CONTINUE_LOADING).sendToTarget();
          }
        }
      }
    }
  }
}
