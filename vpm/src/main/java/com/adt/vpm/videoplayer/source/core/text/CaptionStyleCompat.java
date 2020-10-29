/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.text;

import android.graphics.Color;
import android.graphics.Typeface;
import android.view.accessibility.CaptioningManager;
import android.view.accessibility.CaptioningManager.CaptionStyle;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.adt.vpm.videoplayer.source.common.util.Util;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A compatibility wrapper for {@link CaptionStyle}.
 */
public final class CaptionStyleCompat {

  /**
   * The type of edge, which may be none. One of {@link #EDGE_TYPE_NONE}, {@link
   * #EDGE_TYPE_OUTLINE}, {@link #EDGE_TYPE_DROP_SHADOW}, {@link #EDGE_TYPE_RAISED} or {@link
   * #EDGE_TYPE_DEPRESSED}.
   */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @IntDef({
    EDGE_TYPE_NONE,
    EDGE_TYPE_OUTLINE,
    EDGE_TYPE_DROP_SHADOW,
    EDGE_TYPE_RAISED,
    EDGE_TYPE_DEPRESSED
  })
  public @interface EdgeType {}
  /**
   * Edge type value specifying no character edges.
   */
  public static final int EDGE_TYPE_NONE = 0;
  /**
   * Edge type value specifying uniformly outlined character edges.
   */
  public static final int EDGE_TYPE_OUTLINE = 1;
  /**
   * Edge type value specifying drop-shadowed character edges.
   */
  public static final int EDGE_TYPE_DROP_SHADOW = 2;
  /**
   * Edge type value specifying raised bevel character edges.
   */
  public static final int EDGE_TYPE_RAISED = 3;
  /**
   * Edge type value specifying depressed bevel character edges.
   */
  public static final int EDGE_TYPE_DEPRESSED = 4;

  /**
   * Use color setting specified by the track and fallback to default caption style.
   */
  public static final int USE_TRACK_COLOR_SETTINGS = 1;

  /** Default caption style. */
  public static final CaptionStyleCompat DEFAULT =
      new CaptionStyleCompat(
          Color.WHITE,
          Color.BLACK,
          Color.TRANSPARENT,
          EDGE_TYPE_NONE,
          Color.WHITE,
          /* typeface= */ null);

  /**
   * The preferred foreground color.
   */
  public final int foregroundColor;

  /**
   * The preferred background color.
   */
  public final int backgroundColor;

  /**
   * The preferred window color.
   */
  public final int windowColor;

  /**
   * The preferred edge type. One of:
   * <ul>
   * <li>{@link #EDGE_TYPE_NONE}
   * <li>{@link #EDGE_TYPE_OUTLINE}
   * <li>{@link #EDGE_TYPE_DROP_SHADOW}
   * <li>{@link #EDGE_TYPE_RAISED}
   * <li>{@link #EDGE_TYPE_DEPRESSED}
   * </ul>
   */
  @EdgeType public final int edgeType;

  /**
   * The preferred edge color, if using an edge type other than {@link #EDGE_TYPE_NONE}.
   */
  public final int edgeColor;

  /** The preferred typeface, or {@code null} if unspecified. */
  @Nullable public final Typeface typeface;

  /**
   * Creates a {@link CaptionStyleCompat} equivalent to a provided {@link CaptionStyle}.
   *
   * @param captionStyle A {@link CaptionStyle}.
   * @return The equivalent {@link CaptionStyleCompat}.
   */
  @RequiresApi(19)
  public static CaptionStyleCompat createFromCaptionStyle(
      CaptioningManager.CaptionStyle captionStyle) {
    if (Util.SDK_INT >= 21) {
      return createFromCaptionStyleV21(captionStyle);
    } else {
      // Note - Any caller must be on at least API level 19 or greater (because CaptionStyle did
      // not exist in earlier API levels).
      return createFromCaptionStyleV19(captionStyle);
    }
  }

  /**
   * @param foregroundColor See {@link #foregroundColor}.
   * @param backgroundColor See {@link #backgroundColor}.
   * @param windowColor See {@link #windowColor}.
   * @param edgeType See {@link #edgeType}.
   * @param edgeColor See {@link #edgeColor}.
   * @param typeface See {@link #typeface}.
   */
  public CaptionStyleCompat(
      int foregroundColor,
      int backgroundColor,
      int windowColor,
      @EdgeType int edgeType,
      int edgeColor,
      @Nullable Typeface typeface) {
    this.foregroundColor = foregroundColor;
    this.backgroundColor = backgroundColor;
    this.windowColor = windowColor;
    this.edgeType = edgeType;
    this.edgeColor = edgeColor;
    this.typeface = typeface;
  }

  @RequiresApi(19)
  @SuppressWarnings("ResourceType")
  private static CaptionStyleCompat createFromCaptionStyleV19(
      CaptioningManager.CaptionStyle captionStyle) {
    return new CaptionStyleCompat(
        captionStyle.foregroundColor, captionStyle.backgroundColor, Color.TRANSPARENT,
        captionStyle.edgeType, captionStyle.edgeColor, captionStyle.getTypeface());
  }

  @RequiresApi(21)
  @SuppressWarnings("ResourceType")
  private static CaptionStyleCompat createFromCaptionStyleV21(
      CaptioningManager.CaptionStyle captionStyle) {
    return new CaptionStyleCompat(
        captionStyle.hasForegroundColor() ? captionStyle.foregroundColor : DEFAULT.foregroundColor,
        captionStyle.hasBackgroundColor() ? captionStyle.backgroundColor : DEFAULT.backgroundColor,
        captionStyle.hasWindowColor() ? captionStyle.windowColor : DEFAULT.windowColor,
        captionStyle.hasEdgeType() ? captionStyle.edgeType : DEFAULT.edgeType,
        captionStyle.hasEdgeColor() ? captionStyle.edgeColor : DEFAULT.edgeColor,
        captionStyle.getTypeface());
  }

}
