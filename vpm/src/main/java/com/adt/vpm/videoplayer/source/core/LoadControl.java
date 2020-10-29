/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core;

import com.adt.vpm.videoplayer.source.core.source.TrackGroup;
import com.adt.vpm.videoplayer.source.core.source.TrackGroupArray;
import com.adt.vpm.videoplayer.source.core.trackselection.TrackSelectionArray;
import com.adt.vpm.videoplayer.source.core.upstream.Allocator;

/**
 * Controls buffering of media.
 */
public interface LoadControl {

  /**
   * Called by the player when prepared with a new source.
   */
  void onPrepared();

  /**
   * Called by the player when a track selection occurs.
   *
   * @param renderers The renderers.
   * @param trackGroups The {@link TrackGroup}s from which the selection was made.
   * @param trackSelections The track selections that were made.
   */
  void onTracksSelected(Renderer[] renderers, TrackGroupArray trackGroups,
                        TrackSelectionArray trackSelections);

  /**
   * Called by the player when stopped.
   */
  void onStopped();

  /**
   * Called by the player when released.
   */
  void onReleased();

  /**
   * Returns the {@link Allocator} that should be used to obtain media buffer allocations.
   */
  Allocator getAllocator();

  /**
   * Returns the duration of media to retain in the buffer prior to the current playback position,
   * for fast backward seeking.
   * <p>
   * Note: If {@link #retainBackBufferFromKeyframe()} is false then seeking in the back-buffer will
   * only be fast if the back-buffer contains a keyframe prior to the seek position.
   * <p>
   * Note: Implementations should return a single value. Dynamic changes to the back-buffer are not
   * currently supported.
   *
   * @return The duration of media to retain in the buffer prior to the current playback position,
   *     in microseconds.
   */
  long getBackBufferDurationUs();

  /**
   * Returns whether media should be retained from the keyframe before the current playback position
   * minus {@link #getBackBufferDurationUs()}, rather than any sample before or at that position.
   * <p>
   * Warning: Returning true will cause the back-buffer size to depend on the spacing of keyframes
   * in the media being played. Returning true is not recommended unless you control the media and
   * are comfortable with the back-buffer size exceeding {@link #getBackBufferDurationUs()} by as
   * much as the maximum duration between adjacent keyframes in the media.
   * <p>
   * Note: Implementations should return a single value. Dynamic changes to the back-buffer are not
   * currently supported.
   *
   * @return Whether media should be retained from the keyframe before the current playback position
   * minus {@link #getBackBufferDurationUs()}, rather than any sample before or at that position.
   */
  boolean retainBackBufferFromKeyframe();

  /**
   * Called by the player to determine whether it should continue to load the source.
   *
   * @param playbackPositionUs The current playback position in microseconds, relative to the start
   *     of the {@link Timeline.Period period} that will continue to be loaded if this method
   *     returns {@code true}. If playback of this period has not yet started, the value will be
   *     negative and equal in magnitude to the duration of any media in previous periods still to
   *     be played.
   * @param bufferedDurationUs The duration of media that's currently buffered.
   * @param playbackSpeed The current playback speed.
   * @return Whether the loading should continue.
   */
  boolean shouldContinueLoading(
          long playbackPositionUs, long bufferedDurationUs, float playbackSpeed);

  /**
   * Called repeatedly by the player when it's loading the source, has yet to start playback, and
   * has the minimum amount of data necessary for playback to be started. The value returned
   * determines whether playback is actually started. The load control may opt to return {@code
   * false} until some condition has been met (e.g. a certain amount of media is buffered).
   *
   * @param bufferedDurationUs The duration of media that's currently buffered.
   * @param playbackSpeed The current playback speed.
   * @param rebuffering Whether the player is rebuffering. A rebuffer is defined to be caused by
   *     buffer depletion rather than a user action. Hence this parameter is false during initial
   *     buffering and when buffering as a result of a seek operation.
   * @return Whether playback should be allowed to start or resume.
   */
  boolean shouldStartPlayback(long bufferedDurationUs, float playbackSpeed, boolean rebuffering);
}
