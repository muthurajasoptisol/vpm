/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.drm;

import android.media.MediaDrmException;
import android.os.PersistableBundle;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.adt.vpm.videoplayer.source.common.drm.DrmInitData;
import com.adt.vpm.videoplayer.source.common.drm.ExoMediaCrypto;
import com.adt.vpm.videoplayer.source.common.drm.UnsupportedMediaCrypto;
import com.adt.vpm.videoplayer.source.common.util.Util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** An {@link ExoMediaDrm} that does not support any protection schemes. */
@RequiresApi(18)
public final class DummyExoMediaDrm implements ExoMediaDrm {

  /** Returns a new instance. */
  public static DummyExoMediaDrm getInstance() {
    return new DummyExoMediaDrm();
  }

  @Override
  public void setOnEventListener(@Nullable OnEventListener listener) {
    // Do nothing.
  }

  @Override
  public void setOnKeyStatusChangeListener(@Nullable OnKeyStatusChangeListener listener) {
    // Do nothing.
  }

  @Override
  public void setOnExpirationUpdateListener(@Nullable OnExpirationUpdateListener listener) {
    // Do nothing.
  }

  @Override
  public byte[] openSession() throws MediaDrmException {
    throw new MediaDrmException("Attempting to open a session using a dummy ExoMediaDrm.");
  }

  @Override
  public void closeSession(byte[] sessionId) {
    // Do nothing.
  }

  @Override
  public KeyRequest getKeyRequest(
      byte[] scope,
      @Nullable List<DrmInitData.SchemeData> schemeDatas,
      int keyType,
      @Nullable HashMap<String, String> optionalParameters) {
    // Should not be invoked. No session should exist.
    throw new IllegalStateException();
  }

  @Override
  @Nullable
  public byte[] provideKeyResponse(byte[] scope, byte[] response) {
    // Should not be invoked. No session should exist.
    throw new IllegalStateException();
  }

  @Override
  public ProvisionRequest getProvisionRequest() {
    // Should not be invoked. No provision should be required.
    throw new IllegalStateException();
  }

  @Override
  public void provideProvisionResponse(byte[] response) {
    // Should not be invoked. No provision should be required.
    throw new IllegalStateException();
  }

  @Override
  public Map<String, String> queryKeyStatus(byte[] sessionId) {
    // Should not be invoked. No session should exist.
    throw new IllegalStateException();
  }

  @Override
  public void acquire() {
    // Do nothing.
  }

  @Override
  public void release() {
    // Do nothing.
  }

  @Override
  public void restoreKeys(byte[] sessionId, byte[] keySetId) {
    // Should not be invoked. No session should exist.
    throw new IllegalStateException();
  }

  @Override
  @Nullable
  public PersistableBundle getMetrics() {
    return null;
  }

  @Override
  public String getPropertyString(String propertyName) {
    return "";
  }

  @Override
  public byte[] getPropertyByteArray(String propertyName) {
    return Util.EMPTY_BYTE_ARRAY;
  }

  @Override
  public void setPropertyString(String propertyName, String value) {
    // Do nothing.
  }

  @Override
  public void setPropertyByteArray(String propertyName, byte[] value) {
    // Do nothing.
  }

  @Override
  public ExoMediaCrypto createMediaCrypto(byte[] sessionId) {
    // Should not be invoked. No session should exist.
    throw new IllegalStateException();
  }

  @Override
  public Class<UnsupportedMediaCrypto> getExoMediaCryptoType() {
    return UnsupportedMediaCrypto.class;
  }
}
