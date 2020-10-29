/*
 * Created by ADT author on 10/1/20 11:19 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/22/20 6:23 PM
 */
package com.adt.vpm.videoplayer.source.hls.playlist;

import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.ParserException;
import com.adt.vpm.videoplayer.source.common.util.Assertions;
import com.adt.vpm.videoplayer.source.common.util.Util;
import com.adt.vpm.videoplayer.source.core.source.LoadEventInfo;
import com.adt.vpm.videoplayer.source.core.source.MediaLoadData;
import com.adt.vpm.videoplayer.source.core.source.MediaSourceEventListener.EventDispatcher;
import com.adt.vpm.videoplayer.source.core.upstream.LoadErrorHandlingPolicy;
import com.adt.vpm.videoplayer.source.core.upstream.LoadErrorHandlingPolicy.LoadErrorInfo;
import com.adt.vpm.videoplayer.source.core.upstream.Loader;
import com.adt.vpm.videoplayer.source.core.upstream.Loader.LoadErrorAction;
import com.adt.vpm.videoplayer.source.core.upstream.ParsingLoadable;
import com.adt.vpm.videoplayer.source.hls.HlsDataSourceFactory;
import com.adt.vpm.videoplayer.source.hls.playlist.HlsMasterPlaylist.Variant;
import com.adt.vpm.videoplayer.source.hls.playlist.HlsMediaPlaylist.Segment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static java.lang.Math.max;

/** Default implementation for {@link HlsPlaylistTracker}. */
public final class DefaultHlsPlaylistTracker
    implements HlsPlaylistTracker, Loader.Callback<ParsingLoadable<HlsPlaylist>> {

  /** Factory for {@link DefaultHlsPlaylistTracker} instances. */
  public static final Factory FACTORY = DefaultHlsPlaylistTracker::new;

  /**
   * Default coefficient applied on the target duration of a playlist to determine the amount of
   * time after which an unchanging playlist is considered stuck.
   */
  public static final double DEFAULT_PLAYLIST_STUCK_TARGET_DURATION_COEFFICIENT = 3.5;

  private final HlsDataSourceFactory dataSourceFactory;
  private final HlsPlaylistParserFactory playlistParserFactory;
  private final LoadErrorHandlingPolicy loadErrorHandlingPolicy;
  private final HashMap<Uri, MediaPlaylistBundle> playlistBundles;
  private final List<PlaylistEventListener> listeners;
  private final double playlistStuckTargetDurationCoefficient;

  @Nullable private ParsingLoadable.Parser<HlsPlaylist> mediaPlaylistParser;
  @Nullable private EventDispatcher eventDispatcher;
  @Nullable private Loader initialPlaylistLoader;
  @Nullable private Handler playlistRefreshHandler;
  @Nullable private PrimaryPlaylistListener primaryPlaylistListener;
  @Nullable private HlsMasterPlaylist masterPlaylist;
  @Nullable private Uri primaryMediaPlaylistUrl;
  @Nullable private HlsMediaPlaylist primaryMediaPlaylistSnapshot;
  private boolean isLive;
  private long initialStartTimeUs;

  /**
   * Creates an instance.
   *
   * @param dataSourceFactory A factory for {@link DataSource} instances.
   * @param loadErrorHandlingPolicy The {@link LoadErrorHandlingPolicy}.
   * @param playlistParserFactory An {@link HlsPlaylistParserFactory}.
   */
  public DefaultHlsPlaylistTracker(
      HlsDataSourceFactory dataSourceFactory,
      LoadErrorHandlingPolicy loadErrorHandlingPolicy,
      HlsPlaylistParserFactory playlistParserFactory) {
    this(
        dataSourceFactory,
        loadErrorHandlingPolicy,
        playlistParserFactory,
        DEFAULT_PLAYLIST_STUCK_TARGET_DURATION_COEFFICIENT);
  }

  /**
   * Creates an instance.
   *
   * @param dataSourceFactory A factory for {@link DataSource} instances.
   * @param loadErrorHandlingPolicy The {@link LoadErrorHandlingPolicy}.
   * @param playlistParserFactory An {@link HlsPlaylistParserFactory}.
   * @param playlistStuckTargetDurationCoefficient A coefficient to apply to the target duration of
   *     media playlists in order to determine that a non-changing playlist is stuck. Once a
   *     playlist is deemed stuck, a {@link PlaylistStuckException} is thrown via {@link
   *     #maybeThrowPlaylistRefreshError(Uri)}.
   */
  public DefaultHlsPlaylistTracker(
      HlsDataSourceFactory dataSourceFactory,
      LoadErrorHandlingPolicy loadErrorHandlingPolicy,
      HlsPlaylistParserFactory playlistParserFactory,
      double playlistStuckTargetDurationCoefficient) {
    this.dataSourceFactory = dataSourceFactory;
    this.playlistParserFactory = playlistParserFactory;
    this.loadErrorHandlingPolicy = loadErrorHandlingPolicy;
    this.playlistStuckTargetDurationCoefficient = playlistStuckTargetDurationCoefficient;
    listeners = new ArrayList<>();
    playlistBundles = new HashMap<>();
    initialStartTimeUs = C.TIME_UNSET;
  }

  // HlsPlaylistTracker implementation.

  @Override
  public void start(
      Uri initialPlaylistUri,
      EventDispatcher eventDispatcher,
      PrimaryPlaylistListener primaryPlaylistListener) {
    this.playlistRefreshHandler = Util.createHandlerForCurrentLooper();
    this.eventDispatcher = eventDispatcher;
    this.primaryPlaylistListener = primaryPlaylistListener;
    ParsingLoadable<HlsPlaylist> masterPlaylistLoadable =
        new ParsingLoadable<>(
            dataSourceFactory.createDataSource(C.DATA_TYPE_MANIFEST),
            initialPlaylistUri,
            C.DATA_TYPE_MANIFEST,
            playlistParserFactory.createPlaylistParser());
    Assertions.checkState(initialPlaylistLoader == null);
    initialPlaylistLoader = new Loader("DefaultHlsPlaylistTracker:MasterPlaylist");
    long elapsedRealtime =
        initialPlaylistLoader.startLoading(
            masterPlaylistLoadable,
            this,
            loadErrorHandlingPolicy.getMinimumLoadableRetryCount(masterPlaylistLoadable.type));
    eventDispatcher.loadStarted(
        new LoadEventInfo(
            masterPlaylistLoadable.loadTaskId, masterPlaylistLoadable.dataSpec, elapsedRealtime),
        masterPlaylistLoadable.type);
  }

  @Override
  public void stop() {
    primaryMediaPlaylistUrl = null;
    primaryMediaPlaylistSnapshot = null;
    masterPlaylist = null;
    initialStartTimeUs = C.TIME_UNSET;
    initialPlaylistLoader.release();
    initialPlaylistLoader = null;
    for (MediaPlaylistBundle bundle : playlistBundles.values()) {
      bundle.release();
    }
    playlistRefreshHandler.removeCallbacksAndMessages(null);
    playlistRefreshHandler = null;
    playlistBundles.clear();
  }

  @Override
  public void addListener(PlaylistEventListener listener) {
    Assertions.checkNotNull(listener);
    listeners.add(listener);
  }

  @Override
  public void removeListener(PlaylistEventListener listener) {
    listeners.remove(listener);
  }

  @Override
  @Nullable
  public HlsMasterPlaylist getMasterPlaylist() {
    return masterPlaylist;
  }

  @Override
  @Nullable
  public HlsMediaPlaylist getPlaylistSnapshot(Uri url, boolean isForPlayback) {
    HlsMediaPlaylist snapshot = playlistBundles.get(url).getPlaylistSnapshot();
    if (snapshot != null && isForPlayback) {
      maybeSetPrimaryUrl(url);
    }
    return snapshot;
  }

  @Override
  public long getInitialStartTimeUs() {
    return initialStartTimeUs;
  }

  @Override
  public boolean isSnapshotValid(Uri url) {
    return playlistBundles.get(url).isSnapshotValid();
  }

  @Override
  public void maybeThrowPrimaryPlaylistRefreshError() throws IOException {
    if (initialPlaylistLoader != null) {
      initialPlaylistLoader.maybeThrowError();
    }
    if (primaryMediaPlaylistUrl != null) {
      maybeThrowPlaylistRefreshError(primaryMediaPlaylistUrl);
    }
  }

  @Override
  public void maybeThrowPlaylistRefreshError(Uri url) throws IOException {
    playlistBundles.get(url).maybeThrowPlaylistRefreshError();
  }

  @Override
  public void refreshPlaylist(Uri url) {
    playlistBundles.get(url).loadPlaylist();
  }

  @Override
  public boolean isLive() {
    return isLive;
  }

  // Loader.Callback implementation.

  @Override
  public void onLoadCompleted(
      ParsingLoadable<HlsPlaylist> loadable, long elapsedRealtimeMs, long loadDurationMs) {
    HlsPlaylist result = loadable.getResult();
    HlsMasterPlaylist masterPlaylist;
    boolean isMediaPlaylist = result instanceof HlsMediaPlaylist;
    if (isMediaPlaylist) {
      masterPlaylist = HlsMasterPlaylist.createSingleVariantMasterPlaylist(result.baseUri);
    } else /* result instanceof HlsMasterPlaylist */ {
      masterPlaylist = (HlsMasterPlaylist) result;
    }
    this.masterPlaylist = masterPlaylist;
    mediaPlaylistParser = playlistParserFactory.createPlaylistParser(masterPlaylist);
    primaryMediaPlaylistUrl = masterPlaylist.variants.get(0).url;
    createBundles(masterPlaylist.mediaPlaylistUrls);
    MediaPlaylistBundle primaryBundle = playlistBundles.get(primaryMediaPlaylistUrl);
    LoadEventInfo loadEventInfo =
        new LoadEventInfo(
            loadable.loadTaskId,
            loadable.dataSpec,
            loadable.getUri(),
            loadable.getResponseHeaders(),
            elapsedRealtimeMs,
            loadDurationMs,
            loadable.bytesLoaded());
    if (isMediaPlaylist) {
      // We don't need to load the playlist again. We can use the same result.
      primaryBundle.processLoadedPlaylist((HlsMediaPlaylist) result, loadEventInfo);
    } else {
      primaryBundle.loadPlaylist();
    }
    loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId);
    eventDispatcher.loadCompleted(loadEventInfo, C.DATA_TYPE_MANIFEST);
  }

  @Override
  public void onLoadCanceled(
      ParsingLoadable<HlsPlaylist> loadable,
      long elapsedRealtimeMs,
      long loadDurationMs,
      boolean released) {
    LoadEventInfo loadEventInfo =
        new LoadEventInfo(
            loadable.loadTaskId,
            loadable.dataSpec,
            loadable.getUri(),
            loadable.getResponseHeaders(),
            elapsedRealtimeMs,
            loadDurationMs,
            loadable.bytesLoaded());
    loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId);
    eventDispatcher.loadCanceled(loadEventInfo, C.DATA_TYPE_MANIFEST);
  }

  @Override
  public LoadErrorAction onLoadError(
      ParsingLoadable<HlsPlaylist> loadable,
      long elapsedRealtimeMs,
      long loadDurationMs,
      IOException error,
      int errorCount) {
    LoadEventInfo loadEventInfo =
        new LoadEventInfo(
            loadable.loadTaskId,
            loadable.dataSpec,
            loadable.getUri(),
            loadable.getResponseHeaders(),
            elapsedRealtimeMs,
            loadDurationMs,
            loadable.bytesLoaded());
    MediaLoadData mediaLoadData = new MediaLoadData(loadable.type);
    long retryDelayMs =
        loadErrorHandlingPolicy.getRetryDelayMsFor(
            new LoadErrorInfo(loadEventInfo, mediaLoadData, error, errorCount));
    boolean isFatal = retryDelayMs == C.TIME_UNSET;
    eventDispatcher.loadError(loadEventInfo, loadable.type, error, isFatal);
    if (isFatal) {
      loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId);
    }
    return isFatal
        ? Loader.DONT_RETRY_FATAL
        : Loader.createRetryAction(/* resetErrorCount= */ false, retryDelayMs);
  }

  // Internal methods.

  private boolean maybeSelectNewPrimaryUrl() {
    List<Variant> variants = masterPlaylist.variants;
    int variantsSize = variants.size();
    long currentTimeMs = SystemClock.elapsedRealtime();
    for (int i = 0; i < variantsSize; i++) {
      MediaPlaylistBundle bundle = playlistBundles.get(variants.get(i).url);
      if (currentTimeMs > bundle.excludeUntilMs) {
        primaryMediaPlaylistUrl = bundle.playlistUrl;
        bundle.loadPlaylist();
        return true;
      }
    }
    return false;
  }

  private void maybeSetPrimaryUrl(Uri url) {
    if (url.equals(primaryMediaPlaylistUrl)
        || !isVariantUrl(url)
        || (primaryMediaPlaylistSnapshot != null && primaryMediaPlaylistSnapshot.hasEndTag)) {
      // Ignore if the primary media playlist URL is unchanged, if the media playlist is not
      // referenced directly by a variant, or it the last primary snapshot contains an end tag.
      return;
    }
    primaryMediaPlaylistUrl = url;
    playlistBundles.get(primaryMediaPlaylistUrl).loadPlaylist();
  }

  /** Returns whether any of the variants in the master playlist have the specified playlist URL. */
  private boolean isVariantUrl(Uri playlistUrl) {
    List<Variant> variants = masterPlaylist.variants;
    for (int i = 0; i < variants.size(); i++) {
      if (playlistUrl.equals(variants.get(i).url)) {
        return true;
      }
    }
    return false;
  }

  private void createBundles(List<Uri> urls) {
    int listSize = urls.size();
    for (int i = 0; i < listSize; i++) {
      Uri url = urls.get(i);
      MediaPlaylistBundle bundle = new MediaPlaylistBundle(url);
      playlistBundles.put(url, bundle);
    }
  }

  /**
   * Called by the bundles when a snapshot changes.
   *
   * @param url The url of the playlist.
   * @param newSnapshot The new snapshot.
   */
  private void onPlaylistUpdated(Uri url, HlsMediaPlaylist newSnapshot) {
    if (url.equals(primaryMediaPlaylistUrl)) {
      if (primaryMediaPlaylistSnapshot == null) {
        // This is the first primary url snapshot.
        isLive = !newSnapshot.hasEndTag;
        initialStartTimeUs = newSnapshot.startTimeUs;
      }
      primaryMediaPlaylistSnapshot = newSnapshot;
      primaryPlaylistListener.onPrimaryPlaylistRefreshed(newSnapshot);
    }
    int listenersSize = listeners.size();
    for (int i = 0; i < listenersSize; i++) {
      listeners.get(i).onPlaylistChanged();
    }
  }

  private boolean notifyPlaylistError(Uri playlistUrl, long exclusionDurationMs) {
    int listenersSize = listeners.size();
    boolean anyExclusionFailed = false;
    for (int i = 0; i < listenersSize; i++) {
      anyExclusionFailed |= !listeners.get(i).onPlaylistError(playlistUrl, exclusionDurationMs);
    }
    return anyExclusionFailed;
  }

  private HlsMediaPlaylist getLatestPlaylistSnapshot(
      HlsMediaPlaylist oldPlaylist, HlsMediaPlaylist loadedPlaylist) {
    if (!loadedPlaylist.isNewerThan(oldPlaylist)) {
      if (loadedPlaylist.hasEndTag) {
        // If the loaded playlist has an end tag but is not newer than the old playlist then we have
        // an inconsistent state. This is typically caused by the server incorrectly resetting the
        // media sequence when appending the end tag. We resolve this case as best we can by
        // returning the old playlist with the end tag appended.
        return oldPlaylist.copyWithEndTag();
      } else {
        return oldPlaylist;
      }
    }
    long startTimeUs = getLoadedPlaylistStartTimeUs(oldPlaylist, loadedPlaylist);
    int discontinuitySequence = getLoadedPlaylistDiscontinuitySequence(oldPlaylist, loadedPlaylist);
    return loadedPlaylist.copyWith(startTimeUs, discontinuitySequence);
  }

  private long getLoadedPlaylistStartTimeUs(
      HlsMediaPlaylist oldPlaylist, HlsMediaPlaylist loadedPlaylist) {
    if (loadedPlaylist.hasProgramDateTime) {
      return loadedPlaylist.startTimeUs;
    }
    long primarySnapshotStartTimeUs =
        primaryMediaPlaylistSnapshot != null ? primaryMediaPlaylistSnapshot.startTimeUs : 0;
    if (oldPlaylist == null) {
      return primarySnapshotStartTimeUs;
    }
    int oldPlaylistSize = oldPlaylist.segments.size();
    Segment firstOldOverlappingSegment = getFirstOldOverlappingSegment(oldPlaylist, loadedPlaylist);
    if (firstOldOverlappingSegment != null) {
      return oldPlaylist.startTimeUs + firstOldOverlappingSegment.relativeStartTimeUs;
    } else if (oldPlaylistSize == loadedPlaylist.mediaSequence - oldPlaylist.mediaSequence) {
      return oldPlaylist.getEndTimeUs();
    } else {
      // No segments overlap, we assume the new playlist start coincides with the primary playlist.
      return primarySnapshotStartTimeUs;
    }
  }

  private int getLoadedPlaylistDiscontinuitySequence(
      HlsMediaPlaylist oldPlaylist, HlsMediaPlaylist loadedPlaylist) {
    if (loadedPlaylist.hasDiscontinuitySequence) {
      return loadedPlaylist.discontinuitySequence;
    }
    // TODO: Improve cross-playlist discontinuity adjustment.
    int primaryUrlDiscontinuitySequence =
        primaryMediaPlaylistSnapshot != null
            ? primaryMediaPlaylistSnapshot.discontinuitySequence
            : 0;
    if (oldPlaylist == null) {
      return primaryUrlDiscontinuitySequence;
    }
    Segment firstOldOverlappingSegment = getFirstOldOverlappingSegment(oldPlaylist, loadedPlaylist);
    if (firstOldOverlappingSegment != null) {
      return oldPlaylist.discontinuitySequence
          + firstOldOverlappingSegment.relativeDiscontinuitySequence
          - loadedPlaylist.segments.get(0).relativeDiscontinuitySequence;
    }
    return primaryUrlDiscontinuitySequence;
  }

  private static Segment getFirstOldOverlappingSegment(
      HlsMediaPlaylist oldPlaylist, HlsMediaPlaylist loadedPlaylist) {
    int mediaSequenceOffset = (int) (loadedPlaylist.mediaSequence - oldPlaylist.mediaSequence);
    List<Segment> oldSegments = oldPlaylist.segments;
    return mediaSequenceOffset < oldSegments.size() ? oldSegments.get(mediaSequenceOffset) : null;
  }

  /** Holds all information related to a specific Media Playlist. */
  private final class MediaPlaylistBundle
      implements Loader.Callback<ParsingLoadable<HlsPlaylist>>, Runnable {

    private final Uri playlistUrl;
    private final Loader mediaPlaylistLoader;
    private final ParsingLoadable<HlsPlaylist> mediaPlaylistLoadable;

    @Nullable private HlsMediaPlaylist playlistSnapshot;
    private long lastSnapshotLoadMs;
    private long lastSnapshotChangeMs;
    private long earliestNextLoadTimeMs;
    private long excludeUntilMs;
    private boolean loadPending;
    private IOException playlistError;

    public MediaPlaylistBundle(Uri playlistUrl) {
      this.playlistUrl = playlistUrl;
      mediaPlaylistLoader = new Loader("DefaultHlsPlaylistTracker:MediaPlaylist");
      mediaPlaylistLoadable =
          new ParsingLoadable<>(
              dataSourceFactory.createDataSource(C.DATA_TYPE_MANIFEST),
              playlistUrl,
              C.DATA_TYPE_MANIFEST,
              mediaPlaylistParser);
    }

    @Nullable
    public HlsMediaPlaylist getPlaylistSnapshot() {
      return playlistSnapshot;
    }

    public boolean isSnapshotValid() {
      if (playlistSnapshot == null) {
        return false;
      }
      long currentTimeMs = SystemClock.elapsedRealtime();
      long snapshotValidityDurationMs = max(30000, C.usToMs(playlistSnapshot.durationUs));
      return playlistSnapshot.hasEndTag
          || playlistSnapshot.playlistType == HlsMediaPlaylist.PLAYLIST_TYPE_EVENT
          || playlistSnapshot.playlistType == HlsMediaPlaylist.PLAYLIST_TYPE_VOD
          || lastSnapshotLoadMs + snapshotValidityDurationMs > currentTimeMs;
    }

    public void release() {
      mediaPlaylistLoader.release();
    }

    public void loadPlaylist() {
      excludeUntilMs = 0;
      if (loadPending || mediaPlaylistLoader.isLoading() || mediaPlaylistLoader.hasFatalError()) {
        // Load already pending, in progress, or a fatal error has been encountered. Do nothing.
        return;
      }
      long currentTimeMs = SystemClock.elapsedRealtime();
      if (currentTimeMs < earliestNextLoadTimeMs) {
        loadPending = true;
        playlistRefreshHandler.postDelayed(this, earliestNextLoadTimeMs - currentTimeMs);
      } else {
        loadPlaylistImmediately();
      }
    }

    public void maybeThrowPlaylistRefreshError() throws IOException {
      mediaPlaylistLoader.maybeThrowError();
      if (playlistError != null) {
        throw playlistError;
      }
    }

    // Loader.Callback implementation.

    @Override
    public void onLoadCompleted(
        ParsingLoadable<HlsPlaylist> loadable, long elapsedRealtimeMs, long loadDurationMs) {
      HlsPlaylist result = loadable.getResult();
      LoadEventInfo loadEventInfo =
          new LoadEventInfo(
              loadable.loadTaskId,
              loadable.dataSpec,
              loadable.getUri(),
              loadable.getResponseHeaders(),
              elapsedRealtimeMs,
              loadDurationMs,
              loadable.bytesLoaded());
      if (result instanceof HlsMediaPlaylist) {
        processLoadedPlaylist((HlsMediaPlaylist) result, loadEventInfo);
        eventDispatcher.loadCompleted(loadEventInfo, C.DATA_TYPE_MANIFEST);
      } else {
        playlistError = new ParserException("Loaded playlist has unexpected type.");
        eventDispatcher.loadError(
            loadEventInfo, C.DATA_TYPE_MANIFEST, playlistError, /* wasCanceled= */ true);
      }
      loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId);
    }

    @Override
    public void onLoadCanceled(
        ParsingLoadable<HlsPlaylist> loadable,
        long elapsedRealtimeMs,
        long loadDurationMs,
        boolean released) {
      LoadEventInfo loadEventInfo =
          new LoadEventInfo(
              loadable.loadTaskId,
              loadable.dataSpec,
              loadable.getUri(),
              loadable.getResponseHeaders(),
              elapsedRealtimeMs,
              loadDurationMs,
              loadable.bytesLoaded());
      loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId);
      eventDispatcher.loadCanceled(loadEventInfo, C.DATA_TYPE_MANIFEST);
    }

    @Override
    public LoadErrorAction onLoadError(
        ParsingLoadable<HlsPlaylist> loadable,
        long elapsedRealtimeMs,
        long loadDurationMs,
        IOException error,
        int errorCount) {
      LoadEventInfo loadEventInfo =
          new LoadEventInfo(
              loadable.loadTaskId,
              loadable.dataSpec,
              loadable.getUri(),
              loadable.getResponseHeaders(),
              elapsedRealtimeMs,
              loadDurationMs,
              loadable.bytesLoaded());
      MediaLoadData mediaLoadData = new MediaLoadData(loadable.type);
      LoadErrorInfo loadErrorInfo =
          new LoadErrorInfo(loadEventInfo, mediaLoadData, error, errorCount);
      LoadErrorAction loadErrorAction;
      long exclusionDurationMs = loadErrorHandlingPolicy.getBlacklistDurationMsFor(loadErrorInfo);
      boolean shouldExclude = exclusionDurationMs != C.TIME_UNSET;

      boolean exclusionFailed =
          notifyPlaylistError(playlistUrl, exclusionDurationMs) || !shouldExclude;
      if (shouldExclude) {
        exclusionFailed |= excludePlaylist(exclusionDurationMs);
      }

      if (exclusionFailed) {
        long retryDelay = loadErrorHandlingPolicy.getRetryDelayMsFor(loadErrorInfo);
        loadErrorAction =
            retryDelay != C.TIME_UNSET
                ? Loader.createRetryAction(false, retryDelay)
                : Loader.DONT_RETRY_FATAL;
      } else {
        loadErrorAction = Loader.DONT_RETRY;
      }

      boolean wasCanceled = !loadErrorAction.isRetry();
      eventDispatcher.loadError(loadEventInfo, loadable.type, error, wasCanceled);
      if (wasCanceled) {
        loadErrorHandlingPolicy.onLoadTaskConcluded(loadable.loadTaskId);
      }
      return loadErrorAction;
    }

    // Runnable implementation.

    @Override
    public void run() {
      loadPending = false;
      loadPlaylistImmediately();
    }

    // Internal methods.

    private void loadPlaylistImmediately() {
      long elapsedRealtime =
          mediaPlaylistLoader.startLoading(
              mediaPlaylistLoadable,
              this,
              loadErrorHandlingPolicy.getMinimumLoadableRetryCount(mediaPlaylistLoadable.type));
      eventDispatcher.loadStarted(
          new LoadEventInfo(
              mediaPlaylistLoadable.loadTaskId, mediaPlaylistLoadable.dataSpec, elapsedRealtime),
          mediaPlaylistLoadable.type);
    }

    private void processLoadedPlaylist(
        HlsMediaPlaylist loadedPlaylist, LoadEventInfo loadEventInfo) {
      HlsMediaPlaylist oldPlaylist = playlistSnapshot;
      long currentTimeMs = SystemClock.elapsedRealtime();
      lastSnapshotLoadMs = currentTimeMs;
      playlistSnapshot = getLatestPlaylistSnapshot(oldPlaylist, loadedPlaylist);
      if (playlistSnapshot != oldPlaylist) {
        playlistError = null;
        lastSnapshotChangeMs = currentTimeMs;
        onPlaylistUpdated(playlistUrl, playlistSnapshot);
      } else if (!playlistSnapshot.hasEndTag) {
        if (loadedPlaylist.mediaSequence + loadedPlaylist.segments.size()
            < playlistSnapshot.mediaSequence) {
          // TODO: Allow customization of playlist resets handling.
          // The media sequence jumped backwards. The server has probably reset. We do not try
          // excluding in this case.
          playlistError = new PlaylistResetException(playlistUrl);
          notifyPlaylistError(playlistUrl, C.TIME_UNSET);
        } else if (currentTimeMs - lastSnapshotChangeMs
            > C.usToMs(playlistSnapshot.targetDurationUs)
                * playlistStuckTargetDurationCoefficient) {
          // TODO: Allow customization of stuck playlists handling.
          playlistError = new PlaylistStuckException(playlistUrl);
          LoadErrorInfo loadErrorInfo =
              new LoadErrorInfo(
                  loadEventInfo,
                  new MediaLoadData(C.DATA_TYPE_MANIFEST),
                  playlistError,
                  /* errorCount= */ 1);
          long exclusionDurationMs =
              loadErrorHandlingPolicy.getBlacklistDurationMsFor(loadErrorInfo);
          notifyPlaylistError(playlistUrl, exclusionDurationMs);
          if (exclusionDurationMs != C.TIME_UNSET) {
            excludePlaylist(exclusionDurationMs);
          }
        }
      }
      // Do not allow the playlist to load again within the target duration if we obtained a new
      // snapshot, or half the target duration otherwise.
      earliestNextLoadTimeMs =
          currentTimeMs
              + C.usToMs(
                  playlistSnapshot != oldPlaylist
                      ? playlistSnapshot.targetDurationUs
                      : (playlistSnapshot.targetDurationUs / 2));
      // Schedule a load if this is the primary playlist and it doesn't have an end tag. Else the
      // next load will be scheduled when refreshPlaylist is called, or when this playlist becomes
      // the primary.
      if (playlistUrl.equals(primaryMediaPlaylistUrl) && !playlistSnapshot.hasEndTag) {
        loadPlaylist();
      }
    }

    /**
     * Excludes the playlist.
     *
     * @param exclusionDurationMs The number of milliseconds for which the playlist should be
     *     excluded.
     * @return Whether the playlist is the primary, despite being excluded.
     */
    private boolean excludePlaylist(long exclusionDurationMs) {
      excludeUntilMs = SystemClock.elapsedRealtime() + exclusionDurationMs;
      return playlistUrl.equals(primaryMediaPlaylistUrl) && !maybeSelectNewPrimaryUrl();
    }
  }
}