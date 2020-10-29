/*
 * Created by ADT author on 9/29/20 5:06 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 5:06 PM
 */
package com.adt.vpm.videoplayer.source.extractor;

/**
 * Receives stream level data extracted by an {@link Extractor}.
 */
public interface ExtractorOutput {

  /**
   * Placeholder {@link ExtractorOutput} implementation throwing an {@link
   * UnsupportedOperationException} in each method.
   */
  ExtractorOutput PLACEHOLDER =
      new ExtractorOutput() {

        @Override
        public TrackOutput track(int id, int type) {
          throw new UnsupportedOperationException();
        }

        @Override
        public void endTracks() {
          throw new UnsupportedOperationException();
        }

        @Override
        public void seekMap(SeekMap seekMap) {
          throw new UnsupportedOperationException();
        }
      };

  /**
   * Called by the {@link Extractor} to get the {@link TrackOutput} for a specific track.
   *
   * <p>The same {@link TrackOutput} is returned if multiple calls are made with the same {@code
   * id}.
   *
   * @param id A track identifier.
   * @param type The type of the track. Typically one of the {@link com.adt.vpm.videoplayer.source.common.C}
   *     {@code TRACK_TYPE_*} constants.
   * @return The {@link TrackOutput} for the given track identifier.
   */
  TrackOutput track(int id, int type);

  /**
   * Called when all tracks have been identified, meaning no new {@code trackId} values will be
   * passed to {@link #track(int, int)}.
   */
  void endTracks();

  /**
   * Called when a {@link SeekMap} has been extracted from the stream.
   *
   * @param seekMap The extracted {@link SeekMap}.
   */
  void seekMap(SeekMap seekMap);

}
