/*
 * Created by ADT author on 9/29/20 5:06 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 5:06 PM
 */
package com.adt.vpm.videoplayer.source.extractor;

import com.adt.vpm.videoplayer.source.common.Format;
import com.adt.vpm.videoplayer.source.common.metadata.Metadata;
import com.adt.vpm.videoplayer.source.common.metadata.id3.CommentFrame;
import com.adt.vpm.videoplayer.source.common.metadata.id3.InternalFrame;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.adt.vpm.videoplayer.source.common.util.Util.castNonNull;

/**
 * Holder for gapless playback information.
 */
public final class GaplessInfoHolder {

  private static final String GAPLESS_DOMAIN = "com.apple.iTunes";
  private static final String GAPLESS_DESCRIPTION = "iTunSMPB";
  private static final Pattern GAPLESS_COMMENT_PATTERN =
      Pattern.compile("^ [0-9a-fA-F]{8} ([0-9a-fA-F]{8}) ([0-9a-fA-F]{8})");

  /**
   * The number of samples to trim from the start of the decoded audio stream, or
   * {@link Format#NO_VALUE} if not set.
   */
  public int encoderDelay;

  /**
   * The number of samples to trim from the end of the decoded audio stream, or
   * {@link Format#NO_VALUE} if not set.
   */
  public int encoderPadding;

  /**
   * Creates a new holder for gapless playback information.
   */
  public GaplessInfoHolder() {
    encoderDelay = Format.NO_VALUE;
    encoderPadding = Format.NO_VALUE;
  }

  /**
   * Populates the holder with data from an MP3 Xing header, if valid and non-zero.
   *
   * @param value The 24-bit value to decode.
   * @return Whether the holder was populated.
   */
  public boolean setFromXingHeaderValue(int value) {
    int encoderDelay = value >> 12;
    int encoderPadding = value & 0x0FFF;
    if (encoderDelay > 0 || encoderPadding > 0) {
      this.encoderDelay = encoderDelay;
      this.encoderPadding = encoderPadding;
      return true;
    }
    return false;
  }

  /**
   * Populates the holder with data parsed from ID3 {@link Metadata}.
   *
   * @param metadata The metadata from which to parse the gapless information.
   * @return Whether the holder was populated.
   */
  public boolean setFromMetadata(Metadata metadata) {
    for (int i = 0; i < metadata.length(); i++) {
      Metadata.Entry entry = metadata.get(i);
      if (entry instanceof CommentFrame) {
        CommentFrame commentFrame = (CommentFrame) entry;
        if (GAPLESS_DESCRIPTION.equals(commentFrame.description)
            && setFromComment(commentFrame.text)) {
          return true;
        }
      } else if (entry instanceof InternalFrame) {
        InternalFrame internalFrame = (InternalFrame) entry;
        if (GAPLESS_DOMAIN.equals(internalFrame.domain)
            && GAPLESS_DESCRIPTION.equals(internalFrame.description)
            && setFromComment(internalFrame.text)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Populates the holder with data parsed from a gapless playback comment (stored in an ID3 header
   * or MPEG 4 user data), if valid and non-zero.
   *
   * @param data The comment's payload data.
   * @return Whether the holder was populated.
   */
  private boolean setFromComment(String data) {
    Matcher matcher = GAPLESS_COMMENT_PATTERN.matcher(data);
    if (matcher.find()) {
      try {
        int encoderDelay = Integer.parseInt(castNonNull(matcher.group(1)), 16);
        int encoderPadding = Integer.parseInt(castNonNull(matcher.group(2)), 16);
        if (encoderDelay > 0 || encoderPadding > 0) {
          this.encoderDelay = encoderDelay;
          this.encoderPadding = encoderPadding;
          return true;
        }
      } catch (NumberFormatException e) {
        // Ignore incorrectly formatted comments.
      }
    }
    return false;
  }

  /**
   * Returns whether {@link #encoderDelay} and {@link #encoderPadding} have been set.
   */
  public boolean hasGaplessInfo() {
    return encoderDelay != Format.NO_VALUE && encoderPadding != Format.NO_VALUE;
  }

}
