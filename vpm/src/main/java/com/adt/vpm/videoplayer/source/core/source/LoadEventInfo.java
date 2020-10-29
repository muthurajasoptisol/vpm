/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.source;

import android.net.Uri;
import android.os.SystemClock;
import com.adt.vpm.videoplayer.source.common.upstream.DataSpec;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/** {@link MediaSource} load event information. */
public final class LoadEventInfo {

  /** Used for the generation of unique ids. */
  private static final AtomicLong idSource = new AtomicLong();

  /** Returns an non-negative identifier which is unique to the JVM instance. */
  public static long getNewId() {
    return idSource.getAndIncrement();
  }

  /** Identifies the load task to which this event corresponds. */
  public final long loadTaskId;
  /** Defines the requested data. */
  public final DataSpec dataSpec;
  /**
   * The {@link Uri} from which data is being read. The uri will be identical to the one in {@link
   * #dataSpec}.uri unless redirection has occurred. If redirection has occurred, this is the uri
   * after redirection.
   */
  public final Uri uri;
  /** The response headers associated with the load, or an empty map if unavailable. */
  public final Map<String, List<String>> responseHeaders;
  /** The value of {@link SystemClock#elapsedRealtime} at the time of the load event. */
  public final long elapsedRealtimeMs;
  /** The duration of the load up to the event time. */
  public final long loadDurationMs;
  /** The number of bytes that were loaded up to the event time. */
  public final long bytesLoaded;

  /**
   * Equivalent to {@link #LoadEventInfo(long, DataSpec, Uri, Map, long, long, long)
   * LoadEventInfo(loadTaskId, dataSpec, dataSpec.uri, Collections.emptyMap(), elapsedRealtimeMs, 0,
   * 0)}.
   */
  public LoadEventInfo(long loadTaskId, DataSpec dataSpec, long elapsedRealtimeMs) {
    this(
        loadTaskId,
        dataSpec,
        dataSpec.uri,
        Collections.emptyMap(),
        elapsedRealtimeMs,
        /* loadDurationMs= */ 0,
        /* bytesLoaded= */ 0);
  }

  /**
   * Creates load event info.
   *
   * @param loadTaskId See {@link #loadTaskId}.
   * @param dataSpec See {@link #dataSpec}.
   * @param uri See {@link #uri}.
   * @param responseHeaders See {@link #responseHeaders}.
   * @param elapsedRealtimeMs See {@link #elapsedRealtimeMs}.
   * @param loadDurationMs See {@link #loadDurationMs}.
   * @param bytesLoaded See {@link #bytesLoaded}.
   */
  public LoadEventInfo(
      long loadTaskId,
      DataSpec dataSpec,
      Uri uri,
      Map<String, List<String>> responseHeaders,
      long elapsedRealtimeMs,
      long loadDurationMs,
      long bytesLoaded) {
    this.loadTaskId = loadTaskId;
    this.dataSpec = dataSpec;
    this.uri = uri;
    this.responseHeaders = responseHeaders;
    this.elapsedRealtimeMs = elapsedRealtimeMs;
    this.loadDurationMs = loadDurationMs;
    this.bytesLoaded = bytesLoaded;
  }
}
