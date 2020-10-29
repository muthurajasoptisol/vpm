/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.drm;

import android.media.MediaDrm;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.adt.vpm.videoplayer.source.common.Format;
import com.adt.vpm.videoplayer.source.common.drm.DrmInitData;
import com.adt.vpm.videoplayer.source.common.upstream.HttpDataSource;
import com.adt.vpm.videoplayer.source.common.util.Assertions;
import com.adt.vpm.videoplayer.source.core.drm.DefaultDrmSessionManager.Mode;
import com.adt.vpm.videoplayer.source.core.drm.DrmSession.DrmSessionException;
import com.adt.vpm.videoplayer.source.core.source.MediaSource.MediaPeriodId;

import java.util.Map;
import java.util.UUID;

/** Helper class to download, renew and release offline licenses. */
@RequiresApi(18)
public final class OfflineLicenseHelper {

  private static final Format FORMAT_WITH_EMPTY_DRM_INIT_DATA =
      new Format.Builder().setDrmInitData(new DrmInitData()).build();

  private final ConditionVariable conditionVariable;
  private final DefaultDrmSessionManager drmSessionManager;
  private final HandlerThread handlerThread;
  private final DrmSessionEventListener.EventDispatcher eventDispatcher;

  /**
   * Instantiates a new instance which uses Widevine CDM. Call {@link #release()} when the instance
   * is no longer required.
   *
   * @param defaultLicenseUrl The default license URL. Used for key requests that do not specify
   *     their own license URL.
   * @param httpDataSourceFactory A factory from which to obtain {@link HttpDataSource} instances.
   * @param eventDispatcher A {@link DrmSessionEventListener.EventDispatcher} used to distribute
   *     DRM-related events.
   * @return A new instance which uses Widevine CDM.
   */
  public static OfflineLicenseHelper newWidevineInstance(
      String defaultLicenseUrl,
      HttpDataSource.Factory httpDataSourceFactory,
      DrmSessionEventListener.EventDispatcher eventDispatcher) {
    return newWidevineInstance(
        defaultLicenseUrl,
        /* forceDefaultLicenseUrl= */ false,
        httpDataSourceFactory,
        eventDispatcher);
  }

  /**
   * Instantiates a new instance which uses Widevine CDM. Call {@link #release()} when the instance
   * is no longer required.
   *
   * @param defaultLicenseUrl The default license URL. Used for key requests that do not specify
   *     their own license URL.
   * @param forceDefaultLicenseUrl Whether to use {@code defaultLicenseUrl} for key requests that
   *     include their own license URL.
   * @param httpDataSourceFactory A factory from which to obtain {@link HttpDataSource} instances.
   * @param eventDispatcher A {@link DrmSessionEventListener.EventDispatcher} used to distribute
   *     DRM-related events.
   * @return A new instance which uses Widevine CDM.
   */
  public static OfflineLicenseHelper newWidevineInstance(
      String defaultLicenseUrl,
      boolean forceDefaultLicenseUrl,
      HttpDataSource.Factory httpDataSourceFactory,
      DrmSessionEventListener.EventDispatcher eventDispatcher) {
    return newWidevineInstance(
        defaultLicenseUrl,
        forceDefaultLicenseUrl,
        httpDataSourceFactory,
        /* optionalKeyRequestParameters= */ null,
        eventDispatcher);
  }

  /**
   * Instantiates a new instance which uses Widevine CDM. Call {@link #release()} when the instance
   * is no longer required.
   *
   * @param defaultLicenseUrl The default license URL. Used for key requests that do not specify
   *     their own license URL.
   * @param forceDefaultLicenseUrl Whether to use {@code defaultLicenseUrl} for key requests that
   *     include their own license URL.
   * @param optionalKeyRequestParameters An optional map of parameters to pass as the last argument
   *     to {@link MediaDrm#getKeyRequest}. May be null.
   * @param eventDispatcher A {@link DrmSessionEventListener.EventDispatcher} used to distribute
   *     DRM-related events.
   * @return A new instance which uses Widevine CDM.
   * @see DefaultDrmSessionManager.Builder
   */
  public static OfflineLicenseHelper newWidevineInstance(
      String defaultLicenseUrl,
      boolean forceDefaultLicenseUrl,
      HttpDataSource.Factory httpDataSourceFactory,
      @Nullable Map<String, String> optionalKeyRequestParameters,
      DrmSessionEventListener.EventDispatcher eventDispatcher) {
    return new OfflineLicenseHelper(
        new DefaultDrmSessionManager.Builder()
            .setKeyRequestParameters(optionalKeyRequestParameters)
            .build(
                new HttpMediaDrmCallback(
                    defaultLicenseUrl, forceDefaultLicenseUrl, httpDataSourceFactory)),
        eventDispatcher);
  }

  /**
   * @deprecated Use {@link #OfflineLicenseHelper(DefaultDrmSessionManager,
   *     DrmSessionEventListener.EventDispatcher)} instead.
   */
  @Deprecated
  public OfflineLicenseHelper(
      UUID uuid,
      ExoMediaDrm.Provider mediaDrmProvider,
      MediaDrmCallback callback,
      @Nullable Map<String, String> optionalKeyRequestParameters,
      DrmSessionEventListener.EventDispatcher eventDispatcher) {
    this(
            new DefaultDrmSessionManager.Builder()
                .setUuidAndExoMediaDrmProvider(uuid, mediaDrmProvider)
                .setKeyRequestParameters(optionalKeyRequestParameters)
                .build(callback),
        eventDispatcher);
  }

  /**
   * Constructs an instance. Call {@link #release()} when the instance is no longer required.
   *
   * @param defaultDrmSessionManager The {@link DefaultDrmSessionManager} used to download licenses.
   * @param eventDispatcher A {@link DrmSessionEventListener.EventDispatcher} used to distribute
   *     DRM-related events.
   */
  public OfflineLicenseHelper(
      DefaultDrmSessionManager defaultDrmSessionManager,
      DrmSessionEventListener.EventDispatcher eventDispatcher) {
    this.drmSessionManager = defaultDrmSessionManager;
    this.eventDispatcher = eventDispatcher;
    handlerThread = new HandlerThread("ExoPlayer:OfflineLicenseHelper");
    handlerThread.start();
    conditionVariable = new ConditionVariable();
    DrmSessionEventListener eventListener =
        new DrmSessionEventListener() {
          @Override
          public void onDrmKeysLoaded(int windowIndex, @Nullable MediaPeriodId mediaPeriodId) {
            conditionVariable.open();
          }

          @Override
          public void onDrmSessionManagerError(
              int windowIndex, @Nullable MediaPeriodId mediaPeriodId, Exception e) {
            conditionVariable.open();
          }

          @Override
          public void onDrmKeysRestored(int windowIndex, @Nullable MediaPeriodId mediaPeriodId) {
            conditionVariable.open();
          }

          @Override
          public void onDrmKeysRemoved(int windowIndex, @Nullable MediaPeriodId mediaPeriodId) {
            conditionVariable.open();
          }
        };
    eventDispatcher.addEventListener(new Handler(handlerThread.getLooper()), eventListener);
  }

  /**
   * Downloads an offline license.
   *
   * @param format The {@link Format} of the content whose license is to be downloaded. Must contain
   *     a non-null {@link Format#drmInitData}.
   * @return The key set id for the downloaded license.
   * @throws DrmSessionException Thrown when a DRM session error occurs.
   */
  public synchronized byte[] downloadLicense(Format format) throws DrmSessionException {
    Assertions.checkArgument(format.drmInitData != null);
    return blockingKeyRequest(DefaultDrmSessionManager.MODE_DOWNLOAD, null, format);
  }

  /**
   * Renews an offline license.
   *
   * @param offlineLicenseKeySetId The key set id of the license to be renewed.
   * @return The renewed offline license key set id.
   * @throws DrmSessionException Thrown when a DRM session error occurs.
   */
  public synchronized byte[] renewLicense(byte[] offlineLicenseKeySetId)
      throws DrmSessionException {
    Assertions.checkNotNull(offlineLicenseKeySetId);
    return blockingKeyRequest(
        DefaultDrmSessionManager.MODE_DOWNLOAD,
        offlineLicenseKeySetId,
        FORMAT_WITH_EMPTY_DRM_INIT_DATA);
  }

  /**
   * Releases an offline license.
   *
   * @param offlineLicenseKeySetId The key set id of the license to be released.
   * @throws DrmSessionException Thrown when a DRM session error occurs.
   */
  public synchronized void releaseLicense(byte[] offlineLicenseKeySetId)
      throws DrmSessionException {
    Assertions.checkNotNull(offlineLicenseKeySetId);
    blockingKeyRequest(
        DefaultDrmSessionManager.MODE_RELEASE,
        offlineLicenseKeySetId,
        FORMAT_WITH_EMPTY_DRM_INIT_DATA);
  }

  /**
   * Returns the remaining license and playback durations in seconds, for an offline license.
   *
   * @param offlineLicenseKeySetId The key set id of the license.
   * @return The remaining license and playback durations, in seconds.
   * @throws DrmSessionException Thrown when a DRM session error occurs.
   */
  public synchronized Pair<Long, Long> getLicenseDurationRemainingSec(byte[] offlineLicenseKeySetId)
      throws DrmSessionException {
    Assertions.checkNotNull(offlineLicenseKeySetId);
    drmSessionManager.prepare();
    DrmSession drmSession =
        openBlockingKeyRequest(
            DefaultDrmSessionManager.MODE_QUERY,
            offlineLicenseKeySetId,
            FORMAT_WITH_EMPTY_DRM_INIT_DATA);
    DrmSessionException error = drmSession.getError();
    Pair<Long, Long> licenseDurationRemainingSec =
        WidevineUtil.getLicenseDurationRemainingSec(drmSession);
    drmSession.release(eventDispatcher);
    drmSessionManager.release();
    if (error != null) {
      if (error.getCause() instanceof KeysExpiredException) {
        return Pair.create(0L, 0L);
      }
      throw error;
    }
    return Assertions.checkNotNull(licenseDurationRemainingSec);
  }

  /**
   * Releases the helper. Should be called when the helper is no longer required.
   */
  public void release() {
    handlerThread.quit();
  }

  private byte[] blockingKeyRequest(
      @Mode int licenseMode, @Nullable byte[] offlineLicenseKeySetId, Format format)
      throws DrmSessionException {
    drmSessionManager.prepare();
    DrmSession drmSession = openBlockingKeyRequest(licenseMode, offlineLicenseKeySetId, format);
    DrmSessionException error = drmSession.getError();
    byte[] keySetId = drmSession.getOfflineLicenseKeySetId();
    drmSession.release(eventDispatcher);
    drmSessionManager.release();
    if (error != null) {
      throw error;
    }
    return Assertions.checkNotNull(keySetId);
  }

  private DrmSession openBlockingKeyRequest(
      @Mode int licenseMode, @Nullable byte[] offlineLicenseKeySetId, Format format) {
    Assertions.checkNotNull(format.drmInitData);
    drmSessionManager.setMode(licenseMode, offlineLicenseKeySetId);
    conditionVariable.close();
    DrmSession drmSession =
        drmSessionManager.acquireSession(handlerThread.getLooper(), eventDispatcher, format);
    // Block current thread until key loading is finished
    conditionVariable.block();
    return Assertions.checkNotNull(drmSession);
  }

}
