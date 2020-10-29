/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.upstream;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.common.upstream.HttpDataSource;
import com.adt.vpm.videoplayer.source.common.upstream.HttpDataSource.BaseFactory;
import com.adt.vpm.videoplayer.source.common.upstream.HttpDataSource.Factory;
import com.adt.vpm.videoplayer.source.common.upstream.TransferListener;
import com.adt.vpm.videoplayer.source.common.util.Assertions;

import static com.adt.vpm.videoplayer.source.common.ExoPlayerLibraryInfo.DEFAULT_USER_AGENT;


/** A {@link Factory} that produces {@link DefaultHttpDataSource} instances. */
public final class DefaultHttpDataSourceFactory extends BaseFactory {

  private final String userAgent;
  @Nullable private final TransferListener listener;
  private final int connectTimeoutMillis;
  private final int readTimeoutMillis;
  private final boolean allowCrossProtocolRedirects;

  /**
   * Creates an instance. Sets {@link DefaultHttpDataSource#DEFAULT_CONNECT_TIMEOUT_MILLIS} as the
   * connection timeout, {@link DefaultHttpDataSource#DEFAULT_READ_TIMEOUT_MILLIS} as the read
   * timeout and disables cross-protocol redirects.
   */
  public DefaultHttpDataSourceFactory() {
    this(DEFAULT_USER_AGENT);
  }

  /**
   * Creates an instance. Sets {@link DefaultHttpDataSource#DEFAULT_CONNECT_TIMEOUT_MILLIS} as the
   * connection timeout, {@link DefaultHttpDataSource#DEFAULT_READ_TIMEOUT_MILLIS} as the read
   * timeout and disables cross-protocol redirects.
   *
   * @param userAgent The User-Agent string that should be used.
   */
  public DefaultHttpDataSourceFactory(String userAgent) {
    this(userAgent, null);
  }

  /**
   * Creates an instance. Sets {@link DefaultHttpDataSource#DEFAULT_CONNECT_TIMEOUT_MILLIS} as the
   * connection timeout, {@link DefaultHttpDataSource#DEFAULT_READ_TIMEOUT_MILLIS} as the read
   * timeout and disables cross-protocol redirects.
   *
   * @param userAgent The User-Agent string that should be used.
   * @param listener An optional listener.
   * @see #DefaultHttpDataSourceFactory(String, TransferListener, int, int, boolean)
   */
  public DefaultHttpDataSourceFactory(String userAgent, @Nullable TransferListener listener) {
    this(userAgent, listener, DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
        DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS, false);
  }

  /**
   * @param userAgent The User-Agent string that should be used.
   * @param connectTimeoutMillis The connection timeout that should be used when requesting remote
   *     data, in milliseconds. A timeout of zero is interpreted as an infinite timeout.
   * @param readTimeoutMillis The read timeout that should be used when requesting remote data, in
   *     milliseconds. A timeout of zero is interpreted as an infinite timeout.
   * @param allowCrossProtocolRedirects Whether cross-protocol redirects (i.e. redirects from HTTP
   *     to HTTPS and vice versa) are enabled.
   */
  public DefaultHttpDataSourceFactory(
      String userAgent,
      int connectTimeoutMillis,
      int readTimeoutMillis,
      boolean allowCrossProtocolRedirects) {
    this(
        userAgent,
        /* listener= */ null,
        connectTimeoutMillis,
        readTimeoutMillis,
        allowCrossProtocolRedirects);
  }

  /**
   * @param userAgent The User-Agent string that should be used.
   * @param listener An optional listener.
   * @param connectTimeoutMillis The connection timeout that should be used when requesting remote
   *     data, in milliseconds. A timeout of zero is interpreted as an infinite timeout.
   * @param readTimeoutMillis The read timeout that should be used when requesting remote data, in
   *     milliseconds. A timeout of zero is interpreted as an infinite timeout.
   * @param allowCrossProtocolRedirects Whether cross-protocol redirects (i.e. redirects from HTTP
   *     to HTTPS and vice versa) are enabled.
   */
  public DefaultHttpDataSourceFactory(
      String userAgent,
      @Nullable TransferListener listener,
      int connectTimeoutMillis,
      int readTimeoutMillis,
      boolean allowCrossProtocolRedirects) {
    this.userAgent = Assertions.checkNotEmpty(userAgent);
    this.listener = listener;
    this.connectTimeoutMillis = connectTimeoutMillis;
    this.readTimeoutMillis = readTimeoutMillis;
    this.allowCrossProtocolRedirects = allowCrossProtocolRedirects;
  }

  @Override
  protected DefaultHttpDataSource createDataSourceInternal(
      HttpDataSource.RequestProperties defaultRequestProperties) {
    DefaultHttpDataSource dataSource =
        new DefaultHttpDataSource(
            userAgent,
            connectTimeoutMillis,
            readTimeoutMillis,
            allowCrossProtocolRedirects,
            defaultRequestProperties);
    if (listener != null) {
      dataSource.addTransferListener(listener);
    }
    return dataSource;
  }
}
