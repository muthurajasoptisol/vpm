/*
 * Created by ADT author on 10/19/20 4:50 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/22/20 6:23 PM
 */
package com.adt.vpm.videoplayer.source.ext.cast;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.Format;
import com.google.android.gms.cast.CastStatusCodes;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaTrack;

/**
 * Utility methods for ExoPlayer/Cast integration.
 */
/* package */ final class CastUtils {

  /**
   * Returns the duration in microseconds advertised by a media info, or {@link C#TIME_UNSET} if
   * unknown or not applicable.
   *
   * @param mediaInfo The media info to get the duration from.
   * @return The duration in microseconds, or {@link C#TIME_UNSET} if unknown or not applicable.
   */
  public static long getStreamDurationUs(@Nullable MediaInfo mediaInfo) {
    if (mediaInfo == null) {
      return C.TIME_UNSET;
    }
    long durationMs = mediaInfo.getStreamDuration();
    return durationMs != MediaInfo.UNKNOWN_DURATION ? C.msToUs(durationMs) : C.TIME_UNSET;
  }

  /**
   * Returns a descriptive log string for the given {@code statusCode}, or "Unknown." if not one of
   * {@link CastStatusCodes}.
   *
   * @param statusCode A Cast API status code.
   * @return A descriptive log string for the given {@code statusCode}, or "Unknown." if not one of
   *     {@link CastStatusCodes}.
   */
  public static String getLogString(int statusCode) {
    switch (statusCode) {
      case CastStatusCodes.APPLICATION_NOT_FOUND:
        return "A requested application could not be found.";
      case CastStatusCodes.APPLICATION_NOT_RUNNING:
        return "A requested application is not currently running.";
      case CastStatusCodes.AUTHENTICATION_FAILED:
        return "Authentication failure.";
      case CastStatusCodes.CANCELED:
        return "An in-progress request has been canceled, most likely because another action has "
            + "preempted it.";
      case CastStatusCodes.ERROR_SERVICE_CREATION_FAILED:
        return "The Cast Remote Display service could not be created.";
      case CastStatusCodes.ERROR_SERVICE_DISCONNECTED:
        return "The Cast Remote Display service was disconnected.";
      case CastStatusCodes.FAILED:
        return "The in-progress request failed.";
      case CastStatusCodes.INTERNAL_ERROR:
        return "An internal error has occurred.";
      case CastStatusCodes.INTERRUPTED:
        return "A blocking call was interrupted while waiting and did not run to completion.";
      case CastStatusCodes.INVALID_REQUEST:
        return "An invalid request was made.";
      case CastStatusCodes.MESSAGE_SEND_BUFFER_TOO_FULL:
        return "A message could not be sent because there is not enough room in the send buffer at "
            + "this time.";
      case CastStatusCodes.MESSAGE_TOO_LARGE:
        return "A message could not be sent because it is too large.";
      case CastStatusCodes.NETWORK_ERROR:
        return "Network I/O error.";
      case CastStatusCodes.NOT_ALLOWED:
        return "The request was disallowed and could not be completed.";
      case CastStatusCodes.REPLACED:
        return "The request's progress is no longer being tracked because another request of the "
            + "same type has been made before the first request completed.";
      case CastStatusCodes.SUCCESS:
        return "Success.";
      case CastStatusCodes.TIMEOUT:
        return "An operation has timed out.";
      case CastStatusCodes.UNKNOWN_ERROR:
        return "An unknown, unexpected error has occurred.";
      default:
        return CastStatusCodes.getStatusCodeString(statusCode);
    }
  }

  /**
   * Creates a {@link Format} instance containing all information contained in the given
   * {@link MediaTrack} object.
   *
   * @param mediaTrack The {@link MediaTrack}.
   * @return The equivalent {@link Format}.
   */
  public static Format mediaTrackToFormat(MediaTrack mediaTrack) {
    return new Format.Builder()
        .setId(mediaTrack.getContentId())
        .setContainerMimeType(mediaTrack.getContentType())
        .setLanguage(mediaTrack.getLanguage())
        .build();
  }

  private CastUtils() {}

}
