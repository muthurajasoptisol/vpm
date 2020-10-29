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
package com.adt.vpm.videoplayer.source.rtp.upstream;

import com.adt.vpm.videoplayer.source.core.upstream.UdpDataSink;
import com.adt.vpm.videoplayer.source.rtp.rtcp.RtcpPacket;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/* package */ final class RtcpReportSender {
    public interface EventListener {
        void onReportSent();
    }

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private final EventListener listener;
    private final UdpDataSink dataSink;

    RtcpReportSender(UdpDataSink dataSink, EventListener listener) {
        this.dataSink = dataSink;
        this.listener = listener;
    }

    void cancel() {
        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    synchronized void send(final RtcpPacket packet) {
        try {

            executorService.execute(() -> {
                try {

                    if (packet != null && dataSink != null) {
                        byte[] bytes = packet.getBytes();
                        dataSink.write(bytes, 0, bytes.length);
                    }

                } catch (IOException ex) {
                    // Do nothing
                }
            });

        } catch (RejectedExecutionException ex) {
            // Do nothing

        } finally {
            listener.onReportSent();
        }
    }

    public synchronized void sendTo(final RtcpPacket packet, final InetAddress address,
                                    final int port) {
        try {

            executorService.execute(() -> {
                try {

                    if (packet != null && dataSink != null) {
                        byte[] bytes = packet.getBytes();
                        dataSink.writeTo(bytes, 0, bytes.length, address, port);
                    }

                } catch (IOException ex) {
                    // Do nothing
                }
            });

        } catch (RejectedExecutionException ex) {
            // Do nothing
        }
    }
}