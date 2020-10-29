/*
 * Created by ADT author on 9/17/20 1:56 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/17/20 1:56 PM
 */

package com.adt.vpm.videoplayer

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.pm.ActivityInfo
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Color
import android.graphics.PorterDuff
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.adt.vpm.R
import com.adt.vpm.VPMFactory
import com.adt.vpm.adpater.VideoInfoAdapter
import com.adt.vpm.model.UriDeserializer
import com.adt.vpm.model.VideoFile
import com.adt.vpm.util.Log
import com.adt.vpm.util.LogMsg
import com.adt.vpm.util.Utils
import com.adt.vpm.videoplayer.VideoPlayer.StreamType
import com.adt.vpm.webrtc.util.SessionManager
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.GsonBuilder
import kotlinx.android.synthetic.main.activity_videoplayer.*
import kotlinx.android.synthetic.main.player_control_view.*

class VideoPlayerActivity : AppCompatActivity(), IPlayerCallback, View.OnClickListener,
    SeekBar.OnSeekBarChangeListener, OrientationManager.OrientationListener {

    private var infoList: MutableMap<String, String> = mutableMapOf()
    private var dialog: BottomSheetDialog? = null
    private var mIVideoPlayer: IVideoPlayer? = null
    private var uri: Uri? = null
    private var playWhenReady: Boolean = true
    private var isVideoOnTexture = true
    private var videoItem: VideoFile? = null
    private var playbackState: PlaybackState = PlaybackState.STATE_IDLE
    private var errorState: PlayerError = PlayerError.TYPE_NONE
    private var orientationManager: OrientationManager? = null
    private var isPortraitVideo: Boolean = true
    private var alertDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_videoplayer)

        setVisibilityListener()
        initClickListener()

        getIntentExtras()

    }

    /**
     * This method is used to get intent extras values which is passed from previous activity
     */
    private fun getIntentExtras() {
        val gson = GsonBuilder()
            .registerTypeAdapter(Uri::class.java, UriDeserializer())
            .create()
        videoItem = gson.fromJson(intent.getStringExtra("content"), VideoFile::class.java)
        uri = videoItem?.fileUri

        tvVideoFileName?.text = videoItem?.filename

        infoList[LogMsg.data_file_name] = videoItem?.filename ?: ""
        infoList[LogMsg.data_location] = uri.toString()

        Log.d(TAG, String.format(LogMsg.msg_playback_type, videoItem?.fileType))
        Log.d(TAG, String.format(LogMsg.msg_playback_url, uri.toString()))

        if (videoItem?.fileType != StreamType.LOCAL && !hasNetwork()) {
            showAlert(LogMsg.msg_internet_failure_title, LogMsg.msg_internet_failure_msg)
        } else {
            mIVideoPlayer = uri?.let { VPMFactory.createPlayer(this, it, this) }
        }

        if (mIVideoPlayer != null && mIVideoPlayer?.isCastable()!!) {
            CastButtonFactory.setUpMediaRouteButton(this, mbCast)
            mbCast?.visibility = View.VISIBLE
        } else {
            mbCast?.visibility = View.GONE
        }
    }

    /**
     * This method will be invoked from onCreate method to set click listener for views.
     */
    private fun initClickListener() {
        ivPlay?.setOnClickListener(this)
        ivBackward?.setOnClickListener(this)
        ivForward?.setOnClickListener(this)
        ivInfo?.setOnClickListener(this)
        ivPause?.setOnClickListener(this)
        ivVideoBackBtn?.setOnClickListener(this)
        ivFullScreen?.setOnClickListener(this)
        ivMinimize?.setOnClickListener(this)
        switchVideo?.setOnClickListener(this)
        mbCast?.setOnClickListener(this)
    }

    /**
     * This method is used to initialize the player
     */
    private fun initializePlayer() {
        playerView?.requestFocus()
        mIVideoPlayer?.setVideoView(playerView)
        mIVideoPlayer?.open(playWhenReady)

        playerTextureView?.visibility = View.GONE
        playerSurfaceView?.visibility = View.GONE

        ivPause?.visibility = if (playWhenReady) View.VISIBLE else View.GONE
        ivPlay?.visibility = if (playWhenReady) View.GONE else View.VISIBLE

        ivPause?.isEnabled = false
        ivPause?.alpha = 0.6f

        setSeekBarProgressUI()
        updateProgressBar()
    }

    /**
     * This method is used to check network connection state
     */
    private fun hasNetwork(): Boolean {
        return Utils.haveNetworkConnection(this)
    }

    private fun isAlertDialogShowing(): Boolean {
        return alertDialog != null && alertDialog!!.isShowing
    }

    /**
     * This method is used to customize SeekBar UI
     */
    @Suppress("DEPRECATION")
    private fun setSeekBarProgressUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            seekBarPlayer.progressDrawable.colorFilter = BlendModeColorFilter(
                Color.WHITE,
                BlendMode.SRC_IN
            )
            seekBarPlayer.thumb.colorFilter = BlendModeColorFilter(
                Color.RED,
                BlendMode.SRC_IN
            )
        } else {
            seekBarPlayer.progressDrawable.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
            seekBarPlayer.thumb.setColorFilter(
                Color.RED, PorterDuff.Mode.SRC_IN
            )
        }
        seekBarPlayer.progress = 0
        seekBarPlayer.max = 100
        seekBarPlayer.setOnSeekBarChangeListener(this)

    }

    val mHandler = Handler(Looper.getMainLooper())
    private val durationRunnable = object : Runnable {
        @SuppressLint("SetTextI18n")
        override fun run() {
            val totalDuration = mIVideoPlayer?.getTotalDuration() ?: 0
            var currentDuration = mIVideoPlayer?.getCurrentPosition() ?: 0

            Log.v(TAG, "AAA Current Duration : $currentDuration, TotalDuration : $totalDuration")

            currentDuration =
                if (mIVideoPlayer!!.isSeekable() && currentDuration > totalDuration) 0 else currentDuration

            if (mIVideoPlayer != null && playerView != null) {
                tvCurrentDuration?.text = milliSecondsToTimer(currentDuration)
                seekBarPlayer?.progress = getProgressPercentage(currentDuration, totalDuration)

                if (mIVideoPlayer!!.isSeekable() && totalDuration >= 0) {
                    tvTotalDuration.visibility = View.VISIBLE
                    tvTotalDuration?.text =
                        String.format(LogMsg.txt_duration, milliSecondsToTimer(totalDuration))
                    enableSeek(true)
                } else {
                    tvTotalDuration.visibility = View.GONE
                    enableSeek(false)
                }
                if (errorState != PlayerError.TYPE_NONE && !hasNetwork()) {
                    showAlert(LogMsg.msg_internet_failure_title, LogMsg.msg_internet_failure_msg)
                }
            }
            mHandler.postDelayed(this, 50)
        }
    }

    /**
     * This method is used to enable or disable based on get total duration
     */
    private fun enableSeek(isEnable: Boolean) {
        ivForward?.alpha = if (isEnable) 1.0f else 0.6f
        ivBackward?.alpha = if (isEnable) 1.0f else 0.6f
        ivForward?.isEnabled = isEnable
        ivBackward?.isEnabled = isEnable
        seekBarPlayer?.isEnabled = isEnable
    }

    private fun setPlayWhenReady(playWhenReady: Boolean) {
        this.playWhenReady = playWhenReady
        ivPause?.visibility = if (playWhenReady) View.VISIBLE else View.GONE
        ivPlay?.visibility = if (playWhenReady) View.GONE else View.VISIBLE
        ivPause?.alpha = 1.0F
        ivPlay?.alpha = 1.0F
        ivPlay?.isEnabled = true
        ivPause?.isEnabled = true
    }

    /**
     * This method is used to generate timer text from milliSeconds value
     */
    private fun milliSecondsToTimer(milliseconds: Long): String? {
        // Convert total duration into time
        val hours = (milliseconds / (1000 * 60 * 60)).toInt()
        val minutes = (milliseconds % (1000 * 60 * 60)).toInt() / (1000 * 60)
        val seconds = (milliseconds % (1000 * 60 * 60) % (1000 * 60) / 1000).toInt()
        // Add hours if there
        val finalTimerString = if (hours > 0) "$hours:" else ""
        // Prepending 0 to seconds if it is one digit
        val secondsString = if (seconds < 10) "0$seconds" else "" + seconds

        return "$finalTimerString$minutes:$secondsString"
    }

    /**
     * This method is used to update the progress of seekbar while playing video
     */
    private fun updateProgressBar() {
        mHandler.postDelayed(durationRunnable, 100)
    }

    /**
     * This method is used to get progress percentage based on current duration and total duration
     */
    private fun getProgressPercentage(currentDuration: Long, totalDuration: Long): Int {
        return ((currentDuration / 1000).toDouble() / (totalDuration / 1000) * 100).toInt()
    }

    /**
     * This method is used to get current duration in milliseconds
     */
    private fun progressToTimer(progress: Int, duration: Int): Int {
        return (progress.toDouble() / 100 * (duration / 1000)).toInt() * 1000
    }

    override fun onStopTrackingTouch(p0: SeekBar?) {
        val totalDuration: Int = mIVideoPlayer?.getTotalDuration()!!.toInt()
        val currentPosition: Int = progressToTimer(seekBarPlayer.progress, totalDuration)
        mIVideoPlayer?.seekToPosition(currentPosition.toLong())
        updateProgressBar()
    }

    /**
     * This method will be triggered when player playback state changed
     *
     * @param playbackState Playback State
     */
    override fun onPlaybackStateChanged(playbackState: PlaybackState) {
        when (playbackState) {
            PlaybackState.STATE_ENDED -> {
                setPlayWhenReady(false)
                mIVideoPlayer?.seekToPosition(0)
                mIVideoPlayer?.pause()
                showSystemUI()
                this.playbackState = playbackState
            }
            PlaybackState.STATE_IS_PLAYING -> {
                setPlayWhenReady(true)
                this.playbackState = PlaybackState.STATE_PLAYING
            }
            PlaybackState.STATE_PAUSED -> {
                setPlayWhenReady(false)
                this.playbackState = PlaybackState.STATE_PAUSED
            }
            PlaybackState.STATE_READY -> {
                setPlayWhenReady(false)
                this.playbackState = PlaybackState.STATE_READY
            }
            else -> {
                this.playWhenReady = false
                this.playbackState = playbackState
            }
        }
        tvPlaybackStatus.text = this.playbackState.name
    }

    /**
     * This method will be triggered when error will occur in player session
     *
     * @param error PlayerError
     * @param message Description of error
     */
    override fun onPlayerError(error: PlayerError, title: String, message: String) {
        Log.e(TAG, "onPlayerError type $error Message $message")
        errorState = error
        when (error) {
            PlayerError.TYPE_UNEXPECTED,
            PlayerError.TYPE_SOURCE -> {
                if (!hasNetwork()) {
                    showAlert(LogMsg.msg_internet_failure_title, LogMsg.msg_internet_failure_msg)
                } else {
                    showAlert(title, message)
                }
            }
            else -> {
                showAlert(title, message)
            }
        }
    }

    /**
     * This method will be triggered when video size changed
     *
     * @param width Width of the video
     * @param height Height of the video
     * @param pixelWidthHeightRatio Ratio
     */
    override fun onVideoSizeChanged(width: Int, height: Int, pixelWidthHeightRatio: Float) {
        isPortraitVideo = height > width
        Log.d(TAG, String.format(LogMsg.msg_video_mode, isPortraitVideo.toString()))
    }

    /**
     * Called when the visibility changes.
     *
     * @param visibility The new visibility. Either VISIBLE or GONE.
     */
    override fun onControlsVisibilityChange(visibility: Int) {
        if (visibility == View.GONE) hideSystemUI() else showSystemUI()
    }

    /**
     * This method will be invoked when click on feed fab plus icon.
     * It is passing event to container activity to show dialog to join with room
     *
     * @param view which represents view of the clicked item
     */
    override fun onClick(view: View?) {
        val isTaped = SessionManager.instance?.isTaped() ?: false
        if (isTaped) return

        when (view?.id) {
            R.id.ivPlay -> {
                if (mIVideoPlayer != null && playerView != null) {
                    if (playWhenReady) mIVideoPlayer?.resume() else mIVideoPlayer?.play()
                    ivPause?.visibility = View.VISIBLE
                    ivPlay?.visibility = View.GONE
                }
            }

            R.id.ivPause -> {
                if (mIVideoPlayer != null && playerView != null) {
                    mIVideoPlayer?.pause()
                    ivPause?.visibility = View.GONE
                    ivPlay?.visibility = View.VISIBLE
                }
            }

            R.id.ivFullScreen -> {
                this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                ivFullScreen.visibility = View.GONE
                ivMinimize.visibility = View.VISIBLE
                setResizeMode(true)
                hideSystemUI()
            }

            R.id.ivMinimize -> {
                this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                ivFullScreen.visibility = View.VISIBLE
                ivMinimize.visibility = View.GONE
                setResizeMode(false)
                showSystemUI()
            }

            R.id.ivVideoBackBtn -> {
                onBackPressed()
            }

            R.id.ivForward -> {
                if (mIVideoPlayer != null && playerView != null) {
                    val pos = mIVideoPlayer?.getCurrentPosition()?.plus(10000) ?: 0
                    if (pos <= mIVideoPlayer?.getTotalDuration()!!) {
                        mIVideoPlayer?.seekToPosition(pos)
                    }
                }
            }

            R.id.ivBackward -> {
                if (mIVideoPlayer != null && playerView != null) {
                    val pos = mIVideoPlayer?.getCurrentPosition()?.minus(10000) ?: 0
                    if (mIVideoPlayer?.getCurrentPosition()!! > 10000) {
                        mIVideoPlayer?.seekToPosition(pos)
                    } else {
                        mIVideoPlayer?.seekToPosition(0)
                    }
                }
            }

            R.id.ivInfo -> {
                mIVideoPlayer?.getMetaData()?.let { infoList.putAll(it) }
                showInfoDialog()
            }

            R.id.switchVideo -> {
                playerView?.visibility = View.GONE
                rtlPlayerController?.visibility = View.VISIBLE
                mIVideoPlayer?.setVideoView(null)
                isVideoOnTexture = !isVideoOnTexture

                playerTextureView?.visibility =
                    if (isVideoOnTexture) View.GONE else View.VISIBLE

                playerSurfaceView?.visibility =
                    if (isVideoOnTexture) View.VISIBLE else View.GONE

                mIVideoPlayer?.setVideoViewTexture(if (!isVideoOnTexture) playerTextureView else null)
                mIVideoPlayer?.setVideoViewSurface(if (isVideoOnTexture) playerSurfaceView else null)
                mIVideoPlayer?.open(true)

                val msg = if (isVideoOnTexture) "Using Surface View" else "Using Texture View"
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
            R.id.mbCast -> {
                Log.d(TAG, "EEE Cast clicked")
                mIVideoPlayer?.cast()
            }
        }
    }

    /**
     * This method is used to set resize mode to the player view
     *
     * @param isLandscape which is true if video is landscape otherwise false
     */
    private fun setResizeMode(isLandscape: Boolean) {
        if (isLandscape) {
            val mode =
                if (isPortraitVideo) ResizeMode.RESIZE_MODE_FIXED_HEIGHT else ResizeMode.RESIZE_MODE_FILL
            mIVideoPlayer?.setResizeMode(mode)
        } else {
            val mode =
                if (isPortraitVideo) ResizeMode.RESIZE_MODE_FIT else ResizeMode.RESIZE_MODE_FIXED_WIDTH
            mIVideoPlayer?.setResizeMode(mode)
        }
    }

    override fun onOrientationChange(screenOrientation: OrientationManager.ScreenOrientation?) {
        if (mIVideoPlayer != null) {
            Log.d(TAG, String.format(LogMsg.msg_orientation_change, screenOrientation))
            when (screenOrientation) {
                OrientationManager.ScreenOrientation.PORTRAIT -> {
                    this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    setResizeMode(false)
                    ivFullScreen.visibility = View.VISIBLE
                    ivMinimize.visibility = View.GONE
                    showSystemUI()
                }
                OrientationManager.ScreenOrientation.REVERSED_PORTRAIT -> {
                    this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                    setResizeMode(false)
                    ivFullScreen.visibility = View.VISIBLE
                    ivMinimize.visibility = View.GONE
                    showSystemUI()
                }
                OrientationManager.ScreenOrientation.REVERSED_LANDSCAPE -> {
                    this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                    setResizeMode(true)
                    ivFullScreen.visibility = View.GONE
                    ivMinimize.visibility = View.VISIBLE
                    hideSystemUI()
                }
                OrientationManager.ScreenOrientation.LANDSCAPE -> {
                    this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    setResizeMode(true)
                    ivFullScreen.visibility = View.GONE
                    ivMinimize.visibility = View.VISIBLE
                    hideSystemUI()
                }
            }
        }
    }

    /**
     * This method will be triggered when screen comes to visible state
     */
    override fun onResume() {
        if (mIVideoPlayer != null && playerView != null && playbackState != PlaybackState.STATE_IDLE) {
            updateProgressBar()
            mIVideoPlayer?.resume()
            ivPause?.visibility = View.VISIBLE
            ivPlay?.visibility = View.GONE
        }
        super.onResume()
    }

    /**
     * This method will be triggered when screen goes to invisible state
     */
    override fun onPause() {
        if (mIVideoPlayer != null && playerView != null && playbackState != PlaybackState.STATE_IDLE) {
            ivPause?.visibility = View.GONE
            ivPlay?.visibility = View.VISIBLE
            mIVideoPlayer?.pause()
        }
        super.onPause()
    }

    /**
     * This method will be triggered when screen get destroyed
     */
    override fun onDestroy() {
        if (mIVideoPlayer != null && playerView != null) {
            mIVideoPlayer?.close()
            mHandler.removeCallbacks(durationRunnable)
        }
        orientationManager?.disable()
        super.onDestroy()
    }

    /**
     * This method will be triggered when user click on back press.
     */
    override fun onBackPressed() {
        if (dialog != null && dialog!!.isShowing) hideDialog()
        if (isAlertDialogShowing()) hideDialog()
        else super.onBackPressed()
    }

    /**
     * This method is used to initialize orientation listener and windows UI visibility listener
     */
    @Suppress("DEPRECATION")
    private fun setVisibilityListener() {
        orientationManager = OrientationManager(this, SensorManager.SENSOR_DELAY_NORMAL, this)
        orientationManager?.enable()

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.rltPlayBack)
        ) { _, insets ->

            val footerView = findViewById<RelativeLayout>(R.id.rltFooterView)

            val layoutParams = footerView.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.setMargins(
                0,
                insets.systemWindowInsetTop,
                0,
                insets.systemWindowInsetBottom
            )
            footerView.layoutParams = layoutParams

            insets.consumeSystemWindowInsets()
        }

        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                playerView?.showController()
            }
        }
    }

    /**
     * This method is used to show system UI
     */
    @Suppress("DEPRECATION")
    private fun showSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    /**
     * This method is used to hide system UI
     */
    @Suppress("DEPRECATION")
    private fun hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    /**
     * This method will be invoked when click video icon or feed plus icon to initiate or join in video call.
     */
    @SuppressLint("InflateParams")
    private fun showInfoDialog() {
        val view: View = layoutInflater.inflate(R.layout.player_info_dialog, null)
        dialog = BottomSheetDialog(this, R.style.BottomSheetDialog)

        val rvInfoList = view.findViewById(R.id.rvInfoList) as RecyclerView
        val ivClose = view.findViewById(R.id.ivClose) as AppCompatImageView

        rvInfoList.layoutManager = LinearLayoutManager(this)
        val adapter = VideoInfoAdapter(infoList, this)
        rvInfoList.adapter = adapter

        dialog?.setCanceledOnTouchOutside(true)
        dialog?.setContentView(view)

        ivClose.setOnClickListener { hideDialog() }
        dialog?.setOnCancelListener { hideDialog() }

        dialog?.show()
    }

    /**
     * This method will be invoked to show alert message when get permission and delete feed item.
     *
     * @param message which is a description of message
     */
    private fun showAlert(message: Int) {
        if (isAlertDialogShowing()) return

        alertDialog = AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(
                R.string.yes
            ) { dialog: DialogInterface, _: Int ->
                // User wants to try giving the permissions again.
                dialog.cancel()
                Utils.isPermissionsGranted(this)
            }
            .setNegativeButton(
                R.string.no
            ) { dialog: DialogInterface, _: Int ->
                // User doesn't want to give the permissions.
                dialog.cancel()
                onBackPressed()
            }
            .show()
    }

    private fun showAlert(title: String, message: String) {
        mHandler.removeCallbacks(durationRunnable)
        if (isAlertDialogShowing()) return

        ivPause?.isEnabled = false
        ivPlay?.isEnabled = false
        ivPlay?.alpha = 0.6f
        ivPause?.alpha = 0.6f

        enableSeek(false)
        mIVideoPlayer?.pause()

        Log.e(TAG, "Alert dialog show")

        alertDialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setCancelable(false)
            .setMessage(message)
            .setPositiveButton(
                R.string.ok
            ) { dialog, _ ->
                dialog.dismiss()
                onBackPressed()
            }
            .show()
    }

    /**
     * This method is used to hide dialog if it is showing
     */
    private fun hideDialog() {
        if (dialog != null && dialog!!.isShowing) dialog?.dismiss()
        if (isAlertDialogShowing()) alertDialog?.dismiss()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST) {
            val missingPermissions = Utils.getMissingPermissions(this)
            if (missingPermissions.isNotEmpty() && missingPermissions[0] != "android.permission.FOREGROUND_SERVICE") {
                // User didn't grant all the permissions. Warn that the application might not work correctly.
                showAlert(R.string.missing_permissions_try_again)
            } else {
                // All permissions granted.
                onPermissionsGranted()
            }
        }
    }

    override fun onPermissionsGranted() {
        initializePlayer()
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}

    override fun onStartTrackingTouch(p0: SeekBar?) {}

    companion object {
        private const val TAG = "VideoPlayerActivity"
        private const val PERMISSION_REQUEST = 1
    }

}