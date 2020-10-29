/*
 * Created by ADT author on 9/14/20 11:00 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/14/20 10:49 PM
 */

package com.adt.vpm.webrtc

import com.adt.vpm.webrtc.data.IceCandidate
import com.adt.vpm.webrtc.data.IceServer
import com.adt.vpm.webrtc.data.SessionDescription

/**
 * This class is used to have peer connection parameters which is passed to WebRtcSession class to establish peer connection
 * Config and default values getting from settings page
 */
class WebRTCParams(
    var localVideoEnabled: Boolean,
    var localAudioEnabled: Boolean,
    var remoteAudioEnabled: Boolean,
    val useCamera2: Boolean,
    val captureToTexture: Boolean,
    val videoWidth: Int,
    val videoHeight: Int,
    val videoFps: Int,
    val videoMaxBitrate: Int,
    val videoCodec: String,
    val videoCodecHwAcceleration: Boolean,
    val audioStartBitrate: Int,
    val audioCodec: String?
)

/**
 * This class is used to have signaling parameters which is passed to WebRtcSession class to establish peer connection
 */
data class SignalingParameters(
    @kotlin.jvm.JvmField
    var iceServers: MutableList<IceServer>,
    val initiator: Boolean,
    val clientId: String?,
    val offerSdp: SessionDescription?,
    val iceCandidates: List<IceCandidate>?
)