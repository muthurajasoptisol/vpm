/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.source;

import androidx.annotation.IntDef;

import com.adt.vpm.videoplayer.source.core.FormatHolder;
import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.decoder.DecoderInputBuffer;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A stream of media samples (and associated format information).
 */
public interface SampleStream {

  /** Return values of {@link #readData(FormatHolder, DecoderInputBuffer, boolean)}. */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @IntDef({C.RESULT_NOTHING_READ, C.RESULT_FORMAT_READ, C.RESULT_BUFFER_READ})
  @interface ReadDataResult {}

  /**
   * Returns whether data is available to be read.
   * <p>
   * Note: If the stream has ended then a buffer with the end of stream flag can always be read from
   * {@link #readData(FormatHolder, DecoderInputBuffer, boolean)}. Hence an ended stream is always
   * ready.
   *
   * @return Whether data is available to be read.
   */
  boolean isReady();

  /**
   * Throws an error that's preventing data from being read. Does nothing if no such error exists.
   *
   * @throws IOException The underlying error.
   */
  void maybeThrowError() throws IOException;

  /**
   * Attempts to read from the stream.
   *
   * <p>If the stream has ended then {@link C#BUFFER_FLAG_END_OF_STREAM} flag is set on {@code
   * buffer} and {@link C#RESULT_BUFFER_READ} is returned. Else if no data is available then {@link
   * C#RESULT_NOTHING_READ} is returned. Else if the format of the media is changing or if {@code
   * formatRequired} is set then {@code formatHolder} is populated and {@link C#RESULT_FORMAT_READ}
   * is returned. Else {@code buffer} is populated and {@link C#RESULT_BUFFER_READ} is returned.
   *
   * @param formatHolder A {@link FormatHolder} to populate in the case of reading a format.
   * @param buffer A {@link DecoderInputBuffer} to populate in the case of reading a sample or the
   *     end of the stream. If the end of the stream has been reached, the {@link
   *     C#BUFFER_FLAG_END_OF_STREAM} flag will be set on the buffer. If a {@link
   *     DecoderInputBuffer#isFlagsOnly() flags-only} buffer is passed, then no {@link
   *     DecoderInputBuffer#data} will be read and the read position of the stream will not change,
   *     but the flags of the buffer will be populated.
   * @param formatRequired Whether the caller requires that the format of the stream be read even if
   *     it's not changing. A sample will never be read if set to true, however it is still possible
   *     for the end of stream or nothing to be read.
   * @return The status of read, one of {@link ReadDataResult}.
   */
  @ReadDataResult
  int readData(FormatHolder formatHolder, DecoderInputBuffer buffer, boolean formatRequired);

  /**
   * Attempts to skip to the keyframe before the specified position, or to the end of the stream if
   * {@code positionUs} is beyond it.
   *
   * @param positionUs The specified time.
   * @return The number of samples that were skipped.
   */
  int skipData(long positionUs);

}
