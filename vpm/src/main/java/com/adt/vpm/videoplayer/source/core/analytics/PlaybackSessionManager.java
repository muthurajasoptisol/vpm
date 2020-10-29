/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.analytics;

import com.adt.vpm.videoplayer.source.core.Player;
import com.adt.vpm.videoplayer.source.core.Timeline;
import com.adt.vpm.videoplayer.source.core.source.MediaSource;
import com.adt.vpm.videoplayer.source.core.analytics.AnalyticsListener.EventTime;

/**
 * Manager for active playback sessions.
 *
 * <p>The manager keeps track of the association between window index and/or media period id to
 * session identifier.
 */
public interface PlaybackSessionManager {

  /** A listener for session updates. */
  interface Listener {

    /**
     * Called when a new session is created as a result of {@link #updateSessions(EventTime)}.
     *
     * @param eventTime The {@link EventTime} at which the session is created.
     * @param sessionId The identifier of the new session.
     */
    void onSessionCreated(EventTime eventTime, String sessionId);

    /**
     * Called when a session becomes active, i.e. playing in the foreground.
     *
     * @param eventTime The {@link EventTime} at which the session becomes active.
     * @param sessionId The identifier of the session.
     */
    void onSessionActive(EventTime eventTime, String sessionId);

    /**
     * Called when a session is interrupted by ad playback.
     *
     * @param eventTime The {@link EventTime} at which the ad playback starts.
     * @param contentSessionId The session identifier of the content session.
     * @param adSessionId The identifier of the ad session.
     */
    void onAdPlaybackStarted(EventTime eventTime, String contentSessionId, String adSessionId);

    /**
     * Called when a session is permanently finished.
     *
     * @param eventTime The {@link EventTime} at which the session finished.
     * @param sessionId The identifier of the finished session.
     * @param automaticTransitionToNextPlayback Whether the session finished because of an automatic
     *     transition to the next playback item.
     */
    void onSessionFinished(
        EventTime eventTime, String sessionId, boolean automaticTransitionToNextPlayback);
  }

  /**
   * Sets the listener to be notified of session updates. Must be called before the session manager
   * is used.
   *
   * @param listener The {@link Listener} to be notified of session updates.
   */
  void setListener(Listener listener);

  /**
   * Returns the session identifier for the given media period id.
   *
   * <p>Note that this will reserve a new session identifier if it doesn't exist yet, but will not
   * call any {@link Listener} callbacks.
   *
   * @param timeline The timeline, {@code mediaPeriodId} is part of.
   * @param mediaPeriodId A {@link MediaSource.MediaPeriodId}.
   */
  String getSessionForMediaPeriodId(Timeline timeline, MediaSource.MediaPeriodId mediaPeriodId);

  /**
   * Returns whether an event time belong to a session.
   *
   * @param eventTime The {@link EventTime}.
   * @param sessionId A session identifier.
   * @return Whether the event belongs to the specified session.
   */
  boolean belongsToSession(EventTime eventTime, String sessionId);

  /**
   * Updates or creates sessions based on a player {@link EventTime}.
   *
   * <p>Call {@link #updateSessionsWithTimelineChange(EventTime)} or {@link
   * #updateSessionsWithDiscontinuity(EventTime, int)} if the event is a {@link Timeline} change or
   * a position discontinuity respectively.
   *
   * @param eventTime The {@link EventTime}.
   */
  void updateSessions(EventTime eventTime);

  /**
   * Updates or creates sessions based on a {@link Timeline} change at {@link EventTime}.
   *
   * <p>Should be called instead of {@link #updateSessions(EventTime)} if a {@link Timeline} change
   * occurred.
   *
   * @param eventTime The {@link EventTime} with the timeline change.
   */
  void updateSessionsWithTimelineChange(EventTime eventTime);

  /**
   * Updates or creates sessions based on a position discontinuity at {@link EventTime}.
   *
   * <p>Should be called instead of {@link #updateSessions(EventTime)} if a position discontinuity
   * occurred.
   *
   * @param eventTime The {@link EventTime} of the position discontinuity.
   * @param reason The {@link Player.DiscontinuityReason}.
   */
  void updateSessionsWithDiscontinuity(EventTime eventTime, @Player.DiscontinuityReason int reason);

  /**
   * Finishes all existing sessions and calls their respective {@link
   * Listener#onSessionFinished(EventTime, String, boolean)} callback.
   *
   * @param eventTime The event time at which sessions are finished.
   */
  void finishAllSessions(EventTime eventTime);
}
