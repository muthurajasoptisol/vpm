/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.util;

import androidx.annotation.Nullable;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * {@link XmlPullParser} utility methods.
 */
public final class XmlPullParserUtil {

  private XmlPullParserUtil() {}

  /**
   * Returns whether the current event is an end tag with the specified name.
   *
   * @param xpp The {@link XmlPullParser} to query.
   * @param name The specified name.
   * @return Whether the current event is an end tag with the specified name.
   * @throws XmlPullParserException If an error occurs querying the parser.
   */
  public static boolean isEndTag(XmlPullParser xpp, String name) throws XmlPullParserException {
    return isEndTag(xpp) && xpp.getName().equals(name);
  }

  /**
   * Returns whether the current event is an end tag.
   *
   * @param xpp The {@link XmlPullParser} to query.
   * @return Whether the current event is an end tag.
   * @throws XmlPullParserException If an error occurs querying the parser.
   */
  public static boolean isEndTag(XmlPullParser xpp) throws XmlPullParserException {
    return xpp.getEventType() == XmlPullParser.END_TAG;
  }

  /**
   * Returns whether the current event is a start tag with the specified name.
   *
   * @param xpp The {@link XmlPullParser} to query.
   * @param name The specified name.
   * @return Whether the current event is a start tag with the specified name.
   * @throws XmlPullParserException If an error occurs querying the parser.
   */
  public static boolean isStartTag(XmlPullParser xpp, String name) throws XmlPullParserException {
    return isStartTag(xpp) && xpp.getName().equals(name);
  }

  /**
   * Returns whether the current event is a start tag.
   *
   * @param xpp The {@link XmlPullParser} to query.
   * @return Whether the current event is a start tag.
   * @throws XmlPullParserException If an error occurs querying the parser.
   */
  public static boolean isStartTag(XmlPullParser xpp) throws XmlPullParserException {
    return xpp.getEventType() == XmlPullParser.START_TAG;
  }

  /**
   * Returns whether the current event is a start tag with the specified name. If the current event
   * has a raw name then its prefix is stripped before matching.
   *
   * @param xpp The {@link XmlPullParser} to query.
   * @param name The specified name.
   * @return Whether the current event is a start tag with the specified name.
   * @throws XmlPullParserException If an error occurs querying the parser.
   */
  public static boolean isStartTagIgnorePrefix(XmlPullParser xpp, String name)
      throws XmlPullParserException {
    return isStartTag(xpp) && stripPrefix(xpp.getName()).equals(name);
  }

  /**
   * Returns the value of an attribute of the current start tag.
   *
   * @param xpp The {@link XmlPullParser} to query.
   * @param attributeName The name of the attribute.
   * @return The value of the attribute, or null if the current event is not a start tag or if no
   *     such attribute was found.
   */
  public static @Nullable String getAttributeValue(XmlPullParser xpp, String attributeName) {
    int attributeCount = xpp.getAttributeCount();
    for (int i = 0; i < attributeCount; i++) {
      if (xpp.getAttributeName(i).equals(attributeName)) {
        return xpp.getAttributeValue(i);
      }
    }
    return null;
  }

  /**
   * Returns the value of an attribute of the current start tag. Any raw attribute names in the
   * current start tag have their prefixes stripped before matching.
   *
   * @param xpp The {@link XmlPullParser} to query.
   * @param attributeName The name of the attribute.
   * @return The value of the attribute, or null if the current event is not a start tag or if no
   *     such attribute was found.
   */
  public static @Nullable String getAttributeValueIgnorePrefix(
      XmlPullParser xpp, String attributeName) {
    int attributeCount = xpp.getAttributeCount();
    for (int i = 0; i < attributeCount; i++) {
      if (stripPrefix(xpp.getAttributeName(i)).equals(attributeName)) {
        return xpp.getAttributeValue(i);
      }
    }
    return null;
  }

  private static String stripPrefix(String name) {
    int prefixSeparatorIndex = name.indexOf(':');
    return prefixSeparatorIndex == -1 ? name : name.substring(prefixSeparatorIndex + 1);
  }
}
