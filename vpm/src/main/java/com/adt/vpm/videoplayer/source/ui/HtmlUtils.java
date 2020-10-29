/*
 * Created by ADT author on 9/29/20 5:37 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/22/20 6:23 PM
 */
package com.adt.vpm.videoplayer.source.ui;

import android.graphics.Color;

import androidx.annotation.ColorInt;

import com.adt.vpm.videoplayer.source.common.util.Util;

/**
 * Utility methods for generating HTML and CSS for use with {@link WebViewSubtitleOutput} and {@link
 * SpannedToHtmlConverter}.
 */
/* package */ final class HtmlUtils {

  private HtmlUtils() {}

  public static String toCssRgba(@ColorInt int color) {
    return Util.formatInvariant(
        "rgba(%d,%d,%d,%.3f)",
        Color.red(color), Color.green(color), Color.blue(color), Color.alpha(color) / 255.0);
  }

  /**
   * Returns a CSS selector that selects all elements with {@code class=className} and all their
   * descendants.
   */
  public static String cssAllClassDescendantsSelector(String className) {
    return "." + className + ",." + className + " *";
  }
}
