/*
 * Created by ADT author on 9/29/20 5:06 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 5:06 PM
 */
package com.adt.vpm.videoplayer.source.extractor;

import com.adt.vpm.videoplayer.source.common.C;

import java.io.IOException;

/** Extractor related utility methods. */
/* package */ final class ExtractorUtil {

  /**
   * Peeks {@code length} bytes from the input peek position, or all the bytes to the end of the
   * input if there was less than {@code length} bytes left.
   *
   * <p>If an exception is thrown, there is no guarantee on the peek position.
   *
   * @param input The stream input to peek the data from.
   * @param target A target array into which data should be written.
   * @param offset The offset into the target array at which to write.
   * @param length The maximum number of bytes to peek from the input.
   * @return The number of bytes peeked.
   * @throws IOException If an error occurs peeking from the input.
   */
  public static int peekToLength(ExtractorInput input, byte[] target, int offset, int length)
      throws IOException {
    int totalBytesPeeked = 0;
    while (totalBytesPeeked < length) {
      int bytesPeeked = input.peek(target, offset + totalBytesPeeked, length - totalBytesPeeked);
      if (bytesPeeked == C.RESULT_END_OF_INPUT) {
        break;
      }
      totalBytesPeeked += bytesPeeked;
    }
    return totalBytesPeeked;
  }

  private ExtractorUtil() {}
}
