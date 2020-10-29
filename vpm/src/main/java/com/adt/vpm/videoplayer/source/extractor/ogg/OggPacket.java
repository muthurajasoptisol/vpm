/*
 * Created by ADT author on 9/29/20 5:06 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 5:06 PM
 */
package com.adt.vpm.videoplayer.source.extractor.ogg;

import com.adt.vpm.videoplayer.source.extractor.ExtractorInput;
import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.util.Assertions;
import com.adt.vpm.videoplayer.source.common.util.ParsableByteArray;

import java.io.IOException;
import java.util.Arrays;

import static java.lang.Math.max;

/**
 * OGG packet class.
 */
/* package */ final class OggPacket {

  private final OggPageHeader pageHeader = new OggPageHeader();
  private final ParsableByteArray packetArray = new ParsableByteArray(
      new byte[OggPageHeader.MAX_PAGE_PAYLOAD], 0);

  private int currentSegmentIndex = C.INDEX_UNSET;
  private int segmentCount;
  private boolean populated;

  /**
   * Resets this reader.
   */
  public void reset() {
    pageHeader.reset();
    packetArray.reset(/* limit= */ 0);
    currentSegmentIndex = C.INDEX_UNSET;
    populated = false;
  }

  /**
   * Reads the next packet of the ogg stream. In case of an {@code IOException} the caller must make
   * sure to pass the same instance of {@code ParsableByteArray} to this method again so this reader
   * can resume properly from an error while reading a continued packet spanned across multiple
   * pages.
   *
   * @param input The {@link ExtractorInput} to read data from.
   * @return {@code true} if the read was successful. The read fails if the end of the input is
   *     encountered without reading data.
   * @throws IOException If reading from the input fails.
   */
  public boolean populate(ExtractorInput input) throws IOException {
    Assertions.checkState(input != null);

    if (populated) {
      populated = false;
      packetArray.reset(/* limit= */ 0);
    }

    while (!populated) {
      if (currentSegmentIndex < 0) {
        // We're at the start of a page.
        if (!pageHeader.skipToNextPage(input) || !pageHeader.populate(input, /* quiet= */ true)) {
          return false;
        }
        int segmentIndex = 0;
        int bytesToSkip = pageHeader.headerSize;
        if ((pageHeader.type & 0x01) == 0x01 && packetArray.limit() == 0) {
          // After seeking, the first packet may be the remainder
          // part of a continued packet which has to be discarded.
          bytesToSkip += calculatePacketSize(segmentIndex);
          segmentIndex += segmentCount;
        }
        input.skipFully(bytesToSkip);
        currentSegmentIndex = segmentIndex;
      }

      int size = calculatePacketSize(currentSegmentIndex);
      int segmentIndex = currentSegmentIndex + segmentCount;
      if (size > 0) {
        if (packetArray.capacity() < packetArray.limit() + size) {
          packetArray.reset(Arrays.copyOf(packetArray.getData(), packetArray.limit() + size));
        }
        input.readFully(packetArray.getData(), packetArray.limit(), size);
        packetArray.setLimit(packetArray.limit() + size);
        populated = pageHeader.laces[segmentIndex - 1] != 255;
      }
      // Advance now since we are sure reading didn't throw an exception.
      currentSegmentIndex = segmentIndex == pageHeader.pageSegmentCount ? C.INDEX_UNSET
          : segmentIndex;
    }
    return true;
  }

  /**
   * An OGG Packet may span multiple pages. Returns the {@link OggPageHeader} of the last page read,
   * or an empty header if the packet has yet to be populated.
   *
   * <p>Note that the returned {@link OggPageHeader} is mutable and may be updated during subsequent
   * calls to {@link #populate(ExtractorInput)}.
   *
   * @return the {@code PageHeader} of the last page read or an empty header if the packet has yet
   *     to be populated.
   */
  public OggPageHeader getPageHeader() {
    return pageHeader;
  }

  /**
   * Returns a {@link ParsableByteArray} containing the packet's payload.
   */
  public ParsableByteArray getPayload() {
    return packetArray;
  }

  /**
   * Trims the packet data array.
   */
  public void trimPayload() {
    if (packetArray.getData().length == OggPageHeader.MAX_PAGE_PAYLOAD) {
      return;
    }
    packetArray.reset(
        Arrays.copyOf(
            packetArray.getData(), max(OggPageHeader.MAX_PAGE_PAYLOAD, packetArray.limit())));
  }

  /**
   * Calculates the size of the packet starting from {@code startSegmentIndex}.
   *
   * @param startSegmentIndex the index of the first segment of the packet.
   * @return Size of the packet.
   */
  private int calculatePacketSize(int startSegmentIndex) {
    segmentCount = 0;
    int size = 0;
    while (startSegmentIndex + segmentCount < pageHeader.pageSegmentCount) {
      int segmentLength = pageHeader.laces[startSegmentIndex + segmentCount++];
      size += segmentLength;
      if (segmentLength != 255) {
        // packets end at first lace < 255
        break;
      }
    }
    return size;
  }

}
