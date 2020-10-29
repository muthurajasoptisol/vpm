/*
 * Created by ADT author on 10/1/20 11:19 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/22/20 6:23 PM
 */
package com.adt.vpm.videoplayer.source.hls;

import com.adt.vpm.videoplayer.source.hls.playlist.HlsMasterPlaylist;
import com.adt.vpm.videoplayer.source.hls.playlist.HlsMediaPlaylist;

/**
 * Holds a master playlist along with a snapshot of one of its media playlists.
 */
public final class HlsManifest {

  /**
   * The master playlist of an HLS stream.
   */
  public final HlsMasterPlaylist masterPlaylist;
  /**
   * A snapshot of a media playlist referred to by {@link #masterPlaylist}.
   */
  public final HlsMediaPlaylist mediaPlaylist;

  /**
   * @param masterPlaylist The master playlist.
   * @param mediaPlaylist The media playlist.
   */
  HlsManifest(HlsMasterPlaylist masterPlaylist, HlsMediaPlaylist mediaPlaylist) {
    this.masterPlaylist = masterPlaylist;
    this.mediaPlaylist = mediaPlaylist;
  }

}
