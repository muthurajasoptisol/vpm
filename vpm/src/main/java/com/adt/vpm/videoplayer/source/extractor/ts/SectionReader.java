/*
 * Created by ADT author on 9/29/20 5:06 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 5:06 PM
 */
package com.adt.vpm.videoplayer.source.extractor.ts;

import com.adt.vpm.videoplayer.source.extractor.ExtractorOutput;
import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.util.ParsableByteArray;
import com.adt.vpm.videoplayer.source.common.util.TimestampAdjuster;
import com.adt.vpm.videoplayer.source.common.util.Util;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Reads section data packets and feeds the whole sections to a given {@link SectionPayloadReader}.
 * Useful information on PSI sections can be found in ISO/IEC 13818-1, section 2.4.4.
 */
public final class SectionReader implements TsPayloadReader {

  private static final int SECTION_HEADER_LENGTH = 3;
  private static final int DEFAULT_SECTION_BUFFER_LENGTH = 32;
  private static final int MAX_SECTION_LENGTH = 4098;

  private final SectionPayloadReader reader;
  private final ParsableByteArray sectionData;

  private int totalSectionLength;
  private int bytesRead;
  private boolean sectionSyntaxIndicator;
  private boolean waitingForPayloadStart;

  public SectionReader(SectionPayloadReader reader) {
    this.reader = reader;
    sectionData = new ParsableByteArray(DEFAULT_SECTION_BUFFER_LENGTH);
  }

  @Override
  public void init(TimestampAdjuster timestampAdjuster, ExtractorOutput extractorOutput,
      TrackIdGenerator idGenerator) {
    reader.init(timestampAdjuster, extractorOutput, idGenerator);
    waitingForPayloadStart = true;
  }

  @Override
  public void seek() {
    waitingForPayloadStart = true;
  }

  @Override
  public void consume(ParsableByteArray data, @Flags int flags) {
    boolean payloadUnitStartIndicator = (flags & FLAG_PAYLOAD_UNIT_START_INDICATOR) != 0;
    int payloadStartPosition = C.POSITION_UNSET;
    if (payloadUnitStartIndicator) {
      int payloadStartOffset = data.readUnsignedByte();
      payloadStartPosition = data.getPosition() + payloadStartOffset;
    }

    if (waitingForPayloadStart) {
      if (!payloadUnitStartIndicator) {
        return;
      }
      waitingForPayloadStart = false;
      data.setPosition(payloadStartPosition);
      bytesRead = 0;
    }

    while (data.bytesLeft() > 0) {
      if (bytesRead < SECTION_HEADER_LENGTH) {
        // Note: see ISO/IEC 13818-1, section 2.4.4.3 for detailed information on the format of
        // the header.
        if (bytesRead == 0) {
          int tableId = data.readUnsignedByte();
          data.setPosition(data.getPosition() - 1);
          if (tableId == 0xFF /* forbidden value */) {
            // No more sections in this ts packet.
            waitingForPayloadStart = true;
            return;
          }
        }
        int headerBytesToRead = min(data.bytesLeft(), SECTION_HEADER_LENGTH - bytesRead);
        data.readBytes(sectionData.getData(), bytesRead, headerBytesToRead);
        bytesRead += headerBytesToRead;
        if (bytesRead == SECTION_HEADER_LENGTH) {
          sectionData.reset(SECTION_HEADER_LENGTH);
          sectionData.skipBytes(1); // Skip table id (8).
          int secondHeaderByte = sectionData.readUnsignedByte();
          int thirdHeaderByte = sectionData.readUnsignedByte();
          sectionSyntaxIndicator = (secondHeaderByte & 0x80) != 0;
          totalSectionLength =
              (((secondHeaderByte & 0x0F) << 8) | thirdHeaderByte) + SECTION_HEADER_LENGTH;
          if (sectionData.capacity() < totalSectionLength) {
            // Ensure there is enough space to keep the whole section.
            byte[] bytes = sectionData.getData();
            sectionData.reset(min(MAX_SECTION_LENGTH, max(totalSectionLength, bytes.length * 2)));
            System.arraycopy(bytes, 0, sectionData.getData(), 0, SECTION_HEADER_LENGTH);
          }
        }
      } else {
        // Reading the body.
        int bodyBytesToRead = min(data.bytesLeft(), totalSectionLength - bytesRead);
        data.readBytes(sectionData.getData(), bytesRead, bodyBytesToRead);
        bytesRead += bodyBytesToRead;
        if (bytesRead == totalSectionLength) {
          if (sectionSyntaxIndicator) {
            // This section has common syntax as defined in ISO/IEC 13818-1, section 2.4.4.11.
            if (Util.crc32(sectionData.getData(), 0, totalSectionLength, 0xFFFFFFFF) != 0) {
              // The CRC is invalid so discard the section.
              waitingForPayloadStart = true;
              return;
            }
            sectionData.reset(totalSectionLength - 4); // Exclude the CRC_32 field.
          } else {
            // This is a private section with private defined syntax.
            sectionData.reset(totalSectionLength);
          }
          reader.consume(sectionData);
          bytesRead = 0;
        }
      }
    }
  }

}
