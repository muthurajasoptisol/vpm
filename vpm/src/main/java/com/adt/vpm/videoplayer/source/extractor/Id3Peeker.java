/*
 * Created by ADT author on 9/29/20 5:06 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 5:06 PM
 */

package com.adt.vpm.videoplayer.source.extractor;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.common.metadata.Metadata;
import com.adt.vpm.videoplayer.source.common.metadata.id3.Id3Decoder;
import com.adt.vpm.videoplayer.source.common.util.ParsableByteArray;

import java.io.EOFException;
import java.io.IOException;

/**
 * Peeks data from the beginning of an {@link ExtractorInput} to determine if there is any ID3 tag.
 */
public final class Id3Peeker {

  private final ParsableByteArray scratch;

  public Id3Peeker() {
    scratch = new ParsableByteArray(Id3Decoder.ID3_HEADER_LENGTH);
  }

  /**
   * Peeks ID3 data from the input and parses the first ID3 tag.
   *
   * @param input The {@link ExtractorInput} from which data should be peeked.
   * @param id3FramePredicate Determines which ID3 frames are decoded. May be null to decode all
   *     frames.
   * @return The first ID3 tag decoded into a {@link Metadata} object. May be null if ID3 tag is not
   *     present in the input.
   * @throws IOException If an error occurred peeking from the input.
   */
  @Nullable
  public Metadata peekId3Data(
      ExtractorInput input, @Nullable Id3Decoder.FramePredicate id3FramePredicate)
      throws IOException {
    int peekedId3Bytes = 0;
    @Nullable Metadata metadata = null;
    while (true) {
      try {
        input.peekFully(scratch.getData(), /* offset= */ 0, Id3Decoder.ID3_HEADER_LENGTH);
      } catch (EOFException e) {
        // If input has less than ID3_HEADER_LENGTH, ignore the rest.
        break;
      }
      scratch.setPosition(0);
      if (scratch.readUnsignedInt24() != Id3Decoder.ID3_TAG) {
        // Not an ID3 tag.
        break;
      }
      scratch.skipBytes(3); // Skip major version, minor version and flags.
      int framesLength = scratch.readSynchSafeInt();
      int tagLength = Id3Decoder.ID3_HEADER_LENGTH + framesLength;

      if (metadata == null) {
        byte[] id3Data = new byte[tagLength];
        System.arraycopy(scratch.getData(), 0, id3Data, 0, Id3Decoder.ID3_HEADER_LENGTH);
        input.peekFully(id3Data, Id3Decoder.ID3_HEADER_LENGTH, framesLength);

        metadata = new Id3Decoder(id3FramePredicate).decode(id3Data, tagLength);
      } else {
        input.advancePeekPosition(framesLength);
      }

      peekedId3Bytes += tagLength;
    }

    input.resetPeekPosition();
    input.advancePeekPosition(peekedId3Bytes);
    return metadata;
  }
}
