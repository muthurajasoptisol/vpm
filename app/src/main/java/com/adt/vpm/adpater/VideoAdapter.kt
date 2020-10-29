/*
 * Created by ADT author on 9/14/20 11:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/14/20 10:51 PM
 */

package com.adt.vpm.adpater

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.adt.vpm.R
import com.adt.vpm.interfaces.VideoListener
import com.adt.vpm.model.VideoFile
import com.adt.vpm.videoplayer.VideoPlayer.StreamType


class VideoAdapter(
    private var videoList: List<VideoFile>,
    private val listener: VideoListener,
    var parent: Context
) : RecyclerView.Adapter<VideoAdapter.MyViewHolder>() {

    inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRoomName: AppCompatTextView =
            view.findViewById<View>(R.id.tvRoomName) as AppCompatTextView
        val tvStatus: AppCompatTextView =
            view.findViewById<View>(R.id.tvStatus) as AppCompatTextView

        val videoType: AppCompatImageView =
            view.findViewById<View>(R.id.ic_feed_type) as AppCompatImageView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_feed_item, parent, false)
        return MyViewHolder(itemView)
    }

    @SuppressLint("ResourceType")
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val video = videoList[position]

        holder.tvRoomName.text = video.filename
        holder.tvStatus.visibility = View.VISIBLE
        holder.tvStatus.setBackgroundColor(ContextCompat.getColor(parent, R.color.bgBlue))

        when (video.fileType) {
            StreamType.LOCAL -> {
                holder.tvStatus.text = parent.getString(R.string.local)
            }
            StreamType.RTSP -> {
                holder.tvStatus.text = parent.getString(R.string.rtsp)
            }
            StreamType.HLS -> {
                holder.tvStatus.text = parent.getString(R.string.hls)
            }
            StreamType.HTTP -> {
                holder.tvStatus.text = parent.getString(R.string.https)
            }
            else -> holder.tvStatus.text = parent.getString(R.string.others)
        }

        holder.videoType.setBackgroundResource(R.drawable.ic_movie)

        holder.videoType.setColorFilter(
            ContextCompat.getColor(parent, R.color.white),
            android.graphics.PorterDuff.Mode.MULTIPLY
        )

        holder.itemView.setOnClickListener {
            listener.onVideoSelected(video)
        }

    }

    override fun getItemCount(): Int {
        return videoList.size
    }
}