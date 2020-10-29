/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.drm;

import android.util.Pair;
import androidx.annotation.Nullable;
import com.adt.vpm.videoplayer.source.common.C;
import java.util.Map;

/**
 * Utility methods for Widevine.
 */
public final class WidevineUtil {

  /** Widevine specific key status field name for the remaining license duration, in seconds. */
  public static final String PROPERTY_LICENSE_DURATION_REMAINING = "LicenseDurationRemaining";
  /** Widevine specific key status field name for the remaining playback duration, in seconds. */
  public static final String PROPERTY_PLAYBACK_DURATION_REMAINING = "PlaybackDurationRemaining";

  private WidevineUtil() {}

  /**
   * Returns license and playback durations remaining in seconds.
   *
   * @param drmSession The drm session to query.
   * @return A {@link Pair} consisting of the remaining license and playback durations in seconds,
   *     or null if called before the session has been opened or after it's been released.
   */
  public static @Nullable Pair<Long, Long> getLicenseDurationRemainingSec(DrmSession drmSession) {
    Map<String, String> keyStatus = drmSession.queryKeyStatus();
    if (keyStatus == null) {
      return null;
    }
    return new Pair<>(getDurationRemainingSec(keyStatus, PROPERTY_LICENSE_DURATION_REMAINING),
        getDurationRemainingSec(keyStatus, PROPERTY_PLAYBACK_DURATION_REMAINING));
  }

  private static long getDurationRemainingSec(Map<String, String> keyStatus, String property) {
    if (keyStatus != null) {
      try {
        String value = keyStatus.get(property);
        if (value != null) {
          return Long.parseLong(value);
        }
      } catch (NumberFormatException e) {
        // do nothing.
      }
    }
    return C.TIME_UNSET;
  }

}
