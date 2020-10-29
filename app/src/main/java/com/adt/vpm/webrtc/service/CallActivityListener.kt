/*
 * Created by ADT author on 9/14/20 11:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/14/20 10:51 PM
 */

package com.adt.vpm.webrtc.service

/**
 * Call control interface for container activity.
 */
interface CallActivityListener {
    fun onBackOrClosed(isLive: Boolean)
    fun onCameraSwitch()
    fun onToggleMic(): Boolean
    fun onToggleVideo(): Boolean
}