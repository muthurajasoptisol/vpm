/*
 * Created by ADT author on 9/14/20 11:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/14/20 10:51 PM
 */

package com.adt.vpm.webrtc.manager;

import android.util.Log;

import com.adt.vpm.util.LogMsg;
import com.adt.vpm.webrtc.SignalingParameters;
import com.adt.vpm.webrtc.data.IceCandidate;
import com.adt.vpm.webrtc.data.IceServer;
import com.adt.vpm.webrtc.data.SessionDescription;
import com.adt.vpm.webrtc.service.NatUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * AsyncTask that converts an AppRTC room URL into the set of signaling
 * parameters to use with that room.
 */
public class RoomParametersFetcher {
    private static final String TAG = "RoomRTCClient";
    private static final int TURN_HTTP_TIMEOUT_MS = 5000;
    private final RoomParametersFetcherEvents events;
    private final String roomUrl;
    private final String roomMessage;

    /**
     * Room parameters fetcher callbacks.
     */
    public interface RoomParametersFetcherEvents {
        /**
         * Callback fired once the room's signaling parameters
         * SignalingParameters are extracted.
         */
        void onSignalingParametersReady(final SignalingParameters params, String wssUrl, String wssPostUrl);

        /**
         * Callback for room parameters extraction error.
         */
        void onSignalingParametersError(final String description);
    }

    public RoomParametersFetcher(String roomUrl, String roomMessage, final RoomParametersFetcherEvents events) {
        this.roomUrl = roomUrl;
        this.roomMessage = roomMessage;
        this.events = events;
    }

    public void makeRequest() {
        Log.d(TAG, "Connecting to room: " + roomUrl);
        AsyncHttpURLConnection httpConnection =
                new AsyncHttpURLConnection("POST", roomUrl, roomMessage, new AsyncHttpURLConnection.AsyncHttpEvents() {
                    @Override
                    public void onHttpError(String errorMessage) {
                        Log.e(TAG, "Room connection error: " + errorMessage);
                        events.onSignalingParametersError(errorMessage);
                    }

                    @Override
                    public void onHttpComplete(String response) {
                        roomHttpResponseParse(response);
                    }
                });
        httpConnection.send();
    }

    private void roomHttpResponseParse(String response) {
        Log.d(TAG, "Room response: " + response);
        try {
            List<IceCandidate> iceCandidates = null;
            SessionDescription offerSdp = null;
            JSONObject roomJson = new JSONObject(response);

            String result = roomJson.getString("result");
            if (!result.equals("SUCCESS")) {
                events.onSignalingParametersError("Room response error: " + result);
                return;
            }
            response = roomJson.getString("params");
            roomJson = new JSONObject(response);
            String roomId = roomJson.getString("room_id");
            String clientId = roomJson.getString("client_id");
            String wssUrl = roomJson.getString("wss_url");
            String wssPostUrl = roomJson.getString("wss_post_url");
            boolean initiator = (roomJson.getBoolean("is_initiator"));
            if (!initiator) {
                iceCandidates = new ArrayList<>();
                String messagesString = roomJson.getString("messages");
                JSONArray messages = new JSONArray(messagesString);
                for (int i = 0; i < messages.length(); ++i) {
                    String messageString = messages.getString(i);
                    JSONObject message = new JSONObject(messageString);
                    String messageType = message.getString("type");
                    Log.d(TAG, "GAE->C #" + i + " : " + messageString);
                    if (messageType.equals("offer")) {
                        offerSdp = new SessionDescription(message.getString("sdp"), LogMsg.sdp_type_offer);
                    } else if (messageType.equals("candidate")) {
                        IceCandidate candidateData = new IceCandidate();
                        candidateData.sdp = message.getString("candidate");
                        candidateData.sdpMid = message.getString("id");
                        candidateData.sdpMLineIndex = message.getInt("label");
                        iceCandidates.add(candidateData);
                    } else {
                        Log.e(TAG, "Unknown message: " + messageString);
                    }
                }
            }
            Log.d(TAG, "RoomId: " + roomId + ". ClientId: " + clientId);
            Log.d(TAG, "Initiator: " + initiator);
            Log.d(TAG, "WSS url: " + wssUrl);
            Log.d(TAG, "WSS POST url: " + wssPostUrl);
            List<IceServer> iceServers =
                    iceServersFromPCConfigJSON(roomJson.getString("pc_config"));
            boolean isTurnPresent = false;
            for (IceServer server : iceServers) {
                Log.d(TAG, "IceServer: " + server);
                if (server.urls.get(0).startsWith("turn:")) {
                    isTurnPresent = true;
                    break;
                }
            }
            // Request TURN servers.
            if (!isTurnPresent && !roomJson.optString("ice_server_url").isEmpty()) {
                requestTurnServers(roomJson.getString("ice_server_url"), iceServers);
            }


            SignalingParameters params = new SignalingParameters(
                    iceServers, initiator, clientId, offerSdp, iceCandidates);
            events.onSignalingParametersReady(params, wssUrl, wssPostUrl);
        } catch (JSONException e) {
            events.onSignalingParametersError("Room JSON parsing error: " + e.toString());
        } catch (IOException e) {
            events.onSignalingParametersError("Room IO error: " + e.toString());
        }
    }

    // Requests & returns a TURN ICE Server based on a request URL.  Must be run
    // off the main thread!
    private List<IceServer> requestTurnServers(String url, List<IceServer> turnServers)
            throws IOException, JSONException {
        Log.d(TAG, "Request TURN from: " + url);
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setDoOutput(true);
        connection.setRequestProperty("REFERER", "https://appr.tc");
        connection.setConnectTimeout(TURN_HTTP_TIMEOUT_MS);
        connection.setReadTimeout(TURN_HTTP_TIMEOUT_MS);
        int responseCode = connection.getResponseCode();//
        if (responseCode == 200) {
            InputStream responseStream = connection.getInputStream();
            String response = drainStream(responseStream);
            connection.disconnect();
            Log.d(TAG, "TURN response: " + response);
            JSONObject responseJSON = new JSONObject(response);
            JSONArray iceServers = responseJSON.getJSONArray("iceServers");
            for (int i = 0; i < iceServers.length(); ++i) {
                JSONObject server = iceServers.getJSONObject(i);
                JSONArray turnUrls = server.getJSONArray("urls");
                String username = server.has("username") ? server.getString("username") : "";
                String credential = server.has("credential") ? server.getString("credential") : "";
                ArrayList<String> turnUrlList = new ArrayList<>();
                for (int j = 0; j < turnUrls.length(); j++) {
                    turnUrlList.add(turnUrls.getString(j));
                }

                IceServer iceServer = new IceServer();
                iceServer.urls = turnUrlList;
                iceServer.username = username;
                iceServer.password = credential;
                turnServers.add(iceServer);
            }
        } else if (turnServers.size() == 0) {
            turnServers.add(NatUtils.getSLTurnServer());
        }
        return turnServers;
    }

    // Return the list of ICE servers described by a WebRTCPeerConnection
    // configuration string.
    private List<IceServer> iceServersFromPCConfigJSON(String pcConfig)
            throws JSONException {
        JSONObject json = new JSONObject(pcConfig);
        JSONArray servers = json.getJSONArray("iceServers");
        List<IceServer> ret = new ArrayList<>();
        for (int i = 0; i < servers.length(); ++i) {
            JSONObject server = servers.getJSONObject(i);
            String url = server.getString("urls");
            String username = server.has("username") ? server.getString("username") : "";
            String credential = server.has("credential") ? server.getString("credential") : "";

            IceServer turnServer = new IceServer();
            ArrayList<String> urls = new ArrayList<>();
            urls.add(url);

            turnServer.urls = urls;
            turnServer.username = username;
            turnServer.password = credential;
            ret.add(turnServer);
        }
        return ret;
    }

    // Return the contents of an InputStream as a String.
    private static String drainStream(InputStream in) {
        Scanner s = new Scanner(in, "UTF-8").useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
