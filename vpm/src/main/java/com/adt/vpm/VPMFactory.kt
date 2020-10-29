/*
 * Created by ADT author on 9/14/20 11:00 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/14/20 10:49 PM
 */

package com.adt.vpm

import android.content.Context
import android.net.Uri
import com.adt.vpm.videoplayer.IPlayerCallback
import com.adt.vpm.videoplayer.IVideoPlayer
import com.adt.vpm.videoplayer.VideoPlayer
import com.adt.vpm.webrtc.IWebRTCSession
import com.adt.vpm.webrtc.IWebRTCSessionCallback
import com.adt.vpm.webrtc.WebRTCParams
import com.adt.vpm.webrtc.WebRTCSession

/**
 * Exposed SDK api. VPMFactory will act the initial entry point for
 * a session creation. Rest of the operation on the application side will be
 * based on the two interfaces returned.
 */
object VPMFactory {

    /**
     * CreateWebRTCSession :
     * This method is used to create WebRtc session to establish connection between peer to peer
     * @param appContext - which represents Context of application
     * @param peerConnectionParameters - which contains params about to establish peer connection
     * @param events - which is a callback to acknowledge to app layer
     * @return IWebRTCSession - which is a callback interface used to interact library and app
     */
    fun createWebRTCSession(
        appContext: Context?,
        peerConnectionParameters: WebRTCParams,
        events: IWebRTCSessionCallback
    ): IWebRTCSession {
        return WebRTCSession(appContext, peerConnectionParameters, events)
    }

    fun createPlayer(
        appContext: Context,
        uri: Uri,
        events: IPlayerCallback
    ): IVideoPlayer {
        return VideoPlayer(appContext, uri, events)
    }

}