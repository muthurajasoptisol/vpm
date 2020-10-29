/*
 * Created by ADT author on 9/14/20 11:00 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/14/20 10:49 PM
 */

package com.adt.vpm.videoplayer

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import com.adt.vpm.util.Log
import com.adt.vpm.util.LogMsg
import com.adt.vpm.util.Utils
import com.adt.vpm.videoplayer.source.common.C
import com.adt.vpm.videoplayer.source.common.MediaItem
import com.adt.vpm.videoplayer.source.common.MediaMetadata
import com.adt.vpm.videoplayer.source.common.audio.AudioAttributes
import com.adt.vpm.videoplayer.source.common.util.FileTypes
import com.adt.vpm.videoplayer.source.common.util.Util
import com.adt.vpm.videoplayer.source.core.*
import com.adt.vpm.videoplayer.source.core.ExoPlaybackException.*
import com.adt.vpm.videoplayer.source.core.source.*
import com.adt.vpm.videoplayer.source.core.trackselection.AdaptiveTrackSelection
import com.adt.vpm.videoplayer.source.core.trackselection.DefaultTrackSelector
import com.adt.vpm.videoplayer.source.core.upstream.DefaultDataSourceFactory
import com.adt.vpm.videoplayer.source.core.util.EventLogger
import com.adt.vpm.videoplayer.source.core.video.VideoListener
import com.adt.vpm.videoplayer.source.ext.cast.CastPlayer
import com.adt.vpm.videoplayer.source.ext.cast.SessionAvailabilityListener
import com.adt.vpm.videoplayer.source.rtsp.RtspDefaultClient
import com.adt.vpm.videoplayer.source.rtsp.RtspMediaSource
import com.adt.vpm.videoplayer.source.rtsp.core.Client
import com.adt.vpm.videoplayer.source.ui.AspectRatioFrameLayout
import com.adt.vpm.videoplayer.source.ui.PlayerControlView
import com.adt.vpm.videoplayer.source.ui.PlayerView
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.dynamite.DynamiteModule
import java.io.IOException

/**
 * This class is used to access all the video player functionality
 */
class VideoPlayer(
    private val appContext: Context,
    private val uri: Uri,
    private val events: IPlayerCallback
) : IVideoPlayer, Player.EventListener,
    PlaybackPreparer,
    VideoListener, PlayerControlView.VisibilityListener, MediaSourceEventListener,
    SessionAvailabilityListener {

    enum class StreamType { LOCAL, HLS, HTTP, RTSP, OTHERS }

    private var mPlayer: Player? = null
    private var mPlayerView: PlayerView? = null
    private var streamType: StreamType = StreamType.LOCAL
    private var overrideExtension: String = ""
    private var metaDataList: MutableMap<String, String> = mutableMapOf()
    private var defaultTrackSelector: DefaultTrackSelector? = null
    private val mainThreadHandler = Handler(Looper.getMainLooper())
    private var playbackState: PlaybackState = PlaybackState.STATE_IDLE
    private var rtspMediaSource: RtspMediaSource? = null
    private var mCastPlayer: CastPlayer? = null
    private var mCastContext: CastContext? = null
    private var mExoPlayer: SimpleExoPlayer? = null
    private var mMediaIem: MediaItem? = null

    init {
        if (uri.toString().contains(".")) {
            overrideExtension = uri.toString().substring(uri.toString().lastIndexOf('.'))
        }
        setStreamType(uri)
        requestPermissions()
    }

    private fun initializeCastPlayer() {
        // Getting the cast context later than onStart can cause device discovery not to take place.
        try {
            if (mCastContext == null) mCastContext = CastContext.getSharedInstance(appContext)

            if (mCastPlayer == null) {
                mCastPlayer = CastPlayer(mCastContext!!)
                mCastPlayer?.addListener(this)
                mCastPlayer?.setSessionAvailabilityListener(this)
            }
        } catch (e: RuntimeException) {
            var cause = e.cause
            while (cause != null) {
                if (cause is DynamiteModule.LoadingException) {
                    return
                }
                cause = cause.cause
            }
            throw e
        }
    }

    /**
     * This method is used to initialize  the video player instance
     */
    private fun initVideoPlayer() {
        val extensionRendererMode = DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
        val renderersFactory: RenderersFactory = DefaultRenderersFactory(appContext)
            .setExtensionRendererMode(extensionRendererMode)

        defaultTrackSelector = DefaultTrackSelector(appContext, AdaptiveTrackSelection.Factory())

        mExoPlayer = SimpleExoPlayer.Builder(appContext, renderersFactory)
            .setTrackSelector(defaultTrackSelector)
            .setMediaSourceFactory(getMediaSourceFactory()).build()
        mExoPlayer?.setAudioAttributes(AudioAttributes.DEFAULT, true)

        if (Log.isLogEnabled()) {
            mExoPlayer?.addAnalyticsListener(EventLogger(defaultTrackSelector))
        }
    }

    /**
     * This method is used to get default media source factory
     */
    private fun getMediaSourceFactory(): MediaSourceFactory {
        return DefaultMediaSourceFactory(
            DefaultDataSourceFactory(appContext, Util.getUserAgent(appContext, LogMsg.app_name))
        )
    }

    /**
     * This method is used to get media item from Uri
     *
     * @param uri Uri of the file
     */
    private fun getMediaItem(uri: Uri): MediaItem {
        val fileType = FileTypes.inferFileTypeFromUri(uri)
        val contentType = Util.inferContentType(uri, overrideExtension)
        val adaptiveMimeType = Util.getAdaptiveMimeType(contentType, fileType)

        return MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(LogMsg.app_name).build())
            .setMimeType(adaptiveMimeType)
            .build()
    }

    /**
     * This method is used to get media source to support RTSP stream
     *
     * @param uri Uri of stream
     */
    private fun getMediaSource(uri: Uri): MediaSource? {
        rtspMediaSource = RtspMediaSource.Factory(
            RtspDefaultClient.factory(mExoPlayer)
                .setFlags(Client.FLAG_ENABLE_RTCP_SUPPORT)
                .setNatMethod(Client.RTSP_NAT_DUMMY)
        ).createMediaSource(uri, Looper.myLooper()?.let { Handler(it) }, this)
        return rtspMediaSource
    }

    /**
     * This method is used to set StreamType based on Uri
     *
     * @param uri Uri of the file
     */
    private fun setStreamType(uri: Uri) {
        val type = when {
            Util.isRtspUri(uri) -> {
                C.TYPE_RTSP
            }
            TextUtils.isEmpty(overrideExtension) -> Util.inferContentType(uri)
            else -> Util.inferContentType(
                ".$overrideExtension"
            )
        }
        return when (type) {
            C.TYPE_OTHER -> {
                streamType = StreamType.LOCAL
            }
            C.TYPE_HLS -> {
                streamType = StreamType.HLS
            }
            C.TYPE_RTSP -> {
                streamType = StreamType.RTSP
            }
            C.TYPE_DASH -> {
                streamType = StreamType.OTHERS
            }
            else -> {
                throw IllegalStateException("Unsupported type: $type")
            }
        }
    }

    /**
     * This method is used to open and prepare to render video in video player.
     *
     * @param playWhenReady – which represents video will be play if it is true otherwise pause
     */
    override fun open(playWhenReady: Boolean) {
        Log.i(TAG, LogMsg.msg_player_open)
        if (playbackState == PlaybackState.STATE_IDLE) {
            initVideoPlayer()
            mPlayer = mExoPlayer
            mPlayer?.addListener(this)
            mExoPlayer?.addVideoListener(this)
            mPlayer?.playWhenReady = playWhenReady

            mPlayerView?.player = mPlayer

            mPlayerView?.setPlaybackPreparer(this)
            mPlayerView?.setControllerVisibilityListener(this)

            if (streamType == StreamType.RTSP) {
                mExoPlayer?.setMediaSource(getMediaSource(uri), true)
            } else {
                mMediaIem = getMediaItem(uri)
                mExoPlayer?.setMediaItem(mMediaIem, true)
            }

            mPlayer?.prepare()
        } else {
            sendInvalidStateError()
        }
    }

    /**
     * This method is used to do play action to render video in video player.
     */
    override fun play() {
        Log.i(TAG, LogMsg.msg_player_play)
        when (playbackState) {
            PlaybackState.STATE_IDLE -> {
                sendInvalidStateError()
            }
            PlaybackState.STATE_READY,
            PlaybackState.STATE_PAUSED -> {
                mPlayer?.prepare()
                mPlayer?.playWhenReady = true
            }
            else -> {
            }
        }
    }

    /**
     * This method is used to do pause action in video player.
     */
    override fun pause() {
        Log.i(TAG, LogMsg.msg_player_pause)
        when (playbackState) {
            PlaybackState.STATE_IDLE -> {
                sendInvalidStateError()
            }
            PlaybackState.STATE_PLAYING,
            PlaybackState.STATE_IS_PLAYING,
            PlaybackState.STATE_BUFFERING -> {
                mPlayer?.playWhenReady = false
                mPlayerView?.onPause()
            }
            else -> {
            }
        }
    }

    /**
     * This method is used to do resume action in video player.
     */
    override fun resume() {
        Log.i(TAG, LogMsg.msg_player_resume)
        when (playbackState) {
            PlaybackState.STATE_IDLE -> {
                sendInvalidStateError()
            }
            PlaybackState.STATE_READY,
            PlaybackState.STATE_PAUSED -> {
                mPlayer?.playWhenReady = true
                mPlayerView?.onResume()
            }
            PlaybackState.STATE_BUFFERING -> {
                mPlayer?.playWhenReady = true
            }
            else -> {
            }
        }
    }

    /**
     * This method is used to stop the video if it is playing.
     */
    override fun stop() {
        Log.i(TAG, LogMsg.msg_player_stop)
        when (playbackState) {
            PlaybackState.STATE_IDLE -> {
                sendInvalidStateError()
            }
            PlaybackState.STATE_PLAYING,
            PlaybackState.STATE_IS_PLAYING,
            PlaybackState.STATE_PAUSED -> {
                mPlayer?.stop()
            }
            PlaybackState.STATE_BUFFERING -> {
                mPlayer?.playWhenReady = false
            }
            else -> {
            }
        }
    }

    /**
     * This method is used to seek to specific requested position in video player.
     *
     * @param positionMs seeking position in Ms
     */
    override fun seekToPosition(positionMs: Long) {
        mPlayer?.seekTo(positionMs)
        if (streamType == StreamType.RTSP && playbackState == PlaybackState.STATE_BUFFERING) {
            mPlayer?.pause()
            mPlayer?.play()
        }
        Log.d(
            TAG,
            String.format(LogMsg.msg_player_seek_to_position, milliSecondsToTimer(positionMs))
        )
    }

    /**
     * This method is used to get current position while playing video.
     *
     * @return milliSec CurrentPosition
     */
    override fun getCurrentPosition(): Long {
        return mPlayer?.currentPosition ?: -1
    }

    /**
     * This method is used to get total duration of the video.
     *
     * @return milliSec Total duration
     */
    override fun getTotalDuration(): Long {
        return mPlayer?.duration ?: 0
    }

    /**
     * This method is used to enable or disable volume while playing video.
     *
     * @param enable true if which is enable otherwise false
     */
    override fun mute(enable: Boolean) {

    }

    /**
     * This method is used to set volume while playing video.
     *
     * @param audioVolume level of volume which is need to be set
     */
    override fun setVolume(audioVolume: Float) {

    }

    /**
     * This method is used to identify playing video is a live stream or not.
     *
     * @return Boolean which is true if it is live otherwise false
     */
    override fun isLive(): Boolean {
        return false
    }

    /**
     * This method is used to identify whether we can seek currently playing video.
     *
     * @return Boolean which is true if it is not live stream otherwise false
     */
    override fun isSeekable(): Boolean {
        return getTotalDuration() > 0
    }

    /**
     * This method is used to set rendering view to attach video player.
     *
     * @param playerView which is a view using in exo player
     */
    override fun setVideoView(playerView: PlayerView?) {
        mPlayerView = playerView
    }

    /**
     * This method is used to set rendering view as TextureView to attach video player.
     *
     * @param textureView TextureView
     */
    override fun setVideoViewTexture(textureView: TextureView?) {
        mExoPlayer?.setVideoTextureView(textureView)
    }

    /**
     * This method is used to set rendering view as SurfaceView to attach video player.
     *
     * @param surfaceView SurfaceView
     */
    override fun setVideoViewSurface(surfaceView: SurfaceView?) {
        mExoPlayer?.setVideoSurfaceView(surfaceView)
    }

    /**
     * This method is used to get meta data of the video to display in info dialog.
     *
     * @return MutableMap<String, String> list of string values of meta data
     */
    override fun getMetaData(): MutableMap<String, String> {
        return metaDataList
    }

    /**
     * This method is used to get media mime type.
     *
     * @return MimeType type of media mime
     */
    override fun getMediaMimeType(): String {
        TODO("Not yet implemented")
    }

    /**
     * This method is used to get audio mime type.
     *
     * @return MimeType type of audio mime
     */
    override fun getAudioMimeType(): String {
        TODO("Not yet implemented")
    }

    /**
     * This method is used to get video mime type.
     *
     * @return MimeType type of video mime
     */
    override fun getVideoMimeType(): String {
        TODO("Not yet implemented")
    }

    override fun cast() {
        if (mCastContext == null || mCastPlayer == null) {
            initializeCastPlayer()
            Log.d(TAG, "Initialized cast clicked")
        }
    }

    /**
     * This method is used to identify whether casting is supported or not for selected playback.
     *
     * @return true if it is support casting otherwise false
     */
    override fun isCastable(): Boolean {
        return streamType != StreamType.LOCAL && streamType != StreamType.RTSP
    }

    /**
     * This method is used to identify whether currently casting or not.
     *
     * @return true if it is casting otherwise false
     */
    override fun isCasting(): Boolean {
        return mPlayer != null && mCastPlayer != null && mPlayer == mCastPlayer
    }

    /**
     * This method is used to close player when user try to close or which will called while error occur.
     */
    override fun close() {
        Log.i(TAG, LogMsg.msg_player_close)
        mPlayer?.stop()
        mPlayer?.release()
        mPlayer = null
        if (mExoPlayer != null) {
            mExoPlayer?.release()
            mExoPlayer = null
        }
        if (mCastPlayer != null) {
            mCastPlayer?.release()
            mCastPlayer = null
            mCastContext = null
        }
        if (streamType == StreamType.RTSP) rtspMediaSource?.client?.release()
    }

    /**
     * This method is used to set resize mode to fit or fill video in player view.
     *
     * @param resizeMode Type of mode to resize player view
     */
    override fun setResizeMode(resizeMode: ResizeMode) {
        Log.v(TAG, String.format(LogMsg.msg_player_resize_mode, resizeMode))
        when (resizeMode) {
            ResizeMode.RESIZE_MODE_FIT -> {
                mPlayerView?.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
            ResizeMode.RESIZE_MODE_FIXED_WIDTH -> {
                mPlayerView?.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
            }
            ResizeMode.RESIZE_MODE_FIXED_HEIGHT -> {
                mPlayerView?.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT
            }
            ResizeMode.RESIZE_MODE_FILL -> {
                mPlayerView?.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
            }
        }
    }

    /**
     * Called when the visibility changes.
     *
     * @param visibility The new visibility. Either [View.VISIBLE] or [View.GONE].
     */
    override fun onVisibilityChange(visibility: Int) {
        Log.v(TAG, String.format(LogMsg.msg_controller_visibility_change, visibility))
        mainThreadHandler.post { events.onControlsVisibilityChange(visibility) }
    }

    /** Called to prepare a playback.  */
    override fun preparePlayback() {
        mPlayer?.prepare()
    }

    /**
     * This callback method will be get triggered when playWhenReady state changed
     *
     * @param playWhenReady playWhenReady
     */
    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        playbackState =
            if (playWhenReady) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED

        sendPlaybackStateChanged(playbackState)

        Log.d(TAG, String.format(LogMsg.msg_play_when_ready_changed, playbackState))
    }

    /**
     * This callback method will be get triggered when video size changed
     *
     * @param width Width of the video
     * @param height Height of the video
     * @param pixelWidthHeightRatio Ratio
     */
    override fun onVideoSizeChanged(
        width: Int,
        height: Int,
        unappliedRotationDegrees: Int,
        pixelWidthHeightRatio: Float
    ) {
        setMetaDataList(width, height)

        val value = "width x height : $width x $height PixelWidthRatio : $pixelWidthHeightRatio "
        Log.d(TAG, String.format(LogMsg.msg_on_video_size_changed, value))

        mainThreadHandler.post { events.onVideoSizeChanged(width, height, pixelWidthHeightRatio) }
    }

    /**
     * This callback method will be get triggered when playback state changed in player
     *
     * @param state Playback state of player
     */
    override fun onPlaybackStateChanged(state: Int) {
        when (state) {
            Player.STATE_BUFFERING -> {
                playbackState = PlaybackState.STATE_BUFFERING
                isStreamEnded()
            }
            Player.STATE_ENDED -> {
                playbackState = PlaybackState.STATE_ENDED
            }
            Player.STATE_READY -> {
                playbackState = if (mPlayer?.isPlaying!!) {
                    PlaybackState.STATE_PLAYING
                } else {
                    PlaybackState.STATE_READY
                }
            }
            Player.STATE_IDLE -> {
                this.playbackState = PlaybackState.STATE_IDLE
                if (mPlayer != null && mCastPlayer != null && mPlayer == mCastPlayer && mCastContext?.sessionManager?.currentCastSession?.remoteMediaClient?.idleReason == MediaStatus.IDLE_REASON_FINISHED) {
                    mPlayer?.setMediaItem(mMediaIem)
                    mCastPlayer?.playWhenReady = false
                }
            }
        }
        Log.d(TAG, String.format(LogMsg.msg_playback_state_changed, playbackState))
        sendPlaybackStateChanged(playbackState)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) sendPlaybackStateChanged(PlaybackState.STATE_IS_PLAYING)
        Log.d(TAG, "IsPlaying $isPlaying")
    }

    override fun onLoadCanceled(
        windowIndex: Int,
        mediaPeriodId: MediaSource.MediaPeriodId?,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData
    ) {
        Log.d(
            TAG,
            "onLoadCancelled ${mediaLoadData.dataType} ${mediaLoadData.mediaEndTimeMs} ${loadEventInfo.loadDurationMs}, ${loadEventInfo.bytesLoaded}"
        )
    }

    override fun onLoadCompleted(
        windowIndex: Int,
        mediaPeriodId: MediaSource.MediaPeriodId?,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData
    ) {
        Log.d(
            TAG,
            "onLoadCompleted ${mediaLoadData.dataType} ${mediaLoadData.mediaEndTimeMs} ${loadEventInfo.loadDurationMs}, ${loadEventInfo.bytesLoaded}"
        )
    }

    override fun onIsLoadingChanged(isLoading: Boolean) {
        Log.d(TAG, "onIsLoadingChanged : $isLoading")
    }

    override fun onLoadError(
        windowIndex: Int,
        mediaPeriodId: MediaSource.MediaPeriodId?,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData,
        error: IOException,
        wasCanceled: Boolean
    ) {
        var errorType = PlayerError.TYPE_UNEXPECTED
        var title = LogMsg.msg_invalid_url_title
        var message = LogMsg.msg_un_expected_error
        Log.e(TAG, "onLoadError ${mediaLoadData.dataType}")
        when (mediaLoadData.dataType) {
            C.DATA_TYPE_MEDIA_INITIALIZATION -> {
                errorType = PlayerError.TYPE_SOURCE
                title = LogMsg.msg_invalid_url_title
                message = LogMsg.msg_invalid_url_msg
            }
            Client.TYPE_RTSP_IO_ERROR -> {
                title = LogMsg.msg_invalid_url_title
                message = LogMsg.msg_invalid_url_io_error
            }
            Client.TYPE_RTSP_NO_RESPONSE -> {
                title = LogMsg.msg_invalid_url_title
                message = LogMsg.msg_invalid_url_no_response
            }
            Client.TYPE_RTSP_UNSUCCESS -> {
                title = LogMsg.msg_invalid_url_title
                message = LogMsg.msg_invalid_url_unsuccess
            }
            Client.TYPE_RTSP_UNAUTHORIZE -> {
                title = LogMsg.msg_invalid_url_title
                message = LogMsg.msg_invalid_url_unauthorized
            }
            Client.TYPE_RTSP_REQUEST_TIMEOUT -> {
                title = LogMsg.msg_invalid_url_title
                message = LogMsg.msg_invalid_url_request_timeout
            }
            Client.TYPE_RTSP_MALFUNCTION -> {
                title = LogMsg.msg_invalid_url_title
                message = LogMsg.msg_invalid_url_malfunction
            }
            Client.TYPE_RTSP_SOCKET_TIMEOUT -> {

            }
        }

        sendPlayerError(errorType, title, message)
    }

    /**
     * This callback method will be get triggered when error comes during video player session
     *
     * @param error ExoPlaybackException
     */
    override fun onPlayerError(error: ExoPlaybackException) {
        var errorType = PlayerError.TYPE_UNEXPECTED
        var title = error.type.toString()
        var message = error.message ?: ""
        when (error.type) {
            TYPE_SOURCE -> {
                errorType = PlayerError.TYPE_SOURCE
                title = LogMsg.msg_invalid_url_title
                message = LogMsg.msg_invalid_url_msg
            }
            TYPE_RENDERER -> {
                errorType = PlayerError.TYPE_RENDERER
            }
            TYPE_UNEXPECTED -> {
                errorType = PlayerError.TYPE_UNEXPECTED
            }
            TYPE_REMOTE -> {
                errorType = PlayerError.TYPE_REMOTE
            }
            TYPE_OUT_OF_MEMORY -> {
                errorType = PlayerError.TYPE_OUT_OF_MEMORY
            }
            TYPE_TIMEOUT -> {
                errorType = PlayerError.TYPE_TIMEOUT
            }
        }

        Log.e(TAG, String.format(LogMsg.msg_player_error_type, errorType))
        Log.e(TAG, String.format(LogMsg.msg_player_error_msg, error.message ?: ""))

        sendPlayerError(errorType, title, message)
    }

    /** Called when a cast session becomes available to the player.  */
    override fun onCastSessionAvailable() {
        Log.d(TAG, "onCastSessionAvailable")
        if (mCastPlayer == null) initializeCastPlayer()
        setCurrentPlayer(mCastPlayer!!)
    }

    /** Called when the cast session becomes unavailable.  */
    override fun onCastSessionUnavailable() {
        Log.d(TAG, "onCastSessionUnAvailable")
        if (mExoPlayer == null) initVideoPlayer()
        setCurrentPlayer(mExoPlayer!!)
    }

    /**
     * This method is used to set the player between exoplayer and cast player.
     *
     * @param currentPlayer Player
     */
    private fun setCurrentPlayer(currentPlayer: Player) {
        Log.d(TAG, "SetCurrentPlayer")
        if (mPlayer === currentPlayer) {
            return
        }

        // View management.
        if (currentPlayer === mExoPlayer) {
            mExoPlayer?.addListener(this)
            mExoPlayer?.addVideoListener(this)
        } else  /* currentPlayer == castPlayer */ {
            mCastPlayer?.addListener(this)
            mCastPlayer?.setSessionAvailabilityListener(this)
        }

        // Player state management.
        var playbackPositionMs = C.TIME_UNSET
        var playWhenReady = false
        val previousPlayer: Player? = mPlayer
        if (previousPlayer != null) {
            // Save state from the previous player.
            val playbackState = previousPlayer.playbackState
            Log.d(TAG, "PlaybackState $playbackState")
            if (playbackState != Player.STATE_ENDED && playbackState != Player.STATE_IDLE) {
                playbackPositionMs = previousPlayer.currentPosition
                playWhenReady = previousPlayer.playWhenReady
                Log.d(TAG, "PlaybackPos $playbackPositionMs")
                Log.d(TAG, "playWhenReady $playWhenReady")
            }
            previousPlayer.stop(true)
        }
        mPlayer = currentPlayer
        mPlayerView?.player = mPlayer

        mPlayer?.setMediaItem(mMediaIem, playbackPositionMs)
        mPlayer?.playWhenReady = playWhenReady
        mPlayer?.prepare()
    }

    /**
     * This method is used send player error to application.
     * Which is get trigger whenever player return error
     *
     * @param errorType Type of error
     * @param title Title of the error message
     * @param message Description of error
     */
    private fun sendPlayerError(errorType: PlayerError, title: String, message: String) {
        mainThreadHandler.post {
            events.onPlayerError(
                errorType,
                title,
                message
            )
        }
    }

    /**
     * This method is used to send playback state to the application whenever state get changed
     *
     * @param playbackState State of the playback
     */
    private fun sendPlaybackStateChanged(playbackState: PlaybackState) {
        mainThreadHandler.post { events.onPlaybackStateChanged(playbackState) }
    }

    /**
     * This method is used to send permission granted status to application
     */
    private fun requestPermissions() {
        val isPermissionGranted =
            streamType == StreamType.LOCAL || Utils.isPermissionsGranted(appContext)
        if (isPermissionGranted) mainThreadHandler.post { events.onPermissionsGranted() }
    }

    /**
     * This method is used to detect the stream ended state.
     */
    private fun isStreamEnded() {
        val currentDuration = getCurrentPosition()
        val totalDuration = getTotalDuration() - 100
        if (isSeekable() && currentDuration > 0 && currentDuration > totalDuration && streamType != StreamType.LOCAL) {
            onPlaybackStateChanged(Player.STATE_ENDED)
        }
    }

    /**
     * This method is used to set meta data of the video
     *
     * @param width Width of the video
     * @param height Height of the video
     */
    private fun setMetaDataList(width: Int, height: Int) {
        metaDataList[LogMsg.data_width] = width.toString()
        metaDataList[LogMsg.data_height] = height.toString()
        metaDataList[LogMsg.data_duration] = milliSecondsToTimer(mPlayer?.duration ?: 0).toString()
        metaDataList[LogMsg.data_frame_rate] = mExoPlayer?.videoFormat?.frameRate.toString()
        metaDataList[LogMsg.data_mime_type] = mExoPlayer?.videoFormat?.sampleMimeType.toString()
        Log.d(TAG, String.format(LogMsg.msg_metadata_list, metaDataList.toString()))
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
     * This method is used to send invalid state message to application.
     */
    private fun sendInvalidStateError() {
        sendPlayerError(
            PlayerError.TYPE_PLAYBACK_STATE,
            LogMsg.error_invalid_state,
            LogMsg.error_invalid_state
        )
    }

    companion object {
        private const val TAG = "VideoPlayer"
    }

}