/*
 * Created by ADT author on 9/29/20 5:06 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 5:06 PM
 */
package com.adt.vpm.videoplayer.source.extractor.ts;

import com.adt.vpm.videoplayer.source.extractor.ExtractorOutput;
import com.adt.vpm.videoplayer.source.extractor.TrackOutput;
import com.adt.vpm.videoplayer.source.common.util.ParsableByteArray;
import com.adt.vpm.videoplayer.source.common.util.TimestampAdjuster;

/**
 * Reads section data.
 */
public interface SectionPayloadReader {

  /**
   * Initializes the section payload reader.
   *
   * @param timestampAdjuster A timestamp adjuster for offsetting and scaling sample timestamps.
   * @param extractorOutput The {@link ExtractorOutput} that receives the extracted data.
   * @param idGenerator A {@link PesReader.TrackIdGenerator} that generates unique track ids for the
   *     {@link TrackOutput}s.
   */
  void init(TimestampAdjuster timestampAdjuster, ExtractorOutput extractorOutput,
            TsPayloadReader.TrackIdGenerator idGenerator);

  /**
   * Called by a {@link SectionReader} when a full section is received.
   *
   * @param sectionData The data belonging to a section starting from the table_id. If
   *     section_syntax_indicator is set to '1', {@code sectionData} excludes the CRC_32 field.
   *     Otherwise, all bytes belonging to the table section are included.
   */
  void consume(ParsableByteArray sectionData);

}
