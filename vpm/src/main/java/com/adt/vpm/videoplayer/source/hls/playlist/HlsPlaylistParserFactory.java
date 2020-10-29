/*
 * Created by ADT author on 10/1/20 11:19 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/22/20 6:23 PM
 */
package com.adt.vpm.videoplayer.source.hls.playlist;

import com.adt.vpm.videoplayer.source.core.upstream.ParsingLoadable;

/** Factory for {@link HlsPlaylist} parsers. */
public interface HlsPlaylistParserFactory {

  /**
   * Returns a stand-alone playlist parser. Playlists parsed by the returned parser do not inherit
   * any attributes from other playlists.
   */
  ParsingLoadable.Parser<HlsPlaylist> createPlaylistParser();

  /**
   * Returns a playlist parser for playlists that were referenced by the given {@link
   * HlsMasterPlaylist}. Returned {@link HlsMediaPlaylist} instances may inherit attributes from
   * {@code masterPlaylist}.
   *
   * @param masterPlaylist The master playlist that referenced any parsed media playlists.
   * @return A parser for HLS playlists.
   */
  ParsingLoadable.Parser<HlsPlaylist> createPlaylistParser(HlsMasterPlaylist masterPlaylist);
}
