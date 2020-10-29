/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.drm;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.common.drm.ExoMediaCrypto;
import com.adt.vpm.videoplayer.source.common.util.Assertions;

import java.util.Map;

/** A {@link DrmSession} that's in a terminal error state. */
public final class ErrorStateDrmSession implements DrmSession {

  private final DrmSessionException error;

  public ErrorStateDrmSession(DrmSessionException error) {
    this.error = Assertions.checkNotNull(error);
  }

  @Override
  public int getState() {
    return STATE_ERROR;
  }

  @Override
  public boolean playClearSamplesWithoutKeys() {
    return false;
  }

  @Override
  @Nullable
  public DrmSessionException getError() {
    return error;
  }

  @Override
  @Nullable
  public ExoMediaCrypto getMediaCrypto() {
    return null;
  }

  @Override
  @Nullable
  public Map<String, String> queryKeyStatus() {
    return null;
  }

  @Override
  @Nullable
  public byte[] getOfflineLicenseKeySetId() {
    return null;
  }

  @Override
  public void acquire(@Nullable DrmSessionEventListener.EventDispatcher eventDispatcher) {
    // Do nothing.
  }

  @Override
  public void release(@Nullable DrmSessionEventListener.EventDispatcher eventDispatcher) {
    // Do nothing.
  }
}
