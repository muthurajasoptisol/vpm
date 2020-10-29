/*
 * Created by ADT author on 9/14/20 11:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/14/20 10:51 PM
 */

package com.adt.vpm.webrtc.service

import org.webrtc.EglBase

/**
 * This interface is triggered from WebRtcService class and implemented in Call Activity page.
 */
interface WebRtcServiceListener {

    fun setCallEventListener(mCallActivityListener: CallActivityListener)

    fun disConnectedFromPeer()

    fun onStreamConnected(isConnected: Boolean)

    fun showToast(aMsg: String?)

    fun disconnectWithErrorMessage(errorMessage: String)

    fun onSetLocalVideoButton(enable: Boolean?)

    fun onSetLocalAudioButton(enable: Boolean?)

    fun onSetEglBase(mEglBase: EglBase)
}