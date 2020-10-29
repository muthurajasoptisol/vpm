/*
 * Created by ADT author on 9/14/20 11:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/14/20 10:51 PM
 */
package com.adt.vpm.webrtc.util

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import com.adt.vpm.webrtc.views.SurfaceViewRenderer
import org.webrtc.EglBase
import org.webrtc.RendererCommon.ScalingType
import kotlin.math.sign

/**
 * This class is derived from VPM library and used to render video stream.
 *
 * It holds EGL state and utility methods for handling an egl 1.0 EGLContext, an EGLDisplay,
 * and an EGLSurface.
 */
class SurfaceViewRenderer(context: Context, attrs: AttributeSet) : SurfaceViewRenderer(context, attrs) {
    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var gestureDetector: GestureDetector? = null
    private var zoomAnimator: ValueAnimator? = null
    private var isZoomed = false
    private var scale = 1f

    private enum class Mode { NONE, DRAG, ZOOM }

    private var mode = Mode.NONE
    private var lastScaleFactor = 0f

    // Where the finger first  touches the screen
    private var startX = 0f
    private var startY = 0f

    // How much to translate the canvas
    private var dx = 0f
    private var dy = 0f
    private var prevDx = 0f
    private var prevDy = 0f

    /**
     * This method is triggered to initialize SurfaceViewRender component.
     *
     * @param mEglBase EglBase instance
     * @param scaleType Type of scale to fit or fill
     * @param enableHardwareScale EnableHardwareScale
     */
    fun init(mEglBase: EglBase, scaleType: Int, enableHardwareScale: Boolean) {
        super.init(mEglBase.eglBaseContext, null)

        this.setScalingType(getScaleType(scaleType))
        setEnableHardwareScaler(enableHardwareScale)

        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
        gestureDetector = GestureDetector(context, GestureListener())
    }

    /**
     * This class is used to handle single, long and double tap event to handle zoom in and out
     */
    private inner class GestureListener : SimpleOnGestureListener() {

        /**
         * This callback method will be triggered when single click on view
         */
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            return performClick()
        }

        /**
         * This callback method will be triggered when long press on view
         */
        override fun onLongPress(e: MotionEvent) {
            performLongClick()
        }

        /**
         * This callback method will be triggered when double tap on view
         *
         * Handled zoom in and zoom out
         */
        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (zoomAnimator != null && zoomAnimator!!.isRunning) zoomAnimator?.cancel()

            resetDragPosition()
            val start = if (isZoomed) scale else MIN_ZOOM
            val end = if (isZoomed) MIN_ZOOM else scale.coerceAtLeast(MAX_ZOOM)

            zoomAnimator = ValueAnimator.ofFloat(start, end)
            zoomAnimator?.addUpdateListener { valueAnimator: ValueAnimator ->
                val scaleFactor = valueAnimator.animatedValue as Float
                if (lastScaleFactor == 0f || sign(scaleFactor) == sign(lastScaleFactor)) {
                    scale = scaleFactor
                    lastScaleFactor = scaleFactor
                    applyScale()
                } else {
                    lastScaleFactor = 0f
                }
                isZoomed = scale > MIN_ZOOM
            }

            zoomAnimator?.duration = 300
            zoomAnimator?.start()

            return super.onDoubleTap(e)
        }

        override fun onDoubleTapEvent(e: MotionEvent): Boolean {
            return false
        }
    }

    /**
     * This class is used to zoom in and zoom out by pinch
     */
    private inner class ScaleListener : SimpleOnScaleGestureListener() {

        /**
         * This method will be triggered to scale view when pinch zoom
         */
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = scaleGestureDetector?.scaleFactor
            scaleFactor?.let {
                if (lastScaleFactor == 0f || sign(scaleFactor) == sign(lastScaleFactor)) {
                    scale *= scaleFactor
                    lastScaleFactor = scaleFactor
                    scale = MIN_ZOOM.coerceAtLeast(scale.coerceAtMost(MAX_ZOOM))
                } else {
                    lastScaleFactor = 0f
                }

                isZoomed = scale > MIN_ZOOM
            }

            return true
        }
    }

    /**
     * This method is used to get scale type based on enum scale type
     *
     * @param scaleType
     */
    private fun getScaleType(scaleType: Int): ScalingType {
        var scalingType = ScalingType.SCALE_ASPECT_FIT
        if (scaleType == 0) scalingType = ScalingType.SCALE_ASPECT_FILL
        return scalingType
    }

    /**
     * This callback method will be triggered automatically when user touch on view.
     *
     * Pan feature handled within this method once zoomed
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)

        scaleGestureDetector?.onTouchEvent(event)
        gestureDetector?.onTouchEvent(event)

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> if (scale > MIN_ZOOM) {
                mode = Mode.DRAG
                startX = event.x - prevDx
                startY = event.y - prevDy
            }
            MotionEvent.ACTION_MOVE -> if (mode == Mode.DRAG) {
                dx = event.x - startX
                dy = event.y - startY
            }
            MotionEvent.ACTION_POINTER_DOWN -> mode = Mode.ZOOM
            MotionEvent.ACTION_POINTER_UP -> mode = Mode.NONE
            MotionEvent.ACTION_UP -> {
                mode = Mode.NONE
                prevDx = dx
                prevDy = dy
            }
            else -> {
            }
        }

        if (mode == Mode.DRAG && scale >= MIN_ZOOM || mode == Mode.ZOOM) {
            parent.requestDisallowInterceptTouchEvent(true)
            val maxDx = (this.width - this.width / scale) / 2 * scale
            val maxDy = (this.height - this.height / scale) / 2 * scale
            dx = dx.coerceAtLeast(-maxDx).coerceAtMost(maxDx)
            dy = dy.coerceAtLeast(-maxDy).coerceAtMost(maxDy)
            applyScaleAndTranslation()
        }
        return true
    }

    /**
     * This method is used to apply scale and translation when event triggered
     */
    private fun applyScaleAndTranslation() {
        this.scaleX = scale
        this.scaleY = scale
        this.translationX = dx
        this.translationY = dy
    }

    /**
     * This method is used to apply scale when event triggered
     */
    private fun applyScale() {
        this.scaleX = scale
        this.scaleY = scale
        this.translationX = 0f
        this.translationY = 0f
    }

    /**
     * This method is used to reset drag position
     */
    private fun resetDragPosition() {
        prevDx = 0f
        prevDy = 0f
        dx = 0f
        dy = 0f
    }

    companion object {
        private const val MIN_ZOOM = 1.0f
        private const val MAX_ZOOM = 2.5f
    }
}