/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.source;

import java.io.IOException;

/**
 * Thrown when a live playback falls behind the available media window.
 */
public final class BehindLiveWindowException extends IOException {

  public BehindLiveWindowException() {
    super();
  }

}
