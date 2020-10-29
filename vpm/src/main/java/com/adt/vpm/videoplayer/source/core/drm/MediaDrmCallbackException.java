/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.drm;

import android.net.Uri;
import com.adt.vpm.videoplayer.source.common.upstream.DataSpec;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Thrown when an error occurs while executing a DRM {@link MediaDrmCallback#executeKeyRequest key}
 * or {@link MediaDrmCallback#executeProvisionRequest provisioning} request.
 */
public final class MediaDrmCallbackException extends IOException {

  /** The {@link DataSpec} associated with the request. */
  public final DataSpec dataSpec;
  /**
   * The {@link Uri} after redirections, or {@link #dataSpec dataSpec.uri} if no redirection
   * occurred.
   */
  public final Uri uriAfterRedirects;
  /** The HTTP request headers included in the response. */
  public final Map<String, List<String>> responseHeaders;
  /** The number of bytes obtained from the server. */
  public final long bytesLoaded;

  /**
   * Creates a new instance with the given values.
   *
   * @param dataSpec See {@link #dataSpec}.
   * @param uriAfterRedirects See {@link #uriAfterRedirects}.
   * @param responseHeaders See {@link #responseHeaders}.
   * @param bytesLoaded See {@link #bytesLoaded}.
   * @param cause The cause of the exception.
   */
  public MediaDrmCallbackException(
      DataSpec dataSpec,
      Uri uriAfterRedirects,
      Map<String, List<String>> responseHeaders,
      long bytesLoaded,
      Throwable cause) {
    super(cause);
    this.dataSpec = dataSpec;
    this.uriAfterRedirects = uriAfterRedirects;
    this.responseHeaders = responseHeaders;
    this.bytesLoaded = bytesLoaded;
  }
}
