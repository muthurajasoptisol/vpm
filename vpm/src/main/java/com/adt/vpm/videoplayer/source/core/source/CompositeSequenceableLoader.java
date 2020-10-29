/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.source;

import static java.lang.Math.min;

import com.adt.vpm.videoplayer.source.common.C;

/**
 * A {@link SequenceableLoader} that encapsulates multiple other {@link SequenceableLoader}s.
 */
public class CompositeSequenceableLoader implements SequenceableLoader {

  protected final SequenceableLoader[] loaders;

  public CompositeSequenceableLoader(SequenceableLoader[] loaders) {
    this.loaders = loaders;
  }

  @Override
  public final long getBufferedPositionUs() {
    long bufferedPositionUs = Long.MAX_VALUE;
    for (SequenceableLoader loader : loaders) {
      long loaderBufferedPositionUs = loader.getBufferedPositionUs();
      if (loaderBufferedPositionUs != C.TIME_END_OF_SOURCE) {
        bufferedPositionUs = min(bufferedPositionUs, loaderBufferedPositionUs);
      }
    }
    return bufferedPositionUs == Long.MAX_VALUE ? C.TIME_END_OF_SOURCE : bufferedPositionUs;
  }

  @Override
  public final long getNextLoadPositionUs() {
    long nextLoadPositionUs = Long.MAX_VALUE;
    for (SequenceableLoader loader : loaders) {
      long loaderNextLoadPositionUs = loader.getNextLoadPositionUs();
      if (loaderNextLoadPositionUs != C.TIME_END_OF_SOURCE) {
        nextLoadPositionUs = min(nextLoadPositionUs, loaderNextLoadPositionUs);
      }
    }
    return nextLoadPositionUs == Long.MAX_VALUE ? C.TIME_END_OF_SOURCE : nextLoadPositionUs;
  }

  @Override
  public final void reevaluateBuffer(long positionUs) {
    for (SequenceableLoader loader : loaders) {
      loader.reevaluateBuffer(positionUs);
    }
  }

  @Override
  public boolean continueLoading(long positionUs) {
    boolean madeProgress = false;
    boolean madeProgressThisIteration;
    do {
      madeProgressThisIteration = false;
      long nextLoadPositionUs = getNextLoadPositionUs();
      if (nextLoadPositionUs == C.TIME_END_OF_SOURCE) {
        break;
      }
      for (SequenceableLoader loader : loaders) {
        long loaderNextLoadPositionUs = loader.getNextLoadPositionUs();
        boolean isLoaderBehind =
            loaderNextLoadPositionUs != C.TIME_END_OF_SOURCE
                && loaderNextLoadPositionUs <= positionUs;
        if (loaderNextLoadPositionUs == nextLoadPositionUs || isLoaderBehind) {
          madeProgressThisIteration |= loader.continueLoading(positionUs);
        }
      }
      madeProgress |= madeProgressThisIteration;
    } while (madeProgressThisIteration);
    return madeProgress;
  }

  @Override
  public boolean isLoading() {
    for (SequenceableLoader loader : loaders) {
      if (loader.isLoading()) {
        return true;
      }
    }
    return false;
  }
}
