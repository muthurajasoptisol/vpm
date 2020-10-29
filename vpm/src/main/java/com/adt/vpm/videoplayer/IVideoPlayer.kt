/*
 * Created by ADT author on 9/14/20 11:00 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/14/20 10:49 PM
 */

package com.adt.vpm.videoplayer

import android.view.SurfaceView
import android.view.TextureView
import com.adt.vpm.videoplayer.source.ui.PlayerView

/**
 * IVideoPlayer is the functional interface for video player.
 * Application will use this interface to render local or http playback video files or urls in
 * application.
 */
interface IVideoPlayer {

    /**
     * This method is used to open and prepare to render video in video player.
     *
     * @param playWhenReady – which represents video will be play if it is true otherwise pause
     */
    fun open(playWhenReady: Boolean)

    /**
     * This method is used to do play action to render video in video player.
     */
    fun play()

    /**
     * This method is used to do pause action in video player.
     */
    fun pause()

    /**
     * This method is used to do resume action in video player.
     */
    fun resume()

    /**
     * This method is used to stop the video if it is playing.
     */
    fun stop()

    /**
     * This method is used to seek to specific requested position in video player.
     *
     * @param positionMs seeking position in Ms
     */
    fun seekToPosition(positionMs: Long)

    /**
     * This method is used to get current position while playing video.
     *
     * @return milliSec CurrentPosition
     */
    fun getCurrentPosition(): Long

    /**
     * This method is used to get total duration of the video.
     *
     * @return milliSec Total duration
     */
    fun getTotalDuration(): Long

    /**
     * This method is used to enable or disable volume while playing video.
     *
     * @param enable true if which is enable otherwise false
     */
    fun mute(enable: Boolean)

    /**
     * This method is used to set volume while playing video.
     *
     * @param audioVolume level of volume which is need to be set
     */
    fun setVolume(audioVolume: Float)

    /**
     * This method is used to identify playing video is a live stream or not.
     *
     * @return Boolean which is true if it is live otherwise false
     */
    fun isLive(): Boolean

    /**
     * This method is used to identify whether we can seek currently playing video.
     *
     * @return Boolean which is true if it is not live stream otherwise false
     */
    fun isSeekable(): Boolean

    /**
     * This method is used to switch player between cast player and exoplayer.
     */
    fun cast()

    /**
     * This method is used to identify whether casting is supported or not for selected playback.
     *
     * @return true if it is support casting otherwise false
     */
    fun isCastable() : Boolean

    /**
     * This method is used to identify whether currently casting or not.
     *
     * @return true if it is casting otherwise false
     */
    fun isCasting() : Boolean

    /**
     * This method is used to set rendering view to attach video player.
     *
     * @param playerView which is a view using in exo player
     */
    fun setVideoView(playerView: PlayerView?)

    /**
     * This method is used to set rendering view as TextureView to attach video player.
     *
     * @param textureView TextureView
     */
    fun setVideoViewTexture(textureView: TextureView?)

    /**
     * This method is used to set rendering view as SurfaceView to attach video player.
     *
     * @param surfaceView SurfaceView
     */
    fun setVideoViewSurface(surfaceView: SurfaceView?)

    /**
     * This method is used to get meta data of the video to display in info dialog.
     *
     * @return MutableMap<String, String> list of string values of meta data
     */
    fun getMetaData(): MutableMap<String, String>

    /**
     * This method is used to get media mime type.
     *
     * @return MimeType type of media mime
     */
    fun getMediaMimeType(): String

    /**
     * This method is used to get audio mime type.
     *
     * @return MimeType type of audio mime
     */
    fun getAudioMimeType(): String

    /**
     * This method is used to get video mime type.
     *
     * @return MimeType type of video mime
     */
    fun getVideoMimeType(): String

    /**
     * This method is used to set resize mode to fit or fill video in player view.
     *
     * @param resizeMode Type of mode to resize player view
     */
    fun setResizeMode(resizeMode: ResizeMode)

    /**
     * This method is used to close player when user try to close or which will called while error occur.
     */
    fun close()
}

/**
 * This enum class is defined type of possible error happen while try to play video in this player
 */
enum class PlayerError { TYPE_SOURCE, TYPE_RENDERER, TYPE_UNEXPECTED, TYPE_REMOTE, TYPE_OUT_OF_MEMORY, TYPE_TIMEOUT, TYPE_NONE, TYPE_PLAYBACK_STATE }

/**
 * This enum class is defined type of possible playback states used in this player
 */

enum class PlaybackState { STATE_IDLE, STATE_READY, STATE_BUFFERING, STATE_PLAYING, STATE_IS_PLAYING, STATE_PAUSED, STATE_ENDED }

/**
 * This enum class is defined type of possible resize mode used in this player
 */
enum class ResizeMode { RESIZE_MODE_FIT, RESIZE_MODE_FIXED_WIDTH, RESIZE_MODE_FIXED_HEIGHT, RESIZE_MODE_FILL }

/**
 * Callback interface for getting progress & error event from video player
 * IPlayerCallback user MUST implement this interface to get callback from player end.
 */
interface IPlayerCallback {

    /**
     * This callback method is triggered once playback state changed in player.
     *
     * @param playbackState - which is represent the changed playback state.
     */
    fun onPlaybackStateChanged(playbackState: PlaybackState)

    /**
     * This callback method is triggered once any error occurred during the player session.
     *
     * @param error - Type of error
     * @param title - Alert title
     * @param message - description of error
     */
    fun onPlayerError(error: PlayerError, title:String, message: String)

    /**
     * This callback method is triggered once video size changed.
     *
     * @param width - width of the video
     * @param height - height of the video
     * @param pixelWidthHeightRatio - pixel ratio
     */
    fun onVideoSizeChanged(width: Int, height: Int, pixelWidthHeightRatio: Float)

    /**
     * Called when the visibility changes.
     *
     * @param visibility The new visibility. Either VISIBLE or GONE.
     */
    fun onControlsVisibilityChange(visibility: Int)

    /**
     * This callback method will be triggered if permission granted to access local file storage
     */
    fun onPermissionsGranted()

}
