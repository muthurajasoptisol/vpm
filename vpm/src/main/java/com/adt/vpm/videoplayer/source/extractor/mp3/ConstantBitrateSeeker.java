/*
 * Created by ADT author on 9/29/20 5:06 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 5:06 PM
 */
package com.adt.vpm.videoplayer.source.extractor.mp3;

import com.adt.vpm.videoplayer.source.extractor.ConstantBitrateSeekMap;
import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.audio.MpegAudioUtil;

/**
 * MP3 seeker that doesn't rely on metadata and seeks assuming the source has a constant bitrate.
 */
/* package */ final class ConstantBitrateSeeker extends ConstantBitrateSeekMap implements Seeker {

  /**
   * @param inputLength The length of the stream in bytes, or {@link C#LENGTH_UNSET} if unknown.
   * @param firstFramePosition The position of the first frame in the stream.
   * @param mpegAudioHeader The MPEG audio header associated with the first frame.
   */
  public ConstantBitrateSeeker(
      long inputLength, long firstFramePosition, MpegAudioUtil.Header mpegAudioHeader) {
    // Set the seeker frame size to the size of the first frame (even though some constant bitrate
    // streams have variable frame sizes) to avoid the need to re-synchronize for constant frame
    // size streams.
    super(inputLength, firstFramePosition, mpegAudioHeader.bitrate, mpegAudioHeader.frameSize);
  }

  @Override
  public long getTimeUs(long position) {
    return getTimeUsAtPosition(position);
  }

  @Override
  public long getDataEndPosition() {
    return C.POSITION_UNSET;
  }
}
