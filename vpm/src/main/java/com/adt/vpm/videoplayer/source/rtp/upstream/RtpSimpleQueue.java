package com.adt.vpm.videoplayer.source.rtp.upstream;

import com.adt.vpm.videoplayer.source.rtp.RtpPacket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/* package */ final class RtpSimpleQueue extends RtpQueue {

    private final Queue<RtpPacket> packets;

    RtpSimpleQueue(int clockrate) {
        super(clockrate);

        packets = new ConcurrentLinkedQueue<>();
    }

    @Override
    public synchronized void offer(RtpPacket packet) {
        int sequence = packet.getSequenceNumber();

        calculateJitter(packet.getTimestamp());

        if (!isStarted) {
            stats.maxSequence = sequence - 1;
            stats.baseSequence = sequence;

            isStarted = true;
        }

        int expected = (stats.maxSequence + 1) % RTP_SEQ_MOD;
        int sequenceDelta = sequence - expected;

        if (sequenceDelta >= 0 && sequenceDelta < MAX_DROPOUT) {
            packets.offer(packet);
            stats.maxSequence = sequence;
        }

        if (expected < stats.maxSequence) {
            stats.cycles++;
        }

        stats.received++;
    }

    @Override
    public synchronized RtpPacket pop() {
        if (packets.size() > 0) {
            RtpPacket packet = packets.poll();
            stats.baseSequence = packet.getSequenceNumber();
            return packet;
        }

        return null;
    }

    @Override
    public synchronized void reset() {
        packets.clear();
        isStarted = false;
        stats.jitter = 0;
    }

}
