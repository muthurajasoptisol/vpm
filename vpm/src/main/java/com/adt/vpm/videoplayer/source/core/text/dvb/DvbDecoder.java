/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.text.dvb;

import com.adt.vpm.videoplayer.source.core.text.SimpleSubtitleDecoder;
import com.adt.vpm.videoplayer.source.core.text.Subtitle;
import com.adt.vpm.videoplayer.source.common.util.ParsableByteArray;
import java.util.List;

/** A {@link SimpleSubtitleDecoder} for DVB subtitles. */
public final class DvbDecoder extends SimpleSubtitleDecoder {

  private final DvbParser parser;

  /**
   * @param initializationData The initialization data for the decoder. The initialization data
   *     must consist of a single byte array containing 5 bytes: flag_pes_stripped (1),
   *     composition_page (2), ancillary_page (2).
   */
  public DvbDecoder(List<byte[]> initializationData) {
    super("DvbDecoder");
    ParsableByteArray data = new ParsableByteArray(initializationData.get(0));
    int subtitleCompositionPage = data.readUnsignedShort();
    int subtitleAncillaryPage = data.readUnsignedShort();
    parser = new DvbParser(subtitleCompositionPage, subtitleAncillaryPage);
  }

  @Override
  protected Subtitle decode(byte[] data, int length, boolean reset) {
    if (reset) {
      parser.reset();
    }
    return new DvbSubtitle(parser.decode(data, length));
  }

}
