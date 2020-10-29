/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.source;

import android.net.Uri;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.upstream.DataReader;
import com.adt.vpm.videoplayer.source.common.util.Assertions;
import com.adt.vpm.videoplayer.source.common.util.Util;
import com.adt.vpm.videoplayer.source.extractor.DefaultExtractorInput;
import com.adt.vpm.videoplayer.source.extractor.Extractor;
import com.adt.vpm.videoplayer.source.extractor.ExtractorInput;
import com.adt.vpm.videoplayer.source.extractor.ExtractorOutput;
import com.adt.vpm.videoplayer.source.extractor.ExtractorsFactory;
import com.adt.vpm.videoplayer.source.extractor.PositionHolder;
import com.adt.vpm.videoplayer.source.extractor.mp3.Mp3Extractor;

import java.io.EOFException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * {@link ProgressiveMediaExtractor} built on top of {@link Extractor} instances, whose
 * implementation classes are bundled in the app.
 */
/* package */ final class BundledExtractorsAdapter implements ProgressiveMediaExtractor {

  private final ExtractorsFactory extractorsFactory;

  @Nullable private Extractor extractor;
  @Nullable private ExtractorInput extractorInput;

  /**
   * Creates a holder that will select an extractor and initialize it using the specified output.
   *
   * @param extractorsFactory The {@link ExtractorsFactory} providing the extractors to choose from.
   */
  public BundledExtractorsAdapter(ExtractorsFactory extractorsFactory) {
    this.extractorsFactory = extractorsFactory;
  }

  @Override
  public void init(
      DataReader dataReader,
      Uri uri,
      Map<String, List<String>> responseHeaders,
      long position,
      long length,
      ExtractorOutput output)
      throws IOException {
    ExtractorInput extractorInput = new DefaultExtractorInput(dataReader, position, length);
    this.extractorInput = extractorInput;
    if (extractor != null) {
      return;
    }
    Extractor[] extractors = extractorsFactory.createExtractors(uri, responseHeaders);
    if (extractors.length == 1) {
      this.extractor = extractors[0];
    } else {
      for (Extractor extractor : extractors) {
        try {
          if (extractor.sniff(extractorInput)) {
            this.extractor = extractor;
            break;
          }
        } catch (EOFException e) {
          // Do nothing.
        } finally {
          Assertions.checkState(this.extractor != null || extractorInput.getPosition() == position);
          extractorInput.resetPeekPosition();
        }
      }
      if (extractor == null) {
        throw new UnrecognizedInputFormatException(
            "None of the available extractors ("
                + Util.getCommaDelimitedSimpleClassNames(extractors)
                + ") could read the stream.",
            Assertions.checkNotNull(uri));
      }
    }
    extractor.init(output);
  }

  @Override
  public void release() {
    if (extractor != null) {
      extractor.release();
      extractor = null;
    }
    extractorInput = null;
  }

  @Override
  public void disableSeekingOnMp3Streams() {
    if (extractor instanceof Mp3Extractor) {
      ((Mp3Extractor) extractor).disableSeeking();
    }
  }

  @Override
  public long getCurrentInputPosition() {
    return extractorInput != null ? extractorInput.getPosition() : C.POSITION_UNSET;
  }

  @Override
  public void seek(long position, long seekTimeUs) {
    Assertions.checkNotNull(extractor).seek(position, seekTimeUs);
  }

  @Override
  public int read(PositionHolder positionHolder) throws IOException {
    return Assertions.checkNotNull(extractor)
        .read(Assertions.checkNotNull(extractorInput), positionHolder);
  }
}
