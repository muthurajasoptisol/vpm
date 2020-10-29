/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.video;

import androidx.annotation.Nullable;
import com.adt.vpm.videoplayer.source.common.Format;
import com.adt.vpm.videoplayer.source.common.decoder.DecoderInputBuffer;

/** Input buffer to a video decoder. */
public class VideoDecoderInputBuffer extends DecoderInputBuffer {

  @Nullable public Format format;

  /**
   * Creates a new instance.
   *
   * @param bufferReplacementMode Determines the behavior of {@link #ensureSpaceForWrite(int)}. One
   *     of {@link #BUFFER_REPLACEMENT_MODE_DISABLED}, {@link #BUFFER_REPLACEMENT_MODE_NORMAL} and
   *     {@link #BUFFER_REPLACEMENT_MODE_DIRECT}.
   */
  public VideoDecoderInputBuffer(@BufferReplacementMode int bufferReplacementMode) {
    super(bufferReplacementMode);
  }

  /**
   * Creates a new instance.
   *
   * @param bufferReplacementMode Determines the behavior of {@link #ensureSpaceForWrite(int)}. One
   *     of {@link #BUFFER_REPLACEMENT_MODE_DISABLED}, {@link #BUFFER_REPLACEMENT_MODE_NORMAL} and
   *     {@link #BUFFER_REPLACEMENT_MODE_DIRECT}.
   * @param paddingSize If non-zero, {@link #ensureSpaceForWrite(int)} will ensure that the buffer
   *     is this number of bytes larger than the requested length. This can be useful for decoders
   *     that consume data in fixed size blocks, for efficiency. Setting the padding size to the
   *     decoder's fixed read size is necessary to prevent such a decoder from trying to read beyond
   *     the end of the buffer.
   */
  public VideoDecoderInputBuffer(
      @BufferReplacementMode int bufferReplacementMode, int paddingSize) {
    super(bufferReplacementMode, paddingSize);
  }
}
