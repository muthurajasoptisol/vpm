package com.adt.vpm.videoplayer.source.rtsp.core;

import com.adt.vpm.videoplayer.source.rtsp.message.Request;
import com.adt.vpm.videoplayer.source.rtsp.message.Response;
import com.adt.vpm.videoplayer.source.rtsp.message.InterleavedFrame;

public interface IEventListener {
  void onReceiveSuccess(Request request);
  void onReceiveSuccess(Response response);
  void onReceiveSuccess(InterleavedFrame interleavedFrame);
  void onReceiveFailure(int errorCode);
}