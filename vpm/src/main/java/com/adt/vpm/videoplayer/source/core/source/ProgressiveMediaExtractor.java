/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.source;

import android.net.Uri;

import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.upstream.DataReader;
import com.adt.vpm.videoplayer.source.extractor.ExtractorOutput;
import com.adt.vpm.videoplayer.source.extractor.PositionHolder;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/** Extracts the contents of a container file from a progressive media stream. */
/* package */ interface ProgressiveMediaExtractor {

  /**
   * Initializes the underlying infrastructure for reading from the input.
   *
   * @param dataReader The {@link DataReader} from which data should be read.
   * @param uri The {@link Uri} from which the media is obtained.
   * @param responseHeaders The response headers of the media, or an empty map if there are none.
   * @param position The initial position of the {@code dataReader} in the stream.
   * @param length The length of the stream, or {@link C#LENGTH_UNSET} if length is unknown.
   * @param output The {@link ExtractorOutput} that will be used to initialize the selected
   *     extractor.
   * @throws UnrecognizedInputFormatException Thrown if the input format could not be detected.
   * @throws IOException Thrown if the input could not be read.
   */
  void init(
      DataReader dataReader,
      Uri uri,
      Map<String, List<String>> responseHeaders,
      long position,
      long length,
      ExtractorOutput output)
      throws IOException;

  /** Releases any held resources. */
  void release();

  /**
   * Disables seeking in MP3 streams.
   *
   * <p>MP3 live streams commonly have seekable metadata, despite being unseekable.
   */
  void disableSeekingOnMp3Streams();

  /**
   * Returns the current read position in the input stream, or {@link C#POSITION_UNSET} if no input
   * is available.
   */
  long getCurrentInputPosition();

  /**
   * Notifies the extracting infrastructure that a seek has occurred.
   *
   * @param position The byte offset in the stream from which data will be provided.
   * @param seekTimeUs The seek time in microseconds.
   */
  void seek(long position, long seekTimeUs);

  /**
   * Extracts data starting at the current input stream position.
   *
   * @param positionHolder If {@link Extractor#RESULT_SEEK} is returned, this holder is updated to
   *     hold the position of the required data.
   * @return One of the {@link Extractor}{@code .RESULT_*} values.
   * @throws IOException If an error occurred reading from the input.
   */
  int read(PositionHolder positionHolder) throws IOException;
}
