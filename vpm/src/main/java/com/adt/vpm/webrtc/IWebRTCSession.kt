/*
 * Created by ADT author on 9/14/20 11:00 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/14/20 10:49 PM
 */

package com.adt.vpm.webrtc

import com.adt.vpm.webrtc.data.IceCandidate
import com.adt.vpm.webrtc.data.SessionDescription
import org.webrtc.EglBase
import org.webrtc.VideoSink

/**
 * IWebRTCSession
 * IWebRTCSession is the functional interface for all WebRTC Session.
 * Application will use this interface along with its own signalling to establish
 * WebRTC Session and communicate media via peer to peer connection.
 */
interface IWebRTCSession {


    /**
     * CreatePeerConnection :
     * This method is used to create PeerConnection instance. PeerConnection instance allows an
     * application to establish peer-to-peer communications with another PeerConnection instance.
     * @param localRender – which contains source of videoSink to render local stream
     * @param remoteSink - which contains source of videoSink to render remote stream
     * @param signalingParameters – which contains the parameters about signaling which is needed in order
     *  for two peers to share how they should connect.
     */
    fun createPeerConnection(
        localRender: VideoSink?,
        remoteSink: VideoSink?,
        signalingParameters: SignalingParameters?
    )

    /**
     * Close :
     * This method is used to close peer connection while user disconnect the call.
     */
    fun close()

    /**
     * muteLocalAudio :
     * This method is used to enable or disable local audio stream
     * @param enable – which is true if to enable otherwise false
     */
    fun muteLocalAudio(enable: Boolean)

    /**
     * muteLocalVideo:
     * This method is used to enable or disable local video stream
     * @param enable – which is true if to enable otherwise false
     */
    fun muteLocalVideo(enable: Boolean)

    /**
     * Create Offer :
     * This method is used to Creates an offer(request) to find a remote peer. The two first
     * parameters of this method are success and error callbacks. The optional third parameter are
     * options, like enabling audio or video streams
     */
    fun createOffer()

    /**
     * Create Answer :
     * This method is used to Creates an answer to the offer received by the remote peer during the
     * offer/answer negotiation process. The two first parameters of this method are success and
     * error callbacks. The optional third parameter are options for the answer to be created.
     */
    fun createAnswer()

    /**
     * AddRemoteIceCandidate :
     * This method Provides a remote candidate to the Interactive Connectivity Establishment (ICE) server for establishing a peer connection
     * @param candidate – which represents a new candidate that should be sent to the remote peer.
     */
    fun addRemoteIceCandidate(candidate: IceCandidate)

    /**
     * RemoveRemoteIceCandidates :
     * This method is used to remove remote ice candidate which is received from remote peer.
     * @param candidates – which is candidate list need to be removed.
     */
    fun removeRemoteIceCandidates(candidates: Array<IceCandidate>)

    /**
     * SetRemoteDescription :
     * This method instructs the Peer Connection to apply the supplied Session Description as the
     * remote offer or answer. This API changes the local media state.
     * @param sdp - Changes the remote connection description. The description defines the properties
     * of the connection. This session description is set as the remote description to peer connection.
     */
    fun setRemoteDescription(sdp: SessionDescription)

    /**
     * SetVideoMaxBitrate :
     * This method is used to set maximum bitrate value for the streaming video
     * @param maxBitrateKbps – which represents bitrate value.
     */
    fun setVideoMaxBitrate(maxBitrateKbps: Int?)

    /**
     * SwitchCamera :
     * This method is used to switch camera to front or back.
     */
    fun switchCamera()

    /**
     * This method is used to get current status of WebRTCSessionState of peer connection
     *
     * @return WebRTCSessionState
     */
    fun getWebRTCSessionState(): WebRTCSession.WebRTCSessionState

    /**
     * muteRemoteAudio :
     * This method is used to enable or disable remote audio stream
     * @param enable – which is true if to enable otherwise false
     */
    fun muteRemoteAudio(enable: Boolean)

}

/**
 * IWebRTCSessionCallback
 * Callback interface for getting progress & error event from the web RTC Session
 * IWebRTCSession user MUST implement this interface to SDP & ICE candidates apart from the
 * session related events.
 */
interface IWebRTCSessionCallback {
    /**
     * OnLocalDescription :
     * This callback method is triggered once local SDP is created and set it to local peer connection
     * and send it to remote peer connection
     * @param sdp - Changes the local connection description. The description defines the properties of the
     * connection. This session description is set as the local description to peer connection.
     */
    fun onLocalDescription(sdp: SessionDescription?)

    /**
     * OnIceCandidate :
     * This callback method is triggered once local Ice candidate is generated and send it to remote
     * peer connection
     * @param candidate - which is candidate generated by local peer sent.
     */
    fun onIceCandidate(candidate: IceCandidate?)

    /**
     * OnIceCandidatesRemoved :
     * This callback method is triggered once candidate removed from local peer connection.
     * @param candidates - which is candidate list removed from peer.
     */
    fun onIceCandidatesRemoved(candidates: Array<IceCandidate>)

    /**
     * onConnectionStateChanged :
     * This callback method is triggered once connection State changed as connected or disconnected.
     *
     * @param webRTCSessionState
     */
    fun onConnectionStateChanged(webRTCSessionState: WebRTCSession.WebRTCSessionState)

    /**
     * OnPeerConnectionError :
     * This callback method is triggered once peer connection error happened.
     * @param description - which is description of error
     */
    fun onPeerConnectionError(description: String?)

    /**
     * This  callback method is triggered once WebRtcSession initialized
     *
     * @param mEglBase EglBase
     */
    fun onSetEglBase(mEglBase: EglBase)
}
