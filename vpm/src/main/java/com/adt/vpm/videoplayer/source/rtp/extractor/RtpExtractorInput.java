/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.adt.vpm.videoplayer.source.rtp.extractor;

import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.upstream.DataSource;
import com.adt.vpm.videoplayer.source.common.util.Assertions;
import com.adt.vpm.videoplayer.source.extractor.ExtractorInput;
import com.adt.vpm.videoplayer.source.rtp.RtpPacket;
import com.adt.vpm.videoplayer.source.rtp.upstream.RtpDataSource;

import java.io.IOException;

/**
 * An RTP {@link ExtractorInput} that wraps a {@link DataSource}.
 */
public final class RtpExtractorInput implements ExtractorInput {

  private final DataSource dataSource;

  private long position;
  private byte[] peekBuffer;
  private int peekBufferPosition;
  private int peekBufferLength;

  /**
   * @param dataSource The wrapped {@link RtpDataSource }.
   */
  public RtpExtractorInput(DataSource dataSource) {
    this.dataSource = dataSource;

    peekBuffer = new byte[RtpPacket.MAX_PACKET_SIZE];
  }

  @Override
  public int read(byte[] target, int offset, int length) throws IOException {
    int bytesRead = readFromPeekBuffer(target, offset, length);
    if (bytesRead == 0) {
      try {
        bytesRead = readFromDataSource(target);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    return bytesRead;
  }

  @Override
  public boolean readFully(byte[] target, int offset, int length, boolean allowEndOfInput)
          throws IOException {
    int bytesToRead = Math.min(peekBufferLength - peekBufferPosition, length);
    if (bytesToRead < length) {
      return false;
    }
    read(target, offset, length);
    return false;
  }

  @Override
  public void readFully(byte[] target, int offset, int length)
          throws IOException {
    readFully(target, offset, length, false);
  }

  @Override
  public int skip(int length) throws IOException {
    int bytesSkipped = skipFromPeekBuffer(length);
    if (bytesSkipped == 0) {
      int bytesPeeked = 0;
      try {
        bytesPeeked = readFromDataSource(peekBuffer);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      peekBufferPosition += bytesPeeked;
      peekBufferLength = Math.max(peekBufferLength, peekBufferPosition);
      bytesSkipped = skipFromPeekBuffer(length);
    }
    commitBytesRead(bytesSkipped);
    return bytesSkipped;
  }

  @Override
  public boolean skipFully(int length, boolean allowEndOfInput)
          throws IOException {
    int bytesToSkip = Math.min(peekBufferLength - peekBufferPosition, length);
    if (bytesToSkip < length) {
        return false;
    }
    skip(length);
    return true;
  }

  @Override
  public void skipFully(int length) throws IOException {
    skipFully(length, false);
  }

  @Override
  public int peek(byte[] target, int offset, int length) throws IOException {
    int bytesPeeked = Math.min(peekBufferLength - peekBufferPosition, length);
    if (bytesPeeked == 0) {
      try {
        peekBufferLength = readFromDataSource(peekBuffer);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      bytesPeeked = Math.min(peekBufferLength, length);
      System.arraycopy(peekBuffer, peekBufferPosition, target, offset, bytesPeeked);
    }
    peekBufferPosition += bytesPeeked;
    return bytesPeeked;
  }

  @Override
  public boolean peekFully(byte[] target, int offset, int length, boolean allowEndOfInput)
          throws IOException {
    if (!advancePeekPosition(length, allowEndOfInput)) {
      return false;
    }
    System.arraycopy(peekBuffer, peekBufferPosition - length, target, offset, length);
    return true;
  }

  @Override
  public void peekFully(byte[] target, int offset, int length)
          throws IOException {
    peekFully(target, offset, length, false);
  }

  @Override
  public boolean advancePeekPosition(int length, boolean allowEndOfInput)
          throws IOException {
    int bytesPeeked = Math.min(peekBufferLength - peekBufferPosition, length);
    if (bytesPeeked == 0) {
      try {
        bytesPeeked = readFromDataSource(peekBuffer);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    if (bytesPeeked < length) {
      return false;
    }

    peekBufferPosition += bytesPeeked;
    peekBufferLength = Math.max(peekBufferLength, peekBufferPosition);
    return true;
  }

  @Override
  public void advancePeekPosition(int length) throws IOException {
    advancePeekPosition(length, false);
  }

  @Override
  public void resetPeekPosition() {
    peekBufferPosition = 0;
  }

  @Override
  public long getPeekPosition() {
    return position + peekBufferPosition;
  }

  @Override
  public long getPosition() {
    return position;
  }

  @Override
  public long getLength() {
    return C.LENGTH_UNSET;
  }

  @Override
  public <E extends Throwable> void setRetryPosition(long position, E e) throws E {
    Assertions.checkArgument(position >= 0);
    this.position = position;
    throw e;
  }

  private int readFromPeekBuffer(byte[] target, int offset, int length) {
    if (peekBufferLength == 0) {
      return 0;
    }
    int peekBytes = Math.min(peekBufferLength, length);
    System.arraycopy(peekBuffer, 0, target, offset, peekBytes);
    updatePeekBuffer(peekBytes);
    return peekBytes;
  }

  private void updatePeekBuffer(int bytesConsumed) {
    peekBufferLength -= bytesConsumed;
    peekBufferPosition = 0;
    byte[] newPeekBuffer = peekBuffer;
    System.arraycopy(peekBuffer, bytesConsumed, newPeekBuffer, 0, peekBufferLength);
    peekBuffer = newPeekBuffer;
  }

  private int readFromDataSource(byte[] target) throws InterruptedException, IOException {
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }

    int bytesRead = dataSource.read(target, 0, RtpPacket.MAX_PACKET_SIZE);
    while (bytesRead == 0) {
      bytesRead = dataSource.read(target, 0, RtpPacket.MAX_PACKET_SIZE);
    }

    return bytesRead;
  }

  private int skipFromPeekBuffer(int length) {
    int bytesSkipped = Math.min(peekBufferLength, length);
    updatePeekBuffer(bytesSkipped);
    return bytesSkipped;
  }

  private void commitBytesRead(int bytesRead) {
    if (bytesRead != C.RESULT_END_OF_INPUT) {
      position += bytesRead;
    }
  }
}
