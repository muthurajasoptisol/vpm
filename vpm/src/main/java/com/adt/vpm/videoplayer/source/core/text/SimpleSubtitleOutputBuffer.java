/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.text;

/**
 * A {@link SubtitleOutputBuffer} for decoders that extend {@link SimpleSubtitleDecoder}.
 */
/* package */ final class SimpleSubtitleOutputBuffer extends SubtitleOutputBuffer {

  private final Owner<SubtitleOutputBuffer> owner;

  /** @param owner The decoder that owns this buffer. */
  public SimpleSubtitleOutputBuffer(Owner<SubtitleOutputBuffer> owner) {
    super();
    this.owner = owner;
  }

  @Override
  public final void release() {
    owner.releaseOutputBuffer(this);
  }

}
