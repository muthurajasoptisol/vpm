/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.source.chunk;

import android.util.SparseArray;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.Format;
import com.adt.vpm.videoplayer.source.common.upstream.DataReader;
import com.adt.vpm.videoplayer.source.common.util.Assertions;
import com.adt.vpm.videoplayer.source.common.util.ParsableByteArray;
import com.adt.vpm.videoplayer.source.extractor.ChunkIndex;
import com.adt.vpm.videoplayer.source.extractor.DummyTrackOutput;
import com.adt.vpm.videoplayer.source.extractor.Extractor;
import com.adt.vpm.videoplayer.source.extractor.ExtractorInput;
import com.adt.vpm.videoplayer.source.extractor.ExtractorOutput;
import com.adt.vpm.videoplayer.source.extractor.PositionHolder;
import com.adt.vpm.videoplayer.source.extractor.SeekMap;
import com.adt.vpm.videoplayer.source.extractor.TrackOutput;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.io.IOException;

import static com.adt.vpm.videoplayer.source.common.util.Util.castNonNull;

/**
 * {@link ChunkExtractor} implementation that uses ExoPlayer app-bundled {@link Extractor
 * Extractors}.
 */
public final class BundledChunkExtractor implements ExtractorOutput, ChunkExtractor {

  private static final PositionHolder POSITION_HOLDER = new PositionHolder();

  private final Extractor extractor;
  private final int primaryTrackType;
  private final Format primaryTrackManifestFormat;
  private final SparseArray<BindingTrackOutput> bindingTrackOutputs;

  private boolean extractorInitialized;
  @Nullable private TrackOutputProvider trackOutputProvider;
  private long endTimeUs;
  private @MonotonicNonNull SeekMap seekMap;
  private Format @MonotonicNonNull [] sampleFormats;

  /**
   * Creates an instance.
   *
   * @param extractor The extractor to wrap.
   * @param primaryTrackType The type of the primary track. Typically one of the {@link
   *     com.adt.vpm.videoplayer.source.common.C} {@code TRACK_TYPE_*} constants.
   * @param primaryTrackManifestFormat A manifest defined {@link Format} whose data should be merged
   *     into any sample {@link Format} output from the {@link Extractor} for the primary track.
   */
  public BundledChunkExtractor(
      Extractor extractor, int primaryTrackType, Format primaryTrackManifestFormat) {
    this.extractor = extractor;
    this.primaryTrackType = primaryTrackType;
    this.primaryTrackManifestFormat = primaryTrackManifestFormat;
    bindingTrackOutputs = new SparseArray<>();
  }

  // ChunkExtractor implementation.

  @Override
  @Nullable
  public ChunkIndex getChunkIndex() {
    return seekMap instanceof ChunkIndex ? (ChunkIndex) seekMap : null;
  }

  @Override
  @Nullable
  public Format[] getSampleFormats() {
    return sampleFormats;
  }

  @Override
  public void init(
      @Nullable TrackOutputProvider trackOutputProvider, long startTimeUs, long endTimeUs) {
    this.trackOutputProvider = trackOutputProvider;
    this.endTimeUs = endTimeUs;
    if (!extractorInitialized) {
      extractor.init(this);
      if (startTimeUs != C.TIME_UNSET) {
        extractor.seek(/* position= */ 0, startTimeUs);
      }
      extractorInitialized = true;
    } else {
      extractor.seek(/* position= */ 0, startTimeUs == C.TIME_UNSET ? 0 : startTimeUs);
      for (int i = 0; i < bindingTrackOutputs.size(); i++) {
        bindingTrackOutputs.valueAt(i).bind(trackOutputProvider, endTimeUs);
      }
    }
  }

  @Override
  public void release() {
    extractor.release();
  }

  @Override
  public boolean read(ExtractorInput input) throws IOException {
    int result = extractor.read(input, POSITION_HOLDER);
    Assertions.checkState(result != Extractor.RESULT_SEEK);
    return result == Extractor.RESULT_CONTINUE;
  }

  // ExtractorOutput implementation.

  @Override
  public TrackOutput track(int id, int type) {
    BindingTrackOutput bindingTrackOutput = bindingTrackOutputs.get(id);
    if (bindingTrackOutput == null) {
      // Assert that if we're seeing a new track we have not seen endTracks.
      Assertions.checkState(sampleFormats == null);
      // TODO: Manifest formats for embedded tracks should also be passed here.
      bindingTrackOutput = new BindingTrackOutput(id, type,
          type == primaryTrackType ? primaryTrackManifestFormat : null);
      bindingTrackOutput.bind(trackOutputProvider, endTimeUs);
      bindingTrackOutputs.put(id, bindingTrackOutput);
    }
    return bindingTrackOutput;
  }

  @Override
  public void endTracks() {
    Format[] sampleFormats = new Format[bindingTrackOutputs.size()];
    for (int i = 0; i < bindingTrackOutputs.size(); i++) {
      sampleFormats[i] = Assertions.checkStateNotNull(bindingTrackOutputs.valueAt(i).sampleFormat);
    }
    this.sampleFormats = sampleFormats;
  }

  @Override
  public void seekMap(SeekMap seekMap) {
    this.seekMap = seekMap;
  }

  // Internal logic.

  private static final class BindingTrackOutput implements TrackOutput {

    private final int id;
    private final int type;
    @Nullable private final Format manifestFormat;
    private final DummyTrackOutput fakeTrackOutput;

    public @MonotonicNonNull Format sampleFormat;
    private @MonotonicNonNull TrackOutput trackOutput;
    private long endTimeUs;

    public BindingTrackOutput(int id, int type, @Nullable Format manifestFormat) {
      this.id = id;
      this.type = type;
      this.manifestFormat = manifestFormat;
      fakeTrackOutput = new DummyTrackOutput();
    }

    public void bind(@Nullable TrackOutputProvider trackOutputProvider, long endTimeUs) {
      if (trackOutputProvider == null) {
        trackOutput = fakeTrackOutput;
        return;
      }
      this.endTimeUs = endTimeUs;
      trackOutput = trackOutputProvider.track(id, type);
      if (sampleFormat != null) {
        trackOutput.format(sampleFormat);
      }
    }

    @Override
    public void format(Format format) {
      sampleFormat =
          manifestFormat != null ? format.withManifestFormatInfo(manifestFormat) : format;
      castNonNull(trackOutput).format(sampleFormat);
    }

    @Override
    public int sampleData(
            DataReader input, int length, boolean allowEndOfInput, @SampleDataPart int sampleDataPart)
        throws IOException {
      return castNonNull(trackOutput).sampleData(input, length, allowEndOfInput);
    }

    @Override
    public void sampleData(ParsableByteArray data, int length, @SampleDataPart int sampleDataPart) {
      castNonNull(trackOutput).sampleData(data, length);
    }

    @Override
    public void sampleMetadata(
        long timeUs,
        @C.BufferFlags int flags,
        int size,
        int offset,
        @Nullable CryptoData cryptoData) {
      if (endTimeUs != C.TIME_UNSET && timeUs >= endTimeUs) {
        trackOutput = fakeTrackOutput;
      }
      castNonNull(trackOutput).sampleMetadata(timeUs, flags, size, offset, cryptoData);
    }
  }
}
