/*
 * Created by ADT author on 10/1/20 11:19 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/22/20 6:23 PM
 */
package com.adt.vpm.videoplayer.source.hls.playlist;

import com.adt.vpm.videoplayer.source.core.upstream.ParsingLoadable;

/** Default implementation for {@link HlsPlaylistParserFactory}. */
public final class DefaultHlsPlaylistParserFactory implements HlsPlaylistParserFactory {

  @Override
  public ParsingLoadable.Parser<HlsPlaylist> createPlaylistParser() {
    return new HlsPlaylistParser();
  }

  @Override
  public ParsingLoadable.Parser<HlsPlaylist> createPlaylistParser(
      HlsMasterPlaylist masterPlaylist) {
    return new HlsPlaylistParser(masterPlaylist);
  }
}
