/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.offline;

import com.adt.vpm.videoplayer.source.common.C;

/** Mutable {@link Download} progress. */
public class DownloadProgress {

  /** The number of bytes that have been downloaded. */
  public volatile long bytesDownloaded;

  /** The percentage that has been downloaded, or {@link C#PERCENTAGE_UNSET} if unknown. */
  public volatile float percentDownloaded;
}
