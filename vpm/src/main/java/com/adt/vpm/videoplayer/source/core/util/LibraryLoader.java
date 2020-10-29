/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.util;

import com.adt.vpm.videoplayer.source.common.util.Assertions;
import com.adt.vpm.videoplayer.source.common.util.Log;

import java.util.Arrays;

/**
 * Configurable loader for native libraries.
 */
public final class LibraryLoader {

  private static final String TAG = "LibraryLoader";

  private String[] nativeLibraries;
  private boolean loadAttempted;
  private boolean isAvailable;

  /**
   * @param libraries The names of the libraries to load.
   */
  public LibraryLoader(String... libraries) {
    nativeLibraries = libraries;
  }

  /**
   * Overrides the names of the libraries to load. Must be called before any call to
   * {@link #isAvailable()}.
   */
  public synchronized void setLibraries(String... libraries) {
    Assertions.checkState(!loadAttempted, "Cannot set libraries after loading");
    nativeLibraries = libraries;
  }

  /**
   * Returns whether the underlying libraries are available, loading them if necessary.
   */
  public synchronized boolean isAvailable() {
    if (loadAttempted) {
      return isAvailable;
    }
    loadAttempted = true;
    try {
      for (String lib : nativeLibraries) {
        System.loadLibrary(lib);
      }
      isAvailable = true;
    } catch (UnsatisfiedLinkError exception) {
      // Log a warning as an attempt to check for the library indicates that the app depends on an
      // extension and generally would expect its native libraries to be available.
      Log.w(TAG, "Failed to load " + Arrays.toString(nativeLibraries));
    }
    return isAvailable;
  }

}
