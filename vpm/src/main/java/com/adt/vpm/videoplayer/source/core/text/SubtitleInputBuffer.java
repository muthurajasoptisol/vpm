/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.text;

import com.adt.vpm.videoplayer.source.common.Format;
import com.adt.vpm.videoplayer.source.common.decoder.DecoderInputBuffer;

/** A {@link DecoderInputBuffer} for a {@link SubtitleDecoder}. */
public class SubtitleInputBuffer extends DecoderInputBuffer {

  /**
   * An offset that must be added to the subtitle's event times after it's been decoded, or
   * {@link Format#OFFSET_SAMPLE_RELATIVE} if {@link #timeUs} should be added.
   */
  public long subsampleOffsetUs;

  public SubtitleInputBuffer() {
    super(DecoderInputBuffer.BUFFER_REPLACEMENT_MODE_NORMAL);
  }

}
