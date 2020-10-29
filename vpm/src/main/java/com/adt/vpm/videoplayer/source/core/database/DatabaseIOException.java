/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.database;

import android.database.SQLException;
import java.io.IOException;

/** An {@link IOException} whose cause is an {@link SQLException}. */
public final class DatabaseIOException extends IOException {

  public DatabaseIOException(SQLException cause) {
    super(cause);
  }

  public DatabaseIOException(SQLException cause, String message) {
    super(message, cause);
  }
}
