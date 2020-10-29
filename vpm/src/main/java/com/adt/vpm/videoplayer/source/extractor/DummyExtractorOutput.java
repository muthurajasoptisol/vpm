/*
 * Created by ADT author on 9/29/20 5:06 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 5:06 PM
 */
package com.adt.vpm.videoplayer.source.extractor;

/** A fake {@link ExtractorOutput} implementation. */
public final class DummyExtractorOutput implements ExtractorOutput {

  @Override
  public TrackOutput track(int id, int type) {
    return new DummyTrackOutput();
  }

  @Override
  public void endTracks() {
    // Do nothing.
  }

  @Override
  public void seekMap(SeekMap seekMap) {
    // Do nothing.
  }
}
