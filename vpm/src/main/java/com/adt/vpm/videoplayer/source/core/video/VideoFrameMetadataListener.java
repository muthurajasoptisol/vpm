/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.video;

import android.media.MediaFormat;
import androidx.annotation.Nullable;
import com.adt.vpm.videoplayer.source.common.Format;

/** A listener for metadata corresponding to video frames being rendered. */
public interface VideoFrameMetadataListener {
  /**
   * Called on the playback thread when a video frame is about to be rendered.
   *
   * @param presentationTimeUs The presentation time of the frame, in microseconds.
   * @param releaseTimeNs The wallclock time at which the frame should be displayed, in nanoseconds.
   *     If the platform API version of the device is less than 21, then this is a best effort.
   * @param format The format associated with the frame.
   * @param mediaFormat The framework media format associated with the frame, or {@code null} if not
   *     known or not applicable (e.g., because the frame was not output by a {@link
   *     android.media.MediaCodec MediaCodec}).
   */
  void onVideoFrameAboutToBeRendered(
      long presentationTimeUs,
      long releaseTimeNs,
      Format format,
      @Nullable MediaFormat mediaFormat);
}
