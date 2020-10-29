/*
 * Created by ADT author on 9/15/20 4:00 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/15/20 4:00 PM
 */

package com.adt.vpm.webrtc.service

import com.adt.vpm.webrtc.data.IceServer
import java.util.*

/**
 * This class is used to get our SL turn server details to add as IceServer
 */
object NatUtils {
    /**
     * This method is used to get our SL turn server details to add as IceServer.
     * It is called from SocketIoClient and RoomParameterFetcher class
     *
     * @return iceTurnServer
     */
    @JvmStatic
    fun getSLTurnServer(): IceServer {
        val natServers = ArrayList<String>()
        val iceTurnServer = IceServer()

        natServers.add(Constants.STUN_PREFIX + Constants.NAT_SERVER_IP + ":" + Constants.NAT_SERVER_PORT)
        natServers.add(Constants.TURN_PREFIX + Constants.NAT_SERVER_IP + ":" + Constants.NAT_SERVER_PORT)

        iceTurnServer.urls = natServers
        iceTurnServer.username = Constants.USERNAME
        iceTurnServer.password = Constants.PWORD
        return iceTurnServer
    }
}