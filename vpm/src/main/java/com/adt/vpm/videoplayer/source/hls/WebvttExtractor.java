/*
 * Created by ADT author on 10/1/20 11:19 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/22/20 6:23 PM
 */
package com.adt.vpm.videoplayer.source.hls;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.Format;
import com.adt.vpm.videoplayer.source.common.ParserException;
import com.adt.vpm.videoplayer.source.common.util.Assertions;
import com.adt.vpm.videoplayer.source.common.util.MimeTypes;
import com.adt.vpm.videoplayer.source.common.util.ParsableByteArray;
import com.adt.vpm.videoplayer.source.common.util.TimestampAdjuster;
import com.adt.vpm.videoplayer.source.core.text.webvtt.WebvttParserUtil;
import com.adt.vpm.videoplayer.source.extractor.Extractor;
import com.adt.vpm.videoplayer.source.extractor.ExtractorInput;
import com.adt.vpm.videoplayer.source.extractor.ExtractorOutput;
import com.adt.vpm.videoplayer.source.extractor.PositionHolder;
import com.adt.vpm.videoplayer.source.extractor.SeekMap;
import com.adt.vpm.videoplayer.source.extractor.TrackOutput;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A special purpose extractor for WebVTT content in HLS.
 *
 * <p>This extractor passes through non-empty WebVTT files untouched, however derives the correct
 * sample timestamp for each by sniffing the X-TIMESTAMP-MAP header along with the start timestamp
 * of the first cue header. Empty WebVTT files are not passed through, since it's not possible to
 * derive a sample timestamp in this case.
 */
public final class WebvttExtractor implements Extractor {

  private static final Pattern LOCAL_TIMESTAMP = Pattern.compile("LOCAL:([^,]+)");
  private static final Pattern MEDIA_TIMESTAMP = Pattern.compile("MPEGTS:(-?\\d+)");
  private static final int HEADER_MIN_LENGTH = 6 /* "WEBVTT" */;
  private static final int HEADER_MAX_LENGTH = 3 /* optional Byte Order Mark */ + HEADER_MIN_LENGTH;

  @Nullable private final String language;
  private final TimestampAdjuster timestampAdjuster;
  private final ParsableByteArray sampleDataWrapper;

  private @MonotonicNonNull ExtractorOutput output;

  private byte[] sampleData;
  private int sampleSize;

  public WebvttExtractor(@Nullable String language, TimestampAdjuster timestampAdjuster) {
    this.language = language;
    this.timestampAdjuster = timestampAdjuster;
    this.sampleDataWrapper = new ParsableByteArray();
    sampleData = new byte[1024];
  }

  // Extractor implementation.

  @Override
  public boolean sniff(ExtractorInput input) throws IOException {
    // Check whether there is a header without BOM.
    input.peekFully(
        sampleData, /* offset= */ 0, /* length= */ HEADER_MIN_LENGTH, /* allowEndOfInput= */ false);
    sampleDataWrapper.reset(sampleData, HEADER_MIN_LENGTH);
    if (WebvttParserUtil.isWebvttHeaderLine(sampleDataWrapper)) {
      return true;
    }
    // The header did not match, try including the BOM.
    input.peekFully(
        sampleData,
        /* offset= */ HEADER_MIN_LENGTH,
        HEADER_MAX_LENGTH - HEADER_MIN_LENGTH,
        /* allowEndOfInput= */ false);
    sampleDataWrapper.reset(sampleData, HEADER_MAX_LENGTH);
    return WebvttParserUtil.isWebvttHeaderLine(sampleDataWrapper);
  }

  @Override
  public void init(ExtractorOutput output) {
    this.output = output;
    output.seekMap(new SeekMap.Unseekable(C.TIME_UNSET));
  }

  @Override
  public void seek(long position, long timeUs) {
    // This extractor is only used for the HLS use case, which should not call this method.
    throw new IllegalStateException();
  }

  @Override
  public void release() {
    // Do nothing
  }

  @Override
  public int read(ExtractorInput input, PositionHolder seekPosition) throws IOException {
    // output == null suggests init() hasn't been called
    Assertions.checkNotNull(output);
    int currentFileSize = (int) input.getLength();

    // Increase the size of sampleData if necessary.
    if (sampleSize == sampleData.length) {
      sampleData = Arrays.copyOf(sampleData,
          (currentFileSize != C.LENGTH_UNSET ? currentFileSize : sampleData.length) * 3 / 2);
    }

    // Consume to the input.
    int bytesRead = input.read(sampleData, sampleSize, sampleData.length - sampleSize);
    if (bytesRead != C.RESULT_END_OF_INPUT) {
      sampleSize += bytesRead;
      if (currentFileSize == C.LENGTH_UNSET || sampleSize != currentFileSize) {
        return Extractor.RESULT_CONTINUE;
      }
    }

    // We've reached the end of the input, which corresponds to the end of the current file.
    processSample();
    return Extractor.RESULT_END_OF_INPUT;
  }

  @RequiresNonNull("output")
  private void processSample() throws ParserException {
    ParsableByteArray webvttData = new ParsableByteArray(sampleData);

    // Validate the first line of the header.
    WebvttParserUtil.validateWebvttHeaderLine(webvttData);

    // Defaults to use if the header doesn't contain an X-TIMESTAMP-MAP header.
    long vttTimestampUs = 0;
    long tsTimestampUs = 0;

    // Parse the remainder of the header looking for X-TIMESTAMP-MAP.
    for (String line = webvttData.readLine();
        !TextUtils.isEmpty(line);
        line = webvttData.readLine()) {
      if (line.startsWith("X-TIMESTAMP-MAP")) {
        Matcher localTimestampMatcher = LOCAL_TIMESTAMP.matcher(line);
        if (!localTimestampMatcher.find()) {
          throw new ParserException("X-TIMESTAMP-MAP doesn't contain local timestamp: " + line);
        }
        Matcher mediaTimestampMatcher = MEDIA_TIMESTAMP.matcher(line);
        if (!mediaTimestampMatcher.find()) {
          throw new ParserException("X-TIMESTAMP-MAP doesn't contain media timestamp: " + line);
        }
        vttTimestampUs =
            WebvttParserUtil.parseTimestampUs(
                Assertions.checkNotNull(localTimestampMatcher.group(1)));
        tsTimestampUs =
            TimestampAdjuster.ptsToUs(
                Long.parseLong(Assertions.checkNotNull(mediaTimestampMatcher.group(1))));
      }
    }

    // Find the first cue header and parse the start time.
    Matcher cueHeaderMatcher = WebvttParserUtil.findNextCueHeader(webvttData);
    if (cueHeaderMatcher == null) {
      // No cues found. Don't output a sample, but still output a corresponding track.
      buildTrackOutput(0);
      return;
    }

    long firstCueTimeUs =
        WebvttParserUtil.parseTimestampUs(Assertions.checkNotNull(cueHeaderMatcher.group(1)));
    long sampleTimeUs =
        timestampAdjuster.adjustTsTimestamp(
            TimestampAdjuster.usToWrappedPts(firstCueTimeUs + tsTimestampUs - vttTimestampUs));
    long subsampleOffsetUs = sampleTimeUs - firstCueTimeUs;
    // Output the track.
    TrackOutput trackOutput = buildTrackOutput(subsampleOffsetUs);
    // Output the sample.
    sampleDataWrapper.reset(sampleData, sampleSize);
    trackOutput.sampleData(sampleDataWrapper, sampleSize);
    trackOutput.sampleMetadata(sampleTimeUs, C.BUFFER_FLAG_KEY_FRAME, sampleSize, 0, null);
  }

  @RequiresNonNull("output")
  private TrackOutput buildTrackOutput(long subsampleOffsetUs) {
    TrackOutput trackOutput = output.track(0, C.TRACK_TYPE_TEXT);
    trackOutput.format(
        new Format.Builder()
            .setSampleMimeType(MimeTypes.TEXT_VTT)
            .setLanguage(language)
            .setSubsampleOffsetUs(subsampleOffsetUs)
            .build());
    output.endTracks();
    return trackOutput;
  }

}
