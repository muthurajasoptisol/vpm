/*
 * Created by ADT author on 9/14/20 11:00 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/14/20 10:49 PM
 */

package com.adt.vpm.webrtc.data

import com.adt.vpm.webrtc.data.IceCandidate.DataObj.TlsCertPolicy;

/**
 * This class is used to have IceServer data to app communicate with library
 */
data class IceServer(
    @kotlin.jvm.JvmField
    var urls: List<String> = mutableListOf(),
    @kotlin.jvm.JvmField
    var username: String? = null,
    @kotlin.jvm.JvmField
    var password: String? = null,
    @kotlin.jvm.JvmField
    var tlsCertPolicy: TlsCertPolicy = TlsCertPolicy.NONE,
    @kotlin.jvm.JvmField
    var hostname: String? = null,
    @kotlin.jvm.JvmField
    var tlsAlpnProtocols: List<String> = mutableListOf(),
    @kotlin.jvm.JvmField
    var tlsEllipticCurves: List<String> = mutableListOf()
)