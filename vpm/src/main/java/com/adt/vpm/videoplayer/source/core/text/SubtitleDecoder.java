/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.text;

import com.adt.vpm.videoplayer.source.core.decoder.Decoder;

/**
 * Decodes {@link Subtitle}s from {@link SubtitleInputBuffer}s.
 */
public interface SubtitleDecoder extends
    Decoder<SubtitleInputBuffer, SubtitleOutputBuffer, SubtitleDecoderException> {

  /**
   * Informs the decoder of the current playback position.
   * <p>
   * Must be called prior to each attempt to dequeue output buffers from the decoder.
   *
   * @param positionUs The current playback position in microseconds.
   */
  void setPositionUs(long positionUs);

}
