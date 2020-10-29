/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.offline;

/** Creates {@link Downloader Downloaders} for given {@link DownloadRequest DownloadRequests}. */
public interface DownloaderFactory {

  /**
   * Creates a {@link Downloader} to perform the given {@link DownloadRequest}.
   *
   * @param action The action.
   * @return The downloader.
   */
  Downloader createDownloader(DownloadRequest action);
}
