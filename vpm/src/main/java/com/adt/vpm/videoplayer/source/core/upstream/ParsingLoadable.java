/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.upstream;

import android.net.Uri;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.upstream.DataSource;
import com.adt.vpm.videoplayer.source.common.upstream.DataSpec;
import com.adt.vpm.videoplayer.source.common.util.Assertions;
import com.adt.vpm.videoplayer.source.common.util.Util;
import com.adt.vpm.videoplayer.source.core.source.LoadEventInfo;
import com.adt.vpm.videoplayer.source.core.upstream.Loader.Loadable;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * A {@link Loadable} for objects that can be parsed from binary data using a {@link Parser}.
 *
 * @param <T> The type of the object being loaded.
 */
public final class ParsingLoadable<T> implements Loadable {

  /**
   * Parses an object from loaded data.
   */
  public interface Parser<T> {

    /**
     * Parses an object from a response.
     *
     * @param uri The source {@link Uri} of the response, after any redirection.
     * @param inputStream An {@link InputStream} from which the response data can be read.
     * @return The parsed object.
     * @throws ParserException If an error occurs parsing the data.
     * @throws IOException If an error occurs reading data from the stream.
     */
    T parse(Uri uri, InputStream inputStream) throws IOException;

  }

  /**
   * Loads a single parsable object.
   *
   * @param dataSource The {@link DataSource} through which the object should be read.
   * @param parser The {@link Parser} to parse the object from the response.
   * @param uri The {@link Uri} of the object to read.
   * @param type The type of the data. One of the {@link C}{@code DATA_TYPE_*} constants.
   * @return The parsed object
   * @throws IOException Thrown if there is an error while loading or parsing.
   */
  public static <T> T load(DataSource dataSource, Parser<? extends T> parser, Uri uri, int type)
      throws IOException {
    ParsingLoadable<T> loadable = new ParsingLoadable<>(dataSource, uri, type, parser);
    loadable.load();
    return Assertions.checkNotNull(loadable.getResult());
  }

  /**
   * Loads a single parsable object.
   *
   * @param dataSource The {@link DataSource} through which the object should be read.
   * @param parser The {@link Parser} to parse the object from the response.
   * @param dataSpec The {@link DataSpec} of the object to read.
   * @param type The type of the data. One of the {@link C}{@code DATA_TYPE_*} constants.
   * @return The parsed object
   * @throws IOException Thrown if there is an error while loading or parsing.
   */
  public static <T> T load(
          DataSource dataSource, Parser<? extends T> parser, DataSpec dataSpec, int type)
      throws IOException {
    ParsingLoadable<T> loadable = new ParsingLoadable<>(dataSource, dataSpec, type, parser);
    loadable.load();
    return Assertions.checkNotNull(loadable.getResult());
  }

  /** Identifies the load task for this loadable. */
  public final long loadTaskId;
  /** The {@link DataSpec} that defines the data to be loaded. */
  public final DataSpec dataSpec;
  /**
   * The type of the data. One of the {@code DATA_TYPE_*} constants defined in {@link C}. For
   * reporting only.
   */
  public final int type;

  private final StatsDataSource dataSource;
  private final Parser<? extends T> parser;

  private volatile @Nullable T result;

  /**
   * @param dataSource A {@link DataSource} to use when loading the data.
   * @param uri The {@link Uri} from which the object should be loaded.
   * @param type See {@link #type}.
   * @param parser Parses the object from the response.
   */
  public ParsingLoadable(DataSource dataSource, Uri uri, int type, Parser<? extends T> parser) {
    this(
        dataSource,
        new DataSpec.Builder().setUri(uri).setFlags(DataSpec.FLAG_ALLOW_GZIP).build(),
        type,
        parser);
  }

  /**
   * @param dataSource A {@link DataSource} to use when loading the data.
   * @param dataSpec The {@link DataSpec} from which the object should be loaded.
   * @param type See {@link #type}.
   * @param parser Parses the object from the response.
   */
  public ParsingLoadable(DataSource dataSource, DataSpec dataSpec, int type,
      Parser<? extends T> parser) {
    this.dataSource = new StatsDataSource(dataSource);
    this.dataSpec = dataSpec;
    this.type = type;
    this.parser = parser;
    loadTaskId = LoadEventInfo.getNewId();
  }

  /** Returns the loaded object, or null if an object has not been loaded. */
  @Nullable
  public final T getResult() {
    return result;
  }

  /**
   * Returns the number of bytes loaded. In the case that the network response was compressed, the
   * value returned is the size of the data <em>after</em> decompression. Must only be called after
   * the load completed, failed, or was canceled.
   */
  public long bytesLoaded() {
    return dataSource.getBytesRead();
  }

  /**
   * Returns the {@link Uri} from which data was read. If redirection occurred, this is the
   * redirected uri. Must only be called after the load completed, failed, or was canceled.
   */
  public Uri getUri() {
    return dataSource.getLastOpenedUri();
  }

  /**
   * Returns the response headers associated with the load. Must only be called after the load
   * completed, failed, or was canceled.
   */
  public Map<String, List<String>> getResponseHeaders() {
    return dataSource.getLastResponseHeaders();
  }

  @Override
  public final void cancelLoad() {
    // Do nothing.
  }

  @Override
  public final void load() throws IOException {
    // We always load from the beginning, so reset bytesRead to 0.
    dataSource.resetBytesRead();
    DataSourceInputStream inputStream = new DataSourceInputStream(dataSource, dataSpec);
    try {
      inputStream.open();
      Uri dataSourceUri = Assertions.checkNotNull(dataSource.getUri());
      result = parser.parse(dataSourceUri, inputStream);
    } finally {
      Util.closeQuietly(inputStream);
    }
  }
}
