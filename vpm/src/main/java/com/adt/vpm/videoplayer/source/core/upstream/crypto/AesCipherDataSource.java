/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.upstream.crypto;

import android.net.Uri;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.upstream.DataSource;
import com.adt.vpm.videoplayer.source.common.upstream.DataSpec;
import com.adt.vpm.videoplayer.source.common.upstream.TransferListener;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;

import static com.adt.vpm.videoplayer.source.common.util.Assertions.checkNotNull;
import static com.adt.vpm.videoplayer.source.common.util.Util.castNonNull;

/**
 * A {@link DataSource} that decrypts the data read from an upstream source.
 */
public final class AesCipherDataSource implements DataSource {

  private final DataSource upstream;
  private final byte[] secretKey;

  @Nullable private AesFlushingCipher cipher;

  public AesCipherDataSource(byte[] secretKey, DataSource upstream) {
    this.upstream = upstream;
    this.secretKey = secretKey;
  }

  @Override
  public void addTransferListener(TransferListener transferListener) {
    checkNotNull(transferListener);
    upstream.addTransferListener(transferListener);
  }

  @Override
  public long open(DataSpec dataSpec) throws IOException {
    long dataLength = upstream.open(dataSpec);
    long nonce = CryptoUtil.getFNV64Hash(dataSpec.key);
    cipher =
        new AesFlushingCipher(
            Cipher.DECRYPT_MODE, secretKey, nonce, dataSpec.uriPositionOffset + dataSpec.position);
    return dataLength;
  }

  @Override
  public int read(byte[] data, int offset, int readLength) throws IOException {
    if (readLength == 0) {
      return 0;
    }
    int read = upstream.read(data, offset, readLength);
    if (read == C.RESULT_END_OF_INPUT) {
      return C.RESULT_END_OF_INPUT;
    }
    castNonNull(cipher).updateInPlace(data, offset, read);
    return read;
  }

  @Override
  @Nullable
  public Uri getUri() {
    return upstream.getUri();
  }

  @Override
  public Map<String, List<String>> getResponseHeaders() {
    return upstream.getResponseHeaders();
  }

  @Override
  public void close() throws IOException {
    cipher = null;
    upstream.close();
  }
}
