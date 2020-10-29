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
package com.adt.vpm.videoplayer.source.rtsp.core;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Log;

import androidx.annotation.IntDef;

import com.adt.vpm.videoplayer.source.common.util.Util;
import com.adt.vpm.videoplayer.source.rtsp.media.MediaType;
import com.adt.vpm.videoplayer.source.rtsp.message.Header;
import com.adt.vpm.videoplayer.source.rtsp.message.InterleavedFrame;
import com.adt.vpm.videoplayer.source.rtsp.message.Message;
import com.adt.vpm.videoplayer.source.rtsp.message.MessageBody;
import com.adt.vpm.videoplayer.source.rtsp.message.Method;
import com.adt.vpm.videoplayer.source.rtsp.message.Protocol;
import com.adt.vpm.videoplayer.source.rtsp.message.Request;
import com.adt.vpm.videoplayer.source.rtsp.message.Response;
import com.adt.vpm.videoplayer.source.rtsp.message.Status;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* package */ final class SSLReceiver implements IReceiver {


  private static final String TAG = "RTSP-SSLReceiver";
  private static final int READ_LINE_BUFFER_SIZE = 2048;
  private static final Pattern regexStatus = Pattern.compile(
          "(RTSP/\\d.\\d) (\\d+) (\\w+)", Pattern.CASE_INSENSITIVE);
  private static final Pattern regexRequest = Pattern.compile(
          "([A-Z_]+) rtsp://(.+) RTSP/(\\d.\\d)", Pattern.CASE_INSENSITIVE);
  private static final Pattern rexegHeader = Pattern.compile(
          "(\\S+):\\s+(.+)", Pattern.CASE_INSENSITIVE);

  @Retention(RetentionPolicy.SOURCE)
  @IntDef(flag = true, value = {PARSING_START_LINE, PARSING_HEADER_LINE, PARSING_BODY_LINE})
  private @interface State {
  }

  private final static int PARSING_START_LINE = 1;
  private final static int PARSING_HEADER_LINE = 2;
  private final static int PARSING_BODY_LINE = 3;

  // Below variables are used for storing state between reads from
  // stream
  private boolean isInterleavedDataReadComplete = true;
  private int interleavedDataLen = 0;
  private int interleavedDataRead = 0;
  private int interleavedDataChannel = 0;
  private byte [] interleavedData;

  int mediaLength = 0;
  MediaType mediaType = null;

  final static int IO_ERROR = 1;
  final static int PARSE_ERROR = 2;

  private final Handler handler;
  private final HandlerThread thread;

  private final InputStream inputStream;
  private final IEventListener eventListener;

  private volatile boolean canceled;

  private @State int state;

  public SSLReceiver(InputStream inputStream, IEventListener eventListener) {
    this.inputStream = inputStream;
    this.eventListener = eventListener;

    state = PARSING_START_LINE;

    thread = new HandlerThread("SSLReceiver:Handler", Process.THREAD_PRIORITY_AUDIO);
    thread.start();
    handler = new Handler(thread.getLooper());
    handler.post(loader);
  }

  public void cancel() {
    Log.d(TAG, "cancel isCanceled: " + canceled);

    if (!canceled) {
      canceled = true;
      thread.quit();
    }
  }

  @Override
  public void runInternal() {
    loader.run();
  }

  private void handleMessage(Message message) {
    Log.v("Receiver", "\n" +message.toString());
    switch (message.getType()) {
      case Message.REQUEST:
        eventListener.onReceiveSuccess((Request) message);
        break;

      case Message.RESPONSE:
        eventListener.onReceiveSuccess((Response) message);
        break;
      case Message.NONE:
        break;
    }
  }

  private void handleInterleavedFrame(InterleavedFrame interleavedFrame) {
    eventListener.onReceiveSuccess(interleavedFrame);
  }

  private Runnable loader = new Runnable() {

    private Message.Builder builder;
    private BufferedReader reader;

    private void parseMessageBody(MediaType mediaType, int mediaLength)
            throws NullPointerException {
      byte[] body = new byte[mediaLength];
      reader.readFully(body, 0, mediaLength);

      MessageBody messageBody = new MessageBody(mediaType, new String(body, 0, mediaLength));
      handleMessage(builder.setBody(messageBody).build());
    }

    public void runInternal() {
      boolean error = false;
      byte[] firstFourBytes = new byte[4];

      reader = new BufferedReader(new DataInputStream(inputStream));

      while (!Thread.currentThread().isInterrupted() && !canceled && !error) {

        try {
          if (!isInterleavedDataReadComplete) {
            readInterleavedData();
          } else {

            byte firstByte;
            try {
               firstByte = reader.peekByte();
            } catch (SocketTimeoutException ex) {
              Log.d(TAG, "Socket timedout. Try again..");
              continue;
            }

            if ((firstByte & 0xFF) == 0x24) {
              reader.readFully(firstFourBytes, 0, 4);
              interleavedDataChannel = firstFourBytes[1];
              interleavedDataLen = ((firstFourBytes[2] & 0xFF) << 8) |
                      (firstFourBytes[3] & 0xFF);

              if (interleavedData == null || interleavedData.length != interleavedDataLen) {
                interleavedData = new byte[interleavedDataLen];
              }
              readInterleavedData();
            } else {
              parseRTSPMessage();
            }
          }
        }catch(NullPointerException | IllegalArgumentException ex){
          ex.printStackTrace();
          builder = null;
          state = PARSING_START_LINE;
          eventListener.onReceiveFailure(PARSE_ERROR);

        } catch(IOException ex){
          ex.printStackTrace();
          error = true;
          builder = null;
          state = PARSING_START_LINE;
          if (!canceled) {
            eventListener.onReceiveFailure(IO_ERROR);
          }
        }

      } // End of While
    }

    private void readInterleavedData() throws IOException {
      // set flag to reading interleaved data
      isInterleavedDataReadComplete = false;
      int dataToRead = interleavedDataLen - interleavedDataRead;
      // Attempt to read data
      int dataRead = reader.attemptToRead(interleavedData,
              interleavedDataRead, dataToRead);
      interleavedDataRead += dataRead;
      Log.d(TAG, "Read Interleaved packet ToRead: " + dataToRead + " Red: " +  dataRead);

      if(interleavedDataRead == interleavedDataLen ) {
        Log.d(TAG, "Processing InterleavedFrame for channel: " + interleavedDataChannel);
        handleInterleavedFrame(new InterleavedFrame(interleavedDataChannel, interleavedData));
        interleavedDataLen = 0;
        interleavedDataRead = 0;
        interleavedDataChannel = 0;
        isInterleavedDataReadComplete = true;
      }
    }


    private void parseRTSPMessage() throws IOException {
      String line;
      Matcher matcher;

      line = reader.readLine();

      switch (state) {
        case PARSING_START_LINE:
          // Parses a request or status line
          matcher = regexRequest.matcher(line);
          if (matcher.find()) {
            Method method = Method.parse(matcher.group(1));
            String url = matcher.group(2);
            Protocol protocol = Protocol.parse(matcher.group(3));

            builder = new Request.Builder().setMethod(method).setUrl(url).setProtocol(protocol);
            state = PARSING_HEADER_LINE;

          } else {
            matcher = regexStatus.matcher(line);
            if (matcher.find()) {
              Protocol protocol = Protocol.parse(matcher.group(1));
              Status status = Status.parse(Integer.parseInt(matcher.group(2)));

              builder = new Response.Builder().setProtocol(protocol).setStatus(status);
              state = PARSING_HEADER_LINE;
            }
          }

          break;

        case PARSING_HEADER_LINE:
          // Parses a general, request, response or entity header
          if (line.length() > 0) {
            matcher = rexegHeader.matcher(line);
            if (matcher.find()) {
              Header header = Header.parse(matcher.group(1));

              if (header == Header.ContentType) {
                mediaType = MediaType.parse(matcher.group(2).trim());

              } else if (header == Header.ContentLength) {
                mediaLength = Integer.parseInt(matcher.group(2).trim());

              } else if (header != null) {
                builder.setHeader(header, matcher.group(2).trim());
              }
            }

          } else {
            if (mediaLength > 0) {
              parseMessageBody(mediaType, mediaLength);
              mediaLength = 0;
              state = PARSING_START_LINE;

            } else {
              handleMessage(builder.build());
              state = PARSING_START_LINE;
            }

            builder = null;
          }

          break;
      }
    }

    @Override
    public void run() {
      runInternal();
    }
  };


  /* package */ final static class BufferedReader {
    private static final int CR = 13;
    private static final int LF = 10;

    private static final int PEEK_MIN_FREE_SPACE_AFTER_RESIZE = 1024;
    private static final int PEEK_MAX_FREE_SPACE = 8 * 1024;

    private int peekBufferOffset;
    private int peekBufferLength;
    private int peekBufferPosition;

    private byte[] peekBuffer;

    private final DataInputStream inputStream;

    public BufferedReader(DataInputStream inputStream) {
      this.inputStream = inputStream;
      peekBuffer = new byte[PEEK_MIN_FREE_SPACE_AFTER_RESIZE];
    }

    private void readFullyFromInputStream(byte[] target, int offset, int length) {
      try {
        inputStream.readFully(target, offset, length);
      } catch (Exception ex) {
        Log.d(TAG, "Exception -------->" + ex.getLocalizedMessage());
        Log.d(TAG, "Exception -------->" + ex.getMessage());
      }
    }

    public  int attemptToReadFromStream(byte[] byteArray, int off, int len) throws IOException {
      int count = 0;
      if (len < 0) {
        throw new IndexOutOfBoundsException();
      }

      try {
        count = inputStream.read(byteArray, off, len);
        Log.d(TAG, "Attempted: " + len + " Read: " + count);

      } catch (SocketTimeoutException ex) {
        Log.d(TAG, "Exception -------->" + ex.getLocalizedMessage());
      }

      return count;
    }


    private void ensureSpaceForPeek(int length) {
      int requiredLength = peekBufferPosition + length;
      Log.v(TAG, "ensureSpaceForPeek peekBufferPosition: " + peekBufferPosition + " requiredLength: " + requiredLength);
      if (requiredLength > peekBuffer.length) {
        int newPeekCapacity = Util.constrainValue(peekBuffer.length * 2,
                requiredLength + PEEK_MIN_FREE_SPACE_AFTER_RESIZE,
                requiredLength + PEEK_MAX_FREE_SPACE);
        Log.d(TAG, "ensureSpaceForPeek newPeekCapacity: " + newPeekCapacity);

        peekBuffer = Arrays.copyOf(peekBuffer, newPeekCapacity);
      }
    }

    private void updatePeekBuffer(int bytesConsumed) {
      peekBufferLength -= bytesConsumed;
      peekBufferPosition -= bytesConsumed;
      peekBufferOffset = 0;
      byte[] newPeekBuffer = peekBuffer;
      if (peekBufferLength < peekBuffer.length - PEEK_MAX_FREE_SPACE) {
        newPeekBuffer = new byte[peekBufferLength + PEEK_MIN_FREE_SPACE_AFTER_RESIZE];
      }
      System.arraycopy(peekBuffer, bytesConsumed, newPeekBuffer, 0, peekBufferLength);
      peekBuffer = newPeekBuffer;
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

    public void readFully(byte[] target, int offset, int length) {
      int bytesRead = readFromPeekBuffer(target, offset, length);
      if (bytesRead < length) {
        readFullyFromInputStream(target, offset + bytesRead, length - bytesRead);
      }
    }

    public int attemptToRead(byte[] target, int offset, int length) throws IOException {
      int bytesRead = readFromPeekBuffer(target, offset, length);
      if (bytesRead < length) {
        int dataRead = attemptToReadFromStream(target, offset + bytesRead, length - bytesRead);
        bytesRead += dataRead;
      }
      return bytesRead;
    }

    public byte peekByte() throws IOException {
      if (peekBufferLength == 0) {
        ensureSpaceForPeek(1);
        int read = attemptToRead(peekBuffer, peekBufferPosition, 1);
        if(read == 0) {
          throw new SocketTimeoutException();
        }
        peekBufferPosition++;
        peekBufferLength = Math.max(peekBufferLength, peekBufferPosition);
      }

      return peekBuffer[peekBufferOffset++];
    }

    private int getPeekUnsignedByte() {
      return peekBuffer[peekBufferOffset++] & 0xFF;
    }

    public String readLine() throws IOException {
      boolean foundCr = false;
      boolean notFoundCrLf = true;

      StringBuilder builder = new StringBuilder();

      peekBufferOffset = 0;

      while (notFoundCrLf) {
        if (peekBufferLength == 0) {
          int available = READ_LINE_BUFFER_SIZE;
          ensureSpaceForPeek(available);
          Log.d(TAG, "Reading " + available + " bytes....");
          int dataRead = attemptToReadFromStream(peekBuffer, peekBufferPosition, available);
          peekBufferPosition += dataRead;
          peekBufferLength = Math.max(peekBufferLength, peekBufferPosition);
        } else {
          while (notFoundCrLf && peekBufferOffset < peekBufferPosition) {
            int character = getPeekUnsignedByte();

            if (character == CR) {
              foundCr = true;

            } else if (foundCr && character == LF) {
              notFoundCrLf = false;

            } else {
              foundCr = false;
              builder.append((char) character);
            }
          }
          updatePeekBuffer(peekBufferOffset);
        }
      }

      return builder.toString();
    }

  }
}
