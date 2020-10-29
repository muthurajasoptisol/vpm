/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.text.span;

import android.text.Spannable;
import android.text.style.ForegroundColorSpan;

/**
 * Utility methods for Android <a href="https://developer.android.com/guide/topics/text/spans">span
 * styling</a>.
 */
public final class SpanUtil {

  /**
   * Adds {@code span} to {@code spannable} between {@code start} and {@code end}, removing any
   * existing spans of the same type and with the same indices and flags.
   *
   * <p>This is useful for types of spans that don't make sense to duplicate and where the
   * evaluation order might have an unexpected impact on the final text, e.g. {@link
   * ForegroundColorSpan}.
   *
   * @param spannable The {@link Spannable} to add {@code span} to.
   * @param span The span object to be added.
   * @param start The start index to add the new span at.
   * @param end The end index to add the new span at.
   * @param spanFlags The flags to pass to {@link Spannable#setSpan(Object, int, int, int)}.
   */
  public static void addOrReplaceSpan(
      Spannable spannable, Object span, int start, int end, int spanFlags) {
    Object[] existingSpans = spannable.getSpans(start, end, span.getClass());
    for (Object existingSpan : existingSpans) {
      if (spannable.getSpanStart(existingSpan) == start
          && spannable.getSpanEnd(existingSpan) == end
          && spannable.getSpanFlags(existingSpan) == spanFlags) {
        spannable.removeSpan(existingSpan);
      }
    }
    spannable.setSpan(span, start, end, spanFlags);
  }

  private SpanUtil() {}
}
