/*
 * Created by ADT author on 9/14/20 11:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/14/20 10:51 PM
 */
package com.adt.vpm.activity

import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.Window
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.adt.vpm.R
import com.adt.vpm.fragment.CallFragment
import com.adt.vpm.fragment.CallFragment.OnCallEvents
import com.adt.vpm.model.Feed
import com.adt.vpm.webrtc.service.CallActivityListener
import com.adt.vpm.webrtc.service.Constants
import com.adt.vpm.webrtc.service.Constants.Companion.EXTRA_FEED_ROOM
import com.adt.vpm.webrtc.service.Constants.Companion.EXTRA_ROOMID
import com.adt.vpm.webrtc.service.Constants.Companion.EXTRA_ROOM_IS_FROM
import com.adt.vpm.webrtc.service.Constants.Companion.EXTRA_ROOM_LIVE
import com.adt.vpm.webrtc.service.WebRtcService
import com.adt.vpm.webrtc.service.WebRtcServiceListener
import com.adt.vpm.webrtc.util.SessionManager
import com.adt.vpm.webrtc.util.SurfaceViewRenderer
import com.adt.vpm.webrtc.util.UnhandledExceptionHandler
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_call.*
import org.webrtc.EglBase

/**
 * Activity for peer connection call setup, call waiting
 * and call view.
 */
class CallActivity : FragmentActivity(), OnCallEvents, WebRtcServiceListener {
    private var svPipRenderer: SurfaceViewRenderer? = null
    private var svFullScreenRenderer: SurfaceViewRenderer? = null
    private var logToast: Toast? = null
    private var roomId: String? = null
    private var isFrom: Int = Constants.FEED_JOIN
    private var feed: Feed? = null

    // True if local view is in the fullscreen renderer.
    private var isSwappedFeeds = false

    // Controls
    private var callFragment: CallFragment? = null
    private var mWebRtcService: WebRtcService? = null
    private var mCallActivityListener: CallActivityListener? = null
    private var mEglBase: EglBase? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Thread.setDefaultUncaughtExceptionHandler(UnhandledExceptionHandler(this))

        // Set window styles for fullscreen-window size. Needs to be done before adding content.
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_call)

        // Create UI controls.
        svPipRenderer = findViewById(R.id.svPipVideoView)
        svFullScreenRenderer = findViewById(R.id.svFullScreenVideoView)
        callFragment = CallFragment()

        // Swap feeds on pip view click.
        svPipRenderer?.setOnClickListener {
            val isTaped = SessionManager.instance?.isTaped() ?: false
            if (isTaped) return@setOnClickListener
            setSwappedFeeds(!isSwappedFeeds)
        }

        // Check for mandatory permissions.
        for (permission in MANDATORY_PERMISSIONS) {
            if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                logAndToast("Permission $permission is not granted")
                finish()
                return
            }
        }


        // Get Intent parameters.
        roomId = intent.getStringExtra(EXTRA_ROOMID)
        Log.d(TAG, "Room ID: $roomId")
        if (roomId == null || roomId?.length == 0) {
            logAndToast(getString(R.string.missing_url))
            Log.e(TAG, "Incorrect room ID in intent!")
            finish()
            return
        }

        // Feed item data which contains room data and details of audio and video stream controls
        val feedJson = intent.getStringExtra(EXTRA_FEED_ROOM)
        feed = Gson().fromJson(feedJson, Feed::class.java)

        // It's navigation from feed list, video icon, feed plus icon and rejoin from feed
        isFrom = intent.getIntExtra(EXTRA_ROOM_IS_FROM, Constants.FEED_JOIN)

        // True if it's came from feed list when it is in live state
        val isLive = intent.getBooleanExtra(EXTRA_ROOM_LIVE, false)

        if (!feed?.enableRemoteAudio!! && !feed?.enableRemoteVideo!!)
            ivVideoPlaceHolder?.setImageResource(R.drawable.ic_audio_video_disabled)
        else if (!feed?.enableRemoteAudio!! && feed?.enableRemoteVideo!!) {
            ivVideoPlaceHolder?.setImageResource(R.drawable.ic_audio_disabled)
        } else if (feed?.enableRemoteAudio!! && !feed?.enableRemoteVideo!!)
            ivVideoPlaceHolder?.setImageResource(R.drawable.ic_video_disabled)
        else
            ivVideoPlaceHolder?.setImageResource(R.drawable.ic_bg_video)

        // Get instance of WebRtc live session while rejoin again
        mWebRtcService = roomId?.let { SessionManager.instance?.getWebRtcService(it) }
        if (mWebRtcService != null) {
            mWebRtcService?.setWebRtcServiceListener(this)
            mWebRtcService?.setIsFrom(isFrom)
            if (isLive) {
                this.mEglBase = mWebRtcService?.getEglBase()
                ivVideoPlaceHolder?.visibility = GONE
                createVideoRenderer()
            }
        }

        // Send intent arguments to fragments.
        callFragment?.arguments = intent.extras

        // Activate call and HUD fragments and start the call.
        val aFragmentTrans = supportFragmentManager.beginTransaction()
        aFragmentTrans.add(R.id.callFragmentContainer, callFragment!!)
        aFragmentTrans.commit()

        // Start the video call if isLive is false
        if (!isLive) mWebRtcService?.startCall()

    }

    override fun onSetEglBase(mEglBase: EglBase) {
        this.mEglBase = mEglBase
        createVideoRenderer()
    }

    /**
     * This method is called to initialize SurfaceViewRenderer view
     * It's called from onCreate method
     */
    private fun createVideoRenderer() {
        // Create video renderers.
        mEglBase?.let { svPipRenderer?.init(it, SCALING_FIT, true) }
        mEglBase?.let { svFullScreenRenderer?.init(it, SCALING_FILL, false) }
        svPipRenderer?.setZOrderMediaOverlay(true)

        // Start with local feed in fullscreen and swap it to the pip when the call is connected.
        setSwappedFeeds(isSwappedFeeds)
    }

    /**
     * This method will be triggered when destroy the call page.
     */
    override fun onDestroy() {
        Thread.setDefaultUncaughtExceptionHandler(null)
        logToast?.cancel()
        super.onDestroy()
    }

    /**
     * This method will be triggered when user click on back press.
     */
    override fun onBackPressed() {
        val isJoiner =
            isFrom == Constants.FEED_JOIN || isFrom == Constants.FEED_REJOIN
        onBackOrClosed(isJoiner, false)
        super.onBackPressed()
    }

    /**
     * This method will be invoked when click on back arrow which is in top of the call page.
     * It is passing event to WebRtc service class by call event listener.
     *
     * @param isLive True if video call is in live and user is a joiner otherwise false
     */
    override fun onBackOrClosed(isLive: Boolean, callEndStatus: Boolean) {
        if (feed?.initializer!! && !callEndStatus) {
            AlertDialog.Builder(this)
                .setMessage(this.getText(R.string.exit_room_msg))
                .setCancelable(true)
                .setPositiveButton(
                    R.string.ok
                ) { dialog, _ -> callBackClick(isLive) }
                .setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.cancel()
                }
                .create()
                .show()
        } else {
            callBackClick(isLive)
        }
    }

    private fun callBackClick(isLive: Boolean) {
        mCallActivityListener?.onBackOrClosed(isLive)
        if (!isLive) {
            roomId?.let {
                SessionManager.instance?.updateCallSessionList(
                    it, isLive, null
                )
            }
            roomId?.let {
                SessionManager.instance?.updateRoomStatus(
                    this,
                    it, false
                )
            }
        }
    }

    /**
     * This method will be invoked when user click to switch front and back camera.
     * which is in call page at bottom right.
     * It is passing event to WebRtc service class by call event listener.
     */
    override fun onCameraSwitch() {
        mCallActivityListener?.onCameraSwitch()
    }

    /**
     * This method will be invoked when click on microphone icon which is in call page at bottom.
     * It is passing event to WebRtc service class by call event listener.
     *
     * @return True if local audio is enabled otherwise false
     */
    override fun onToggleMic(): Boolean {
        return mCallActivityListener?.onToggleMic() ?: false
    }

    /**
     * This method will be invoked when click on video icon which is in call page at bottom.
     * It is passing event to WebRtc service class by call event listener.
     *
     * @return True if local video is enabled otherwise false
     */
    override fun onToggleVideo(): Boolean {
        return mCallActivityListener?.onToggleVideo() ?: false
    }

    /**
     * This method will be invoked once call page views are initialized and ready to set state of video and audio stream controls.
     * It is passing event to WebRtc service class by call event listener
     */
    override fun setStreamControls() {
        mWebRtcService?.setStreamControls()
    }

    /**
     * This method will be invoked to show message as toast.
     *
     * @param msg description of message to show toast.
     */
    private fun logAndToast(msg: String?) {
        runOnUiThread {
            msg?.let { Log.d(TAG, it) }
            logToast?.cancel()
            logToast = Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG)
            logToast?.show()
        }

    }

    /**
     * This method is used to set video source for local and remote stream.
     *
     * @param isSwappedFeeds True if local view is in the fullscreen renderer.
     */
    private fun setSwappedFeeds(isSwappedFeeds: Boolean) {
        Log.d(TAG, "setSwappedFeeds: $isSwappedFeeds")
        this.isSwappedFeeds = isSwappedFeeds
        val mLocalVideoSink = mWebRtcService?.getLocalVideoSink()
        val mRemoteVideoSink = mWebRtcService?.getRemoteVideoSink()
        mLocalVideoSink?.setTarget(if (isSwappedFeeds) svFullScreenRenderer else svPipRenderer)
        mRemoteVideoSink?.setTarget(if (isSwappedFeeds) svPipRenderer else svFullScreenRenderer)
        svFullScreenRenderer?.setMirror(isSwappedFeeds)
        svPipRenderer?.setMirror(!isSwappedFeeds)
    }

    /**
     * This method will be triggered to enable or disable local video stream
     * which is calling from WebRtc service class by WebRtcServiceListener
     *
     * @param enable True if enable local video stream otherwise false
     */
    override fun onSetLocalVideoButton(enable: Boolean?) {
        if (callFragment != null) callFragment?.setLocalVideoButton(enable)
    }

    /**
     * This method will be triggered to enable or disable local audio stream
     * which is calling from WebRtc service class by WebRtcServiceListener
     *
     * @param enable True if enable local audio stream otherwise false
     */
    override fun onSetLocalAudioButton(enable: Boolean?) {
        if (callFragment != null) callFragment?.setLocalAudioButton(enable)
    }

    /**
     * This method is used to get instance of CallActivityListener which is triggered from WebRtc service class.
     *
     * @param mCallActivityListener which is instance of CallActivityListener
     */
    override fun setCallEventListener(mCallActivityListener: CallActivityListener) {
        this.mCallActivityListener = mCallActivityListener
    }

    /**
     * This method will be invoked when disconnect from remote resources, dispose of local resources, and exit.
     * Which is calling from WebRtc service class by WebRtcServiceListener
     */
    override fun disConnectedFromPeer() {

        svPipRenderer?.release()
        svPipRenderer = null

        svFullScreenRenderer?.release()
        svFullScreenRenderer = null

        this.mWebRtcService = null
        this.mCallActivityListener = null
        this.mEglBase = null

        finish()
    }

    /**
     * This method will be invoked when remote stream get connected.
     * Which is calling from WebRtc service class by WebRtcServiceListener
     *
     * @param isConnected True if remote stream get connected
     */
    override fun onStreamConnected(isConnected: Boolean) {
        val hidePlaceHolder =
            feed?.enableRemoteAudio!! && feed?.enableRemoteVideo!! && isConnected
        ivVideoPlaceHolder?.visibility = if (hidePlaceHolder) GONE else View.VISIBLE

        if (isConnected) {
            feed?.isLive = true
            SessionManager.instance?.addRoomToList(feed!!)
            roomId?.let {
                SessionManager.instance?.updateCallSessionList(
                    it, true, mWebRtcService
                )
            }
            roomId?.let {
                SessionManager.instance?.updateRoomStatus(
                    this,
                    it, true
                )
            }
        }
    }

    /**
     * This method will be invoked whenever get error from socket connection or peer connection.
     * Which is calling from WebRtc service class by WebRtcServiceListener
     *
     * @param errorMessage description of error message to show.
     */
    override fun disconnectWithErrorMessage(errorMessage: String) {
        AlertDialog.Builder(this)
            .setTitle(this.getText(R.string.channel_error_title))
            .setMessage(errorMessage)
            .setCancelable(false)
            .setNeutralButton(
                R.string.ok
            ) { dialog, _ ->
                dialog.cancel()
                mCallActivityListener?.onBackOrClosed(false)
            }
            .create()
            .show()

        roomId?.let {
            SessionManager.instance?.updateCallSessionList(
                it, false, null
            )
        }
        roomId?.let {
            SessionManager.instance?.updateRoomStatus(
                this,
                it, false
            )
        }
    }

    /**
     * This method will be invoked to show message as toast.
     * Which is calling from WebRtc service class by WebRtcServiceListener
     *
     * @param aMsg description of message to show toast
     */
    override fun showToast(aMsg: String?) {
        logAndToast(aMsg)
    }

    companion object {
        private const val TAG = "CallActivity"

        private const val SCALING_FIT: Int = 1
        private const val SCALING_FILL: Int = 0

        // List of mandatory application permissions.
        private val MANDATORY_PERMISSIONS = arrayOf(
            "android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.RECORD_AUDIO", "android.permission.INTERNET"
        )
    }
}