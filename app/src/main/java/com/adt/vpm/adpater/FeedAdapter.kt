/*
 * Created by ADT author on 9/14/20 11:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/14/20 10:51 PM
 */

package com.adt.vpm.adpater

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color.TRANSPARENT
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.adt.vpm.R
import com.adt.vpm.adpater.FeedAdapter.MyViewHolder
import com.adt.vpm.interfaces.FeedListener
import com.adt.vpm.model.Feed


class FeedAdapter(
    private var feedList: List<Feed>,
    private val listener: FeedListener,
    var parent: Context
) :
    RecyclerView.Adapter<MyViewHolder>() {

    inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRoomName: AppCompatTextView =
            view.findViewById<View>(R.id.tvRoomName) as AppCompatTextView
        val tvStatus: AppCompatTextView =
            view.findViewById<View>(R.id.tvStatus) as AppCompatTextView
      //  val ivPlay: ImageButton = view.findViewById<View>(R.id.ivPlay) as ImageButton
        val feedType: AppCompatImageView =
            view.findViewById<View>(R.id.ic_feed_type) as AppCompatImageView
        val rltFeedItem: RelativeLayout =
            view.findViewById<View>(R.id.rltFeedItem) as RelativeLayout
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_feed_item, parent, false)
        return MyViewHolder(itemView)
    }

    @SuppressLint("ResourceType")
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val feed = feedList[position]

        holder.tvRoomName.text = feed.roomName
        holder.tvStatus.visibility = if (feed.isLive) View.VISIBLE else View.GONE
        holder.rltFeedItem.setBackgroundResource(if (feed.isCheck) R.drawable.bg_cardview else TRANSPARENT)

        holder.feedType.setBackgroundResource(if (feed.initializer) R.drawable.ic_video_enable else R.drawable.ic_feed_list)
        holder.feedType.setColorFilter(
            ContextCompat.getColor(parent, R.color.white),
            android.graphics.PorterDuff.Mode.MULTIPLY
        );
        holder.itemView.setOnClickListener {
            Log.v("FeedItem", "Room name " + feed.roomName)
            Log.v("FeedItem", "Room is live " + feed.isLive)
            Log.v("FeedItem", "Room is initializer " + feed.initializer)
            Log.v("FeedItem", "Room local video " + feed.enableLocalVideo)
            Log.v("FeedItem", "Room local audio " + feed.enableLocalAudio)
            Log.v("FeedItem", "Room remote video " + feed.enableRemoteVideo)
            Log.v("FeedItem", "Room remote audio " + feed.enableRemoteAudio)
            if (getListItem().isNotEmpty()) {
                feedList[position].isCheck = !feed.isCheck
                listener.onShowDeleteIcon()
                holder.rltFeedItem.setBackgroundResource(if (feed.isCheck) R.drawable.bg_cardview else TRANSPARENT)
            } else {
                listener.onVideoSelected(feed)
            }
        }

        holder.itemView.setOnLongClickListener {
            feedList[position].isCheck = !feed.isCheck
            listener.onShowDeleteIcon()
            holder.rltFeedItem.setBackgroundResource(if (feed.isCheck) R.drawable.bg_cardview else TRANSPARENT)
            true
        }
    }

    override fun getItemCount(): Int {
        return feedList.size
    }

    fun getListItem(): List<Feed?> {
        val aItem = feedList.filter { it.isCheck }
        Log.e("Size", "" + aItem.size)
        return aItem
    }

    fun updateList(updateList: List<Feed>?) {
        if (updateList != null) {
            feedList = updateList
            notifyDataSetChanged()
        }
    }

}