/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.drm;

import com.adt.vpm.videoplayer.source.common.util.Assertions;
import java.util.UUID;

/**
 * A {@link MediaDrmCallback} that provides a fixed response to key requests. Provisioning is not
 * supported. This implementation is primarily useful for providing locally stored keys to decrypt
 * ClearKey protected content. It is not suitable for use with Widevine or PlayReady protected
 * content.
 */
public final class LocalMediaDrmCallback implements MediaDrmCallback {

  private final byte[] keyResponse;

  /**
   * @param keyResponse The fixed response for all key requests.
   */
  public LocalMediaDrmCallback(byte[] keyResponse) {
    this.keyResponse = Assertions.checkNotNull(keyResponse);
  }

  @Override
  public byte[] executeProvisionRequest(UUID uuid, ExoMediaDrm.ProvisionRequest request) {
    throw new UnsupportedOperationException();
  }

  @Override
  public byte[] executeKeyRequest(UUID uuid, ExoMediaDrm.KeyRequest request) {
    return keyResponse;
  }

}
