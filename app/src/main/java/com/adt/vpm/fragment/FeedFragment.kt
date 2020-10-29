/*
 * Created by ADT author on 9/14/20 11:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/14/20 10:51 PM
 */

package com.adt.vpm.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.adt.vpm.R
import com.adt.vpm.adpater.FeedAdapter
import com.adt.vpm.interfaces.ActivityListener
import com.adt.vpm.interfaces.FeedListener
import com.adt.vpm.model.Feed
import com.adt.vpm.webrtc.service.Constants
import com.adt.vpm.webrtc.util.SessionManager

class FeedFragment : Fragment(), FeedListener, View.OnClickListener {
    private var activityListener: ActivityListener? = null
    private var feedListItem: List<Feed>? = null
    private var adapter: FeedAdapter? = null
    private var feedRemoveListItem: List<Feed>? = ArrayList()
    private var rvFeedList: RecyclerView? = null
    private var fabPlusIcon: FloatingActionButton? = null
    private var tvNoRecord: AppCompatTextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val controlView = inflater.inflate(R.layout.fragment_feed, container, false)

        rvFeedList = controlView.findViewById(R.id.rvFeedList)
        fabPlusIcon = controlView.findViewById(R.id.fabPlusIcon)
        tvNoRecord = controlView.findViewById(R.id.tvNoRecord)
        registerToRefreshItem()
        fabPlusIcon?.setOnClickListener(this)

        return controlView
    }

    /**
     * This method will be invoked when resume the visibility of this page.
     * Also triggered by broadcast receiver to refresh list while call disconnected by other end
     * when we are in feed list with live indicator.
     */
    private fun refreshFeedList() {
        feedListItem = SessionManager.instance?.getRoomList()

        val isEmptyFeed = feedListItem == null || feedListItem?.isNullOrEmpty()!!
        tvNoRecord?.visibility = if (isEmptyFeed) View.VISIBLE else View.GONE

        if (!isEmptyFeed) {
            rvFeedList?.layoutManager = LinearLayoutManager(activity)
            adapter = FeedAdapter(feedListItem as MutableList<Feed>, this, activity?.baseContext!!)
            rvFeedList?.adapter = adapter
        }
    }

    /**
     * This method will be invoked whenever user try to delete the feed items.
     * Which is calling from adapter by FeedListener while do click and long press.
     * It is passing event to container activity to update visibility of delete, select all and clear icons
     */
    override fun onShowDeleteIcon() {
        feedRemoveListItem = adapter?.getListItem() as List<Feed>?
        activityListener?.showDeleteIcon(feedRemoveListItem?.size!! > 0)
    }

    /**
     * This method will be invoked when click on delete icon which is in home page at top header.
     * It is using to remove feed item which is select to delete.
     */
    fun deleteRoom() {
        feedRemoveListItem = adapter?.getListItem() as List<Feed>?
        feedListItem = SessionManager.instance?.removeRoomToList(feedRemoveListItem)
        adapter?.updateList(feedListItem)
        onShowDeleteIcon()
        tvNoRecord?.visibility = if (adapter?.itemCount == 0) View.VISIBLE else View.GONE
    }

    /**
     * This method will be invoked when click on select all label which is in home page at top header.
     * It is using to select all feed items to delete.
     */
    fun selectAllRoom() {
        feedListItem = SessionManager.instance?.getRoomList()
        feedListItem?.forEach { it.isCheck = true }
        adapter?.updateList(feedListItem)
        onShowDeleteIcon()
    }

    /**
     * This method will be invoked when click on clear label which is in home page at top header.
     * It is using to deselect all feed items which one selected for delete.
     */
    fun clearAll() {
        feedListItem = SessionManager.instance?.getRoomList()
        feedListItem?.forEach { it.isCheck = false }
        adapter?.updateList(feedListItem)
        onShowDeleteIcon()
    }

    /**
     * This method will be invoked from onCreateView method to register broadcast receiver to get update when room status updated.
     */
    private fun registerToRefreshItem() {
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            mStatusReceiver,
            IntentFilter(Constants.ITEM_REFRESH)
        )
    }

    /**
     * This is triggered when get room status update to refresh feed list.
     */
    private val mStatusReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refreshFeedList()
        }
    }

    /**
     * This method will be invoked when click on feed fab plus icon.
     * It is passing event to container activity to show dialog to join with room
     *
     * @param view which represents view of the clicked item
     */
    override fun onClick(view: View?) {
        if (view?.id == R.id.fabPlusIcon) activityListener?.showCreateRoomDialog(Constants.FEED_JOIN)
    }

    /**
     * This method will be invoked when click on feed list item to connect video call with same room name.
     * It is passing event to container activity to connect or join with same room
     *
     * @param feed which contains data about room, audio, video stream controls detail.
     */
    override fun onVideoSelected(feed: Feed) {
        val isTaped = SessionManager.instance?.isTaped() ?: false
        if (!isTaped) activityListener?.onVideoSelected(feed)
    }

    override fun onResume() {
        super.onResume()
        refreshFeedList()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activityListener = context as ActivityListener
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mStatusReceiver)
        super.onDestroy()
    }

}
