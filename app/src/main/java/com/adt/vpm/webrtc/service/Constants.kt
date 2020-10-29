/*
 * Created by ADT author on 9/14/20 11:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/14/20 10:51 PM
 */

package com.adt.vpm.webrtc.service

interface Constants {
    companion object {
        const val EXTRA_ROOMID = "org.appspot.apprtc.ROOMID"
        const val EXTRA_ROOM_NAME = "org.appspot.apprtc.ROOMNAME"
        const val EXTRA_ROOM_LIVE = "org.appspot.apprtc.ROOMLIVE"
        const val EXTRA_ROOM_IS_FROM = "org.appspot.apprtc.ROOMISFROM"
        const val EXTRA_FEED_ROOM = "org.appspot.apprtc.FEEDROOM"

        const val ITEM_REFRESH = "item_refresh"

        const val SIGNAL_WEB_SOCKET = "WebSocket"
        const val SIGNAL_SOCKET_IO = "SocketIo"

        const val ROOM_NAME_PREFIX = "VpmSession"
        const val CAMERA_CREATE = 1
        const val FEED_JOIN = 2
        const val FEED_REJOIN = 3

        const val NAT_SERVER_IP = "3.133.160.5"
        const val NAT_SERVER_PORT = 3478
        const val USERNAME = "androidcl"
        const val PWORD = "andydroid"

        const val STUN_PREFIX = "stun:"
        const val TURN_PREFIX = "turn:"

    }


}