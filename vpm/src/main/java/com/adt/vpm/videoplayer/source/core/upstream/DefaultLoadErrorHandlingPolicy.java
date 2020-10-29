/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.upstream;

import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.ParserException;
import com.adt.vpm.videoplayer.source.common.upstream.HttpDataSource.InvalidResponseCodeException;

import java.io.FileNotFoundException;
import java.io.IOException;

import static java.lang.Math.min;

/** Default implementation of {@link LoadErrorHandlingPolicy}. */
public class DefaultLoadErrorHandlingPolicy implements LoadErrorHandlingPolicy {

  /** The default minimum number of times to retry loading data prior to propagating the error. */
  public static final int DEFAULT_MIN_LOADABLE_RETRY_COUNT = 3;
  /**
   * The default minimum number of times to retry loading prior to failing for progressive live
   * streams.
   */
  public static final int DEFAULT_MIN_LOADABLE_RETRY_COUNT_PROGRESSIVE_LIVE = 6;
  /** The default duration for which a track is excluded in milliseconds. */
  public static final long DEFAULT_TRACK_BLACKLIST_MS = 60_000;

  private static final int DEFAULT_BEHAVIOR_MIN_LOADABLE_RETRY_COUNT = -1;

  private final int minimumLoadableRetryCount;

  /**
   * Creates an instance with default behavior.
   *
   * <p>{@link #getMinimumLoadableRetryCount} will return {@link
   * #DEFAULT_MIN_LOADABLE_RETRY_COUNT_PROGRESSIVE_LIVE} for {@code dataType} {@link
   * C#DATA_TYPE_MEDIA_PROGRESSIVE_LIVE}. For other {@code dataType} values, it will return {@link
   * #DEFAULT_MIN_LOADABLE_RETRY_COUNT}.
   */
  public DefaultLoadErrorHandlingPolicy() {
    this(DEFAULT_BEHAVIOR_MIN_LOADABLE_RETRY_COUNT);
  }

  /**
   * Creates an instance with the given value for {@link #getMinimumLoadableRetryCount(int)}.
   *
   * @param minimumLoadableRetryCount See {@link #getMinimumLoadableRetryCount}.
   */
  public DefaultLoadErrorHandlingPolicy(int minimumLoadableRetryCount) {
    this.minimumLoadableRetryCount = minimumLoadableRetryCount;
  }

  /**
   * Returns the exclusion duration, given by {@link #DEFAULT_TRACK_BLACKLIST_MS}, if the load error
   * was an {@link InvalidResponseCodeException} with response code HTTP 404, 410 or 416, or {@link
   * C#TIME_UNSET} otherwise.
   */
  @Override
  public long getBlacklistDurationMsFor(LoadErrorInfo loadErrorInfo) {
    IOException exception = loadErrorInfo.exception;
    if (exception instanceof InvalidResponseCodeException) {
      int responseCode = ((InvalidResponseCodeException) exception).responseCode;
      return responseCode == 404 // HTTP 404 Not Found.
              || responseCode == 410 // HTTP 410 Gone.
              || responseCode == 416 // HTTP 416 Range Not Satisfiable.
          ? DEFAULT_TRACK_BLACKLIST_MS
          : C.TIME_UNSET;
    }
    return C.TIME_UNSET;
  }

  /**
   * Retries for any exception that is not a subclass of {@link ParserException}, {@link
   * FileNotFoundException} or {@link Loader.UnexpectedLoaderException}. The retry delay is calculated as
   * {@code Math.min((errorCount - 1) * 1000, 5000)}.
   */
  @Override
  public long getRetryDelayMsFor(LoadErrorInfo loadErrorInfo) {
    IOException exception = loadErrorInfo.exception;
    return exception instanceof ParserException
            || exception instanceof FileNotFoundException
            || exception instanceof Loader.UnexpectedLoaderException
        ? C.TIME_UNSET
        : min((loadErrorInfo.errorCount - 1) * 1000, 5000);
  }

  /**
   * See {@link #DefaultLoadErrorHandlingPolicy()} and {@link #DefaultLoadErrorHandlingPolicy(int)}
   * for documentation about the behavior of this method.
   */
  @Override
  public int getMinimumLoadableRetryCount(int dataType) {
    if (minimumLoadableRetryCount == DEFAULT_BEHAVIOR_MIN_LOADABLE_RETRY_COUNT) {
      return dataType == C.DATA_TYPE_MEDIA_PROGRESSIVE_LIVE
          ? DEFAULT_MIN_LOADABLE_RETRY_COUNT_PROGRESSIVE_LIVE
          : DEFAULT_MIN_LOADABLE_RETRY_COUNT;
    } else {
      return minimumLoadableRetryCount;
    }
  }
}
