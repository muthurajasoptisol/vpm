/*
 * Created by ADT author on 9/14/20 11:00 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/14/20 10:49 PM
 */

package com.adt.vpm.webrtc.views

import org.webrtc.VideoFrame
import org.webrtc.VideoSink

/**
 * This class derived from WebRtc library and used to set target frame to render video stream
 */
open class VideoSink : VideoSink {

    private var target: VideoSink? = null

    /**
     * This method is override from parent class to set frame for target
     */
    @Synchronized
    override fun onFrame(frame: VideoFrame) {
        if (target == null) {
            // Logging.d(TAG, "Dropping frame in proxy because target is null.");
            return
        }

        target?.onFrame(frame)
    }

    /**
     * This method is used to set target source for video stream
     */
    @Synchronized
    fun setTarget(target: SurfaceViewRenderer?) {
        this.target = target
    }
}