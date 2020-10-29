/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.offline;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import java.io.IOException;

/** An index of {@link Download Downloads}. */
@WorkerThread
public interface DownloadIndex {

  /**
   * Returns the {@link Download} with the given {@code id}, or null.
   *
   * <p>This method may be slow and shouldn't normally be called on the main thread.
   *
   * @param id ID of a {@link Download}.
   * @return The {@link Download} with the given {@code id}, or null if a download state with this
   *     id doesn't exist.
   * @throws IOException If an error occurs reading the state.
   */
  @Nullable
  Download getDownload(String id) throws IOException;

  /**
   * Returns a {@link DownloadCursor} to {@link Download}s with the given {@code states}.
   *
   * <p>This method may be slow and shouldn't normally be called on the main thread.
   *
   * @param states Returns only the {@link Download}s with this states. If empty, returns all.
   * @return A cursor to {@link Download}s with the given {@code states}.
   * @throws IOException If an error occurs reading the state.
   */
  DownloadCursor getDownloads(@Download.State int... states) throws IOException;
}
