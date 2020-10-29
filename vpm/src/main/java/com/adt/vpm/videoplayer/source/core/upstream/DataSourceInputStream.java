/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.upstream;

import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.upstream.DataSource;
import com.adt.vpm.videoplayer.source.common.upstream.DataSpec;
import com.adt.vpm.videoplayer.source.common.util.Assertions;

import java.io.IOException;
import java.io.InputStream;

/**
 * Allows data corresponding to a given {@link DataSpec} to be read from a {@link DataSource} and
 * consumed through an {@link InputStream}.
 */
public final class DataSourceInputStream extends InputStream {

  private final DataSource dataSource;
  private final DataSpec dataSpec;
  private final byte[] singleByteArray;

  private boolean opened = false;
  private boolean closed = false;
  private long totalBytesRead;

  /**
   * @param dataSource The {@link DataSource} from which the data should be read.
   * @param dataSpec The {@link DataSpec} defining the data to be read from {@code dataSource}.
   */
  public DataSourceInputStream(DataSource dataSource, DataSpec dataSpec) {
    this.dataSource = dataSource;
    this.dataSpec = dataSpec;
    singleByteArray = new byte[1];
  }

  /**
   * Returns the total number of bytes that have been read or skipped.
   */
  public long bytesRead() {
    return totalBytesRead;
  }

  /**
   * Optional call to open the underlying {@link DataSource}.
   * <p>
   * Calling this method does nothing if the {@link DataSource} is already open. Calling this
   * method is optional, since the read and skip methods will automatically open the underlying
   * {@link DataSource} if it's not open already.
   *
   * @throws IOException If an error occurs opening the {@link DataSource}.
   */
  public void open() throws IOException {
    checkOpened();
  }

  @Override
  public int read() throws IOException {
    int length = read(singleByteArray);
    return length == -1 ? -1 : (singleByteArray[0] & 0xFF);
  }

  @Override
  public int read(byte[] buffer) throws IOException {
    return read(buffer, 0, buffer.length);
  }

  @Override
  public int read(byte[] buffer, int offset, int length) throws IOException {
    Assertions.checkState(!closed);
    checkOpened();
    int bytesRead = dataSource.read(buffer, offset, length);
    if (bytesRead == C.RESULT_END_OF_INPUT) {
      return -1;
    } else {
      totalBytesRead += bytesRead;
      return bytesRead;
    }
  }

  @Override
  public void close() throws IOException {
    if (!closed) {
      dataSource.close();
      closed = true;
    }
  }

  private void checkOpened() throws IOException {
    if (!opened) {
      dataSource.open(dataSpec);
      opened = true;
    }
  }

}
