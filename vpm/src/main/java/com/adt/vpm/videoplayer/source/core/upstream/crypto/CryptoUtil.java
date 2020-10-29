/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.upstream.crypto;

import androidx.annotation.Nullable;

/**
 * Utility functions for the crypto package.
 */
/* package */ final class CryptoUtil {

  private CryptoUtil() {}

  /**
   * Returns the hash value of the input as a long using the 64 bit FNV-1a hash function. The hash
   * values produced by this function are less likely to collide than those produced by {@link
   * #hashCode()}.
   */
  public static long getFNV64Hash(@Nullable String input) {
    if (input == null) {
      return 0;
    }

    long hash = 0;
    for (int i = 0; i < input.length(); i++) {
      hash ^= input.charAt(i);
      // This is equivalent to hash *= 0x100000001b3 (the FNV magic prime number).
      hash += (hash << 1) + (hash << 4) + (hash << 5) + (hash << 7) + (hash << 8) + (hash << 40);
    }
    return hash;
  }

}
