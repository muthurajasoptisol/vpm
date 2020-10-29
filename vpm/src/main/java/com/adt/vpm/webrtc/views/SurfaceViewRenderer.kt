/*
 * Created by ADT author on 9/14/20 11:00 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/14/20 10:49 PM
 */
package com.adt.vpm.webrtc.views

import android.content.Context
import android.util.AttributeSet

/**
 * This class is derived from WebRtc library and used to render video stream.
 *
 * It holds EGL state and utility methods for handling an egl 1.0 EGLContext, an EGLDisplay,
 * and an EGLSurface.
 *
 * @param context Context
 * @param attrs AttributeSet
 */
open class SurfaceViewRenderer @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : org.webrtc.SurfaceViewRenderer(context, attrs)