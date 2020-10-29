/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.drm;

import androidx.annotation.IntDef;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Thrown when the requested DRM scheme is not supported.
 */
public final class UnsupportedDrmException extends Exception {

  /**
   * The reason for the exception. One of {@link #REASON_UNSUPPORTED_SCHEME} or {@link
   * #REASON_INSTANTIATION_ERROR}.
   */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @IntDef({REASON_UNSUPPORTED_SCHEME, REASON_INSTANTIATION_ERROR})
  public @interface Reason {}
  /**
   * The requested DRM scheme is unsupported by the device.
   */
  public static final int REASON_UNSUPPORTED_SCHEME = 1;
  /**
   * There device advertises support for the requested DRM scheme, but there was an error
   * instantiating it. The cause can be retrieved using {@link #getCause()}.
   */
  public static final int REASON_INSTANTIATION_ERROR = 2;

  /**
   * Either {@link #REASON_UNSUPPORTED_SCHEME} or {@link #REASON_INSTANTIATION_ERROR}.
   */
  @Reason public final int reason;

  /**
   * @param reason {@link #REASON_UNSUPPORTED_SCHEME} or {@link #REASON_INSTANTIATION_ERROR}.
   */
  public UnsupportedDrmException(@Reason int reason) {
    this.reason = reason;
  }

  /**
   * @param reason {@link #REASON_UNSUPPORTED_SCHEME} or {@link #REASON_INSTANTIATION_ERROR}.
   * @param cause The cause of this exception.
   */
  public UnsupportedDrmException(@Reason int reason, Exception cause) {
    super(cause);
    this.reason = reason;
  }

}
