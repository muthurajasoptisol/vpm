/*
 * Created by ADT author on 9/14/20 11:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/14/20 10:51 PM
 */

package com.adt.vpm.webrtc.service

import android.app.Activity
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.adt.vpm.R
import com.adt.vpm.VPMFactory
import com.adt.vpm.model.Feed
import com.adt.vpm.util.Log
import com.adt.vpm.util.LogMsg
import com.adt.vpm.webrtc.*
import com.adt.vpm.webrtc.data.IceCandidate
import com.adt.vpm.webrtc.data.SessionDescription
import com.adt.vpm.webrtc.manager.AppRTCAudioManager
import com.adt.vpm.webrtc.signal.SignalingClient
import com.adt.vpm.webrtc.signal.SocketIOClient
import com.adt.vpm.webrtc.signal.WebSocketRTCClient
import com.adt.vpm.webrtc.util.SessionManager
import com.adt.vpm.webrtc.views.VideoSink
import com.google.gson.Gson
import org.webrtc.EglBase

/**
 * This class is used to establish video call by communicate between CallActivity and WebRtcSession class
 * IWebRTCSession interface used to implement methods with WebRtcSession class to access with peer connection
 * IWebRTCSessionCallback interface used to get callback events from WebRtcSession
 * AppRTCClient interface used to implement methods with Socket IO and WebSocket client to access with socket message transfer with peer
 * SignalingEvents interface used to get callback events from Socket IO and WebSocket client
 * WebRtcServiceListener interface used to implement methods with CallActivity to control video stream controls
 * CallActivityListener interface used to get callback method events from CallActivity
 */
class WebRtcService internal constructor(
    private var mContext: Activity,
    private var isFrom: Int,
    private var feed: Feed?,
    private val mSignalType: String,
    private val webRTCParams: WebRTCParams?,
    private val roomConnectionParameters: SignalingClient.RoomConnectionParameters
) : SignalingClient.SignalingEvents, CallActivityListener, IWebRTCSessionCallback {

    private val TAG = "WebRtcService"
    private var mSignalingClient: SignalingClient? = null
    private var callStartedTimeMs: Long = 0
    private var mIWebRTCSession: IWebRTCSession? = null
    private var signalingParameters: SignalingParameters? = null
    private var isError = false
    private var micEnabled = true
    private var videoEnable = true

    private var audioManager: AppRTCAudioManager? = null
    private val mRemoteVideoSink = VideoSink()
    private val mLocalVideoSink = VideoSink()
    private val mRemoteSinkList: MutableList<VideoSink> = ArrayList()
    private var mWebRtcServiceListener: WebRtcServiceListener? = null
    private var mEglBase: EglBase? = null
    private var sharedPref: SharedPreferences? = null

    init {
        initRoomConnection()
    }

    /**
     * This method is used to initiate socket client based on signaling type
     */
    private fun initRoomConnection() {
        //set Signaling by type
        mSignalingClient = when (mSignalType) {
            Constants.SIGNAL_SOCKET_IO -> {
                SocketIOClient(this)
            }
            else -> {
                WebSocketRTCClient(this)
            }
        }
        mSignalingClient?.useSLNATServer(getNATServerPreference())
        mRemoteSinkList.add(mRemoteVideoSink)
    }

    /**
     * This method will be invoked from CallActivity to start the call establishment if room doesn't have live session
     * This method used to initialize WebRtcSession and AudioManager
     * Socket room connection method is calling from this method to establish socket signaling client
     */
    fun startCall() {

        initIWebRtcSession()

        if (mSignalingClient == null) {
            Log.e(TAG, "AppRTC client is not allocated for a call.")
            return
        }
        callStartedTimeMs = System.currentTimeMillis()

        val signalType =
            "Signaling type : $mSignalType Server Url : ${roomConnectionParameters.roomUrl}"
        Log.i(TAG, signalType)

        // Start room connection.
        mWebRtcServiceListener?.showToast(
            mContext.getString(
                R.string.connecting_to,
                roomConnectionParameters.roomUrl
            )
        )

        mSignalingClient?.connectToRoom(roomConnectionParameters)

        initAudioManager()
    }

    /**
     * This method is used to initialize WebRtcSession to access peer connection
     */
    private fun initIWebRtcSession() {
        // Create peer connection client.
        mIWebRTCSession =
            webRTCParams?.let {
                VPMFactory.createWebRTCSession(
                    mContext,
                    it, this
                )
            }
        PreferenceManager.setDefaultValues(mContext, R.xml.preferences, false)
        sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext)
    }

    private fun getNATServerPreference() : Boolean {
        sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext)
        return sharedPref?.getBoolean(mContext.getString(R.string.pref_enable_sl_turn_server_key), true)!!
    }
    /**
     * This method is used to initialize AudioManager to access audio controls
     */
    private fun initAudioManager() {
        // Create and audio manager that will take care of audio routing,
        // audio modes, audio device enumeration etc.
        audioManager = AppRTCAudioManager.create(mContext)
        // Store existing audio settings and change audio mode to
        // MODE_IN_COMMUNICATION for best possible VoIP performance.
        audioManager?.start { audioDevice, availableAudioDevices ->
            // This method will be called each time the number of available audio
            // devices has changed.
            onAudioManagerDevicesChanged(audioDevice, availableAudioDevices)
        }
    }

    // This method is called when the audio manager reports audio device change,
    // e.g. from wired headset to speakerphone.
    private fun onAudioManagerDevicesChanged(
        device: AppRTCAudioManager.AudioDevice,
        availableDevices: Set<AppRTCAudioManager.AudioDevice>
    ) {
        Log.d(
            TAG, "onAudioManagerDevicesChanged: " + availableDevices + ", "
                    + "selected: " + device
        )
    }

    /**
     * This callback method fired from WebRtcSession once local SDP is created and send it to remote peer.
     *
     * @param sdp session description of local peer
     */
    override fun onLocalDescription(sdp: SessionDescription?) {
        val delta = System.currentTimeMillis() - callStartedTimeMs
        mContext.runOnUiThread {
            if (mSignalingClient != null) {
                mWebRtcServiceListener?.showToast("Sending " + sdp?.type + ", delay=" + delta + "ms")
                Log.d(TAG, "onLocalDescription sdp: " + sdp?.description)
                if (signalingParameters!!.initiator) {
                    mSignalingClient?.sendOfferSdp(sdp)
                } else {
                    mSignalingClient?.sendAnswerSdp(sdp)
                }
            }
            if (webRTCParams?.videoMaxBitrate!! > 0) {
                Log.d(
                    TAG,
                    "Set video maximum bitrate: " + webRTCParams.videoMaxBitrate
                )
                mIWebRTCSession?.setVideoMaxBitrate(webRTCParams.videoMaxBitrate)
            }
        }
    }

    /**
     * This callback method fired from WebRtcSession once local Ice candidate is generated.
     *
     * @param candidate candidate generated by local peer.
     */
    override fun onIceCandidate(candidate: IceCandidate?) {
        mContext.runOnUiThread {
            Log.d(TAG, "onIceCandidate: $candidate")
            mSignalingClient?.sendLocalIceCandidate(candidate)
        }
    }

    /**
     * This callback method fired from WebRtcSession once local ICE candidates are removed.
     *
     * @param candidates candidate list removed from peer.
     */
    override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) {
        mContext.runOnUiThread {
            mSignalingClient?.sendLocalIceCandidateRemovals(candidates)
        }
    }

    /**
     * This callback method is triggered once connection State changed as connected or disconnected.
     *
     * @param webRTCSessionState
     */
    override fun onConnectionStateChanged(webRTCSessionState: WebRTCSession.WebRTCSessionState) {
        mContext.runOnUiThread {
            if (webRTCSessionState == WebRTCSession.WebRTCSessionState.CONNECTED) {
                val delta = System.currentTimeMillis() - callStartedTimeMs
                mWebRtcServiceListener?.showToast("ICE connected, delay=" + delta + "ms")
                callConnected()
            } else {
                mWebRtcServiceListener?.showToast("ICE disconnected")
                if (mIWebRTCSession != null) disConnect()

                roomConnectionParameters.roomId?.let {
                    SessionManager.instance?.updateRoomStatus(
                        mContext,
                        it, false
                    )
                }
                roomConnectionParameters.roomId?.let {
                    SessionManager.instance?.updateCallSessionList(
                        it, false, null
                    )
                }
            }
        }
    }

    /**
     * This callback method fired from WebRtcSession once peer connection error happened.
     *
     * @param description error message
     */
    override fun onPeerConnectionError(description: String?) {

    }

    /**
     * This method triggered internally from onConnected method once peer connection connected
     * Should be called from UI thread
     * Stats events enabled here
     */
    private fun callConnected() {
        val delta = System.currentTimeMillis() - callStartedTimeMs
        Log.i(TAG, "Call connected: delay=" + delta + "ms")
        Log.i(TAG, LogMsg.msg_stream_connected)
        mWebRtcServiceListener?.onStreamConnected(true)

        if (mIWebRTCSession == null || isError) {
            Log.w(TAG, "Call is connected in closed or error state")
            mWebRtcServiceListener?.onStreamConnected(false)
            return
        }
    }

    /**
     * This callback method fired from Socket signaling client once remote SDP is received.
     *
     * @param sdp session description of remote peer
     */
    override fun onRemoteDescription(sdp: SessionDescription) {
        val delta = System.currentTimeMillis() - callStartedTimeMs
        mContext.runOnUiThread(Runnable {
            if (mIWebRTCSession == null) {
                Log.e(
                    TAG,
                    "Received remote SDP for non-initilized peer connection."
                )
                return@Runnable
            }
            mWebRtcServiceListener?.showToast("Received remote " + sdp.type + ", delay=" + delta + "ms")
            Log.d(TAG, "onRemoteDescription sdp: " + sdp.description)
            mIWebRTCSession?.setRemoteDescription(sdp)
            if (!signalingParameters!!.initiator) {
                mWebRtcServiceListener?.showToast("Creating ANSWER...")
                // Create answer. Answer SDP will be sent to offering client in
                // PeerConnectionEvents.onLocalDescription event.
                mIWebRTCSession?.createAnswer()
            }
        })
    }

    /**
     * This callback method fired from Socket signaling client once remote Ice candidate is received.
     *
     * @param candidate candidate received from remote
     */
    override fun onRemoteIceCandidate(candidate: IceCandidate) {
        mContext.runOnUiThread(Runnable {
            Log.d(TAG, "onRemoteIceCandidate: $candidate")
            if (mIWebRTCSession == null) {
                Log.e(
                    TAG,
                    "Received ICE candidate for a non-initialized peer connection."
                )
                return@Runnable
            }
            mIWebRTCSession?.addRemoteIceCandidate(candidate)
        })
    }

    /**
     * This callback method fired from Socket signaling client once remote Ice candidate removals are received.
     *
     * @param candidates list of candidates to be removed
     */
    override fun onRemoteIceCandidatesRemoved(candidates: Array<IceCandidate>) {
        mContext.runOnUiThread(Runnable {
            Log.d(TAG, "onRemoteIceCandidatesRemoved: $candidates")
            if (mIWebRTCSession == null) {
                Log.e(
                    TAG,
                    "Received ICE candidate removals for a non-initialized peer connection."
                )
                return@Runnable
            }
            mIWebRTCSession?.removeRemoteIceCandidates(candidates)
        })
    }

    /**
     * This callback method fired from Socket signaling client once the room's signaling parameters are extracted.
     * Peer connection creation, offer creation, answer creation, add IceCandidate happening within this method
     *
     * @param params signaling parameters which contains data about signaling
     * @param roomId id of the room
     */
    override fun onConnectedToRoom(params: SignalingParameters?, roomId: String) {
        val delta = System.currentTimeMillis() - callStartedTimeMs
        signalingParameters = params

        val roomConnected = String.format(LogMsg.msg_room_connected, roomId)
        val signalParam = String.format(LogMsg.msg_signaling_param, Gson().toJson(params))
        Log.i(TAG, roomConnected)
        Log.d(TAG, signalParam)

        mWebRtcServiceListener?.showToast("Creating peer connection, delay=" + delta + "ms")

        var localVideoSink: org.webrtc.VideoSink? = null
        var remoteVideoSink: org.webrtc.VideoSink? = null

        if (feed!!.enableLocalVideo) {
            localVideoSink = mLocalVideoSink
        }
        if (feed!!.enableRemoteVideo) {
            remoteVideoSink = mRemoteVideoSink
        }
        mIWebRTCSession?.createPeerConnection(
            localVideoSink,
            remoteVideoSink,
            signalingParameters
        )

        if (signalingParameters!!.initiator) {
            mWebRtcServiceListener?.showToast("Creating OFFER...")
            // Create offer. Offer SDP will be sent to answering client in
            // PeerConnectionEvents.onLocalDescription event.
            mIWebRTCSession?.createOffer()
        } else {
            if (params?.offerSdp != null) {
                mIWebRTCSession?.setRemoteDescription(params.offerSdp!!)
                mWebRtcServiceListener?.showToast("Creating ANSWER...")
                // Create answer. Answer SDP will be sent to offering client in
                // PeerConnectionEvents.onLocalDescription event.
                mIWebRTCSession?.createAnswer()
            }
            if (params?.iceCandidates != null) {
                // Add remote ICE candidates from room.
                for (iceCandidate in params.iceCandidates!!) {
                    mIWebRTCSession?.addRemoteIceCandidate(iceCandidate)
                }
            }
        }
    }


    /**
     * This callback method fired from Socket signaling client once channel is closed.
     */
    override fun onChannelClose() {
        mContext.runOnUiThread {
            Log.e(TAG, LogMsg.msg_disconnect_remote_end)
            mWebRtcServiceListener?.showToast("Remote end hung up; dropping PeerConnection")
            if (mIWebRTCSession != null) disConnect()

            roomConnectionParameters.roomId?.let {
                SessionManager.instance?.updateRoomStatus(
                    mContext,
                    it, false
                )
            }
            roomConnectionParameters.roomId?.let {
                SessionManager.instance?.updateCallSessionList(
                    it, false, null
                )
            }
        }
    }

    /**
     * This callback method fired once disconnected from local and remote as well
     */
    private fun disConnect() {
        mWebRtcServiceListener?.disConnectedFromPeer()
        mSignalingClient?.disconnectFromRoom()
        mSignalingClient = null

        mIWebRTCSession?.close()
        mIWebRTCSession = null

        audioManager?.stop()
        audioManager = null

        mEglBase = null

        mWebRtcServiceListener = null

        mRemoteVideoSink.setTarget(null)
        mLocalVideoSink.setTarget(null)
    }

    /**
     * This method invoked from CallActivity when click on back arrow which is in top of the call page
     *
     * @param isLive True if video call is in live and user is a joiner otherwise false
     */
    override fun onBackOrClosed(isLive: Boolean) {
        if (isLive) {
            mWebRtcServiceListener?.disConnectedFromPeer()
            mRemoteVideoSink.setTarget(null)
            mLocalVideoSink.setTarget(null)
            if (isFrom == Constants.FEED_JOIN || isFrom == Constants.FEED_REJOIN) {
                micEnabled = false
                videoEnable = false
            }
            mIWebRTCSession?.muteLocalVideo(videoEnable)
            mIWebRTCSession?.muteLocalAudio(micEnabled)
            mIWebRTCSession?.muteRemoteAudio(false)
        } else {
            disConnect()
        }
    }

    /**
     * This callback method fired once channel error happened.
     *
     * @param description error message
     */
    override fun onChannelError(description: String) {
        Log.e(TAG, description)
        reportError(description)
    }

    /**
     * This method will be invoked internally to show error messages related to connection
     *
     * @param description error message
     */
    private fun reportError(description: String) {
        mContext.runOnUiThread {
            if (!isError) {
                isError = true
                mWebRtcServiceListener?.disconnectWithErrorMessage(description)
            }
        }
    }

    /**
     * This method will be invoked from CallActivity when user click to switch front and back camera.
     * which is in call page at bottom right.
     * It is passing event to WebRtcSession class by IWebRtcSession.
     */
    override fun onCameraSwitch() {
        mIWebRTCSession?.switchCamera()
    }

    /**
     * This method will be invoked from CallActivity when click on microphone icon which is in call page at bottom.
     * which is in call page at bottom right.
     * It is passing event to WebRtcSession class by IWebRtcSession.
     *
     * @return True if local audio is enabled otherwise false
     */
    override fun onToggleMic(): Boolean {
        if (mIWebRTCSession != null) {
            micEnabled = !micEnabled
            mIWebRTCSession?.muteLocalAudio(micEnabled)
        }
        return micEnabled
    }

    /**
     * This method will be invoked from CallActivity when click on video icon which is in call page at bottom
     * It is passing event to WebRtcSession class by IWebRtcSession.
     *
     * @return True if local video is enabled otherwise false
     */
    override fun onToggleVideo(): Boolean {
        if (mIWebRTCSession != null) {
            videoEnable = !videoEnable
            mIWebRTCSession?.muteLocalVideo(videoEnable)
        }
        return videoEnable
    }

    /**
     * This method will be invoked from CallActivity to get local video sink instance
     *
     * @return mLocalVideoSink
     */
    fun getLocalVideoSink(): VideoSink {
        return mLocalVideoSink
    }

    /**
     * This method will be invoked from CallActivity to get remote video sink instance
     *
     * @return mRemoteVideoSink
     */
    fun getRemoteVideoSink(): VideoSink {
        return mRemoteVideoSink
    }

    /**
     * This method will be invoked from CallActivity to set WebRtcServiceListener and CallActivityListener
     */
    fun setWebRtcServiceListener(mWebRtcServiceListener: WebRtcServiceListener) {
        this.mWebRtcServiceListener = mWebRtcServiceListener
        mWebRtcServiceListener.setCallEventListener(this)
    }

    /**
     * This method will be invoked from CallActivity to get EglBase instance
     *
     * @return mEglBase
     */
    fun getEglBase(): EglBase? {
        return mEglBase
    }

    /**
     * This callback method fired from WebRtcSession once that class initiated.
     *
     * @param mEglBase EglBase
     */
    override fun onSetEglBase(mEglBase: EglBase) {
        this.mEglBase = mEglBase
        mWebRtcServiceListener?.onSetEglBase(mEglBase)
    }

    fun setIsFrom(isFrom: Int) {
        this.isFrom = isFrom
    }

    /**
     * This method will be invoked from CallActivity to set Stream controls
     */
    fun setStreamControls() {
        if (feed == null || mIWebRTCSession == null) return
        when (isFrom) {
            Constants.CAMERA_CREATE,
            Constants.FEED_JOIN -> {
                micEnabled = feed!!.enableLocalAudio
                videoEnable = feed!!.enableLocalVideo
                //   mIWebRTCSession?.muteRemoteAudio(true)
            }
            Constants.FEED_REJOIN -> {
                micEnabled = feed!!.enableLocalAudio
                videoEnable = feed!!.enableLocalVideo
                mIWebRTCSession?.muteRemoteAudio(true)
            }

        }

        mWebRtcServiceListener?.onSetLocalVideoButton(videoEnable)
        mWebRtcServiceListener?.onSetLocalAudioButton(micEnabled)
        mIWebRTCSession?.muteLocalVideo(videoEnable)
        mIWebRTCSession?.muteLocalAudio(micEnabled)

    }
}