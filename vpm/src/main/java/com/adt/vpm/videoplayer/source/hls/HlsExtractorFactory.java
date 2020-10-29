/*
 * Created by ADT author on 10/1/20 11:19 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/22/20 6:23 PM
 */
package com.adt.vpm.videoplayer.source.hls;

import android.net.Uri;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.common.Format;
import com.adt.vpm.videoplayer.source.common.util.TimestampAdjuster;
import com.adt.vpm.videoplayer.source.extractor.ExtractorInput;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Factory for HLS media chunk extractors.
 */
public interface HlsExtractorFactory {

  HlsExtractorFactory DEFAULT = new DefaultHlsExtractorFactory();

  /**
   * Creates an {@link Extractor} for extracting HLS media chunks.
   *
   * @param uri The URI of the media chunk.
   * @param format A {@link Format} associated with the chunk to extract.
   * @param muxedCaptionFormats List of muxed caption {@link Format}s. Null if no closed caption
   *     information is available in the master playlist.
   * @param timestampAdjuster Adjuster corresponding to the provided discontinuity sequence number.
   * @param responseHeaders The HTTP response headers associated with the media segment or
   *     initialization section to extract.
   * @param sniffingExtractorInput The first extractor input that will be passed to the returned
   *     extractor's {@link Extractor#read(ExtractorInput, PositionHolder)}. Must only be used to
   *     call {@link Extractor#sniff(ExtractorInput)}.
   * @return An {@link HlsMediaChunkExtractor}.
   * @throws IOException If an I/O error is encountered while sniffing.
   */
  HlsMediaChunkExtractor createExtractor(
          Uri uri,
          Format format,
          @Nullable List<Format> muxedCaptionFormats,
          TimestampAdjuster timestampAdjuster,
          Map<String, List<String>> responseHeaders,
          ExtractorInput sniffingExtractorInput)
      throws IOException;
}
