/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.source;

/**
 * Default implementation of {@link CompositeSequenceableLoaderFactory}.
 */
public final class DefaultCompositeSequenceableLoaderFactory
    implements CompositeSequenceableLoaderFactory {

  @Override
  public SequenceableLoader createCompositeSequenceableLoader(SequenceableLoader... loaders) {
    return new CompositeSequenceableLoader(loaders);
  }

}
