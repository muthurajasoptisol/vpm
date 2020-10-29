/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.source;

/**
 * A factory to create composite {@link SequenceableLoader}s.
 */
public interface CompositeSequenceableLoaderFactory {

  /**
   * Creates a composite {@link SequenceableLoader}.
   *
   * @param loaders The sub-loaders that make up the {@link SequenceableLoader} to be built.
   * @return A composite {@link SequenceableLoader} that comprises the given loaders.
   */
  SequenceableLoader createCompositeSequenceableLoader(SequenceableLoader... loaders);

}
