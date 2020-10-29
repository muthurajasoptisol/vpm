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
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.adt.vpm.R


class VideoInfoAdapter(
    private var infoList: MutableMap<String, String>,
    var parent: Context
) : RecyclerView.Adapter<VideoInfoAdapter.MyViewHolder>() {

    inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvKey: AppCompatTextView =
            view.findViewById<View>(R.id.tvKey) as AppCompatTextView
        val tvValue: AppCompatTextView =
            view.findViewById<View>(R.id.tvValue) as AppCompatTextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_player_info, parent, false)
        return MyViewHolder(itemView)
    }

    @SuppressLint("ResourceType")
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.tvKey.text = infoList.keys.elementAt(position)
        holder.tvValue.text = infoList.values.elementAt(position)
    }

    override fun getItemCount(): Int {
        return infoList.size
    }

}