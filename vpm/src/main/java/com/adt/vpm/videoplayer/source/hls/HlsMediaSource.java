/*
 * Created by ADT author on 10/1/20 11:19 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/22/20 6:23 PM
 */
package com.adt.vpm.videoplayer.source.hls;

import android.net.Uri;
import android.os.Handler;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.ExoPlayerLibraryInfo;
import com.adt.vpm.videoplayer.source.common.MediaItem;
import com.adt.vpm.videoplayer.source.common.offline.StreamKey;
import com.adt.vpm.videoplayer.source.common.upstream.DataSource;
import com.adt.vpm.videoplayer.source.common.upstream.HttpDataSource;
import com.adt.vpm.videoplayer.source.common.upstream.TransferListener;
import com.adt.vpm.videoplayer.source.common.util.MimeTypes;
import com.adt.vpm.videoplayer.source.core.drm.DrmSessionEventListener;
import com.adt.vpm.videoplayer.source.core.drm.DrmSessionManager;
import com.adt.vpm.videoplayer.source.core.source.BaseMediaSource;
import com.adt.vpm.videoplayer.source.core.source.CompositeSequenceableLoaderFactory;
import com.adt.vpm.videoplayer.source.core.source.DefaultCompositeSequenceableLoaderFactory;
import com.adt.vpm.videoplayer.source.core.source.MediaPeriod;
import com.adt.vpm.videoplayer.source.core.source.MediaSourceDrmHelper;
import com.adt.vpm.videoplayer.source.core.source.MediaSourceEventListener;
import com.adt.vpm.videoplayer.source.core.source.MediaSourceFactory;
import com.adt.vpm.videoplayer.source.core.source.SinglePeriodTimeline;
import com.adt.vpm.videoplayer.source.core.upstream.Allocator;
import com.adt.vpm.videoplayer.source.core.upstream.DefaultLoadErrorHandlingPolicy;
import com.adt.vpm.videoplayer.source.core.upstream.LoadErrorHandlingPolicy;
import com.adt.vpm.videoplayer.source.hls.playlist.DefaultHlsPlaylistParserFactory;
import com.adt.vpm.videoplayer.source.hls.playlist.DefaultHlsPlaylistTracker;
import com.adt.vpm.videoplayer.source.hls.playlist.FilteringHlsPlaylistParserFactory;
import com.adt.vpm.videoplayer.source.hls.playlist.HlsMediaPlaylist;
import com.adt.vpm.videoplayer.source.hls.playlist.HlsPlaylistParserFactory;
import com.adt.vpm.videoplayer.source.hls.playlist.HlsPlaylistTracker;

import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.util.Collections;
import java.util.List;

import static com.adt.vpm.videoplayer.source.common.util.Assertions.checkNotNull;
import static java.lang.Math.max;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/** An HLS {@link MediaSource}. */
public final class HlsMediaSource extends BaseMediaSource
    implements HlsPlaylistTracker.PrimaryPlaylistListener {

  static {
    ExoPlayerLibraryInfo.registerModule("goog.exo.hls");
  }

  /**
   * The types of metadata that can be extracted from HLS streams.
   *
   * <p>Allowed values:
   *
   * <ul>
   *   <li>{@link #METADATA_TYPE_ID3}
   *   <li>{@link #METADATA_TYPE_EMSG}
   * </ul>
   *
   * <p>See {@link Factory#setMetadataType(int)}.
   */
  @Documented
  @Retention(SOURCE)
  @IntDef({METADATA_TYPE_ID3, METADATA_TYPE_EMSG})
  public @interface MetadataType {}

  /** Type for ID3 metadata in HLS streams. */
  public static final int METADATA_TYPE_ID3 = 1;
  /** Type for ESMG metadata in HLS streams. */
  public static final int METADATA_TYPE_EMSG = 3;

  /** Factory for {@link HlsMediaSource}s. */
  public static final class Factory implements MediaSourceFactory {

    private final HlsDataSourceFactory hlsDataSourceFactory;
    private final MediaSourceDrmHelper mediaSourceDrmHelper;

    private HlsExtractorFactory extractorFactory;
    private HlsPlaylistParserFactory playlistParserFactory;
    private HlsPlaylistTracker.Factory playlistTrackerFactory;
    private CompositeSequenceableLoaderFactory compositeSequenceableLoaderFactory;
    @Nullable private DrmSessionManager drmSessionManager;
    private LoadErrorHandlingPolicy loadErrorHandlingPolicy;
    private boolean allowChunklessPreparation;
    @MetadataType private int metadataType;
    private boolean useSessionKeys;
    private List<StreamKey> streamKeys;
    @Nullable private Object tag;

    /**
     * Creates a new factory for {@link HlsMediaSource}s.
     *
     * @param dataSourceFactory A data source factory that will be wrapped by a {@link
     *     DefaultHlsDataSourceFactory} to create {@link DataSource}s for manifests, segments and
     *     keys.
     */
    public Factory(DataSource.Factory dataSourceFactory) {
      this(new DefaultHlsDataSourceFactory(dataSourceFactory));
    }

    /**
     * Creates a new factory for {@link HlsMediaSource}s.
     *
     * @param hlsDataSourceFactory An {@link HlsDataSourceFactory} for {@link DataSource}s for
     *     manifests, segments and keys.
     */
    public Factory(HlsDataSourceFactory hlsDataSourceFactory) {
      this.hlsDataSourceFactory = checkNotNull(hlsDataSourceFactory);
      mediaSourceDrmHelper = new MediaSourceDrmHelper();
      playlistParserFactory = new DefaultHlsPlaylistParserFactory();
      playlistTrackerFactory = DefaultHlsPlaylistTracker.FACTORY;
      extractorFactory = HlsExtractorFactory.DEFAULT;
      loadErrorHandlingPolicy = new DefaultLoadErrorHandlingPolicy();
      compositeSequenceableLoaderFactory = new DefaultCompositeSequenceableLoaderFactory();
      metadataType = METADATA_TYPE_ID3;
      streamKeys = Collections.emptyList();
    }

    /**
     * @deprecated Use {@link MediaItem.Builder#setTag(Object)} and {@link
     *     #createMediaSource(MediaItem)} instead.
     */
    @Deprecated
    public Factory setTag(@Nullable Object tag) {
      this.tag = tag;
      return this;
    }

    /**
     * Sets the factory for {@link Extractor}s for the segments. The default value is {@link
     * HlsExtractorFactory#DEFAULT}.
     *
     * @param extractorFactory An {@link HlsExtractorFactory} for {@link Extractor}s for the
     *     segments.
     * @return This factory, for convenience.
     */
    public Factory setExtractorFactory(@Nullable HlsExtractorFactory extractorFactory) {
      this.extractorFactory =
          extractorFactory != null ? extractorFactory : HlsExtractorFactory.DEFAULT;
      return this;
    }

    /**
     * Sets the {@link LoadErrorHandlingPolicy}. The default value is created by calling {@link
     * DefaultLoadErrorHandlingPolicy#DefaultLoadErrorHandlingPolicy()}.
     *
     * <p>Calling this method overrides any calls to {@link #setMinLoadableRetryCount(int)}.
     *
     * @param loadErrorHandlingPolicy A {@link LoadErrorHandlingPolicy}.
     * @return This factory, for convenience.
     */
    public Factory setLoadErrorHandlingPolicy(
        @Nullable LoadErrorHandlingPolicy loadErrorHandlingPolicy) {
      this.loadErrorHandlingPolicy =
          loadErrorHandlingPolicy != null
              ? loadErrorHandlingPolicy
              : new DefaultLoadErrorHandlingPolicy();
      return this;
    }

    /** @deprecated Use {@link #setLoadErrorHandlingPolicy(LoadErrorHandlingPolicy)} instead. */
    @Deprecated
    public Factory setMinLoadableRetryCount(int minLoadableRetryCount) {
      this.loadErrorHandlingPolicy = new DefaultLoadErrorHandlingPolicy(minLoadableRetryCount);
      return this;
    }

    /**
     * Sets the factory from which playlist parsers will be obtained. The default value is a {@link
     * DefaultHlsPlaylistParserFactory}.
     *
     * @param playlistParserFactory An {@link HlsPlaylistParserFactory}.
     * @return This factory, for convenience.
     */
    public Factory setPlaylistParserFactory(
        @Nullable HlsPlaylistParserFactory playlistParserFactory) {
      this.playlistParserFactory =
          playlistParserFactory != null
              ? playlistParserFactory
              : new DefaultHlsPlaylistParserFactory();
      return this;
    }

    /**
     * Sets the {@link HlsPlaylistTracker} factory. The default value is {@link
     * DefaultHlsPlaylistTracker#FACTORY}.
     *
     * @param playlistTrackerFactory A factory for {@link HlsPlaylistTracker} instances.
     * @return This factory, for convenience.
     */
    public Factory setPlaylistTrackerFactory(
        @Nullable HlsPlaylistTracker.Factory playlistTrackerFactory) {
      this.playlistTrackerFactory =
          playlistTrackerFactory != null
              ? playlistTrackerFactory
              : DefaultHlsPlaylistTracker.FACTORY;
      return this;
    }

    /**
     * Sets the factory to create composite {@link SequenceableLoader}s for when this media source
     * loads data from multiple streams (video, audio etc...). The default is an instance of {@link
     * DefaultCompositeSequenceableLoaderFactory}.
     *
     * @param compositeSequenceableLoaderFactory A factory to create composite {@link
     *     SequenceableLoader}s for when this media source loads data from multiple streams (video,
     *     audio etc...).
     * @return This factory, for convenience.
     */
    public Factory setCompositeSequenceableLoaderFactory(
        @Nullable CompositeSequenceableLoaderFactory compositeSequenceableLoaderFactory) {
      this.compositeSequenceableLoaderFactory =
          compositeSequenceableLoaderFactory != null
              ? compositeSequenceableLoaderFactory
              : new DefaultCompositeSequenceableLoaderFactory();
      return this;
    }

    /**
     * Sets whether chunkless preparation is allowed. If true, preparation without chunk downloads
     * will be enabled for streams that provide sufficient information in their master playlist.
     *
     * @param allowChunklessPreparation Whether chunkless preparation is allowed.
     * @return This factory, for convenience.
     */
    public Factory setAllowChunklessPreparation(boolean allowChunklessPreparation) {
      this.allowChunklessPreparation = allowChunklessPreparation;
      return this;
    }

    /**
     * Sets the type of metadata to extract from the HLS source (defaults to {@link
     * #METADATA_TYPE_ID3}).
     *
     * <p>HLS supports in-band ID3 in both TS and fMP4 streams, but in the fMP4 case the data is
     * wrapped in an EMSG box [<a href="https://aomediacodec.github.io/av1-id3/">spec</a>].
     *
     * <p>If this is set to {@link #METADATA_TYPE_ID3} then raw ID3 metadata of will be extracted
     * from TS sources. From fMP4 streams EMSGs containing metadata of this type (in the variant
     * stream only) will be unwrapped to expose the inner data. All other in-band metadata will be
     * dropped.
     *
     * <p>If this is set to {@link #METADATA_TYPE_EMSG} then all EMSG data from the fMP4 variant
     * stream will be extracted. No metadata will be extracted from TS streams, since they don't
     * support EMSG.
     *
     * @param metadataType The type of metadata to extract.
     * @return This factory, for convenience.
     */
    public Factory setMetadataType(@MetadataType int metadataType) {
      this.metadataType = metadataType;
      return this;
    }

    /**
     * Sets whether to use #EXT-X-SESSION-KEY tags provided in the master playlist. If enabled, it's
     * assumed that any single session key declared in the master playlist can be used to obtain all
     * of the keys required for playback. For media where this is not true, this option should not
     * be enabled.
     *
     * @param useSessionKeys Whether to use #EXT-X-SESSION-KEY tags.
     * @return This factory, for convenience.
     */
    public Factory setUseSessionKeys(boolean useSessionKeys) {
      this.useSessionKeys = useSessionKeys;
      return this;
    }

    @Override
    public Factory setDrmSessionManager(@Nullable DrmSessionManager drmSessionManager) {
      this.drmSessionManager = drmSessionManager;
      return this;
    }

    @Override
    public Factory setDrmHttpDataSourceFactory(
        @Nullable HttpDataSource.Factory drmHttpDataSourceFactory) {
      mediaSourceDrmHelper.setDrmHttpDataSourceFactory(drmHttpDataSourceFactory);
      return this;
    }

    @Override
    public MediaSourceFactory setDrmUserAgent(@Nullable String userAgent) {
      mediaSourceDrmHelper.setDrmUserAgent(userAgent);
      return this;
    }

    /**
     * @deprecated Use {@link MediaItem.Builder#setStreamKeys(List)} and {@link
     *     #createMediaSource(MediaItem)} instead.
     */
    @SuppressWarnings("deprecation")
    @Deprecated
    @Override
    public Factory setStreamKeys(@Nullable List<StreamKey> streamKeys) {
      this.streamKeys = streamKeys != null ? streamKeys : Collections.emptyList();
      return this;
    }

    /**
     * @deprecated Use {@link #createMediaSource(MediaItem)} and {@link #addEventListener(Handler,
     *     MediaSourceEventListener)} instead.
     */
    @SuppressWarnings("deprecation")
    @Deprecated
    public HlsMediaSource createMediaSource(
        Uri playlistUri,
        @Nullable Handler eventHandler,
        @Nullable MediaSourceEventListener eventListener) {
      HlsMediaSource mediaSource = createMediaSource(playlistUri);
      if (eventHandler != null && eventListener != null) {
        mediaSource.addEventListener(eventHandler, eventListener);
      }
      return mediaSource;
    }

    /** @deprecated Use {@link #createMediaSource(MediaItem)} instead. */
    @SuppressWarnings("deprecation")
    @Deprecated
    @Override
    public HlsMediaSource createMediaSource(Uri uri) {
      return createMediaSource(
          new MediaItem.Builder().setUri(uri).setMimeType(MimeTypes.APPLICATION_M3U8).build());
    }

    /**
     * Returns a new {@link HlsMediaSource} using the current parameters.
     *
     * @param mediaItem The {@link MediaItem}.
     * @return The new {@link HlsMediaSource}.
     * @throws NullPointerException if {@link MediaItem#playbackProperties} is {@code null}.
     */
    @Override
    public HlsMediaSource createMediaSource(MediaItem mediaItem) {
      checkNotNull(mediaItem.playbackProperties);
      HlsPlaylistParserFactory playlistParserFactory = this.playlistParserFactory;
      List<StreamKey> streamKeys =
          mediaItem.playbackProperties.streamKeys.isEmpty()
              ? this.streamKeys
              : mediaItem.playbackProperties.streamKeys;
      if (!streamKeys.isEmpty()) {
        playlistParserFactory =
            new FilteringHlsPlaylistParserFactory(playlistParserFactory, streamKeys);
      }

      boolean needsTag = mediaItem.playbackProperties.tag == null && tag != null;
      boolean needsStreamKeys =
          mediaItem.playbackProperties.streamKeys.isEmpty() && !streamKeys.isEmpty();
      if (needsTag && needsStreamKeys) {
        mediaItem = mediaItem.buildUpon().setTag(tag).setStreamKeys(streamKeys).build();
      } else if (needsTag) {
        mediaItem = mediaItem.buildUpon().setTag(tag).build();
      } else if (needsStreamKeys) {
        mediaItem = mediaItem.buildUpon().setStreamKeys(streamKeys).build();
      }
      return new HlsMediaSource(
          mediaItem,
          hlsDataSourceFactory,
          extractorFactory,
          compositeSequenceableLoaderFactory,
          drmSessionManager != null ? drmSessionManager : mediaSourceDrmHelper.create(mediaItem),
          loadErrorHandlingPolicy,
          playlistTrackerFactory.createTracker(
              hlsDataSourceFactory, loadErrorHandlingPolicy, playlistParserFactory),
          allowChunklessPreparation,
          metadataType,
          useSessionKeys);
    }

    @Override
    public int[] getSupportedTypes() {
      return new int[] {C.TYPE_HLS};
    }
  }

  private final HlsExtractorFactory extractorFactory;
  private final MediaItem mediaItem;
  private final MediaItem.PlaybackProperties playbackProperties;
  private final HlsDataSourceFactory dataSourceFactory;
  private final CompositeSequenceableLoaderFactory compositeSequenceableLoaderFactory;
  private final DrmSessionManager drmSessionManager;
  private final LoadErrorHandlingPolicy loadErrorHandlingPolicy;
  private final boolean allowChunklessPreparation;
  private final @MetadataType int metadataType;
  private final boolean useSessionKeys;
  private final HlsPlaylistTracker playlistTracker;

  @Nullable private TransferListener mediaTransferListener;

  private HlsMediaSource(
      MediaItem mediaItem,
      HlsDataSourceFactory dataSourceFactory,
      HlsExtractorFactory extractorFactory,
      CompositeSequenceableLoaderFactory compositeSequenceableLoaderFactory,
      DrmSessionManager drmSessionManager,
      LoadErrorHandlingPolicy loadErrorHandlingPolicy,
      HlsPlaylistTracker playlistTracker,
      boolean allowChunklessPreparation,
      @MetadataType int metadataType,
      boolean useSessionKeys) {
    this.playbackProperties = checkNotNull(mediaItem.playbackProperties);
    this.mediaItem = mediaItem;
    this.dataSourceFactory = dataSourceFactory;
    this.extractorFactory = extractorFactory;
    this.compositeSequenceableLoaderFactory = compositeSequenceableLoaderFactory;
    this.drmSessionManager = drmSessionManager;
    this.loadErrorHandlingPolicy = loadErrorHandlingPolicy;
    this.playlistTracker = playlistTracker;
    this.allowChunklessPreparation = allowChunklessPreparation;
    this.metadataType = metadataType;
    this.useSessionKeys = useSessionKeys;
  }

  /**
   * @deprecated Use {@link #getMediaItem()} and {@link MediaItem.PlaybackProperties#tag} instead.
   */
  @SuppressWarnings("deprecation")
  @Deprecated
  @Override
  @Nullable
  public Object getTag() {
    return playbackProperties.tag;
  }

  @Override
  public MediaItem getMediaItem() {
    return mediaItem;
  }

  @Override
  protected void prepareSourceInternal(@Nullable TransferListener mediaTransferListener) {
    this.mediaTransferListener = mediaTransferListener;
    drmSessionManager.prepare();
    MediaSourceEventListener.EventDispatcher eventDispatcher =
        createEventDispatcher(/* mediaPeriodId= */ null);
    playlistTracker.start(playbackProperties.uri, eventDispatcher, /* listener= */ this);
  }

  @Override
  public void maybeThrowSourceInfoRefreshError() throws IOException {
    playlistTracker.maybeThrowPrimaryPlaylistRefreshError();
  }

  @Override
  public MediaPeriod createPeriod(MediaPeriodId id, Allocator allocator, long startPositionUs) {
    MediaSourceEventListener.EventDispatcher mediaSourceEventDispatcher = createEventDispatcher(id);
    DrmSessionEventListener.EventDispatcher drmEventDispatcher = createDrmEventDispatcher(id);
    return new HlsMediaPeriod(
        extractorFactory,
        playlistTracker,
        dataSourceFactory,
        mediaTransferListener,
        drmSessionManager,
        drmEventDispatcher,
        loadErrorHandlingPolicy,
        mediaSourceEventDispatcher,
        allocator,
        compositeSequenceableLoaderFactory,
        allowChunklessPreparation,
        metadataType,
        useSessionKeys);
  }

  @Override
  public void releasePeriod(MediaPeriod mediaPeriod) {
    ((HlsMediaPeriod) mediaPeriod).release();
  }

  @Override
  protected void releaseSourceInternal() {
    playlistTracker.stop();
    drmSessionManager.release();
  }

  @Override
  public void onPrimaryPlaylistRefreshed(HlsMediaPlaylist playlist) {
    SinglePeriodTimeline timeline;
    long windowStartTimeMs = playlist.hasProgramDateTime ? C.usToMs(playlist.startTimeUs)
        : C.TIME_UNSET;
    // For playlist types EVENT and VOD we know segments are never removed, so the presentation
    // started at the same time as the window. Otherwise, we don't know the presentation start time.
    long presentationStartTimeMs =
        playlist.playlistType == HlsMediaPlaylist.PLAYLIST_TYPE_EVENT
                || playlist.playlistType == HlsMediaPlaylist.PLAYLIST_TYPE_VOD
            ? windowStartTimeMs
            : C.TIME_UNSET;
    long windowDefaultStartPositionUs = playlist.startOffsetUs;
    // masterPlaylist is non-null because the first playlist has been fetched by now.
    HlsManifest manifest =
        new HlsManifest(checkNotNull(playlistTracker.getMasterPlaylist()), playlist);
    if (playlistTracker.isLive()) {
      long offsetFromInitialStartTimeUs =
          playlist.startTimeUs - playlistTracker.getInitialStartTimeUs();
      long periodDurationUs =
          playlist.hasEndTag ? offsetFromInitialStartTimeUs + playlist.durationUs : C.TIME_UNSET;
      List<HlsMediaPlaylist.Segment> segments = playlist.segments;
      if (windowDefaultStartPositionUs == C.TIME_UNSET) {
        windowDefaultStartPositionUs = 0;
        if (!segments.isEmpty()) {
          int defaultStartSegmentIndex = max(0, segments.size() - 3);
          // We attempt to set the default start position to be at least twice the target duration
          // behind the live edge.
          long minStartPositionUs = playlist.durationUs - playlist.targetDurationUs * 2;
          while (defaultStartSegmentIndex > 0
              && segments.get(defaultStartSegmentIndex).relativeStartTimeUs > minStartPositionUs) {
            defaultStartSegmentIndex--;
          }
          windowDefaultStartPositionUs = segments.get(defaultStartSegmentIndex).relativeStartTimeUs;
        }
      }
      timeline =
          new SinglePeriodTimeline(
              presentationStartTimeMs,
              windowStartTimeMs,
              /* elapsedRealtimeEpochOffsetMs= */ C.TIME_UNSET,
              periodDurationUs,
              /* windowDurationUs= */ playlist.durationUs,
              /* windowPositionInPeriodUs= */ offsetFromInitialStartTimeUs,
              windowDefaultStartPositionUs,
              /* isSeekable= */ true,
              /* isDynamic= */ !playlist.hasEndTag,
              /* isLive= */ true,
              manifest,
              mediaItem);
    } else /* not live */ {
      if (windowDefaultStartPositionUs == C.TIME_UNSET) {
        windowDefaultStartPositionUs = 0;
      }
      timeline =
          new SinglePeriodTimeline(
              presentationStartTimeMs,
              windowStartTimeMs,
              /* elapsedRealtimeEpochOffsetMs= */ C.TIME_UNSET,
              /* periodDurationUs= */ playlist.durationUs,
              /* windowDurationUs= */ playlist.durationUs,
              /* windowPositionInPeriodUs= */ 0,
              windowDefaultStartPositionUs,
              /* isSeekable= */ true,
              /* isDynamic= */ false,
              /* isLive= */ false,
              manifest,
              mediaItem);
    }
    refreshSourceInfo(timeline);
  }

}
