/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.drm;

/**
 * Thrown when a non-platform component fails to decrypt data.
 */
public class DecryptionException extends Exception {

  /**
   * A component specific error code.
   */
  public final int errorCode;

  /**
   * @param errorCode A component specific error code.
   * @param message The detail message.
   */
  public DecryptionException(int errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

}
