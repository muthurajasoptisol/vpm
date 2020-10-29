/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.util;

import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Looper;
import androidx.annotation.Nullable;

/**
 * The standard implementation of {@link Clock}, an instance of which is available via {@link
 * SystemClock#DEFAULT}.
 */
public class SystemClock implements Clock {

  protected SystemClock() {}

  @Override
  public long currentTimeMillis() {
    return System.currentTimeMillis();
  }

  @Override
  public long elapsedRealtime() {
    return android.os.SystemClock.elapsedRealtime();
  }

  @Override
  public long uptimeMillis() {
    return android.os.SystemClock.uptimeMillis();
  }

  @Override
  public void sleep(long sleepTimeMs) {
    android.os.SystemClock.sleep(sleepTimeMs);
  }

  @Override
  public HandlerWrapper createHandler(Looper looper, @Nullable Callback callback) {
    return new SystemHandlerWrapper(new Handler(looper, callback));
  }
}
