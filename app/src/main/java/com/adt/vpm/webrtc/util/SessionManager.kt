/*
 * Created by ADT author on 9/14/20 11:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/14/20 10:51 PM
 */

package com.adt.vpm.webrtc.util

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.SystemClock
import android.webkit.URLUtil
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import com.adt.vpm.R
import com.adt.vpm.activity.CallActivity
import com.adt.vpm.activity.HomeActivity
import com.adt.vpm.model.Feed
import com.adt.vpm.model.Room
import com.adt.vpm.util.Log
import com.adt.vpm.util.LogMsg
import com.adt.vpm.webrtc.WebRTCParams
import com.adt.vpm.webrtc.signal.SignalingClient
import com.adt.vpm.webrtc.service.Constants
import com.adt.vpm.webrtc.service.WebRtcService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * SessionManager class is used to maintain the WebRtc session to rejoin and keep update FeedList with preference
 */
class SessionManager {
    private var sharedPref: SharedPreferences? = null
    private var keyPrefResolution: String? = null
    private var keyPrefFps: String? = null
    private var keyPrefVideoBitrateType: String? = null
    private var keyPrefVideoBitrateValue: String? = null
    private var keyPrefAudioBitrateType: String? = null
    private var keyPrefAudioBitrateValue: String? = null
    private var keyPrefRoomServerUrl: String? = null
    private var keyPrefServerUrl: String? = null
    private var keyPrefRoomList: String? = null
    private var roomList: MutableList<Room> = mutableListOf()
    private var feedList: MutableList<Feed> = mutableListOf()
    private var mWebRtcService: WebRtcService? = null

    /**
     * This method is called to initialize feed list from preference
     *
     * @param context Context
     */
    fun init(context: Context) {
        PreferenceManager.setDefaultValues(context, R.xml.preferences, false)
        sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        keyPrefRoomList = context.getString(R.string.pref_room_list_key)

        keyPrefResolution = context.getString(R.string.pref_resolution_key)
        keyPrefFps = context.getString(R.string.pref_fps_key)
        keyPrefVideoBitrateType = context.getString(R.string.pref_maxvideobitrate_key)
        keyPrefVideoBitrateValue = context.getString(R.string.pref_maxvideobitratevalue_key)
        keyPrefAudioBitrateType = context.getString(R.string.pref_startaudiobitrate_key)
        keyPrefAudioBitrateValue = context.getString(R.string.pref_startaudiobitratevalue_key)
        keyPrefRoomServerUrl = context.getString(R.string.pref_room_server_url_key)
        keyPrefServerUrl = context.getString(R.string.pref_sl_server_url_key)

        val serializedObject: String? = sharedPref?.getString(keyPrefRoomList, null)
        if (serializedObject != null) {
            val type = object : TypeToken<List<Feed?>?>() {}.type
            feedList = Gson().fromJson(serializedObject, type)
        }
    }

    /**
     * This method is used to create or update room list to maintain live session instances
     *
     * @param roomId id of the room
     * @param isLive status of room whether it is live or not
     * @param webRtcService instance of WebRtcService class
     */
    fun updateCallSessionList(
        roomId: String,
        isLive: Boolean,
        webRtcService: WebRtcService?
    ) {
        var index = 0
        var id = ""
        if (roomList.isNotEmpty()) {
            for (room in roomList) {
                if (roomId == room.roomId) {
                    index = roomList.indexOf(room)
                    id = room.roomId
                    break
                }
            }
        }
        val room = Room()
        room.roomId = roomId
        if (roomId.contains(Constants.ROOM_NAME_PREFIX)) {
            room.roomName = roomId.removePrefix(Constants.ROOM_NAME_PREFIX)
        } else {
            room.roomName = roomId
        }
        room.isLive = isLive
        room.webRtcService = webRtcService

        if (webRtcService != null && (roomList.isEmpty() || id.isEmpty())) {
            roomList.add(room)
        } else if (index < roomList.size && id == roomId) {
            if (webRtcService == null && !isLive) roomList.removeAt(index) else roomList[index] =
                room
        }
    }

    /**
     * This method will be invoked from feed fragment to reset feed list status as false when session end from remote end
     */
    fun resetFeedListStatus() {
        if (feedList.isNotEmpty()) {
            for (feeds in feedList) {
                feeds.isLive = false
            }
            updateToSharedPref(feedList)
        }
    }

    /**
     * This method is used to update feed list into shared preference
     */
    private fun updateToSharedPref(feedList: List<Feed>?) {
        val updateList = Gson().toJson(feedList)
        val editor: SharedPreferences.Editor? = sharedPref?.edit()
        editor?.putString(keyPrefRoomList, updateList)
        editor?.apply()
    }

    /**
     * This method is used to check whether room is in live or not
     *
     * @param roomId id of the room
     */
    private fun isRoomLive(roomId: String): Boolean {
        var isLive = false
        if (roomList.isNotEmpty()) {
            for (room in roomList) {
                if (roomId == room.roomId && room.webRtcService != null) {
                    isLive = true
                    break
                }
            }
        }
        return isLive
    }

    /**
     * This method is used to check whether feed list having atleast one room has live or not.
     */
    fun hasLiveRoom(): Boolean {
        if (feedList.isNotEmpty()) {
            for (feed in feedList) {
                if (feed.isLive) return true
            }
        }
        return false
    }

    /**
     * This method is used to update room status whether it is live or not
     *
     * @param context Context
     * @param roomId id of the room
     * @param isLive True if it is live otherwise false
     */
    fun updateRoomStatus(context: Context, roomId: String, isLive: Boolean) {
        if (feedList.isNotEmpty()) {
            var index: Int? = null
            for (list in feedList) {
                if (list.roomId == roomId) {
                    index = feedList.indexOf(list)
                    break
                }
            }
            if (index != null && index < feedList.size) {
                feedList[index].isLive = isLive
                updateToSharedPref(feedList)
            }
        }
        sendUpdateStatus(context)
    }

    /**
     * This method will be invoked internally to sent update to feed to refresh via broadcast receiver
     *
     * @param context Context
     */
    private fun sendUpdateStatus(context: Context) {
        val intent = Intent(Constants.ITEM_REFRESH)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    /**
     * This method is used to add room list in feed once stream get connected
     *
     * @param feed Feed
     */
    fun addRoomToList(feed: Feed) {
        var isRoomExist = false
        if (feedList.isNotEmpty()) {
            for ((index, value) in feedList.withIndex()) {
                if (value.roomId == feed.roomId) {
                    isRoomExist = true
                    feedList[index] = feed
                    break
                }
            }
        }

        if (!isRoomExist) {
            feedList.add(feed)
            val changedList: MutableList<Feed>? = setupFeedsStatusOrder(feedList)
            updateToSharedPref(changedList)
        }
    }

    /**
     * This method is used to remove list of rooms selected to delete in feed list
     *
     * @param removeItem list of items to be removed
     *
     * @return feedList
     */
    fun removeRoomToList(removeItem: List<Feed>?): MutableList<Feed>? {
        if (feedList.isNotEmpty()) {
            if (removeItem != null) {
                for (i in removeItem.indices) {
                    for (k in feedList.indices) {
                        if (removeItem[i].roomId == feedList[k].roomId) {
                            feedList.remove(feedList[k])
                            break
                        }
                    }
                }
            }
        }
        updateToSharedPref(feedList)
        return feedList
    }

    /**
     * This method is used to get list of feed list items maintained as history
     */
    fun getRoomList(): MutableList<Feed>? {
        return setupFeedsStatusOrder(feedList)
    }

    /**
     * This method is used to setup feed status order based on live as top
     *
     * @param feedList list of feed items
     */
    private fun setupFeedsStatusOrder(feedList: List<Feed>?): MutableList<Feed>? {
        val grouped = feedList?.groupBy { it.isLive }
        return listOf(
            grouped?.get(true)?.sortedBy { it.isLive }.orEmpty(),
            grouped?.get(false)?.reversed().orEmpty()
        ).flatten().toMutableList()
    }

    /**
     * This method will be invoked when video icon, feed plus icon click and feed list item click
     *
     * @param context HomeActivity
     * @param feed feed
     * @param isFrom where it is came from to establish call
     */
    fun connectToRoom(context: HomeActivity, feed: Feed, isFrom: Int) {
        val roomUrl = sharedPref?.getString(
            keyPrefRoomServerUrl, context.getString(R.string.pref_room_server_url_default)
        )

        val socketIoUrl = sharedPref?.getString(
            keyPrefServerUrl, context.getString(R.string.pref_default_sl_server_url_demo)
        )

        // Use Camera2 option.
        val useCamera2 = sharedPrefGetBoolean(
            context, R.string.pref_camera2_key, R.string.pref_camera2_default
        )

        // Get default codecs.
        val videoCodec = sharedPrefGetString(
            context, R.string.pref_videocodec_key, R.string.pref_videocodec_default
        )

        val audioCodec = sharedPrefGetString(
            context, R.string.pref_audiocodec_key, R.string.pref_audiocodec_default
        )

        // Check HW codec flag.
        val hwCodec = sharedPrefGetBoolean(
            context, R.string.pref_hwcodec_key, R.string.pref_hwcodec_default
        )

        // Check Capture to texture.
        val captureToTexture = sharedPrefGetBoolean(
            context, R.string.pref_capturetotexture_key, R.string.pref_capturetotexture_default
        )

        // Get video resolution from settings.
        var videoWidth = 0
        var videoHeight = 0
        val resolution =
            sharedPref?.getString(
                keyPrefResolution,
                context.getString(R.string.pref_resolution_default)
            )
        val dimensions =
            resolution!!.split("[ x]+".toRegex()).toTypedArray()
        if (dimensions.size == 2) {
            try {
                videoWidth = dimensions[0].toInt()
                videoHeight = dimensions[1].toInt()
            } catch (e: NumberFormatException) {
                videoWidth = 0
                videoHeight = 0
                Log.e(
                    TAG,
                    "Wrong video resolution setting: $resolution"
                )
            }
        }

        // Get camera fps from settings.
        var cameraFps = 0
        val fps =
            sharedPref?.getString(keyPrefFps, context.getString(R.string.pref_fps_default))
        val fpsValues = fps!!.split("[ x]+".toRegex()).toTypedArray()
        if (fpsValues.size == 2) {
            try {
                cameraFps = fpsValues[0].toInt()
            } catch (e: NumberFormatException) {
                cameraFps = 0
                Log.e(
                    TAG,
                    "Wrong camera fps setting: $fps"
                )
            }
        }

        // Get video and audio start bitrate.
        var videoStartBitrate = 0
        val videoBitrateTypeDefault = context.getString(R.string.pref_maxvideobitrate_default)
        val videoBitrateType =
            sharedPref?.getString(keyPrefVideoBitrateType, videoBitrateTypeDefault)
        if (videoBitrateType != videoBitrateTypeDefault) {
            val bitrateValue = sharedPref?.getString(
                keyPrefVideoBitrateValue,
                context.getString(R.string.pref_maxvideobitratevalue_default)
            )
            videoStartBitrate = bitrateValue!!.toInt()
        }

        var audioStartBitrate = 0
        val bitrateTypeDefault = context.getString(R.string.pref_startaudiobitrate_default)
        val bitrateType = sharedPref?.getString(keyPrefAudioBitrateType, bitrateTypeDefault)
        if (bitrateType != bitrateTypeDefault) {
            val bitrateValue = sharedPref?.getString(
                keyPrefAudioBitrateValue,
                context.getString(R.string.pref_startaudiobitratevalue_default)
            )
            audioStartBitrate = bitrateValue!!.toInt()
        }

        val isRoomLive = isRoomLive(feed.roomId)

        if (validateUrl(context, roomUrl)) {
            val signalType = getSignalType(context)
            val uri =
                if (signalType == Constants.SIGNAL_SOCKET_IO) Uri.parse(socketIoUrl) else Uri.parse(
                    roomUrl
                )
            val roomConnectionParameters = SignalingClient.RoomConnectionParameters(
                uri.toString(),
                feed.roomId,
                ""
            )
            val peerConnectionParameters =
                videoCodec?.let {
                    WebRTCParams(
                        feed.enableLocalVideo,
                        feed.enableLocalAudio,
                        feed.enableRemoteAudio,
                        useCamera2,
                        captureToTexture,
                        videoWidth,
                        videoHeight,
                        cameraFps,
                        videoStartBitrate,
                        it,
                        hwCodec,
                        audioStartBitrate,
                        audioCodec
                    )
                }

            val feedJson = Gson().toJson(feed, Feed::class.java)

            val sessionType = if (feed.initializer) LogMsg.msg_creator else LogMsg.msg_joiner
            val session = "Session type : $sessionType RoomId : ${feed.roomId}"

            val webRtcParam = String.format(
                LogMsg.msg_peer_connection_param, Gson().toJson(peerConnectionParameters)
            )
            val roomParam =
                String.format(LogMsg.msg_room_params, Gson().toJson(roomConnectionParameters))

            Log.i(TAG, session)
            Log.d(TAG, feedJson)
            Log.d(TAG, webRtcParam)
            Log.d(TAG, roomParam)

            val intent = Intent(context, CallActivity::class.java)
            intent.data = uri
            intent.putExtra(Constants.EXTRA_ROOMID, feed.roomId)
            intent.putExtra(Constants.EXTRA_ROOM_NAME, feed.roomName)
            intent.putExtra(Constants.EXTRA_ROOM_LIVE, isRoomLive)
            intent.putExtra(Constants.EXTRA_ROOM_IS_FROM, isFrom)
            intent.putExtra(Constants.EXTRA_FEED_ROOM, feedJson)

            if (isRoomLive) {
                mWebRtcService = getWebRtcService(feed.roomId)
            }

            if (!isRoomLive || mWebRtcService == null) {
                mWebRtcService = createWebRtcService(
                    context,
                    isFrom,
                    feed,
                    signalType,
                    peerConnectionParameters,
                    roomConnectionParameters
                )
                updateCallSessionList(feed.roomId, isRoomLive, mWebRtcService)
            }

            context.startActivity(intent)
        }
    }

    /**
     * This method is used to get signaling type which is selected in setting page
     *
     * @param activity
     */
    private fun getSignalType(activity: HomeActivity): String {
        // Create connection client.
        val useSLServer =
            sharedPref?.getBoolean(activity.getString(R.string.pref_enable_sl_server_key), false)
        val mSignalType: String
        mSignalType = if (useSLServer != null && useSLServer) {
            Constants.SIGNAL_SOCKET_IO
        } else {
            Constants.SIGNAL_WEB_SOCKET
        }

        return mSignalType
    }

    /**
     * This method is used to get state of log which is enabled or not.
     *
     * @param activity
     */
    fun isLoggingEnabled(activity: AppCompatActivity): Boolean {
        // Create connection client.
        return sharedPref?.getBoolean(activity.getString(R.string.pref_enable_logging_key), false)!!
    }

    /**
     * This method is used to create instance of WebRtcService class
     *
     * @param activity HomeActivity
     * @param isFrom where it is came from
     * @param feed Feed
     * @param signalType signaling type
     * @param peerConnectionParameters peer connection parameters
     * @param roomConnectionParameters room connection parameters
     *
     * @return WebRtcService
     */
    private fun createWebRtcService(
        activity: HomeActivity,
        isFrom: Int,
        feed: Feed,
        signalType: String,
        peerConnectionParameters: WebRTCParams?,
        roomConnectionParameters: SignalingClient.RoomConnectionParameters
    ): WebRtcService {
        return WebRtcService(
            activity,
            isFrom,
            feed,
            signalType,
            peerConnectionParameters,
            roomConnectionParameters
        )
    }

    /**
     * This method is used to get WebRtcService instance if it is in live
     *
     * @param roomId id of the room
     */
    fun getWebRtcService(roomId: String): WebRtcService? {
        var mWebRtcService: WebRtcService? = null
        if (roomList.isNotEmpty()) {
            for (room in roomList) {
                Log.v("WEB SERVE", "Room id " + room.roomId + " webRtcService " + room.webRtcService)
                if (roomId == room.roomId && room.webRtcService != null) {
                    mWebRtcService = room.webRtcService
                    break
                }
            }
        }
        return mWebRtcService
    }

    /**
     * Get a value from the shared preference or from the intent, if it does not
     * exist the default is used.
     */
    private fun sharedPrefGetString(context: Context, attributeId: Int, defaultId: Int): String? {
        val defaultValue = context.getString(defaultId)
        val attributeName = context.getString(attributeId)
        return sharedPref?.getString(attributeName, defaultValue)
    }

    /**
     * Get a value from the shared preference or from the intent, if it does not
     * exist the default is used.
     */
    private fun sharedPrefGetBoolean(context: Context, attributeId: Int, defaultId: Int): Boolean {
        val defaultValue = java.lang.Boolean.parseBoolean(context.getString(defaultId))
        val attributeName = context.getString(attributeId)
        return sharedPref?.getBoolean(attributeName, defaultValue) ?: false
    }

    /**
     * This method is used to validate url
     *
     * @param context Context
     * @param url url
     */
    private fun validateUrl(context: Context, url: String?): Boolean {
        if (URLUtil.isHttpsUrl(url) || URLUtil.isHttpUrl(url)) {
            return true
        }

        AlertDialog.Builder(context)
            .setTitle(context.getText(R.string.invalid_url_title))
            .setMessage(context.getString(R.string.invalid_url_text, url))
            .setCancelable(false)
            .setNeutralButton(
                R.string.ok
            ) { dialog, _ -> dialog.cancel() }
            .create()
            .show()
        return false
    }

    /**
     * This method is used to restrict double click
     */
    fun isTaped(): Boolean {
        if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) return true
        mLastClickTime = SystemClock.elapsedRealtime()
        return false
    }


    companion object {
        private const val TAG = "SessionManager"
        var mLastClickTime: Long = 0

        // This is used to get singleton instance of this class
        var instance: SessionManager? = null
            get() {
                if (field == null) {
                    field = SessionManager()
                }
                return field
            }
            private set
    }

}