/*
 * Created by ADT author on 10/1/20 11:19 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/22/20 6:23 PM
 */
package com.adt.vpm.videoplayer.source.hls.offline;

import android.net.Uri;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.common.MediaItem;
import com.adt.vpm.videoplayer.source.common.offline.StreamKey;
import com.adt.vpm.videoplayer.source.common.upstream.DataSource;
import com.adt.vpm.videoplayer.source.common.upstream.DataSpec;
import com.adt.vpm.videoplayer.source.core.offline.SegmentDownloader;
import com.adt.vpm.videoplayer.source.core.upstream.ParsingLoadable;
import com.adt.vpm.videoplayer.source.core.upstream.cache.CacheDataSource;
import com.adt.vpm.videoplayer.source.core.util.UriUtil;
import com.adt.vpm.videoplayer.source.hls.playlist.HlsMasterPlaylist;
import com.adt.vpm.videoplayer.source.hls.playlist.HlsMediaPlaylist;
import com.adt.vpm.videoplayer.source.hls.playlist.HlsPlaylist;
import com.adt.vpm.videoplayer.source.hls.playlist.HlsPlaylistParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * A downloader for HLS streams.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * SimpleCache cache = new SimpleCache(downloadFolder, new NoOpCacheEvictor(), databaseProvider);
 * CacheDataSource.Factory cacheDataSourceFactory =
 *     new CacheDataSource.Factory()
 *         .setCache(cache)
 *         .setUpstreamDataSourceFactory(new DefaultHttpDataSourceFactory(userAgent));
 * // Create a downloader for the first variant in a master playlist.
 * HlsDownloader hlsDownloader =
 *     new HlsDownloader(
 *         new MediaItem.Builder()
 *             .setUri(playlistUri)
 *             .setStreamKeys(
 *                 Collections.singletonList(
 *                     new StreamKey(HlsMasterPlaylist.GROUP_INDEX_VARIANT, 0)))
 *             .build(),
 *         Collections.singletonList();
 * // Perform the download.
 * hlsDownloader.download(progressListener);
 * // Use the downloaded data for playback.
 * HlsMediaSource mediaSource =
 *     new HlsMediaSource.Factory(cacheDataSourceFactory).createMediaSource(mediaItem);
 * }</pre>
 */
public final class HlsDownloader extends SegmentDownloader<HlsPlaylist> {

  /** @deprecated Use {@link #HlsDownloader(MediaItem, CacheDataSource.Factory)} instead. */
  @SuppressWarnings("deprecation")
  @Deprecated
  public HlsDownloader(
          Uri playlistUri, List<StreamKey> streamKeys, CacheDataSource.Factory cacheDataSourceFactory) {
    this(playlistUri, streamKeys, cacheDataSourceFactory, Runnable::run);
  }

  /**
   * Creates a new instance.
   *
   * @param mediaItem The {@link MediaItem} to be downloaded.
   * @param cacheDataSourceFactory A {@link CacheDataSource.Factory} for the cache into which the
   *     download will be written.
   */
  public HlsDownloader(MediaItem mediaItem, CacheDataSource.Factory cacheDataSourceFactory) {
    this(mediaItem, cacheDataSourceFactory, Runnable::run);
  }

  /**
   * @deprecated Use {@link #HlsDownloader(MediaItem, CacheDataSource.Factory, Executor)} instead.
   */
  @Deprecated
  public HlsDownloader(
      Uri playlistUri,
      List<StreamKey> streamKeys,
      CacheDataSource.Factory cacheDataSourceFactory,
      Executor executor) {
    this(
        new MediaItem.Builder().setUri(playlistUri).setStreamKeys(streamKeys).build(),
        cacheDataSourceFactory,
        executor);
  }

  /**
   * Creates a new instance.
   *
   * @param mediaItem The {@link MediaItem} to be downloaded.
   * @param cacheDataSourceFactory A {@link CacheDataSource.Factory} for the cache into which the
   *     download will be written.
   * @param executor An {@link Executor} used to make requests for the media being downloaded.
   *     Providing an {@link Executor} that uses multiple threads will speed up the download by
   *     allowing parts of it to be executed in parallel.
   */
  public HlsDownloader(
      MediaItem mediaItem, CacheDataSource.Factory cacheDataSourceFactory, Executor executor) {
    this(mediaItem, new HlsPlaylistParser(), cacheDataSourceFactory, executor);
  }

  /**
   * Creates a new instance.
   *
   * @param mediaItem The {@link MediaItem} to be downloaded.
   * @param manifestParser A parser for HLS playlists.
   * @param cacheDataSourceFactory A {@link CacheDataSource.Factory} for the cache into which the
   *     download will be written.
   * @param executor An {@link Executor} used to make requests for the media being downloaded.
   *     Providing an {@link Executor} that uses multiple threads will speed up the download by
   *     allowing parts of it to be executed in parallel.
   */
  public HlsDownloader(
      MediaItem mediaItem,
      ParsingLoadable.Parser<HlsPlaylist> manifestParser,
      CacheDataSource.Factory cacheDataSourceFactory,
      Executor executor) {
    super(mediaItem, manifestParser, cacheDataSourceFactory, executor);
  }

  @Override
  protected List<Segment> getSegments(DataSource dataSource, HlsPlaylist playlist, boolean removing)
      throws IOException, InterruptedException {
    ArrayList<DataSpec> mediaPlaylistDataSpecs = new ArrayList<>();
    if (playlist instanceof HlsMasterPlaylist) {
      HlsMasterPlaylist masterPlaylist = (HlsMasterPlaylist) playlist;
      addMediaPlaylistDataSpecs(masterPlaylist.mediaPlaylistUrls, mediaPlaylistDataSpecs);
    } else {
      mediaPlaylistDataSpecs.add(
          SegmentDownloader.getCompressibleDataSpec(Uri.parse(playlist.baseUri)));
    }

    ArrayList<Segment> segments = new ArrayList<>();
    HashSet<Uri> seenEncryptionKeyUris = new HashSet<>();
    for (DataSpec mediaPlaylistDataSpec : mediaPlaylistDataSpecs) {
      segments.add(new Segment(/* startTimeUs= */ 0, mediaPlaylistDataSpec));
      HlsMediaPlaylist mediaPlaylist;
      try {
        mediaPlaylist = (HlsMediaPlaylist) getManifest(dataSource, mediaPlaylistDataSpec, removing);
      } catch (IOException e) {
        if (!removing) {
          throw e;
        }
        // Generating an incomplete segment list is allowed. Advance to the next media playlist.
        continue;
      }
      @Nullable HlsMediaPlaylist.Segment lastInitSegment = null;
      List<HlsMediaPlaylist.Segment> hlsSegments = mediaPlaylist.segments;
      for (int i = 0; i < hlsSegments.size(); i++) {
        HlsMediaPlaylist.Segment segment = hlsSegments.get(i);
        HlsMediaPlaylist.Segment initSegment = segment.initializationSegment;
        if (initSegment != null && initSegment != lastInitSegment) {
          lastInitSegment = initSegment;
          addSegment(mediaPlaylist, initSegment, seenEncryptionKeyUris, segments);
        }
        addSegment(mediaPlaylist, segment, seenEncryptionKeyUris, segments);
      }
    }
    return segments;
  }

  private void addMediaPlaylistDataSpecs(List<Uri> mediaPlaylistUrls, List<DataSpec> out) {
    for (int i = 0; i < mediaPlaylistUrls.size(); i++) {
      out.add(SegmentDownloader.getCompressibleDataSpec(mediaPlaylistUrls.get(i)));
    }
  }

  private void addSegment(
      HlsMediaPlaylist mediaPlaylist,
      HlsMediaPlaylist.Segment segment,
      HashSet<Uri> seenEncryptionKeyUris,
      ArrayList<Segment> out) {
    String baseUri = mediaPlaylist.baseUri;
    long startTimeUs = mediaPlaylist.startTimeUs + segment.relativeStartTimeUs;
    if (segment.fullSegmentEncryptionKeyUri != null) {
      Uri keyUri = UriUtil.resolveToUri(baseUri, segment.fullSegmentEncryptionKeyUri);
      if (seenEncryptionKeyUris.add(keyUri)) {
        out.add(new Segment(startTimeUs, SegmentDownloader.getCompressibleDataSpec(keyUri)));
      }
    }
    Uri segmentUri = UriUtil.resolveToUri(baseUri, segment.url);
    DataSpec dataSpec = new DataSpec(segmentUri, segment.byteRangeOffset, segment.byteRangeLength);
    out.add(new Segment(startTimeUs, dataSpec));
  }
}
