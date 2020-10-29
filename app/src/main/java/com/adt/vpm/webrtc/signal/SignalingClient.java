/*
 * Created by ADT author on 10/22/20 10:08 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/16/20 11:15 AM
 */

package com.adt.vpm.webrtc.signal;

import com.adt.vpm.webrtc.SignalingParameters;
import com.adt.vpm.webrtc.data.IceCandidate;
import com.adt.vpm.webrtc.data.SessionDescription;

/**
 * AppRTCClient is the interface representing an AppRTC client.
 */
public interface SignalingClient {
    /**
     * Struct holding the connection parameters of an AppRTC room.
     */
    class RoomConnectionParameters {
        public final String roomUrl;
        public final String roomId;
        public final String urlParameters;

        public RoomConnectionParameters(
                String roomUrl, String roomId, String urlParameters) {
            this.roomUrl = roomUrl;
            this.roomId = roomId;
            this.urlParameters = urlParameters;
        }
    }

    /**
     * Asynchronously connect to an AppRTC room URL using supplied connection
     * parameters. Once connection is established onConnectedToRoom()
     * callback with room parameters is invoked.
     */
    void connectToRoom(RoomConnectionParameters connectionParameters);

    /**
     * Send offer SDP to the other participant.
     */
    void sendOfferSdp(final SessionDescription sdp);

    /**
     * Send answer SDP to the other participant.
     */
    void sendAnswerSdp(final SessionDescription sdp);

    /**
     * Send Ice candidate to the other participant.
     */
    void sendLocalIceCandidate(final IceCandidate candidate);

    /**
     * Send removed ICE candidates to the other participant.
     */
    void sendLocalIceCandidateRemovals(final IceCandidate[] candidates);

    /**
     * Disconnect from room.
     */
    void disconnectFromRoom();

    /**
     * Sets the preference for NAT server
     */
    void useSLNATServer(final Boolean enable);

    /**
     * Callback interface for messages delivered on signaling channel.
     *
     * <p>Methods are guaranteed to be invoked on the UI thread of |activity|.
     */
    interface SignalingEvents {
        /**
         * Callback fired once the room's signaling parameters
         * SignalingParameters are extracted.
         */
        void onConnectedToRoom(final SignalingParameters params, final String roomId);

        /**
         * Callback fired once remote SDP is received.
         */
        void onRemoteDescription(final SessionDescription sdp);

        /**
         * Callback fired once remote Ice candidate is received.
         */
        void onRemoteIceCandidate(final IceCandidate candidate);

        /**
         * Callback fired once remote Ice candidate removals are received.
         */
        void onRemoteIceCandidatesRemoved(final IceCandidate[] candidates);

        /**
         * Callback fired once channel is closed.
         */
        void onChannelClose();

        /**
         * Callback fired once channel error happened.
         */
        void onChannelError(final String description);
    }
}
