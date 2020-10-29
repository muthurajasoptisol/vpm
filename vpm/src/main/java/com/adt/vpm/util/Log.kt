/*
 * Created by ADT author on 9/14/20 11:00 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/14/20 10:49 PM
 */

package com.adt.vpm.util

import android.util.Log
import org.webrtc.Loggable
import org.webrtc.Logging.Severity

/**
 * This class is used to print logs which is printed to debug vpm library functionality
 */
object Log {

    private const val PREFIX = "VPMLog:"

    private var loggingEnabled = false

    /**
     * Enable or disable additional logging - this might be helpful while resolving problems and should be enabled only in debug builds.
     */
    fun enable(logsEnabled: Boolean) {
        loggingEnabled = logsEnabled
    }

    /**
     * This method is used to get vpm log enabled state
     *
     * @return loggingEnabled which is true if enabled otherwise false
     */
    fun isLogEnabled(): Boolean {
        return loggingEnabled
    }

    fun v(tag: String, message: String) {
        if (loggingEnabled) Log.v("$PREFIX $tag", message)
    }

    fun d(tag: String, message: String) {
        if (loggingEnabled) Log.d("$PREFIX $tag", message)
    }

    fun i(tag: String, message: String) {
        if (loggingEnabled) Log.i("$PREFIX $tag", message)
    }

    fun w(tag: String, message: String) {
        if (loggingEnabled) Log.w("$PREFIX $tag", message)
    }

    fun e(tag: String, message: String) {
        if (loggingEnabled) Log.e("$PREFIX $tag", message)
    }

}

/**
 * Logger for injecting into WebRTC module that will respect
 * VPM's log enable/disable control
 */
class VPMLogger : Loggable {
    override fun onLogMessage(message: String, severity: Severity, tag: String) {
        when (severity) {
            Severity.LS_VERBOSE -> com.adt.vpm.util.Log.v(tag, message)
            Severity.LS_INFO -> com.adt.vpm.util.Log.i(tag, message)
            Severity.LS_WARNING -> com.adt.vpm.util.Log.w(tag, message)
            Severity.LS_ERROR -> com.adt.vpm.util.Log.e(tag, message)
            else -> {
            } // drop the message
        }
    }

}