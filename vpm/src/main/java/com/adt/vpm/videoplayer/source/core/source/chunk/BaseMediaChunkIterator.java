/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.source.chunk;

import java.util.NoSuchElementException;

/**
 * Base class for {@link MediaChunkIterator}s. Handles {@link #next()} and {@link #isEnded()}, and
 * provides a bounds check for child classes.
 */
public abstract class BaseMediaChunkIterator implements MediaChunkIterator {

  private final long fromIndex;
  private final long toIndex;

  private long currentIndex;

  /**
   * Creates base iterator.
   *
   * @param fromIndex The first available index.
   * @param toIndex The last available index.
   */
  @SuppressWarnings("method.invocation.invalid")
  public BaseMediaChunkIterator(long fromIndex, long toIndex) {
    this.fromIndex = fromIndex;
    this.toIndex = toIndex;
    reset();
  }

  @Override
  public boolean isEnded() {
    return currentIndex > toIndex;
  }

  @Override
  public boolean next() {
    currentIndex++;
    return !isEnded();
  }

  @Override
  public void reset() {
    currentIndex = fromIndex - 1;
  }

  /**
   * Verifies that the iterator points to a valid element.
   *
   * @throws NoSuchElementException If the iterator does not point to a valid element.
   */
  protected final void checkInBounds() {
    if (currentIndex < fromIndex || currentIndex > toIndex) {
      throw new NoSuchElementException();
    }
  }

  /** Returns the current index this iterator is pointing to. */
  protected final long getCurrentIndex() {
    return currentIndex;
  }
}
