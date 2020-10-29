/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.util;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;

/**
 * An interface through which system clocks can be read and {@link HandlerWrapper}s created. The
 * {@link #DEFAULT} implementation must be used for all non-test cases.
 */
public interface Clock {

  /**
   * Default {@link Clock} to use for all non-test cases.
   */
  Clock DEFAULT = new SystemClock();

  /**
   * Returns the current time in milliseconds since the Unix Epoch.
   *
   * @see System#currentTimeMillis()
   */
  long currentTimeMillis();

  /** @see android.os.SystemClock#elapsedRealtime() */
  long elapsedRealtime();

  /** @see android.os.SystemClock#uptimeMillis() */
  long uptimeMillis();

  /** @see android.os.SystemClock#sleep(long) */
  void sleep(long sleepTimeMs);

  /**
   * Creates a {@link HandlerWrapper} using a specified looper and a specified callback for handling
   * messages.
   *
   * @see Handler#Handler(Looper, Handler.Callback)
   */
  HandlerWrapper createHandler(Looper looper, @Nullable Handler.Callback callback);
}
