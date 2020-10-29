/*
 * Created by ADT author on 10/1/20 11:19 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/22/20 6:23 PM
 */
package com.adt.vpm.videoplayer.source.hls.playlist;

import com.adt.vpm.videoplayer.source.common.offline.StreamKey;
import com.adt.vpm.videoplayer.source.core.offline.FilteringManifestParser;
import com.adt.vpm.videoplayer.source.core.upstream.ParsingLoadable;

import java.util.List;

/**
 * A {@link HlsPlaylistParserFactory} that includes only the streams identified by the given stream
 * keys.
 */
public final class FilteringHlsPlaylistParserFactory implements HlsPlaylistParserFactory {

  private final HlsPlaylistParserFactory hlsPlaylistParserFactory;
  private final List<StreamKey> streamKeys;

  /**
   * @param hlsPlaylistParserFactory A factory for the parsers of the playlists which will be
   *     filtered.
   * @param streamKeys The stream keys. If null or empty then filtering will not occur.
   */
  public FilteringHlsPlaylistParserFactory(
      HlsPlaylistParserFactory hlsPlaylistParserFactory, List<StreamKey> streamKeys) {
    this.hlsPlaylistParserFactory = hlsPlaylistParserFactory;
    this.streamKeys = streamKeys;
  }

  @Override
  public ParsingLoadable.Parser<HlsPlaylist> createPlaylistParser() {
    return new FilteringManifestParser<>(
        hlsPlaylistParserFactory.createPlaylistParser(), streamKeys);
  }

  @Override
  public ParsingLoadable.Parser<HlsPlaylist> createPlaylistParser(
      HlsMasterPlaylist masterPlaylist) {
    return new FilteringManifestParser<>(
        hlsPlaylistParserFactory.createPlaylistParser(masterPlaylist), streamKeys);
  }
}
