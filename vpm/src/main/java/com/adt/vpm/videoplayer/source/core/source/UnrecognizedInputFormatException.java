/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.source;

import android.net.Uri;
import com.adt.vpm.videoplayer.source.common.ParserException;

/**
 * Thrown if the input format was not recognized.
 */
public class UnrecognizedInputFormatException extends ParserException {

  /**
   * The {@link Uri} from which the unrecognized data was read.
   */
  public final Uri uri;

  /**
   * @param message The detail message for the exception.
   * @param uri The {@link Uri} from which the unrecognized data was read.
   */
  public UnrecognizedInputFormatException(String message, Uri uri) {
    super(message);
    this.uri = uri;
  }

}
