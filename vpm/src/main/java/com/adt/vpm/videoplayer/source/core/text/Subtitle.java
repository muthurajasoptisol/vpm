/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.text;

import com.adt.vpm.videoplayer.source.common.C;
import java.util.List;

/**
 * A subtitle consisting of timed {@link Cue}s.
 */
public interface Subtitle {

  /**
   * Returns the index of the first event that occurs after a given time (exclusive).
   *
   * @param timeUs The time in microseconds.
   * @return The index of the next event, or {@link C#INDEX_UNSET} if there are no events after the
   *     specified time.
   */
  int getNextEventTimeIndex(long timeUs);

  /**
   * Returns the number of event times, where events are defined as points in time at which the cues
   * returned by {@link #getCues(long)} changes.
   *
   * @return The number of event times.
   */
  int getEventTimeCount();

  /**
   * Returns the event time at a specified index.
   *
   * @param index The index of the event time to obtain.
   * @return The event time in microseconds.
   */
  long getEventTime(int index);

  /**
   * Retrieve the cues that should be displayed at a given time.
   *
   * @param timeUs The time in microseconds.
   * @return A list of cues that should be displayed, possibly empty.
   */
  List<Cue> getCues(long timeUs);

}
