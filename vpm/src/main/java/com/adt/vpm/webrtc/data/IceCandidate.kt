/*
 * Created by ADT author on 9/14/20 11:00 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/14/20 10:49 PM
 */

package com.adt.vpm.webrtc.data

import org.json.JSONException
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.IceServer
import kotlin.jvm.Throws

/**
 * This class is used to have IceCandidate data to app communicate with library
 */
data class IceCandidate(
    @kotlin.jvm.JvmField
    var sdp: String = "",
    @kotlin.jvm.JvmField
    var sdpMid: String = "",
    @kotlin.jvm.JvmField
    var sdpMLineIndex: Int = -1
) {

    object DataObj {

        enum class TlsCertPolicy { NONE, POLICY_SECURE, POLICY_INSECURE_NO_CHECK }

        /**
         * This method is used to get candidate JSON object from candidateData
         *
         * @param iceCandidate instance of CandidateData
         *
         * @return json
         */
        fun toJsonCandidate(iceCandidate: com.adt.vpm.webrtc.data.IceCandidate): JSONObject? {
            val json = JSONObject()
            json.put("type", "candidate")
            json.put("label", iceCandidate.sdpMLineIndex)
            json.put("id", iceCandidate.sdpMid)
            json.put("candidate", iceCandidate.sdp)
            return json
        }

        /**
         * This method is used to get CandidateData from candidateData JSON object
         *
         * @param json instance of CandidateData
         *
         * @return iceCandidate
         */
        @Throws(JSONException::class)
        fun toJavaCandidate(json: JSONObject): com.adt.vpm.webrtc.data.IceCandidate? {
            val iceCandidate = IceCandidate()
            iceCandidate.sdp = json.getString("candidate")
            iceCandidate.sdpMid = json.getString("id")
            iceCandidate.sdpMLineIndex = json.getInt("label")
            return iceCandidate
        }


        /**
         * This method is used to get CandidateData from IceCandidate
         *
         * @param iceCandidate instance of IceCandidate
         *
         * @return candidate
         */
        fun getCandidateData(iceCandidate: IceCandidate): com.adt.vpm.webrtc.data.IceCandidate {
            val candidate = IceCandidate()
            candidate.sdp = iceCandidate.sdp
            candidate.sdpMid = iceCandidate.sdpMid
            candidate.sdpMLineIndex = iceCandidate.sdpMLineIndex
            return candidate
        }

        /**
         * This method is used to get peer connection IceServer list
         *
         * @param iceServerDataList list of IceServerData
         *
         * @return iceServerList
         */
        fun getIceServerList(iceServerDataList: MutableList<com.adt.vpm.webrtc.data.IceServer>): List<IceServer> {
            val iceServerList: MutableList<IceServer> = mutableListOf()
            if (iceServerDataList.isNotEmpty()) {
                for (iceServerData in iceServerDataList) {

                    val iceServerBuilder = IceServer.builder(iceServerData.urls)

                    if (iceServerData.username != null) {
                        iceServerBuilder.setUsername(iceServerData.username)
                    }
                    if (iceServerData.password != null) {
                        iceServerBuilder.setPassword(iceServerData.password)
                    }
                    if (iceServerData.hostname != null) {
                        iceServerBuilder.setHostname(iceServerData.hostname)
                    }
                    if (iceServerData.tlsAlpnProtocols.isNotEmpty()) {
                        iceServerBuilder.setTlsAlpnProtocols(iceServerData.tlsAlpnProtocols)
                    }
                    if (iceServerData.tlsEllipticCurves.isNotEmpty()) {
                        iceServerBuilder.setTlsEllipticCurves(iceServerData.tlsEllipticCurves)
                    }
                    if (iceServerData.tlsCertPolicy != TlsCertPolicy.NONE) {
                        iceServerBuilder.setTlsCertPolicy(getTlsPolicyType(iceServerData.tlsCertPolicy))
                    }
                    iceServerList.add(iceServerBuilder.createIceServer())
                }
            }
            return iceServerList
        }

        /**
         * This method is used to get peer connection TlsCertPolicy based on type
         *
         * @param tlsCertPolicy tlsCertPolicy
         *
         * @return TlsCertPolicy
         */
        private fun getTlsPolicyType(tlsCertPolicy: TlsCertPolicy): PeerConnection.TlsCertPolicy {
            return when (tlsCertPolicy) {
                TlsCertPolicy.POLICY_SECURE -> {
                    PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_SECURE
                }
                TlsCertPolicy.POLICY_INSECURE_NO_CHECK -> {
                    PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK
                }
                else -> {
                    PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_SECURE
                }
            }
        }


        /**
         * This method is used to get IceCandidate from CandidateData
         *
         * @param iceCandidate instance of CandidateData
         *
         * @return IceCandidate
         */
        fun getIceCandidate(iceCandidate: com.adt.vpm.webrtc.data.IceCandidate): IceCandidate {
            return IceCandidate(
                iceCandidate.sdpMid,
                iceCandidate.sdpMLineIndex,
                iceCandidate.sdp
            )
        }

        /**
         * This method is used to get IceCandidate list from list of CandidateData
         *
         * @param iceCandidateList list of iceCandidate data
         *
         * @return candidateList IceCandidate list
         */
        fun getIceCandidateList(iceCandidateList: Array<com.adt.vpm.webrtc.data.IceCandidate>): Array<IceCandidate> {
            val candidateList: MutableList<IceCandidate> = mutableListOf()
            if (iceCandidateList.isNotEmpty()) {
                for (iceCandidate in iceCandidateList) {
                    candidateList.add(getIceCandidate(iceCandidate))
                }
            }
            return candidateList.toTypedArray()
        }

        /**
         * This method is used to get CandidateData list from list of IceCandidate
         *
         * @param iceCandidates instance of IceCandidate
         *
         * @return candidateDataList
         */
        fun getCandidateDataList(iceCandidates: Array<IceCandidate>): Array<com.adt.vpm.webrtc.data.IceCandidate> {
            val candidateDataList: MutableList<com.adt.vpm.webrtc.data.IceCandidate> =
                mutableListOf()
            if (iceCandidates.isNotEmpty()) {
                for (iceCandidate in iceCandidates) {
                    candidateDataList.add(getCandidateData(iceCandidate))
                }
            }
            return candidateDataList.toTypedArray()
        }


    }
}
