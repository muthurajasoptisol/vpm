/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.database;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/** A {@link DatabaseProvider} that provides instances obtained from a {@link SQLiteOpenHelper}. */
public final class DefaultDatabaseProvider implements DatabaseProvider {

  private final SQLiteOpenHelper sqliteOpenHelper;

  /**
   * @param sqliteOpenHelper An {@link SQLiteOpenHelper} from which to obtain database instances.
   */
  public DefaultDatabaseProvider(SQLiteOpenHelper sqliteOpenHelper) {
    this.sqliteOpenHelper = sqliteOpenHelper;
  }

  @Override
  public SQLiteDatabase getWritableDatabase() {
    return sqliteOpenHelper.getWritableDatabase();
  }

  @Override
  public SQLiteDatabase getReadableDatabase() {
    return sqliteOpenHelper.getReadableDatabase();
  }
}
