/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.upstream;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.common.upstream.DataSource;
import com.adt.vpm.videoplayer.source.common.upstream.DataSpec;
import com.adt.vpm.videoplayer.source.common.upstream.TransferListener;
import com.adt.vpm.videoplayer.source.common.util.Log;

import java.util.ArrayList;

import static com.adt.vpm.videoplayer.source.common.util.Assertions.checkNotNull;
import static com.adt.vpm.videoplayer.source.common.util.Util.castNonNull;

/**
 * Base {@link DataSource} implementation to keep a list of {@link TransferListener}s.
 *
 * <p>Subclasses must call {@link #transferInitializing(DataSpec)}, {@link
 * #transferStarted(DataSpec)}, {@link #bytesTransferred(int)}, and {@link #transferEnded()} to
 * inform listeners of data transfers.
 */
public abstract class BaseDataSource implements DataSource {

  private final boolean isNetwork;
  private final ArrayList<TransferListener> listeners;

  private int listenerCount;
  @Nullable private DataSpec dataSpec;

  /**
   * Creates base data source.
   *
   * @param isNetwork Whether the data source loads data through a network.
   */
  protected BaseDataSource(boolean isNetwork) {
    this.isNetwork = isNetwork;
    this.listeners = new ArrayList<>(/* initialCapacity= */ 1);
  }

  @Override
  public final void addTransferListener(TransferListener transferListener) {
    checkNotNull(transferListener);
    if (!listeners.contains(transferListener)) {
      listeners.add(transferListener);
      listenerCount++;
    }
  }

  /**
   * Notifies listeners that data transfer for the specified {@link DataSpec} is being initialized.
   *
   * @param dataSpec {@link DataSpec} describing the data for initializing transfer.
   */
  protected final void transferInitializing(DataSpec dataSpec) {
    for (int i = 0; i < listenerCount; i++) {
      listeners.get(i).onTransferInitializing(/* source= */ this, dataSpec, isNetwork);
    }
  }

  /**
   * Notifies listeners that data transfer for the specified {@link DataSpec} started.
   *
   * @param dataSpec {@link DataSpec} describing the data being transferred.
   */
  protected final void transferStarted(DataSpec dataSpec) {
    this.dataSpec = dataSpec;
    for (int i = 0; i < listenerCount; i++) {
      listeners.get(i).onTransferStart(/* source= */ this, dataSpec, isNetwork);
    }
  }

  /**
   * Notifies listeners that bytes were transferred.
   *
   * @param bytesTransferred The number of bytes transferred since the previous call to this method
   *     (or if the first call, since the transfer was started).
   */
  protected final void bytesTransferred(int bytesTransferred) {
    DataSpec dataSpec = castNonNull(this.dataSpec);
    if(dataSpec == null) {
      Log.d("xxx byteTransferred", "dataspec null");
    }
    for (int i = 0; i < listenerCount; i++) {
      listeners
          .get(i)
          .onBytesTransferred(/* source= */ this, dataSpec, isNetwork, bytesTransferred);
    }
  }

  /** Notifies listeners that a transfer ended. */
  protected final void transferEnded() {
    DataSpec dataSpec = castNonNull(this.dataSpec);
    for (int i = 0; i < listenerCount; i++) {
      listeners.get(i).onTransferEnd(/* source= */ this, dataSpec, isNetwork);
    }
    this.dataSpec = null;
  }
}
