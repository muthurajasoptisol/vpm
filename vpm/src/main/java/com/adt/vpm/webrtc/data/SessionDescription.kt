/*
 * Created by ADT author on 9/14/20 11:00 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/14/20 10:49 PM
 */

package com.adt.vpm.webrtc.data

import com.adt.vpm.util.LogMsg
import org.webrtc.SessionDescription

/**
 * This class is used to have session description data to app communicate with library
 */
data class SessionDescription(
    @kotlin.jvm.JvmField
    var description: String = "",
    var type: String = LogMsg.sdp_type_offer
) {

    object DataObj {
        /**
         * This method is used to get SdpData from SessionDescription
         *
         * @param sessionDescription instance of sessionDescription
         *
         * @return SdpData
         */
        fun getSdpData(sessionDescription: SessionDescription): com.adt.vpm.webrtc.data.SessionDescription {
            val sdpData = SessionDescription()
            sdpData.description = sessionDescription.description
            sdpData.type = sessionDescription.type.canonicalForm()
            return sdpData
        }

    }
}