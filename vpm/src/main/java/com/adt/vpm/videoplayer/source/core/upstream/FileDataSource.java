/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.upstream;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.upstream.DataSource;
import com.adt.vpm.videoplayer.source.common.upstream.DataSpec;
import com.adt.vpm.videoplayer.source.common.upstream.TransferListener;
import com.adt.vpm.videoplayer.source.common.util.Assertions;

import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import static com.adt.vpm.videoplayer.source.common.util.Util.castNonNull;
import static java.lang.Math.min;

/** A {@link DataSource} for reading local files. */
public final class FileDataSource extends BaseDataSource {

  /** Thrown when a {@link FileDataSource} encounters an error reading a file. */
  public static class FileDataSourceException extends IOException {

    public FileDataSourceException(IOException cause) {
      super(cause);
    }

    public FileDataSourceException(String message, IOException cause) {
      super(message, cause);
    }
  }

  /** {@link DataSource.Factory} for {@link FileDataSource} instances. */
  public static final class Factory implements DataSource.Factory {

    @Nullable private TransferListener listener;

    /**
     * Sets a {@link TransferListener} for {@link FileDataSource} instances created by this factory.
     *
     * @param listener The {@link TransferListener}.
     * @return This factory.
     */
    public Factory setListener(@Nullable TransferListener listener) {
      this.listener = listener;
      return this;
    }

    @Override
    public FileDataSource createDataSource() {
      FileDataSource dataSource = new FileDataSource();
      if (listener != null) {
        dataSource.addTransferListener(listener);
      }
      return dataSource;
    }
  }

  @Nullable private RandomAccessFile file;
  @Nullable private Uri uri;
  private long bytesRemaining;
  private boolean opened;

  public FileDataSource() {
    super(/* isNetwork= */ false);
  }

  @Override
  public long open(DataSpec dataSpec) throws FileDataSourceException {
    try {
      Uri uri = dataSpec.uri;
      this.uri = uri;

      transferInitializing(dataSpec);

      this.file = openLocalFile(uri);
      file.seek(dataSpec.position);
      bytesRemaining = dataSpec.length == C.LENGTH_UNSET ? file.length() - dataSpec.position
          : dataSpec.length;
      if (bytesRemaining < 0) {
        throw new EOFException();
      }
    } catch (IOException e) {
      throw new FileDataSourceException(e);
    }

    opened = true;
    transferStarted(dataSpec);

    return bytesRemaining;
  }

  @Override
  public int read(byte[] buffer, int offset, int readLength) throws FileDataSourceException {
    if (readLength == 0) {
      return 0;
    } else if (bytesRemaining == 0) {
      return C.RESULT_END_OF_INPUT;
    } else {
      int bytesRead;
      try {
        bytesRead = castNonNull(file).read(buffer, offset, (int) min(bytesRemaining, readLength));
      } catch (IOException e) {
        throw new FileDataSourceException(e);
      }

      if (bytesRead > 0) {
        bytesRemaining -= bytesRead;
        bytesTransferred(bytesRead);
      }

      return bytesRead;
    }
  }

  @Override
  @Nullable
  public Uri getUri() {
    return uri;
  }

  @Override
  public void close() throws FileDataSourceException {
    uri = null;
    try {
      if (file != null) {
        file.close();
      }
    } catch (IOException e) {
      throw new FileDataSourceException(e);
    } finally {
      file = null;
      if (opened) {
        opened = false;
        transferEnded();
      }
    }
  }

  private static RandomAccessFile openLocalFile(Uri uri) throws FileDataSourceException {
    try {
      return new RandomAccessFile(Assertions.checkNotNull(uri.getPath()), "r");
    } catch (FileNotFoundException e) {
      if (!TextUtils.isEmpty(uri.getQuery()) || !TextUtils.isEmpty(uri.getFragment())) {
        throw new FileDataSourceException(
            String.format(
                "uri has query and/or fragment, which are not supported. Did you call Uri.parse()"
                    + " on a string containing '?' or '#'? Use Uri.fromFile(new File(path)) to"
                    + " avoid this. path=%s,query=%s,fragment=%s",
                uri.getPath(), uri.getQuery(), uri.getFragment()),
            e);
      }
      throw new FileDataSourceException(e);
    }
  }
}
