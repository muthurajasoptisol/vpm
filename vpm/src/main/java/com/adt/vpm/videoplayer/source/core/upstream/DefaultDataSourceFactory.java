/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.upstream;

import android.content.Context;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.common.upstream.DataSource;
import com.adt.vpm.videoplayer.source.common.upstream.DataSource.Factory;
import com.adt.vpm.videoplayer.source.common.upstream.TransferListener;

import static com.adt.vpm.videoplayer.source.common.ExoPlayerLibraryInfo.DEFAULT_USER_AGENT;


/**
 * A {@link Factory} that produces {@link DefaultDataSource} instances that delegate to
 * {@link DefaultHttpDataSource}s for non-file/asset/content URIs.
 */
public final class DefaultDataSourceFactory implements Factory {

  private final Context context;
  @Nullable private final TransferListener listener;
  private final DataSource.Factory baseDataSourceFactory;

  /**
   * Creates an instance.
   *
   * @param context A context.
   */
  public DefaultDataSourceFactory(Context context) {
    this(context, DEFAULT_USER_AGENT, /* listener= */ null);
  }

  /**
   * Creates an instance.
   *
   * @param context A context.
   * @param userAgent The User-Agent string that should be used.
   */
  public DefaultDataSourceFactory(Context context, String userAgent) {
    this(context, userAgent, /* listener= */ null);
  }

  /**
   * Creates an instance.
   *
   * @param context A context.
   * @param userAgent The User-Agent string that should be used.
   * @param listener An optional listener.
   */
  public DefaultDataSourceFactory(
      Context context, String userAgent, @Nullable TransferListener listener) {
    this(context, listener, new DefaultHttpDataSourceFactory(userAgent, listener));
  }

  /**
   * Creates an instance.
   *
   * @param context A context.
   * @param baseDataSourceFactory A {@link Factory} to be used to create a base {@link DataSource}
   *     for {@link DefaultDataSource}.
   * @see DefaultDataSource#DefaultDataSource(Context, DataSource)
   */
  public DefaultDataSourceFactory(Context context, DataSource.Factory baseDataSourceFactory) {
    this(context, /* listener= */ null, baseDataSourceFactory);
  }

  /**
   * Creates an instance.
   *
   * @param context A context.
   * @param listener An optional listener.
   * @param baseDataSourceFactory A {@link Factory} to be used to create a base {@link DataSource}
   *     for {@link DefaultDataSource}.
   * @see DefaultDataSource#DefaultDataSource(Context, DataSource)
   */
  public DefaultDataSourceFactory(
      Context context,
      @Nullable TransferListener listener,
      DataSource.Factory baseDataSourceFactory) {
    this.context = context.getApplicationContext();
    this.listener = listener;
    this.baseDataSourceFactory = baseDataSourceFactory;
  }

  @Override
  public DefaultDataSource createDataSource() {
    DefaultDataSource dataSource =
        new DefaultDataSource(context, baseDataSourceFactory.createDataSource());
    if (listener != null) {
      dataSource.addTransferListener(listener);
    }
    return dataSource;
  }
}
