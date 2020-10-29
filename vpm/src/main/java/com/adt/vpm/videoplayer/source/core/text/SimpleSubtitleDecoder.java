/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.text;

import androidx.annotation.Nullable;
import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.core.decoder.SimpleDecoder;
import com.adt.vpm.videoplayer.source.common.util.Assertions;
import java.nio.ByteBuffer;

/**
 * Base class for subtitle parsers that use their own decode thread.
 */
public abstract class SimpleSubtitleDecoder extends
    SimpleDecoder<SubtitleInputBuffer, SubtitleOutputBuffer, SubtitleDecoderException> implements
    SubtitleDecoder {

  private final String name;

  /** @param name The name of the decoder. */
  @SuppressWarnings("nullness:method.invocation.invalid")
  protected SimpleSubtitleDecoder(String name) {
    super(new SubtitleInputBuffer[2], new SubtitleOutputBuffer[2]);
    this.name = name;
    setInitialInputBufferSize(1024);
  }

  @Override
  public final String getName() {
    return name;
  }

  @Override
  public void setPositionUs(long timeUs) {
    // Do nothing
  }

  @Override
  protected final SubtitleInputBuffer createInputBuffer() {
    return new SubtitleInputBuffer();
  }

  @Override
  protected final SubtitleOutputBuffer createOutputBuffer() {
    return new SimpleSubtitleOutputBuffer(this::releaseOutputBuffer);
  }

  @Override
  protected final SubtitleDecoderException createUnexpectedDecodeException(Throwable error) {
    return new SubtitleDecoderException("Unexpected decode error", error);
  }

  @SuppressWarnings("ByteBufferBackingArray")
  @Override
  @Nullable
  protected final SubtitleDecoderException decode(
      SubtitleInputBuffer inputBuffer, SubtitleOutputBuffer outputBuffer, boolean reset) {
    try {
      ByteBuffer inputData = Assertions.checkNotNull(inputBuffer.data);
      Subtitle subtitle = decode(inputData.array(), inputData.limit(), reset);
      outputBuffer.setContent(inputBuffer.timeUs, subtitle, inputBuffer.subsampleOffsetUs);
      // Clear BUFFER_FLAG_DECODE_ONLY (see [Internal: b/27893809]).
      outputBuffer.clearFlag(C.BUFFER_FLAG_DECODE_ONLY);
      return null;
    } catch (SubtitleDecoderException e) {
      return e;
    }
  }

  /**
   * Decodes data into a {@link Subtitle}.
   *
   * @param data An array holding the data to be decoded, starting at position 0.
   * @param size The size of the data to be decoded.
   * @param reset Whether the decoder must be reset before decoding.
   * @return The decoded {@link Subtitle}.
   * @throws SubtitleDecoderException If a decoding error occurs.
   */
  protected abstract Subtitle decode(byte[] data, int size, boolean reset)
      throws SubtitleDecoderException;

}
