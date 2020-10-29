/*
 * Created by ADT author on 9/14/20 11:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/14/20 10:51 PM
 */
package com.adt.vpm.webrtc.signal

import android.os.Handler
import android.os.HandlerThread
import com.adt.vpm.util.Log
import com.adt.vpm.util.LogMsg
import com.adt.vpm.webrtc.SignalingParameters
import com.adt.vpm.webrtc.data.IceCandidate
import com.adt.vpm.webrtc.data.IceCandidate.DataObj.toJavaCandidate
import com.adt.vpm.webrtc.data.IceCandidate.DataObj.toJsonCandidate
import com.adt.vpm.webrtc.data.SessionDescription
import com.adt.vpm.webrtc.manager.AsyncHttpURLConnection
import com.adt.vpm.webrtc.manager.RoomParametersFetcher
import com.adt.vpm.webrtc.service.NatUtils
import com.adt.vpm.webrtc.signal.WebSocketChannelClient.WebSocketChannelEvents
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Negotiates signaling for chatting with https://appr.tc "rooms".
 * Uses the client<->server specifics of the apprtc AppEngine webapp.
 *
 *
 * To use: create an instance of this object (registering a message handler) and
 * call connectToRoom().  Once room connection is established
 * onConnectedToRoom() callback with room parameters is invoked.
 * Messages to other party (with local Ice candidates and answer SDP) can
 * be sent after WebSocket connection is established.
 */
class WebSocketRTCClient(private val events: SignalingClient.SignalingEvents) : WebSocketChannelEvents,
    SignalingClient {

    private enum class ConnectionState {
        NEW, CONNECTED, CLOSED, ERROR
    }

    private enum class MessageType {
        MESSAGE, LEAVE
    }

    private val handler: Handler
    private var initiator = false
    private var wsClient: WebSocketChannelClient? = null
    private var roomState: ConnectionState
    private var connectionParameters: SignalingClient.RoomConnectionParameters? = null
    private var messageUrl: String? = null
    private var leaveUrl: String? = null
    private var useSLNATServer = false

    // --------------------------------------------------------------------
    // AppRTCClient interface implementation.
    // Asynchronously connect to an AppRTC room URL using supplied connection
    // parameters, retrieves room parameters and connect to WebSocket server.
    override fun connectToRoom(connectionParameters: SignalingClient.RoomConnectionParameters) {
        this.connectionParameters = connectionParameters
        handler.post { connectToRoomInternal() }
    }

    override fun disconnectFromRoom() {
        handler.post {
            disconnectFromRoomInternal()
            handler.looper.quit()
        }
    }

    override fun useSLNATServer(enable: Boolean?) {
        if (enable != null) {
            useSLNATServer = enable
        }
    }

    // Connects to room - function runs on a local looper thread.
    private fun connectToRoomInternal() {
        val connectionUrl = getConnectionUrl(connectionParameters)
        Log.d(TAG, "Connect to room: $connectionUrl")

        roomState = ConnectionState.NEW
        wsClient = WebSocketChannelClient(handler, this)

        val callbacks: RoomParametersFetcher.RoomParametersFetcherEvents = object :
            RoomParametersFetcher.RoomParametersFetcherEvents {
            override fun onSignalingParametersReady(
                params: SignalingParameters,
                wssUrl: String,
                wssPostUrl: String
            ) {
                handler.post { signalingParametersReady(params, wssUrl, wssPostUrl) }
            }

            override fun onSignalingParametersError(description: String) {
                reportError(description)
            }
        }
        RoomParametersFetcher(
            connectionUrl,
            null,
            callbacks
        ).makeRequest()
    }

    // Disconnect from room and send bye messages - runs on a local looper thread.
    private fun disconnectFromRoomInternal() {
        Log.i(TAG, LogMsg.msg_room_disconnected)
        if (roomState == ConnectionState.CONNECTED) {
            Log.d(TAG, "Closing room.")
            sendPostMessage(MessageType.LEAVE, leaveUrl, null)
        }
        roomState = ConnectionState.CLOSED
        wsClient?.disconnect(true)
    }

    // Helper functions to get connection, post message and leave message URLs
    private fun getConnectionUrl(connectionParameters: SignalingClient.RoomConnectionParameters?): String {
        return (connectionParameters?.roomUrl + "/" + ROOM_JOIN + "/" + connectionParameters?.roomId
                + getQueryString(connectionParameters))
    }

    private fun getMessageUrl(
        connectionParameters: SignalingClient.RoomConnectionParameters?,
        signalingParameters: SignalingParameters
    ): String {
        return (connectionParameters?.roomUrl + "/" + ROOM_MESSAGE + "/" + connectionParameters?.roomId
                + "/" + signalingParameters.clientId + getQueryString(connectionParameters))
    }

    private fun getLeaveUrl(
        connectionParameters: SignalingClient.RoomConnectionParameters?,
        signalingParameters: SignalingParameters
    ): String {
        return (connectionParameters?.roomUrl + "/" + ROOM_LEAVE + "/" + connectionParameters?.roomId + "/"
                + signalingParameters.clientId + getQueryString(connectionParameters))
    }

    private fun getQueryString(connectionParameters: SignalingClient.RoomConnectionParameters?): String {
        return if (connectionParameters?.urlParameters != null) {
            "?" + connectionParameters.urlParameters
        } else {
            ""
        }
    }

    //
    // Callback issued when room parameters are extracted. Runs on local
    // looper thread.
    private fun signalingParametersReady(
        signalingParameters: SignalingParameters,
        wssUrl: String,
        wssPostUrl: String
    ) {
        Log.i(TAG, LogMsg.msg_room_connected)
        initiator = signalingParameters.initiator
        messageUrl = getMessageUrl(connectionParameters, signalingParameters)
        leaveUrl = getLeaveUrl(connectionParameters, signalingParameters)
        Log.d(TAG, "Message URL: $messageUrl")
        Log.d(TAG, "Leave URL: $leaveUrl")
        roomState = ConnectionState.CONNECTED

        if(useSLNATServer) {
            Log.d(TAG, "using SL TURN Server")
            signalingParameters.iceServers.clear()
            signalingParameters.iceServers.add(NatUtils.getSLTurnServer())
        }

        // Fire connection and signaling parameters events.
        events.onConnectedToRoom(signalingParameters, connectionParameters?.roomId)

        // Connect and register WebSocket client.
        wssUrl.let {
            wssPostUrl.let { it1 ->
                wsClient?.connect(
                    it,
                    it1
                )
            }
        }
        signalingParameters.clientId?.let { wsClient?.register(connectionParameters!!.roomId, it) }
    }

    // Send local offer SDP to the other participant.
    override fun sendOfferSdp(sdp: SessionDescription) {
        handler.post(Runnable {
            if (roomState != ConnectionState.CONNECTED) {
                reportError("Sending offer SDP in non connected state.")
                return@Runnable
            }
            val json = JSONObject()
            jsonPut(json, "sdp", sdp.description)
            jsonPut(json, "type", "offer")
            sendPostMessage(
                MessageType.MESSAGE,
                messageUrl,
                json.toString()
            )
            Log.d(TAG, String.format(LogMsg.msg_send_offer, json.toString()))
        })
    }

    // Send local answer SDP to the other participant.
    override fun sendAnswerSdp(sdp: SessionDescription) {
        handler.post(Runnable {
            val json = JSONObject()
            jsonPut(json, "sdp", sdp.description)
            jsonPut(json, "type", "answer")
            wsClient?.send(json.toString())
            Log.d(TAG, String.format(LogMsg.msg_send_answer, json.toString()))
        })
    }

    // Send Ice candidate to the other participant.
    override fun sendLocalIceCandidate(candidate: IceCandidate) {
        handler.post(Runnable {
            val json = JSONObject()
            jsonPut(json, "type", "candidate")
            jsonPut(json, "label", candidate.sdpMLineIndex)
            jsonPut(json, "id", candidate.sdpMid)
            jsonPut(json, "candidate", candidate.sdp)
            if (initiator) {
                // Call initiator sends ice candidates to GAE server.
                if (roomState != ConnectionState.CONNECTED) {
                    reportError("Sending ICE candidate in non connected state.")
                    return@Runnable
                }
                sendPostMessage(
                    MessageType.MESSAGE,
                    messageUrl,
                    json.toString()
                )
            } else {
                // Call receiver sends ice candidates to websocket server.
                wsClient?.send(json.toString())
            }
        })
    }

    // Send removed Ice candidates to the other participant.
    override fun sendLocalIceCandidateRemovals(candidates: Array<IceCandidate>) {
        handler.post(Runnable {
            val json = JSONObject()
            jsonPut(json, "type", "remove-candidates")
            val jsonArray = JSONArray()
            for (candidate in candidates) {
                jsonArray.put(toJsonCandidate(candidate))
            }
            jsonPut(json, "candidates", jsonArray)
            if (initiator) {
                // Call initiator sends ice candidates to GAE server.
                if (roomState != ConnectionState.CONNECTED) {
                    reportError("Sending ICE candidate removals in non connected state.")
                    return@Runnable
                }
                sendPostMessage(
                    MessageType.MESSAGE,
                    messageUrl,
                    json.toString()
                )
            } else {
                // Call receiver sends ice candidates to websocket server.
                wsClient?.send(json.toString())
            }
        })
    }

    // --------------------------------------------------------------------
    // WebSocketChannelEvents interface implementation.
    // All events are called by WebSocketChannelClient on a local looper thread
    // (passed to WebSocket client constructor).
    override fun onWebSocketMessage(msg: String?) {
        if (wsClient?.state != WebSocketChannelClient.WebSocketConnectionState.REGISTERED) {
            Log.e(
                TAG,
                "Got WebSocket message in non registered state."
            )
            return
        }
        try {
            var json = JSONObject(msg)
            val msgText = json.getString("msg")
            val errorText = json.optString("error")
            Log.i(TAG, LogMsg.msg_received)
            if (msgText.length > 0) {
                json = JSONObject(msgText)
                Log.d(TAG, String.format(LogMsg.msg_received_resp, msgText))
                val type = json.optString("type")
                if (type == "candidate") {
                    events.onRemoteIceCandidate(toJavaCandidate(json))
                } else if (type == "remove-candidates") {
                    val candidateArray = json.getJSONArray("candidates")
                    val candidates =
                        arrayOfNulls<IceCandidate>(candidateArray.length())
                    for (i in 0 until candidateArray.length()) {
                        candidates[i] = toJavaCandidate(candidateArray.getJSONObject(i))
                    }
                    events.onRemoteIceCandidatesRemoved(candidates)
                } else if (type == "answer") {
                    if (initiator) {
                        val sdp = SessionDescription(json.getString("sdp"), LogMsg.sdp_type_answer)
                        events.onRemoteDescription(sdp)
                    } else {
                        reportError("Received answer for call initiator: $msg")
                    }
                } else if (type == "offer") {
                    if (!initiator) {
                        val sdp = SessionDescription(json.getString("sdp"), LogMsg.sdp_type_offer)
                        events.onRemoteDescription(sdp)
                    } else {
                        reportError("Received offer for call receiver: $msg")
                    }
                } else if (type == "bye") {
                    Log.i(TAG, LogMsg.msg_bye)
                    events.onChannelClose()
                } else {
                    reportError("Unexpected WebSocket message: $msg")
                }
            } else {
                if (errorText.isNotEmpty()) {
                    reportError("WebSocket error message: $errorText")
                } else {
                    reportError("Unexpected WebSocket message: $msg")
                }
            }
        } catch (e: JSONException) {
            reportError("WebSocket message JSON parsing error: $e")
        }
    }

    override fun onWebSocketClose() {
        events.onChannelClose()
        Log.i(TAG, LogMsg.msg_bye)
    }

    override fun onWebSocketError(description: String?) {
        reportError("WebSocket error: $description")
    }

    // --------------------------------------------------------------------
    // Helper functions.
    private fun reportError(errorMessage: String) {
        Log.e(TAG, errorMessage)
        handler.post {
            if (roomState != ConnectionState.ERROR) {
                roomState = ConnectionState.ERROR
                events.onChannelError(errorMessage)
            }
        }
    }

    // Send SDP or ICE candidate to a room server.
    private fun sendPostMessage(
        messageType: MessageType,
        url: String?,
        message: String?
    ) {
        var logInfo = url
        if (message != null) {
            logInfo += ". Message: $message"
        }
        Log.d(TAG, "C->GAE: $logInfo")
        val httpConnection =
            AsyncHttpURLConnection("POST", url, message, object :
                AsyncHttpURLConnection.AsyncHttpEvents {
                override fun onHttpError(errorMessage: String) {
                    reportError("GAE POST error: $errorMessage")
                }

                override fun onHttpComplete(response: String) {
                    if (messageType == MessageType.MESSAGE) {
                        try {
                            val roomJson = JSONObject(response)
                            val result = roomJson.getString("result")
                            if (result != "SUCCESS") {
                                reportError("GAE POST error: $result")
                            }
                        } catch (e: JSONException) {
                            reportError("GAE POST JSON error: $e")
                        }
                    }
                }
            })
        httpConnection.send()
    }

    companion object {
        private const val TAG = "WSRTCClient"
        private const val ROOM_JOIN = "join"
        private const val ROOM_MESSAGE = "message"
        private const val ROOM_LEAVE = "leave"

        // Put a |key|->|value| mapping in |json|.
        private fun jsonPut(json: JSONObject, key: String, value: Any) {
            try {
                json.put(key, value)
            } catch (e: JSONException) {
                throw RuntimeException(e)
            }
        }
    }

    init {
        roomState = ConnectionState.NEW
        val handlerThread = HandlerThread(TAG)
        handlerThread.start()
        handler = Handler(handlerThread.looper)
    }
}