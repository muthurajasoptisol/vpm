package com.adt.vpm.videoplayer.source.rtsp;

import com.adt.vpm.videoplayer.source.core.source.MediaSource;
import com.adt.vpm.videoplayer.source.rtsp.core.Client;
import com.adt.vpm.videoplayer.source.rtsp.core.FallbackPolicy;

/* package */ final class RtspFallbackPolicy extends FallbackPolicy  {

  RtspFallbackPolicy(MediaSource mediaSource, Client.Factory factory) {
    super(mediaSource, factory);
  }

  protected boolean isAllowError(Throwable error) {
    return (error instanceof RtspMediaException &&
        RtspMediaException.LOAD_TIMEOUT == ((RtspMediaException)error).type);
  }
}
