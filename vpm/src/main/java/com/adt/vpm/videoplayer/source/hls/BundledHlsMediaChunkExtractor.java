/*
 * Created by ADT author on 10/1/20 11:19 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/22/20 6:23 PM
 */
package com.adt.vpm.videoplayer.source.hls;

import androidx.annotation.VisibleForTesting;

import com.adt.vpm.videoplayer.source.common.Format;
import com.adt.vpm.videoplayer.source.common.util.Assertions;
import com.adt.vpm.videoplayer.source.common.util.TimestampAdjuster;
import com.adt.vpm.videoplayer.source.extractor.Extractor;
import com.adt.vpm.videoplayer.source.extractor.ExtractorInput;
import com.adt.vpm.videoplayer.source.extractor.ExtractorOutput;
import com.adt.vpm.videoplayer.source.extractor.PositionHolder;
import com.adt.vpm.videoplayer.source.extractor.mp3.Mp3Extractor;
import com.adt.vpm.videoplayer.source.extractor.mp4.FragmentedMp4Extractor;
import com.adt.vpm.videoplayer.source.extractor.ts.Ac3Extractor;
import com.adt.vpm.videoplayer.source.extractor.ts.Ac4Extractor;
import com.adt.vpm.videoplayer.source.extractor.ts.AdtsExtractor;
import com.adt.vpm.videoplayer.source.extractor.ts.TsExtractor;

import java.io.IOException;

/**
 * {@link HlsMediaChunkExtractor} implementation that uses ExoPlayer app-bundled {@link Extractor
 * Extractors}.
 */
public final class BundledHlsMediaChunkExtractor implements HlsMediaChunkExtractor {

  private static final PositionHolder POSITION_HOLDER = new PositionHolder();

  @VisibleForTesting /* package */ final Extractor extractor;
  private final Format masterPlaylistFormat;
  private final TimestampAdjuster timestampAdjuster;

  /**
   * Creates a new instance.
   *
   * @param extractor The underlying {@link Extractor}.
   * @param masterPlaylistFormat The {@link Format} obtained from the master playlist.
   * @param timestampAdjuster A {@link TimestampAdjuster} to adjust sample timestamps.
   */
  public BundledHlsMediaChunkExtractor(
      Extractor extractor, Format masterPlaylistFormat, TimestampAdjuster timestampAdjuster) {
    this.extractor = extractor;
    this.masterPlaylistFormat = masterPlaylistFormat;
    this.timestampAdjuster = timestampAdjuster;
  }

  @Override
  public void init(ExtractorOutput extractorOutput) {
    extractor.init(extractorOutput);
  }

  @Override
  public boolean read(ExtractorInput extractorInput) throws IOException {
    return extractor.read(extractorInput, POSITION_HOLDER) == Extractor.RESULT_CONTINUE;
  }

  @Override
  public boolean isPackedAudioExtractor() {
    return extractor instanceof AdtsExtractor
        || extractor instanceof Ac3Extractor
        || extractor instanceof Ac4Extractor
        || extractor instanceof Mp3Extractor;
  }

  @Override
  public boolean isReusable() {
    return extractor instanceof TsExtractor || extractor instanceof FragmentedMp4Extractor;
  }

  @Override
  public HlsMediaChunkExtractor recreate() {
    Assertions.checkState(!isReusable());
    Extractor newExtractorInstance;
    if (extractor instanceof WebvttExtractor) {
      newExtractorInstance = new WebvttExtractor(masterPlaylistFormat.language, timestampAdjuster);
    } else if (extractor instanceof AdtsExtractor) {
      newExtractorInstance = new AdtsExtractor();
    } else if (extractor instanceof Ac3Extractor) {
      newExtractorInstance = new Ac3Extractor();
    } else if (extractor instanceof Ac4Extractor) {
      newExtractorInstance = new Ac4Extractor();
    } else if (extractor instanceof Mp3Extractor) {
      newExtractorInstance = new Mp3Extractor();
    } else {
      throw new IllegalStateException(
          "Unexpected extractor type for recreation: " + extractor.getClass().getSimpleName());
    }
    return new BundledHlsMediaChunkExtractor(
        newExtractorInstance, masterPlaylistFormat, timestampAdjuster);
  }
}
