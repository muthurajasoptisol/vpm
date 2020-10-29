/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.upstream;

import android.net.Uri;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.upstream.DataSpec;
import com.adt.vpm.videoplayer.source.common.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import static java.lang.Math.min;

/** A UDP {@link DataSource}. */
public class UdpDataSource extends BaseDataSource {

  /**
   * Thrown when an error is encountered when trying to read from a {@link UdpDataSource}.
   */
  public static final class UdpDataSourceException extends IOException {

    public UdpDataSourceException(IOException cause) {
      super(cause);
    }

  }

  /** The default datagram packet size, in bytes.
   * 1500 bytes (MTU) minus IP header (20 bytes) and UDP header (8 bytes)
   */
  public static final int DEFAULT_PACKET_SIZE = 2000;

  /** The default maximum datagram packet size, in bytes.
   * 65535 bytes minus IP header (20 bytes) and UDP header (8 bytes)
   */
  public static final int DEFAULT_MAX_PACKET_SIZE = 65507;

  /** The default maximum receive buffer size, in bytes. */
  public static final int DEFAULT_RECEIVE_BUFFER_SIZE = 200 * 1024;

  /** The default socket timeout, in milliseconds. */
  public static final int DEFAULT_SOCKET_TIMEOUT_MILLIS = 8 * 1000;

  private final int receiveBufferSize;
  private final int socketTimeoutMillis;
  private final byte[] packetBuffer;
  private final DatagramPacket packet;

  @Nullable private Uri uri;
  @Nullable DatagramSocket socket;
  @Nullable private MulticastSocket multicastSocket;
  @Nullable private InetAddress address;
  @Nullable private InetSocketAddress socketAddress;
  private boolean opened;

  private int packetRemaining;

  public UdpDataSource() {
    this(DEFAULT_PACKET_SIZE);
  }

  /**
   * Constructs a new instance.
   *
   * @param maxPacketSize The maximum datagram packet size, in bytes.
   */
  public UdpDataSource(int maxPacketSize) {
    this(maxPacketSize, DEFAULT_RECEIVE_BUFFER_SIZE, DEFAULT_SOCKET_TIMEOUT_MILLIS);
  }

  /**
   * Constructs a new instance.
   *
   * @param maxPacketSize The maximum datagram packet size, in bytes.
   * @param receiveBufferSize The maximum receive buffer size, in bytes.
   */
  public UdpDataSource(int maxPacketSize, int receiveBufferSize) {
    this(maxPacketSize, receiveBufferSize, DEFAULT_SOCKET_TIMEOUT_MILLIS);
  }

  /**
   * Constructs a new instance.
   *
   * @param maxPacketSize The maximum datagram packet size, in bytes.
   * @param receiveBufferSize The maximum receive buffer size, in bytes.
   * @param socketTimeoutMillis The socket timeout in milliseconds. A timeout of zero is interpreted
   *     as an infinite timeout.
   */
  public UdpDataSource(int maxPacketSize, int receiveBufferSize, int socketTimeoutMillis) {
    super(/* isNetwork= */ true);
    this.receiveBufferSize = receiveBufferSize;
    this.socketTimeoutMillis = socketTimeoutMillis;
    packetBuffer = new byte[maxPacketSize];
    packet = new DatagramPacket(packetBuffer, 0, maxPacketSize);
  }

  @Override
  public long open(DataSpec dataSpec) throws UdpDataSourceException, IOException {
    uri = dataSpec.uri;
    String host = uri.getHost();
    int port = uri.getPort();
    transferInitializing(dataSpec);
    try {
      address = InetAddress.getByName(host);
      socketAddress = new InetSocketAddress(address, port);
      if (address.isMulticastAddress()) {
        multicastSocket = new MulticastSocket(socketAddress);
        multicastSocket.joinGroup(address);
        socket = multicastSocket;
      } else {
        if (dataSpec.isFlagSet(DataSpec.FLAG_FORCE_BOUND_LOCAL_ADDRESS)) {
          socket = new DatagramSocket(uri.getPort());
        } else {
          socket = new DatagramSocket(socketAddress);
        }
      }
    } catch (IOException e) {
//      throw new UdpDataSourceException(e);
      Log.e("xxx UdpDataSource", "Open() DatagramSocket IOExp");
    }

    try {
      socket.setSoTimeout(socketTimeoutMillis);
      socket.setReceiveBufferSize(receiveBufferSize);
    } catch (SocketException e) {
//      throw new UdpDataSourceException(e);
      Log.e("xxx UdpDataSource", "Open() setSoTimeout SocketExp");
    }

    opened = true;
    transferStarted(dataSpec);
    return C.LENGTH_UNSET;
  }

  @Override
  public int read(byte[] buffer, int offset, int readLength) throws IOException {
    if (readLength == 0) {
      return 0;
    }
    if (packetRemaining == 0) {
      // We've read all of the data from the current packet. Get another.
      try {
        socket.receive(packet);
      } catch (SocketTimeoutException e) {
        Log.e("UdpDataSource ", "read() SocketTimeOutExp");
        e.printStackTrace();
      } catch (IOException e) {
        Log.e("UdpDataSource ", "read() IOExp");
//        throw new UdpDataSourceException(e);
        e.printStackTrace();
      }
      packetRemaining = packet.getLength();
      bytesTransferred(packetRemaining);
    }

    int packetOffset = packet.getLength() - packetRemaining;
    int bytesToRead = min(packetRemaining, readLength);
    System.arraycopy(packetBuffer, packetOffset, buffer, offset, bytesToRead);
    packetRemaining -= bytesToRead;
    return bytesToRead;
  }

  @Override
  @Nullable
  public Uri getUri() {
    return uri;
  }

  @Override
  public void close() {
    uri = null;
    if (multicastSocket != null) {
      try {
        multicastSocket.leaveGroup(address);
      } catch (IOException e) {
        // Do nothing.
      }
      multicastSocket = null;
    }
    if (socket != null) {
      socket.close();
      socket = null;
    }
    address = null;
    socketAddress = null;
    packetRemaining = 0;
    if (opened) {
      opened = false;
      transferEnded();
    }
  }

}
