/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.video;

/** Renders the {@link VideoDecoderOutputBuffer}. */
public interface VideoDecoderOutputBufferRenderer {

  /**
   * Sets the output buffer to be rendered. The renderer is responsible for releasing the buffer.
   *
   * @param outputBuffer The output buffer to be rendered.
   */
  void setOutputBuffer(VideoDecoderOutputBuffer outputBuffer);
}
