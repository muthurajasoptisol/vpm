/*
 * Created by ADT author on 9/14/20 11:00 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/14/20 10:49 PM
 */

package com.adt.vpm.webrtc

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.adt.vpm.util.Log
import com.adt.vpm.util.LogMsg
import com.adt.vpm.util.VPMLogger
import com.adt.vpm.webrtc.data.IceCandidate
import com.adt.vpm.webrtc.data.IceCandidate.DataObj.getCandidateData
import com.adt.vpm.webrtc.data.IceCandidate.DataObj.getCandidateDataList
import com.adt.vpm.webrtc.data.IceCandidate.DataObj.getIceCandidate
import com.adt.vpm.webrtc.data.IceCandidate.DataObj.getIceCandidateList
import com.adt.vpm.webrtc.data.IceCandidate.DataObj.getIceServerList
import com.adt.vpm.webrtc.data.SessionDescription
import com.adt.vpm.webrtc.data.SessionDescription.DataObj.getSdpData
import org.webrtc.*
import org.webrtc.PeerConnection.*
import org.webrtc.PeerConnection.Observer
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule.*
import java.util.*
import java.util.concurrent.Executors
import java.util.regex.Pattern
import kotlin.collections.ArrayList

/*
*  Copyright 2014 The WebRTC Project Authors. All rights reserved.
*
*  Use of this source code is governed by a BSD-style license
*  that can be found in the LICENSE file in the root of the source
*  tree. An additional intellectual property rights grant can be found
*  in the file PATENTS.  All contributing project authors may
*  be found in the AUTHORS file in the root of the source tree.
*/

/**
 * Peer connection client implementation.
 *
 * All public methods are routed to local looper thread.
 * All PeerConnectionEvents callbacks are invoked from the same looper thread.
 * This class is a singleton.
 */
class WebRTCSession(
    private val appContext: Context?,
    private val webRTCParams: WebRTCParams,
    private val events: IWebRTCSessionCallback
) : IWebRTCSession {
    private val pcObserver = PCObserver()
    private val sdpObserver = SDPObserver()
    private val statsTimer = Timer()
    private var factory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var audioSource: AudioSource? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var videoSource: VideoSource? = null
    private var preferIsac = false
    private var videoCapturerStopped = false
    private var isError = false
    private var localRender: VideoSink? = null
    private var remoteSinks: List<VideoSink?>? = null
    private var signalingParameters: SignalingParameters? = null
    private var videoWidth = 0
    private var videoHeight = 0
    private var videoFps = 0
    private var audioConstraints: MediaConstraints? = null
    private var sdpMediaConstraints: MediaConstraints? = null

    // Queued remote ICE candidates are consumed only after both local and
    // remote descriptions are set. Similarly local ICE candidates are sent to
    // remote peer after both local and remote description are set.
    private var queuedRemoteCandidates: MutableList<org.webrtc.IceCandidate>? = null
    private var isInitiator = false
    private var localSdp: org.webrtc.SessionDescription? = null // either offer or answer SDP
    private var videoCapturer: VideoCapturer? = null

    // enableVideo is set to true if video should be rendered and sent.
    private var muteLocalVideo = true
    private var localVideoTrack: VideoTrack? = null
    private var remoteVideoTrack: VideoTrack? = null
    private var remoteAudioTrack: AudioTrack? = null
    private var localVideoSender: RtpSender? = null

    // enableAudio is set to true if audio should be sent.
    private var muteLocalAudio = true
    private var localAudioTrack: AudioTrack? = null

    private var enableLocalAudio = true
    private var enableLocalVideo = true
    private var enableRemoteVideo = true
    private var enableRemoteAudio = true

    private val rootEglBase: EglBase? = EglBase.create()
    private val mainThreadHandler = Handler(Looper.getMainLooper())

    enum class WebRTCSessionState { NEW, CONNECTING, CONNECTED, DISCONNECTED, FAILED }

    private var webRTCSessionState: WebRTCSessionState = WebRTCSessionState.NEW

    companion object {
        const val MEDIA_STREAM_PREFIX = "VPMMS"
        const val VIDEO_TRACK_ID = "VPMMSv0"
        const val AUDIO_TRACK_ID = "VPMMSa0"
        const val VIDEO_TRACK_TYPE = "video"
        private const val TAG = "WebRTCSession"
        private const val VIDEO_CODEC_VP8 = "VP8"
        private const val VIDEO_CODEC_VP9 = "VP9"
        private const val VIDEO_CODEC_H264 = "H264"
        private const val VIDEO_CODEC_H264_BASELINE = "H264 Baseline"
        private const val VIDEO_CODEC_H264_HIGH = "H264 High"
        private const val AUDIO_CODEC_OPUS = "opus"
        private const val AUDIO_CODEC_ISAC = "ISAC"
        private const val VIDEO_VP8_INTEL_HW_ENCODER_FIELDTRIAL = "WebRTC-IntelVP8/Enabled/"
        private const val AUDIO_CODEC_PARAM_BITRATE = "maxaveragebitrate"
        private const val HD_VIDEO_WIDTH = 1280
        private const val HD_VIDEO_HEIGHT = 720
        private const val BPS_IN_KBPS = 1000

        // Executor thread is started once in private factor and is used for all
        // peer connection API calls to ensure new peer connection factory is
        // created on the same thread as previously destroyed factory.
        private val executor = Executors.newSingleThreadExecutor()

        /**
         * This method is used to get video codec name based on selected video codec which is in WebRtc Param
         *
         * @param parameters WebRtc Params
         *
         * @return codec name
         */
        private fun getSdpVideoCodecName(parameters: WebRTCParams?): String {
            return when (parameters?.videoCodec) {
                VIDEO_CODEC_VP8 -> VIDEO_CODEC_VP8
                VIDEO_CODEC_VP9 -> VIDEO_CODEC_VP9
                VIDEO_CODEC_H264_HIGH, VIDEO_CODEC_H264_BASELINE -> VIDEO_CODEC_H264
                else -> VIDEO_CODEC_VP8
            }
        }

        /**
         * This method is used to get hardware encoder field trial
         *
         * @return Video field trial value
         */
        private fun getFieldTrials(): String {
            return VIDEO_VP8_INTEL_HW_ENCODER_FIELDTRIAL
        }

        /**
         * This method is used to set start bitrate for video stream
         *
         * @param sdpDescription session description
         * @param bitrateKbps bitrate kbps
         *
         * @return session description
         */
        private fun setStartBitrate(
            sdpDescription: String,
            bitrateKbps: Int
        ): String {
            val lines = sdpDescription.split("\r\n".toRegex()).toTypedArray()
            var rtpMapLineIndex = -1
            var sdpFormatUpdated = false
            var codecRtpMap: String? = null
            // Search for codec rtpmap in format
            // a=rtpmap:<payload type> <encoding name>/<clock rate> [/<encoding parameters>]
            var regex = "^a=rtpmap:(\\d+) $AUDIO_CODEC_OPUS(/\\d+)+[\r]?$"
            var codecPattern = Pattern.compile(regex)
            for (i in lines.indices) {
                val codecMatcher = codecPattern.matcher(lines[i])
                if (codecMatcher.matches()) {
                    codecRtpMap = codecMatcher.group(1)
                    rtpMapLineIndex = i
                    break
                }
            }
            if (codecRtpMap == null) {
                Log.w(TAG, "No rtpmap for $AUDIO_CODEC_OPUS codec")
                return sdpDescription
            }
            Log.d(
                TAG,
                "Found " + AUDIO_CODEC_OPUS + " rtpmap " + codecRtpMap + " at " + lines[rtpMapLineIndex]
            )

            // Check if a=fmtp string already exist in remote SDP for this codec and
            // update it with new bitrate parameter.
            regex = "^a=fmtp:$codecRtpMap \\w+=\\d+.*[\r]?$"
            codecPattern = Pattern.compile(regex)
            for (i in lines.indices) {
                val codecMatcher = codecPattern.matcher(lines[i])
                if (codecMatcher.matches()) {
                    Log.d(TAG, "Found " + AUDIO_CODEC_OPUS + " " + lines[i])
                    lines[i] += "; " + AUDIO_CODEC_PARAM_BITRATE + "=" + bitrateKbps * 1000
                    Log.d(TAG, "Update remote SDP line: " + lines[i])
                    sdpFormatUpdated = true
                    break
                }
            }
            val newSdpDescription = StringBuilder()
            for (i in lines.indices) {
                newSdpDescription.append(lines[i]).append("\r\n")
                // Append new a=fmtp line if no such line exist for a codec.
                if (!sdpFormatUpdated && i == rtpMapLineIndex) {
                    val bitrateSet: String =
                        ("a=fmtp:" + codecRtpMap + " " + AUDIO_CODEC_PARAM_BITRATE + "="
                                + bitrateKbps * 1000)
                    Log.d(TAG, "Add remote SDP line: $bitrateSet")
                    newSdpDescription.append(bitrateSet).append("\r\n")
                }
            }
            return newSdpDescription.toString()
        }

        /**
         * Returns the line number containing "m=audio|video", or -1 if no such line exists.
         */
        private fun findMediaDescriptionLine(isAudio: Boolean, sdpLines: Array<String>): Int {
            val mediaDescription = if (isAudio) "m=audio " else "m=video "
            for (i in sdpLines.indices) {
                if (sdpLines[i].startsWith(mediaDescription)) {
                    return i
                }
            }
            return -1
        }

        /**
         * This method is used to join string with session description string
         */
        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        private fun joinString(
            s: Iterable<CharSequence?>,
            delimiter: String,
            delimiterAtEnd: Boolean
        ): String {
            val iterator = s.iterator()
            if (!iterator.hasNext()) {
                return ""
            }
            val buffer = StringBuilder(iterator.next())
            while (iterator.hasNext()) {
                buffer.append(delimiter).append(iterator.next())
            }
            if (delimiterAtEnd) {
                buffer.append(delimiter)
            }
            return buffer.toString()
        }

        /**
         * This method is used to reconstruct the line with |preferredPayloadTypes| moved to the beginning of the payload types.
         */
        private fun movePayloadTypesToFront(
            preferredPayloadTypes: List<String?>,
            mLine: String
        ): String? {
            // The format of the media description line should be: m=<media> <port> <proto> <fmt> ...
            val origLineParts = listOf(*mLine.split(" ".toRegex()).toTypedArray())
            if (origLineParts.size <= 3) {
                Log.e(TAG, "Wrong SDP media description format: $mLine")
                return null
            }
            val header: List<String?> = origLineParts.subList(0, 3)
            val unPreferredPayloadTypes: MutableList<String?> =
                ArrayList(origLineParts.subList(3, origLineParts.size))
            unPreferredPayloadTypes.removeAll(preferredPayloadTypes)

            // Reconstruct the line with |preferredPayloadTypes| moved to the beginning of the payload types.
            val newLineParts: MutableList<String?> = ArrayList()
            newLineParts.addAll(header)
            newLineParts.addAll(preferredPayloadTypes)
            newLineParts.addAll(unPreferredPayloadTypes)

            return joinString(newLineParts, " ", false)
        }

        /**
         * This method is used to set preferred codec in session description
         */
        private fun preferCodec(
            sdpDescription: String,
            codec: String,
            isAudio: Boolean
        ): String {
            val lines = sdpDescription.split("\r\n".toRegex()).toTypedArray()
            val mLineIndex = findMediaDescriptionLine(isAudio, lines)
            if (mLineIndex == -1) {
                Log.w(TAG, "No mediaDescription line, so can't prefer $codec")
                return sdpDescription
            }
            // A list with all the payload types with name |codec|. The payload types are integers in the
            // range 96-127, but they are stored as strings here.
            val codecPayloadTypes: MutableList<String?> = ArrayList()
            // a=rtpmap:<payload type> <encoding name>/<clock rate> [/<encoding parameters>]
            val codecPattern = Pattern.compile("^a=rtpmap:(\\d+) $codec(/\\d+)+[\r]?$")
            for (line in lines) {
                val codecMatcher = codecPattern.matcher(line)
                if (codecMatcher.matches()) {
                    codecPayloadTypes.add(codecMatcher.group(1))
                }
            }
            if (codecPayloadTypes.isEmpty()) {
                Log.w(TAG, "No payload types with name $codec")
                return sdpDescription
            }
            val newMLine = movePayloadTypesToFront(codecPayloadTypes, lines[mLineIndex])
                ?: return sdpDescription
            lines[mLineIndex] = newMLine
            return joinString(listOf(*lines), "\r\n", true)
        }
    }

    /**
     * Create a PeerConnectionClient with the specified parameters. PeerConnectionClient takes
     * ownership of |eglBase|.
     * CreatePeerConnectionFactory :
     * This method is used to create a builder object used to create peer connection instance
     * and initialized before create peer connection.
     */
    init {
        mainThreadHandler.post { this.rootEglBase?.let { events.onSetEglBase(it) } }
        val fieldTrials = getFieldTrials()
        executor.execute {
            Log.d(TAG, "Initialize WebRTC. Field trials: $fieldTrials")
            PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(appContext)
                    .setFieldTrials(fieldTrials)
                    .setInjectableLogger(VPMLogger(), Logging.Severity.LS_VERBOSE)
                    .setEnableInternalTracer(true)
                    .createInitializationOptions()
            )
        }
        check(factory == null) { "PeerConnectionFactory has already been constructed" }
        executor.execute {
            createPeerConnectionFactoryInternal(PeerConnectionFactory.Options())
        }

    }


    /**
     * This method will be invoked from WebRtcService class once Peer connection factory created.
     * This method is used to create PeerConnection instance. PeerConnection instance allows an
     * application to establish peer-to-peer communications with another PeerConnection instance.
     * @param localRender – which contains source of videoSink to render local stream
     * @param remoteSink - which contains source of videoSink to render remote stream
     * @param signalingParameters – which contains the parameters about signaling which is needed in order
     *  for two peers to share how they should connect.
     */
    override fun createPeerConnection(
        localRender: VideoSink?, remoteSink: VideoSink?, signalingParameters: SignalingParameters?
    ) {
        /*
         Prepare media constraints
         if videocapturer is not provided, the assumption is
         that the intention is not to transfer video

         if remote video sink is not provided , the assumption is
         that the intention is not to receive video
         */
        if (webRTCParams.localVideoEnabled) videoCapturer = createVideoCapturer()

        enableLocalAudio = webRTCParams.localAudioEnabled
        enableLocalVideo = videoCapturer != null

        enableRemoteAudio = webRTCParams.remoteAudioEnabled
        enableRemoteVideo = remoteSink != null

        createPeerConnection(localRender, listOf(remoteSink), signalingParameters)
    }

    /**
     * This method will be invoked from WebRtcService class when socket room connection get connected as initiator
     * This method is used to Creates an offer(request) to find a remote peer. The two first
     * parameters of this method are success and error callbacks. The optional third parameter are
     * options, like enabling audio or video streams
     */
    override fun createOffer() {
        executor.execute {
            if (!isError) {
                peerConnection?.let {
                    isInitiator = true
                    it.createOffer(sdpObserver, sdpMediaConstraints)
                    Log.i(TAG, LogMsg.msg_create_offer)
                }

            }
        }
    }

    /**
     * This method will be invoked from WebRtcService class when socket room connection get connected and received remote sdp as joiner
     * This method is used to Creates an answer to the offer received by the remote peer during the
     * offer/answer negotiation process. The two first parameters of this method are success and
     * error callbacks. The optional third parameter are options for the answer to be created.
     */
    override fun createAnswer() {
        executor.execute {
            if (!isError) {
                peerConnection?.let {
                    isInitiator = false
                    it.createAnswer(sdpObserver, sdpMediaConstraints)
                    Log.i(TAG, LogMsg.msg_create_answer)
                }

            }
        }
    }

    /**
     * This method will be invoked from WebRtcService class when receive remote IceCandidate which to be added to peer via Socket client
     * This method Provides a remote candidate to the Interactive Connectivity Establishment (ICE) server for establishing a peer connection
     * @param candidate – which represents a new candidate that should be sent to the remote peer.
     */
    override fun addRemoteIceCandidate(candidate: IceCandidate) {
        executor.execute {
            if (!isError) {
                queuedRemoteCandidates?.add(getIceCandidate(candidate))
                    ?: peerConnection?.let {
                        it.addIceCandidate(getIceCandidate(candidate))
                        Log.i(TAG, LogMsg.msg_add_candidates)
                    }

            }
        }
    }

    /**
     * This method will be invoked from WebRtcService class when receive remote IceCandidate which to be removed from peer via Socket client
     * This method is used to remove remote ice candidate which is received from remote peer.
     * @param candidates – which is candidate list need to be removed.
     */
    override fun removeRemoteIceCandidates(candidates: Array<IceCandidate>) {
        executor.execute {
            if (peerConnection == null || isError) {
                return@execute
            }
            // Drain the queued remote candidates if there is any so that
            // they are processed in the proper order.
            drainCandidates()
            peerConnection?.removeIceCandidates(getIceCandidateList(candidates))
            Log.i(TAG, LogMsg.msg_remove_candidates)
        }
    }

    /**
     * This method is used to get current status of WebRTCSessionState of peer connection
     *
     * @return WebRTCSessionState
     */
    override fun getWebRTCSessionState(): WebRTCSessionState {
        return webRTCSessionState
    }

    /**
     * This method will be invoked from WebRtcService class when receive remote sdp from Socket client
     * This method instructs the Peer Connection to apply the supplied Session Description as the
     * remote offer or answer. This API changes the local media state.
     * @param sdp - Changes the remote connection description. The description defines the properties
     * of the connection. This session description is set as the remote description to peer connection.
     */
    override fun setRemoteDescription(sdp: SessionDescription) {
        executor.execute {
            if (peerConnection == null || isError) {
                return@execute
            }
            var sdpDescription = sdp.description
            if (preferIsac) {
                sdpDescription = preferCodec(sdpDescription, AUDIO_CODEC_ISAC, true)
            }
            if (enableRemoteVideo) {
                sdpDescription = preferCodec(
                    sdpDescription,
                    getSdpVideoCodecName(
                        webRTCParams
                    ),
                    false
                )
            }
            if (webRTCParams.audioStartBitrate > 0) {
                sdpDescription = setStartBitrate(sdpDescription, webRTCParams.audioStartBitrate)
            }
            val sdpRemote = SessionDescription(
                org.webrtc.SessionDescription.Type.fromCanonicalForm(sdp.type),
                "${sdpDescription.trim()}\r\n"
            )
            peerConnection?.setRemoteDescription(sdpObserver, sdpRemote)
        }
    }

    /**
     * This method is used to enable or disable remote audio stream
     * @param enable – which is true if to enable otherwise false
     */
    override fun muteRemoteAudio(enable: Boolean) {
        executor.execute {
            remoteAudioTrack?.setEnabled(enable)
            Log.d(TAG, String.format(LogMsg.msg_mute_remote_audio, enable.toString()))
        }
    }

    /**
     * This method will be invoked from WebRtcService class when click on microphone icon which is in call page
     * This method is used to enable or disable local audio stream
     * @param enable – which is true if to enable otherwise false
     */
    override fun muteLocalAudio(enable: Boolean) {
        executor.execute {
            muteLocalAudio = enable
            localAudioTrack?.setEnabled(muteLocalAudio)
            Log.d(TAG, String.format(LogMsg.msg_mute_local_audio, enable.toString()))
        }
    }

    /**
     * This method will be invoked from WebRtcService class when click on video icon which is in call page
     * This method is used to enable or disable local video stream
     * @param enable – which is true if to enable otherwise false
     */
    override fun muteLocalVideo(enable: Boolean) {
        executor.execute {
            muteLocalVideo = enable
            localVideoTrack?.setEnabled(muteLocalVideo)
            Log.d(TAG, String.format(LogMsg.msg_mute_local_video, enable.toString()))
        }
    }

    /**
     * This method will be invoked from WebRtcService class when need to change max bit rate
     * This method is used to set maximum bitrate value for the streaming video
     * @param maxBitrateKbps – which represents bitrate value.
     */
    override fun setVideoMaxBitrate(maxBitrateKbps: Int?) {
        executor.execute {
            if (peerConnection == null || localVideoSender == null || isError) {
                return@execute
            }
            if (localVideoSender == null) {
                Log.w(TAG, LogMsg.msg_sender_not_ready)
                return@execute
            }
            val parameters = localVideoSender?.parameters
            if (parameters?.encodings?.size == 0) {
                Log.w(TAG, LogMsg.msg_rtp_param_not_ready)
                return@execute
            }
            for (encoding in parameters!!.encodings) {
                // Null value means no limit.
                encoding.maxBitrateBps =
                    if (maxBitrateKbps == null) null else maxBitrateKbps * BPS_IN_KBPS
            }
            if (!localVideoSender!!.setParameters(parameters)) {
                Log.e(TAG, LogMsg.msg_rtp_send_param_failed)
            }
            Log.d(TAG, String.format(LogMsg.msg_max_video_bitrate, maxBitrateKbps.toString()))
        }
    }

    /**
     * This method will be invoked from WebRtcService class when click on camera switch icon which is in call page
     * This method is used to switch camera to front or back.
     */
    override fun switchCamera() {
        executor.execute { switchCameraInternal() }
    }

    /**
     * This method will be invoked when get disconnected the call or socket connection.
     * It is used to close peer connection while user disconnect the call.
     */
    override fun close() {
        executor.execute { closeInternal() }
    }

    /**
     * This method will be invoked from WebRtcService class once Peer connection factory created.
     * This method is used to create PeerConnection instance. PeerConnection instance allows an
     * application to establish peer-to-peer communications with another PeerConnection instance.
     * @param localRender – which contains source of videoSink to render local stream
     * @param remoteSinks - which contains list of videoSink to refer remote videoSink
     * @param signalingParameters – which contains the parameters about signaling which is needed in order
     *  for two peers to share how they should connect.
     */
    private fun createPeerConnection(
        localRender: VideoSink?,
        remoteSinks: List<VideoSink?>?,
        signalingParameters: SignalingParameters?
    ) {

        this.localRender = localRender
        this.remoteSinks = remoteSinks
        this.signalingParameters = signalingParameters
        executor.execute {
            try {
                createMediaConstraintsInternal()
                createPeerConnectionInternal()
            } catch (e: Exception) {
                reportError(String.format(LogMsg.error_peer_failed_to_create, e.message))
                throw e
            }
        }
    }

    /**
     * This method will be triggered internally from createPeerConnection method to create peer connection
     */
    private fun createPeerConnectionInternal() {
        if (factory == null || isError) {
            Log.e(TAG, LogMsg.error_peer_connection_not_created)
            return
        }

        queuedRemoteCandidates = ArrayList()
        val rtcConfig =
            RTCConfiguration(signalingParameters?.iceServers?.let { getIceServerList(it) })
        // TCP candidates are only useful when connecting to a server that supports
        // ICE-TCP.
        rtcConfig.tcpCandidatePolicy = TcpCandidatePolicy.ENABLED
        rtcConfig.bundlePolicy = BundlePolicy.MAXBUNDLE
        rtcConfig.rtcpMuxPolicy = RtcpMuxPolicy.REQUIRE
        rtcConfig.continualGatheringPolicy = ContinualGatheringPolicy.GATHER_CONTINUALLY
        // Use ECDSA encryption.
        rtcConfig.keyType = KeyType.ECDSA
        // Enable DTLS for normal calls and disable for loopback calls.
        rtcConfig.enableDtlsSrtp = true
        rtcConfig.sdpSemantics = SdpSemantics.UNIFIED_PLAN
        peerConnection = factory?.createPeerConnection(rtcConfig, pcObserver)

        isInitiator = false
        val mediaStreamLabels = listOf(MEDIA_STREAM_PREFIX)
        if (enableLocalVideo) {
            peerConnection?.addTrack(createVideoTrack(videoCapturer), mediaStreamLabels)
            findVideoSender()
        }

        if (enableLocalAudio) {
            peerConnection?.addTrack(createAudioTrack(), mediaStreamLabels)
        }

        Log.i(TAG, LogMsg.msg_peer_connected)
    }

    /**
     * This method will be triggered internally  from createPeerConnection method to create media constraints
     */
    private fun createMediaConstraintsInternal() {
        // Create video constraints if video call is enabled.
        if (enableLocalVideo) {
            videoWidth = webRTCParams.videoWidth
            videoHeight = webRTCParams.videoHeight
            videoFps = webRTCParams.videoFps

            // If video resolution is not specified, default to HD.
            if (videoWidth == 0 || videoHeight == 0) {
                videoWidth = HD_VIDEO_WIDTH
                videoHeight = HD_VIDEO_HEIGHT
            }

            // If fps is not specified, default to 30.
            if (videoFps == 0) videoFps = 30

            Log.d(TAG, "Capturing format: " + videoWidth + "x" + videoHeight + "@" + videoFps)
        }

        // Create audio constraints.
        audioConstraints = MediaConstraints()

        // Create SDP constraints.
        sdpMediaConstraints = MediaConstraints()
        sdpMediaConstraints!!.mandatory.add(
            MediaConstraints.KeyValuePair("OfferToReceiveAudio", enableRemoteAudio.toString())
        )
        sdpMediaConstraints!!.mandatory.add(
            MediaConstraints.KeyValuePair("OfferToReceiveVideo", enableRemoteVideo.toString())
        )
        Log.i(
            TAG,
            String.format(LogMsg.msg_remote_offer_receive_audio, enableRemoteAudio.toString())
        )
        Log.i(
            TAG, String.format(LogMsg.msg_remote_offer_receive_video, enableRemoteVideo.toString())
        )
    }

    /**
     * This method will be triggered internally from createPeerConnectionFactory method
     */
    private fun createPeerConnectionFactoryInternal(options: PeerConnectionFactory.Options?) {
        isError = false

        // Check if ISAC is used by default.
        preferIsac = (webRTCParams.audioCodec != null
                && webRTCParams.audioCodec == AUDIO_CODEC_ISAC)

        val adm = createJavaAudioDevice()

        // Create peer connection factory.
        val enableH264HighProfile = VIDEO_CODEC_H264_HIGH == webRTCParams.videoCodec
        val encoderFactory: VideoEncoderFactory
        val decoderFactory: VideoDecoderFactory
        if (webRTCParams.videoCodecHwAcceleration) {
            encoderFactory = DefaultVideoEncoderFactory(
                rootEglBase?.eglBaseContext,
                true /* enableIntelVp8Encoder */,
                enableH264HighProfile
            )
            decoderFactory = DefaultVideoDecoderFactory(rootEglBase?.eglBaseContext)
        } else {
            encoderFactory = SoftwareVideoEncoderFactory()
            decoderFactory = SoftwareVideoDecoderFactory()
        }

        factory = PeerConnectionFactory.builder()
            .setOptions(options)
            .setAudioDeviceModule(adm)
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory()

        Log.v(TAG, LogMsg.msg_peer_factory_created)

        adm.release()
    }


    /**
     * This method will invoked once video track added which is used to find the sender from peer connection
     */
    private fun findVideoSender() {
        for (sender in peerConnection!!.senders) {
            if (sender.track() != null) {
                val trackType = sender.track()?.kind()
                if (trackType == VIDEO_TRACK_TYPE) {
                    localVideoSender = sender
                }
            }
        }
    }

    /**
     * This method will be invoked from AddTrack callback method which to be added as remote video track
     */
    private fun setRemoteVideoRenderer(track: MediaStreamTrack?) {
        if (enableRemoteVideo && remoteVideoTrack == null && track is VideoTrack) {
            remoteVideoTrack = getRemoteVideoTrack()
            remoteVideoTrack?.setEnabled(true)
            for (remoteSink in remoteSinks!!) {
                remoteVideoTrack?.addSink(remoteSink)
            }
            Log.i(TAG, LogMsg.msg_set_remote_video_track)
        }
    }

    /**
     * This method will be invoked internally from onConnectedToRoom method if isVideoCalled param is true
     *
     * @return videoCapturer
     */
    private fun createVideoCapturer(): VideoCapturer? {
        val videoCapturer: VideoCapturer? = when {
            useCamera2() -> {
                if (!webRTCParams.captureToTexture) {
                    reportError(LogMsg.camera2_texture_only_error)
                    return null
                }
                createCameraCapturer(Camera2Enumerator(appContext))
            }
            else -> {
                createCameraCapturer(Camera1Enumerator(webRTCParams.captureToTexture))
            }
        }
        if (videoCapturer == null) {
            reportError("Failed to open camera")
            return null
        }
        return videoCapturer
    }

    /**
     * This method will be invoked internally from createVideoCapturer method
     *
     * @param enumerator CameraEnumerator
     */
    private fun createCameraCapturer(enumerator: CameraEnumerator): VideoCapturer? {
        val deviceNames = enumerator.deviceNames

        // First, try to find front facing camera
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                val videoCapturer: VideoCapturer? = enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }

        // Front facing camera not found, try something else
        Log.d(TAG, "Looking for other cameras.")
        for (deviceName in deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Log.d(TAG, "Creating other camera capturer.")
                val videoCapturer: VideoCapturer? = enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }
        return null
    }

    /**
     * This method is used to create audio device module to manage with audio and error callback method
     */
    private fun createJavaAudioDevice(): AudioDeviceModule {
        // Set audio record error callbacks.
        val audioRecordErrorCallback: AudioRecordErrorCallback = object : AudioRecordErrorCallback {
            override fun onWebRtcAudioRecordInitError(errorMessage: String) {
                reportError(String.format(LogMsg.error_audio_record_init, errorMessage))
            }

            override fun onWebRtcAudioRecordStartError(
                errorCode: AudioRecordStartErrorCode,
                errorMessage: String
            ) {
                reportError(String.format(LogMsg.error_audio_record_start, errorMessage))
            }

            override fun onWebRtcAudioRecordError(errorMessage: String) {
                reportError(String.format(LogMsg.error_audio_record, errorMessage))
            }
        }
        val audioTrackErrorCallback: AudioTrackErrorCallback = object : AudioTrackErrorCallback {
            override fun onWebRtcAudioTrackInitError(errorMessage: String) {
                reportError(String.format(LogMsg.error_audio_record_track_init, errorMessage))
            }

            override fun onWebRtcAudioTrackStartError(
                errorCode: AudioTrackStartErrorCode,
                errorMessage: String
            ) {
                reportError(String.format(LogMsg.error_audio_record_track_start, errorMessage))
            }

            override fun onWebRtcAudioTrackError(errorMessage: String) {
                reportError(String.format(LogMsg.error_audio_record_track, errorMessage))
            }
        }

        // Set audio record state callbacks.
        val audioRecordStateCallback: AudioRecordStateCallback = object : AudioRecordStateCallback {
            override fun onWebRtcAudioRecordStart() {
            }

            override fun onWebRtcAudioRecordStop() {
            }
        }

        // Set audio track state callbacks.
        val audioTrackStateCallback: AudioTrackStateCallback = object : AudioTrackStateCallback {
            override fun onWebRtcAudioTrackStart() {
            }

            override fun onWebRtcAudioTrackStop() {
            }
        }
        return builder(appContext)
            .setUseHardwareAcousticEchoCanceler(false)
            .setUseHardwareNoiseSuppressor(false)
            .setAudioRecordErrorCallback(audioRecordErrorCallback)
            .setAudioTrackErrorCallback(audioTrackErrorCallback)
            .setAudioRecordStateCallback(audioRecordStateCallback)
            .setAudioTrackStateCallback(audioTrackStateCallback)
            .createAudioDeviceModule()
    }


    /**
     * This method will be invoked when get error related to peer connection
     *
     * @param errorMessage description message of error
     */
    private fun reportError(errorMessage: String) {
        Log.e(TAG, errorMessage)
        executor.execute {
            if (!isError) {
                mainThreadHandler.post { events.onPeerConnectionError(errorMessage) }
                isError = true
            }
        }
    }

    /**
     * This method used to create audio track to manage audio streams of peer connection
     *
     * @return localAudioTrack
     */
    private fun createAudioTrack(): AudioTrack? {
        audioSource = factory?.createAudioSource(audioConstraints)
        localAudioTrack = factory?.createAudioTrack(AUDIO_TRACK_ID, audioSource)
        localAudioTrack?.setEnabled(muteLocalAudio)
        return localAudioTrack
    }

    /**
     * This method used to create video track to manage video streams of peer connection
     *
     * @return localVideoTrack
     */
    private fun createVideoTrack(capturer: VideoCapturer?): VideoTrack? {
        surfaceTextureHelper =
            SurfaceTextureHelper.create("CaptureThread", rootEglBase?.eglBaseContext)
        videoSource = capturer?.isScreencast?.let { factory?.createVideoSource(it) }
        capturer?.initialize(
            surfaceTextureHelper,
            appContext,
            videoSource?.capturerObserver
        )
        capturer?.startCapture(videoWidth, videoHeight, videoFps)
        localVideoTrack = factory?.createVideoTrack(VIDEO_TRACK_ID, videoSource)
        localVideoTrack?.setEnabled(muteLocalVideo)
        localVideoTrack?.addSink(localRender)
        return localVideoTrack
    }

    /**
     * This method will invoked once video track added which is used to find the sender from peer connection
     * Returns the remote VideoTrack, assuming there is only one.
     */
    private fun getRemoteVideoTrack(): VideoTrack? {
        for (transceiver in peerConnection!!.transceivers) {
            val track = transceiver.receiver.track()
            if (track is VideoTrack) {
                return track
            }
        }
        return null
    }

    /**
     * This method will be invoked internally from switchCamera method
     */
    private fun switchCameraInternal() {
        if (videoCapturer is CameraVideoCapturer) {
            if (!enableLocalVideo || isError) {
                Log.e(TAG, "Failed to switch camera. Video: $enableLocalVideo. Error : $isError")
                return  // No video is sent or only one camera is available or error happened.
            }
            Log.d(TAG, "Switch camera")
            val cameraVideoCapturer = videoCapturer as CameraVideoCapturer
            cameraVideoCapturer.switchCamera(null)
        } else {
            Log.d(TAG, "Will not switch camera, video caputurer is not a camera")
        }
    }


    /**
     * This method used to check camera2 API support
     */
    private fun useCamera2(): Boolean {
        return Camera2Enumerator.isSupported(appContext) && webRTCParams.useCamera2
    }

    /**
     * This method will be invoked from AddTrack callback method which to be added as remote audio track
     */
    private fun setRemoteAudioTrack(audioTracks: MediaStreamTrack?) {
        if (remoteAudioTrack == null && audioTracks is AudioTrack) {
            remoteAudioTrack = audioTracks
            remoteAudioTrack?.setEnabled(enableRemoteAudio)
            Log.i(TAG, LogMsg.msg_set_remote_audio_track)
        }
    }

    /**
     * This class is used as session description observer to get updates from peer connection
     * Implementation detail: handle offer creation/signaling and answer setting,
     * as well as adding remote ICE candidates once the answer SDP is set.
     */
    private inner class SDPObserver : SdpObserver {
        /**
         * This method will be triggered once session description created successfully
         *
         * @param origSdp session description
         */
        override fun onCreateSuccess(origSdp: org.webrtc.SessionDescription) {
            if (localSdp != null) {
                reportError(LogMsg.error_multiple_sdp_create)
                return
            }
            var sdpDescription = origSdp.description
            if (preferIsac) {
                sdpDescription = preferCodec(sdpDescription, AUDIO_CODEC_ISAC, true)
            }
            if (enableLocalVideo) {
                val videoCodecName = getSdpVideoCodecName(webRTCParams)
                sdpDescription = preferCodec(sdpDescription, videoCodecName, false)
            }
            val sdp = SessionDescription(origSdp.type, "${sdpDescription.trim()}\r\n")
            localSdp = sdp
            executor.execute {
                if (!isError) {
                    peerConnection?.setLocalDescription(sdpObserver, sdp)
                }
            }
        }

        /**
         * This method will be triggered once local sdp, remote sdp or candidates set successfully to peer connection
         */
        override fun onSetSuccess() {
            executor.execute {
                if (peerConnection == null || isError) {
                    return@execute
                }
                if (isInitiator) {
                    // For offering peer connection we first create offer and set
                    // local SDP, then after receiving answer set remote SDP.
                    if (peerConnection?.remoteDescription == null) {
                        // We've just set our local SDP so time to send it.
                        Log.i(TAG, LogMsg.msg_set_local_sdp)
                        mainThreadHandler.post {
                            events.onLocalDescription(localSdp?.let {
                                getSdpData(it)
                            })
                        }
                    } else {
                        // We've just set remote description, so drain remote
                        // and send local ICE candidates.
                        Log.i(TAG, LogMsg.msg_set_remote_sdp)
                        drainCandidates()
                    }
                } else {
                    // For answering peer connection we set remote SDP and then
                    // create answer and set local SDP.
                    if (peerConnection?.localDescription != null) {
                        // We've just set our local SDP so time to send it, drain
                        // remote and send local ICE candidates.
                        Log.i(TAG, LogMsg.msg_set_local_sdp)
                        mainThreadHandler.post {
                            events.onLocalDescription(localSdp?.let {
                                getSdpData(it)
                            })
                        }
                        drainCandidates()
                    } else {
                        // We've just set remote SDP - do nothing for now -
                        // answer will be created soon.
                        Log.i(TAG, LogMsg.msg_set_remote_sdp)
                    }
                }
            }
        }

        /**
         * This method will be triggered if get failure when creating sdp or candidates
         *
         * @param error error message
         */
        override fun onCreateFailure(error: String) {
            reportError(String.format(LogMsg.msg_sdp_create_fail, error))
        }

        /**
         * This method will be triggered if get failure when set sdp or candidates to peer connection
         *
         * @param error error message
         */
        override fun onSetFailure(error: String) {
            reportError(String.format(LogMsg.msg_sdp_set_fail, error))
        }
    }

    /**
     * This class is used as peer connection observer to get updates from peer connection establishment
     * Implementation detail: observe ICE & stream changes and react accordingly.
     */
    private inner class PCObserver : Observer {
        /**
         * This method will be triggered once IceCandidate generated from peer connection
         *
         * @param candidate - which is candidate generated by local peer.
         */
        override fun onIceCandidate(candidate: org.webrtc.IceCandidate) {
            executor.execute {
                mainThreadHandler.post { events.onIceCandidate(getCandidateData(candidate)) }
            }
        }

        /**
         * This method will be triggered once IceCandidate list removed from peer connection
         * @param candidates - which is candidate list removed from peer.
         */
        override fun onIceCandidatesRemoved(candidates: Array<org.webrtc.IceCandidate>) {
            executor.execute {
                mainThreadHandler.post {
                    events.onIceCandidatesRemoved(getCandidateDataList(candidates))
                }
            }
        }

        /**
         * This method will be triggered when signaling state changed
         *
         * @param newState state of changed signaling
         */
        override fun onSignalingChange(newState: SignalingState) {
            Log.d(TAG, "SignalingState: $newState")
        }

        /**
         * This method will be triggered when IceConnection state changed
         *
         * @param newState state of changed IceConnection
         */
        override fun onIceConnectionChange(newState: IceConnectionState) {
            executor.execute {
                Log.d(TAG, String.format(LogMsg.msg_ice_connection_state, newState))
                when (newState) {
                    IceConnectionState.CONNECTED -> {
                        webRTCSessionState = WebRTCSessionState.CONNECTED
                    }
                    IceConnectionState.DISCONNECTED -> {
                        webRTCSessionState = WebRTCSessionState.DISCONNECTED
                    }
                    IceConnectionState.FAILED -> {
                        webRTCSessionState = WebRTCSessionState.FAILED
                        reportError(LogMsg.error_ice_connect_failed)
                    }
                    else -> {
                    }
                }
            }
        }

        /**
         * This method will be triggered when peer connection state changed
         *
         * @param newState state of changed peer connection
         */
        override fun onConnectionChange(newState: PeerConnectionState) {
            executor.execute {
                Log.d(TAG, String.format(LogMsg.msg_peer_connection_state, newState))
                when (newState) {
                    PeerConnectionState.CONNECTING -> {
                        webRTCSessionState = WebRTCSessionState.CONNECTING
                    }
                    PeerConnectionState.CONNECTED -> {
                        webRTCSessionState = WebRTCSessionState.CONNECTED
                        mainThreadHandler.post { events.onConnectionStateChanged(webRTCSessionState) }
                    }
                    PeerConnectionState.DISCONNECTED -> {
                        webRTCSessionState = WebRTCSessionState.DISCONNECTED
                        mainThreadHandler.post { events.onConnectionStateChanged(webRTCSessionState) }
                    }
                    PeerConnectionState.FAILED -> {
                        webRTCSessionState = WebRTCSessionState.FAILED
                        reportError(LogMsg.msg_peer_connection_fail)
                    }
                    else -> {
                    }
                }
            }
        }

        /**
         * This method will be triggered when IceGathering state changed
         *
         * @param newState state of changed IceGathering
         */
        override fun onIceGatheringChange(newState: IceGatheringState) {
            Log.d(TAG, "IceGatheringState: $newState")
        }

        override fun onIceConnectionReceivingChange(receiving: Boolean) {
            Log.d(TAG, "IceConnectionReceiving changed to $receiving")
        }

        override fun onSelectedCandidatePairChanged(event: CandidatePairChangeEvent) {
            Log.d(TAG, "Selected candidate pair changed because: $event")
        }

        /**
         * This method will be triggered once media stream added
         */
        override fun onAddStream(stream: MediaStream) {
        }

        override fun onRemoveStream(stream: MediaStream) {}
        override fun onDataChannel(dc: DataChannel) {
        }

        override fun onRenegotiationNeeded() {
        }

        /**
         * This method will be triggered when video and audio track added
         */
        override fun onAddTrack(receiver: RtpReceiver, mediaStreams: Array<MediaStream>) {
            setRemoteAudioTrack(receiver.track())
            setRemoteVideoRenderer(receiver.track())
        }
    }

    /**
     * This method will be invoked internally from addIceCandidate method to add it into peer connection
     */
    private fun drainCandidates() {
        if (queuedRemoteCandidates != null) {
            Log.d(
                TAG,
                "Add " + queuedRemoteCandidates!!.size + " remote candidates"
            )
            for (candidate in queuedRemoteCandidates!!) {
                peerConnection?.addIceCandidate(candidate)
            }
            queuedRemoteCandidates = null
        }
    }

    /**
     * This method is called internally from close method to close peer connection
     */
    private fun closeInternal() {
        Log.v(TAG, LogMsg.msg_closing_peer)
        statsTimer.cancel()

        peerConnection?.let {
            it.dispose()
            peerConnection = null
        }

        audioSource?.let {
            it.dispose()
            audioSource = null
        }

        videoCapturer.let {
            try {
                it?.stopCapture()
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }
            it?.dispose()
            videoCapturerStopped = true
            videoCapturer = null
        }

        videoSource?.let {
            it.dispose()
            videoSource = null
        }

        surfaceTextureHelper?.let {
            it.dispose()
            surfaceTextureHelper = null
        }

        localRender = null
        remoteSinks = null

        factory?.let {
            it.dispose()
            factory = null
        }

        rootEglBase?.release()
        PeerConnectionFactory.stopInternalTracingCapture()
        PeerConnectionFactory.shutdownInternalTracer()
    }


}