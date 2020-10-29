/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.offline;

import java.io.IOException;

/** Thrown on an error during downloading. */
public final class DownloadException extends IOException {

  /** @param message The message for the exception. */
  public DownloadException(String message) {
    super(message);
  }

  /** @param cause The cause for the exception. */
  public DownloadException(Throwable cause) {
    super(cause);
  }

}
