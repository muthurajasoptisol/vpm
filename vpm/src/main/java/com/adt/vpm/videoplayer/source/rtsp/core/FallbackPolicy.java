package com.adt.vpm.videoplayer.source.rtsp.core;

import android.os.Handler;
import android.os.Looper;

import com.adt.vpm.videoplayer.source.core.ExoPlayer;
import com.adt.vpm.videoplayer.source.core.source.MediaSource;

public abstract class FallbackPolicy {

  private boolean isExecuteCalled;
  private final Client.Factory factory;
  private final MediaSource mediaSource;

  public FallbackPolicy(MediaSource mediaSource, Client.Factory factory) {
    this.factory = factory;
    this.mediaSource = mediaSource;
  }

  protected abstract boolean isAllowError(Throwable error);

  public final void retryIfAllowError(Throwable error) {
    if (!isExecuteCalled && factory.mode != Client.RTSP_INTERLEAVED && isAllowError(error)) {
      isExecuteCalled = true;
      factory.mode = Client.RTSP_INTERLEAVED;

      new Handler(Looper.getMainLooper()).post(() -> {
        ExoPlayer player = factory.player;
        player.stop(true);
        player.setMediaSource(mediaSource);
        player.prepare();
      });
    }
  }

}
