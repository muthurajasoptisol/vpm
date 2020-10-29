/*
 * Created by ADT author on 9/14/20 11:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/14/20 10:51 PM
 */
package com.adt.vpm.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import com.adt.vpm.R
import com.adt.vpm.model.Feed
import com.adt.vpm.webrtc.service.Constants
import com.adt.vpm.webrtc.service.Constants.Companion.EXTRA_ROOMID
import com.adt.vpm.webrtc.service.Constants.Companion.EXTRA_ROOM_IS_FROM
import com.adt.vpm.webrtc.service.Constants.Companion.EXTRA_ROOM_NAME
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson


/**
 * Fragment for call control.
 */
class CallFragment : Fragment(), View.OnClickListener {
    private var feed: Feed? = null
    private var tvContactNameCall: AppCompatTextView? = null
    private var btnSwitchCamera: ImageButton? = null
    private var btnCallToggleMic: FloatingActionButton? = null
    private var btnVideoDisable: FloatingActionButton? = null
    private var btnCallDisconnect: FloatingActionButton? = null
    private var ivBackArrow: AppCompatImageView? = null

    private var callEvents: OnCallEvents? = null
    private var roomId: String? = null
    private var isFrom: Int = Constants.FEED_JOIN

    /**
     * Call control interface for container activity.
     */
    interface OnCallEvents {
        fun onBackOrClosed(isLive: Boolean, callEndStatus: Boolean)
        fun onCameraSwitch()
        fun onToggleMic(): Boolean
        fun onToggleVideo(): Boolean
        fun setStreamControls()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val controlView =
            inflater.inflate(R.layout.fragment_call, container, false)

        initView(controlView)
        initListener()
        callEvents?.setStreamControls()

        return controlView
    }

    /**
     * This method will be invoked from onCreate method to initialize the views
     *
     * @param controlView which is instance of parent view
     */
    private fun initView(controlView: View) {
        tvContactNameCall = controlView.findViewById(R.id.tvContactNameCall)
        btnCallDisconnect = controlView.findViewById<FloatingActionButton?>(R.id.btnCallDisconnect)
        btnSwitchCamera = controlView.findViewById(R.id.btnSwitchCamera)
        btnCallToggleMic = controlView.findViewById(R.id.btnCallToggleMic)
        btnVideoDisable = controlView.findViewById(R.id.btnVideoDisable)
        ivBackArrow = controlView.findViewById(R.id.ivBackArrow)
    }

    /**
     * This method will be invoked from onCreate method to set callback listener for views.
     */
    private fun initListener() {
        btnCallDisconnect?.setOnClickListener(this)
        ivBackArrow?.setOnClickListener(this)
        btnSwitchCamera?.setOnClickListener(this)
        btnVideoDisable?.setOnClickListener(this)
        btnCallToggleMic?.setOnClickListener(this)
    }

    /**
     * This method is invoked when click on bottom video icon to enable or disable the local video stream.
     * Also which method is calling from WebRtc service class through container activity when start the call.
     *
     * @param enabled True if enable local video stream otherwise false
     */
    fun setLocalVideoButton(enabled: Boolean?) {
        val isEnabled = enabled != null && enabled
        btnVideoDisable?.setImageResource(if (isEnabled) R.drawable.ic_video_enable else R.drawable.ic_video_disable)
        btnVideoDisable?.alpha = if (isEnabled) 1.0f else 0.6f
    }

    /**
     * This method is invoked when click on bottom microphone icon to enable or disable the local audio stream.
     * Also which method is calling from WebRtc service class through container activity when start the call.
     *
     * @param enabled True if enable local audio stream otherwise false
     */
    fun setLocalAudioButton(enabled: Boolean?) {
        val isEnabled = enabled != null && enabled
        btnCallToggleMic?.setImageResource(if (isEnabled) R.drawable.ic_unmute else R.drawable.ic_mute)
        btnCallToggleMic?.alpha = if (isEnabled) 1.0f else 0.6f
    }

    /**
     * This method will be triggered when start up the page
     */
    override fun onStart() {
        super.onStart()
        val args = arguments
        if (args != null) {
            roomId = args.getString(EXTRA_ROOMID)
            isFrom = args.getInt(EXTRA_ROOM_IS_FROM)
            val feedJson = args.getString(Constants.EXTRA_FEED_ROOM)
            feed = Gson().fromJson(feedJson, Feed::class.java)

            tvContactNameCall?.text = args.getString(EXTRA_ROOM_NAME) ?: ""
        }

        btnSwitchCamera?.visibility = if (feed?.enableLocalVideo!!) View.VISIBLE else View.INVISIBLE

        btnVideoDisable?.isEnabled = feed?.enableLocalVideo!!
        btnCallToggleMic?.isEnabled = feed?.enableLocalAudio!!
        btnVideoDisable?.alpha = if (feed?.enableLocalVideo!!) 1.0f else 0.5f
        btnCallToggleMic?.alpha = if (feed?.enableLocalAudio!!) 1.0f else 0.5f

    }

    /**
     * This method will be invoked when click on view of close icon, video icon, microphone icon, camera switch icon and back arrow.
     *
     * @param view which represents view of the clicked item
     */
    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.btnCallDisconnect -> {
                callEvents?.onBackOrClosed(false, true)
            }
            R.id.btnSwitchCamera -> {
                callEvents?.onCameraSwitch()
            }
            R.id.btnCallToggleMic -> {
                val enabled = callEvents?.onToggleMic()
                setLocalAudioButton(enabled)
            }
            R.id.btnVideoDisable -> {
                val enabled = callEvents?.onToggleVideo()
                setLocalVideoButton(enabled)
                btnSwitchCamera?.visibility = if (enabled!!) View.VISIBLE else View.INVISIBLE
            }
            R.id.ivBackArrow -> {
                val isJoiner =
                    isFrom == Constants.FEED_JOIN || isFrom == Constants.FEED_REJOIN
                callEvents?.onBackOrClosed(isJoiner, false)
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callEvents = context as OnCallEvents
    }
}