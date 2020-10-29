/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.util;

import android.util.Pair;

/** Converts throwables into error codes and user readable error messages. */
public interface ErrorMessageProvider<T extends Throwable> {

  /**
   * Returns a pair consisting of an error code and a user readable error message for the given
   * throwable.
   *
   * @param throwable The throwable for which an error code and message should be generated.
   * @return A pair consisting of an error code and a user readable error message.
   */
  Pair<Integer, String> getErrorMessage(T throwable);
}
