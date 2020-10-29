/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.text.span;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import androidx.annotation.IntDef;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

/**
 * A styling span for ruby text.
 *
 * <p>The text covered by this span is known as the "base text", and the ruby text is stored in
 * {@link #rubyText}.
 *
 * <p>More information on <a href="https://en.wikipedia.org/wiki/Ruby_character">ruby characters</a>
 * and <a href="https://developer.android.com/guide/topics/text/spans">span styling</a>.
 */
// NOTE: There's no Android layout support for rubies, so this span currently doesn't extend any
// styling superclasses (e.g. MetricAffectingSpan). The only way to render these rubies is to
// extract the spans and do the layout manually.
// TODO: Consider adding support for parenthetical text to be used when rendering doesn't support
// rubies (e.g. HTML <rp> tag).
public final class RubySpan {

  /** The ruby position is unknown. */
  public static final int POSITION_UNKNOWN = -1;

  /**
   * The ruby text should be positioned above the base text.
   *
   * <p>For vertical text it should be positioned to the right, same as CSS's <a
   * href="https://developer.mozilla.org/en-US/docs/Web/CSS/ruby-position">ruby-position</a>.
   */
  public static final int POSITION_OVER = 1;

  /**
   * The ruby text should be positioned below the base text.
   *
   * <p>For vertical text it should be positioned to the left, same as CSS's <a
   * href="https://developer.mozilla.org/en-US/docs/Web/CSS/ruby-position">ruby-position</a>.
   */
  public static final int POSITION_UNDER = 2;

  /**
   * The possible positions of the ruby text relative to the base text.
   *
   * <p>One of:
   *
   * <ul>
   *   <li>{@link #POSITION_UNKNOWN}
   *   <li>{@link #POSITION_OVER}
   *   <li>{@link #POSITION_UNDER}
   * </ul>
   */
  @Documented
  @Retention(SOURCE)
  @IntDef({POSITION_UNKNOWN, POSITION_OVER, POSITION_UNDER})
  public @interface Position {}

  /** The ruby text, i.e. the smaller explanatory characters. */
  public final String rubyText;

  /** The position of the ruby text relative to the base text. */
  @Position public final int position;

  public RubySpan(String rubyText, @Position int position) {
    this.rubyText = rubyText;
    this.position = position;
  }
}
