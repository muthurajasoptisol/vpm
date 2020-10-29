/*
 * Created by ADT author on 10/1/20 11:19 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/22/20 6:23 PM
 */
package com.adt.vpm.videoplayer.source.hls.playlist;

import com.adt.vpm.videoplayer.source.core.offline.FilterableManifest;

import java.util.Collections;
import java.util.List;

/** Represents an HLS playlist. */
public abstract class HlsPlaylist implements FilterableManifest<HlsPlaylist> {

  /**
   * The base uri. Used to resolve relative paths.
   */
  public final String baseUri;
  /**
   * The list of tags in the playlist.
   */
  public final List<String> tags;
  /**
   * Whether the media is formed of independent segments, as defined by the
   * #EXT-X-INDEPENDENT-SEGMENTS tag.
   */
  public final boolean hasIndependentSegments;

  /**
   * @param baseUri See {@link #baseUri}.
   * @param tags See {@link #tags}.
   * @param hasIndependentSegments See {@link #hasIndependentSegments}.
   */
  protected HlsPlaylist(String baseUri, List<String> tags, boolean hasIndependentSegments) {
    this.baseUri = baseUri;
    this.tags = Collections.unmodifiableList(tags);
    this.hasIndependentSegments = hasIndependentSegments;
  }

}
