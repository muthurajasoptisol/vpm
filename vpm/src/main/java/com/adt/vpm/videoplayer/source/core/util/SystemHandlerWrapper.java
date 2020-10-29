/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.util;

import android.os.Looper;
import android.os.Message;
import androidx.annotation.Nullable;

/** The standard implementation of {@link HandlerWrapper}. */
/* package */ final class SystemHandlerWrapper implements HandlerWrapper {

  private final android.os.Handler handler;

  public SystemHandlerWrapper(android.os.Handler handler) {
    this.handler = handler;
  }

  @Override
  public Looper getLooper() {
    return handler.getLooper();
  }

  @Override
  public Message obtainMessage(int what) {
    return handler.obtainMessage(what);
  }

  @Override
  public Message obtainMessage(int what, @Nullable Object obj) {
    return handler.obtainMessage(what, obj);
  }

  @Override
  public Message obtainMessage(int what, int arg1, int arg2) {
    return handler.obtainMessage(what, arg1, arg2);
  }

  @Override
  public Message obtainMessage(int what, int arg1, int arg2, @Nullable Object obj) {
    return handler.obtainMessage(what, arg1, arg2, obj);
  }

  @Override
  public boolean sendEmptyMessage(int what) {
    return handler.sendEmptyMessage(what);
  }

  @Override
  public boolean sendEmptyMessageAtTime(int what, long uptimeMs) {
    return handler.sendEmptyMessageAtTime(what, uptimeMs);
  }

  @Override
  public void removeMessages(int what) {
    handler.removeMessages(what);
  }

  @Override
  public void removeCallbacksAndMessages(@Nullable Object token) {
    handler.removeCallbacksAndMessages(token);
  }

  @Override
  public boolean post(Runnable runnable) {
    return handler.post(runnable);
  }

  @Override
  public boolean postDelayed(Runnable runnable, long delayMs) {
    return handler.postDelayed(runnable, delayMs);
  }
}
