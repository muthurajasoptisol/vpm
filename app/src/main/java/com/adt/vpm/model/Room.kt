/*
 * Created by ADT author on 9/14/20 11:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/14/20 10:51 PM
 */

package com.adt.vpm.model

import com.adt.vpm.webrtc.service.WebRtcService

class Room {
    var roomId = ""
    var roomName = ""
    var isLive: Boolean = false
    var webRtcService: WebRtcService? = null
}