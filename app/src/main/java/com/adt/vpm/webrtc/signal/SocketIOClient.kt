/*
 * Created by ADT author on 9/14/20 11:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/14/20 10:51 PM
 */

package com.adt.vpm.webrtc.signal

import com.adt.vpm.util.Log
import com.adt.vpm.util.LogMsg
import com.adt.vpm.webrtc.SignalingParameters
import com.adt.vpm.webrtc.data.IceCandidate
import com.adt.vpm.webrtc.data.IceCandidate.DataObj.toJavaCandidate
import com.adt.vpm.webrtc.data.IceCandidate.DataObj.toJsonCandidate
import com.adt.vpm.webrtc.data.IceServer
import com.adt.vpm.webrtc.data.SessionDescription
import com.adt.vpm.webrtc.service.NatUtils.getSLTurnServer
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.engineio.client.transports.WebSocket
import kotlinx.coroutines.*
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URISyntaxException
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList
import kotlin.jvm.Throws


class SocketIOClient(events: SignalingClient.SignalingEvents) :
    SignalingClient {

    private val TURN_HTTP_TIMEOUT_MS: Int = 60000
    private val APPRTC_TURN_URL: String = "https://appr.tc/params"
    private val TAG = "SocketIOClient"
    private lateinit var ioSocket: Socket
    private var callback: SignalingClient.SignalingEvents = events
    private var initiator = false
    private var connectionParameters: SignalingClient.RoomConnectionParameters? = null
    private var useSLNATServer = true

    enum class RoomState { NOT_CONNECTED, CONNECTING, CONNECTED }

    private var roomState: RoomState = RoomState.NOT_CONNECTED

    override fun connectToRoom(connectionParameters: SignalingClient.RoomConnectionParameters?) {
        this.connectionParameters = connectionParameters
        GlobalScope.launch {
            connectToRoomInternal(connectionParameters)
        }
    }

    override fun disconnectFromRoom() {
        Log.i(TAG, LogMsg.msg_room_disconnected)
        if (roomState == RoomState.CONNECTED) {
            ioSocket.emit("bye", connectionParameters?.roomId)
        }
        resetSession()
    }

    override fun useSLNATServer(enable: Boolean?) {
        if (enable != null) useSLNATServer = enable
    }

    override fun sendOfferSdp(sdp: SessionDescription?) {
        if (roomState == RoomState.CONNECTED) {
            val json = JSONObject()
            json.put("sdp", sdp?.description)
            json.put("type", "offer")
            ioSocket.emit("message", json)
            Log.d(TAG, String.format(LogMsg.msg_send_offer, json.toString()))
        } else {
            Log.e(TAG, LogMsg.error_send_offer_failed)
        }
    }

    override fun sendAnswerSdp(sdp: SessionDescription?) {
        if (roomState == RoomState.CONNECTED) {
            val json = JSONObject()
            json.put("sdp", sdp?.description)
            json.put("type", "answer")
            ioSocket.emit("message", json)
            Log.d(TAG, String.format(LogMsg.msg_send_answer, json.toString()))
        } else {
            Log.e(TAG, LogMsg.error_send_answer_failed)
        }
    }

    override fun sendLocalIceCandidate(candidate: IceCandidate?) {
        if (roomState == RoomState.CONNECTED && candidate != null) {
            val json = toJsonCandidate(candidate)
            ioSocket.emit("message", json)
        }
    }

    override fun sendLocalIceCandidateRemovals(candidates: Array<out IceCandidate>?) {}

    private fun connectToRoomInternal(connectionParameters: SignalingClient.RoomConnectionParameters?) {
        val options = IO.Options()
        options.transports = arrayOf(WebSocket.NAME)
        options.forceNew = true
        options.reconnection = false
        try {
            ioSocket = IO.socket(connectionParameters?.roomUrl, options)

            ioSocket.on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "Received Event_Connect..")
                // send join room
                ioSocket.emit("create or join", connectionParameters?.roomId)

            }

            ioSocket.on("created") {
                Log.i(TAG, LogMsg.msg_room_created)
                initiator = true
                val iceServers: MutableList<IceServer> = createHardcodedIceServers()
                val params = SignalingParameters(
                    iceServers, initiator, null,
                    null, null
                )
                roomState = RoomState.CONNECTED
                callback.onConnectedToRoom(params, connectionParameters?.roomId)
            }

            ioSocket.on("joined") {
                initiator = false

                val joinedObj = it[0] as JSONObject
                val offerSdpObj = joinedObj.getJSONObject("offersdp")
                val candidatesArr = joinedObj.getJSONArray("candidates")
                val sdp = SessionDescription(offerSdpObj.getString("sdp"), LogMsg.sdp_type_offer)
                val iceCandidates: MutableList<IceCandidate> = ArrayList()

                for (i in 0 until candidatesArr.length()) {
                    val candidateObj = candidatesArr.getJSONObject(i)
                    val candidate = toJavaCandidate(candidateObj)
                    iceCandidates.add(candidate!!)
                }

                val iceServers: MutableList<IceServer> = createHardcodedIceServers()

                val params = SignalingParameters(
                    iceServers, initiator, null,
                    sdp, iceCandidates
                )

                Log.i(TAG, LogMsg.msg_room_joined)
                Log.d(TAG, String.format(LogMsg.msg_room_joined_resp, joinedObj.toString()))

                roomState = RoomState.CONNECTED
                callback.onConnectedToRoom(params, connectionParameters?.roomId)
            }

            ioSocket.on(Socket.EVENT_DISCONNECT) {
                if (roomState == RoomState.CONNECTED) callback.onChannelClose()
            }

            ioSocket.on(Socket.EVENT_CONNECT_ERROR) {
                val ex = it[0] as Exception
                ex.printStackTrace()
                callback.onChannelError(Socket.EVENT_CONNECT_ERROR)
            }

            ioSocket.on(Socket.EVENT_CONNECT_TIMEOUT) {
                val ex = it[0] as Exception
                ex.printStackTrace()
                callback.onChannelError(Socket.EVENT_CONNECT_TIMEOUT)
            }

            ioSocket.on(Socket.EVENT_CONNECTING) {
            }

            ioSocket.on(Socket.EVENT_ERROR) {
                callback.onChannelError(Socket.EVENT_ERROR)
            }

            ioSocket.on("message") {
                Log.i(TAG, LogMsg.msg_received)

                if (it != null && it.isNotEmpty()) {
                    try {
                        val msgObj = JSONObject(it[0].toString())
                        Log.d(TAG, String.format(LogMsg.msg_received_resp, msgObj.toString()))
                        when (val type: String = msgObj.getString("type")) {
                            "answer", "offer" -> {
                                val typeValue =
                                    if (type == "answer") LogMsg.sdp_type_answer else LogMsg.sdp_type_offer
                                val sdp = SessionDescription(msgObj.getString("sdp"), typeValue)
                                callback.onRemoteDescription(sdp)
                            }
                            "candidate" -> {
                                callback.onRemoteIceCandidate(toJavaCandidate(msgObj))
                            }
                            "bye" -> {
                                Log.i(TAG, LogMsg.msg_bye)
                                callback.onChannelClose()
                            }
                        }

                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }

            ioSocket.on("bye") {
                Log.i(TAG, LogMsg.msg_bye)
                if (it != null && it.isNotEmpty()) {
                    val roomId = it[0].toString()
                    if (roomId.equals(connectionParameters?.roomId)) {
                        Log.d(TAG, String.format(LogMsg.msg_bye_resp, roomId))
                        callback.onChannelClose()
                    }
                }
            }

        } catch (e: URISyntaxException) {
            e.printStackTrace()
        }

        roomState = RoomState.CONNECTING
        ioSocket.connect()
    }

    private fun resetSession() {
        initiator = false
        roomState = RoomState.NOT_CONNECTED
        if (ioSocket.connected()) {
            ioSocket.close()
        }
    }

    private fun createHardcodedIceServers(): MutableList<IceServer> {

        var iceServers: MutableList<IceServer> = ArrayList()

        // We are adding AppRTC's Stun & Turn server too.
        try {
            if (useSLNATServer) {
                Log.d(TAG, "Using SL Turn Server")
                iceServers.add(getSLTurnServer())
            } else {
                Log.d(TAG, "Requesting server for TURN Servers")
                iceServers = requestTurnServers(getIceServerURL(), iceServers)
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Exception: " + ex.localizedMessage)
            Log.e(TAG, "Exception: " + ex.stackTrace)
            // We will use SL turn server in case of error
            iceServers.add(getSLTurnServer())
        }

        return iceServers
    }

    private fun getIceServerURL(): String {
        var serverUrl =
            "https://networktraversal.googleapis.com/v1alpha/iceconfig?key=AIzaSyArJnQRd2kEen3RoVrsQOLxP1TnJJ-y8d8"

        Log.d(TAG, "getIceServerURL from $APPRTC_TURN_URL")
        val connection = URL(APPRTC_TURN_URL).openConnection() as HttpURLConnection
        connection.connectTimeout = TURN_HTTP_TIMEOUT_MS
        connection.readTimeout = TURN_HTTP_TIMEOUT_MS
        val responseCode = connection.responseCode
        if (responseCode == 200) {
            val responseStream = connection.inputStream
            val response = drainStream(responseStream)
            connection.disconnect()
            Log.d(TAG, "TURN response: $response")
            val responseJSON = JSONObject(response)
            val iceServerUrl = responseJSON.optString("ice_server_url")

            serverUrl = iceServerUrl
        }
        return serverUrl
    }

    // Requests & returns a TURN ICE Server based on a request URL.  Must be run
    // off the main thread!
    @Throws(IOException::class, JSONException::class)
    private fun requestTurnServers(
        url: String,
        turnServers: MutableList<IceServer>
    ): MutableList<IceServer> {
        Log.d(TAG, "Request TURN from: $url")
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.doOutput = true
        connection.setRequestProperty("REFERER", "https://appr.tc")
        connection.connectTimeout = TURN_HTTP_TIMEOUT_MS
        connection.readTimeout = TURN_HTTP_TIMEOUT_MS
        val responseCode = connection.responseCode //
        if (responseCode == 200) {
            val responseStream = connection.inputStream
            val response = drainStream(responseStream)
            connection.disconnect()
            Log.d(TAG, "TURN response: $response")
            val responseJSON = JSONObject(response)
            val iceServers = responseJSON.getJSONArray("iceServers")
            for (i in 0 until iceServers.length()) {
                val server = iceServers.getJSONObject(i)
                val turnsUrlArray = server.getJSONArray("urls")
                val username = if (server.has("username")) server.getString("username") else ""
                val credential =
                    if (server.has("credential")) server.getString("credential") else ""
                val turnsUrl =
                    Gson().fromJson(turnsUrlArray.toString(), Array<String>::class.java).toList()

                val iceServer = IceServer()
                iceServer.urls = turnsUrl
                iceServer.username = username
                iceServer.password = credential
                turnServers.add(iceServer)
            }
        } else if (turnServers.size == 0) {
            Log.w(TAG, "Error fetching TURN Servers from WebRTC, using SL Turn server")
            turnServers.add(getSLTurnServer())
        }
        return turnServers
    }

    // Return the contents of an InputStream as a String.
    private fun drainStream(`in`: InputStream): String {
        val s = Scanner(`in`, "UTF-8").useDelimiter("\\A")
        return if (s.hasNext()) s.next() else ""
    }

}