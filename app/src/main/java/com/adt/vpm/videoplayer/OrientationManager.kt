/*
 * Created by ADT author on 9/24/20 1:08 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 10/4/17 12:10 PM
 */
package com.adt.vpm.videoplayer

import android.content.Context
import android.view.OrientationEventListener

class OrientationManager(context: Context?, rate: Int, listener: OrientationListener?) :
    OrientationEventListener(context, rate) {
    enum class ScreenOrientation {
        REVERSED_LANDSCAPE, LANDSCAPE, PORTRAIT, REVERSED_PORTRAIT
    }

    private var screenOrientation: ScreenOrientation? = null
    private var listener: OrientationListener? = null

    init {
        setListener(listener)
    }

    override fun onOrientationChanged(orientation: Int) {
        if (orientation == -1) {
            return
        }
        val newOrientation: ScreenOrientation = when (orientation) {
            in 60..140 -> {
                ScreenOrientation.REVERSED_LANDSCAPE
            }
            in 140..220 -> {
                ScreenOrientation.REVERSED_PORTRAIT
            }
            in 220..300 -> {
                ScreenOrientation.LANDSCAPE
            }
            else -> {
                ScreenOrientation.PORTRAIT
            }
        }
        if (newOrientation != screenOrientation) {
            screenOrientation = newOrientation
            if (listener != null) {
                listener?.onOrientationChange(screenOrientation)
            }
        }
    }

    private fun setListener(listener: OrientationListener?) {
        this.listener = listener
    }

    interface OrientationListener {
        fun onOrientationChange(screenOrientation: ScreenOrientation?)
    }
}