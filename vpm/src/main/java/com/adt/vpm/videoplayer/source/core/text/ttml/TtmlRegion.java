/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.text.ttml;

import com.adt.vpm.videoplayer.source.core.text.Cue;

/**
 * Represents a TTML Region.
 */
/* package */ final class TtmlRegion {

  public final String id;
  public final float position;
  public final float line;
  @Cue.LineType
  public final int lineType;
  @Cue.AnchorType public final int lineAnchor;
  public final float width;
  public final float height;
  @Cue.TextSizeType public final int textSizeType;
  public final float textSize;
  @Cue.VerticalType public final int verticalType;

  public TtmlRegion(String id) {
    this(
        id,
        /* position= */ Cue.DIMEN_UNSET,
        /* line= */ Cue.DIMEN_UNSET,
        /* lineType= */ Cue.TYPE_UNSET,
        /* lineAnchor= */ Cue.TYPE_UNSET,
        /* width= */ Cue.DIMEN_UNSET,
        /* height= */ Cue.DIMEN_UNSET,
        /* textSizeType= */ Cue.TYPE_UNSET,
        /* textSize= */ Cue.DIMEN_UNSET,
        /* verticalType= */ Cue.TYPE_UNSET);
  }

  public TtmlRegion(
      String id,
      float position,
      float line,
      @Cue.LineType int lineType,
      @Cue.AnchorType int lineAnchor,
      float width,
      float height,
      int textSizeType,
      float textSize,
      @Cue.VerticalType int verticalType) {
    this.id = id;
    this.position = position;
    this.line = line;
    this.lineType = lineType;
    this.lineAnchor = lineAnchor;
    this.width = width;
    this.height = height;
    this.textSizeType = textSizeType;
    this.textSize = textSize;
    this.verticalType = verticalType;
  }

}
