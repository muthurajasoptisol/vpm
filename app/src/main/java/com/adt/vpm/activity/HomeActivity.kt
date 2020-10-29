/*
 * Created by ADT author on 9/14/20 11:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/14/20 10:51 PM
 */

package com.adt.vpm.activity

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager
import com.adt.vpm.R
import com.adt.vpm.adpater.MyPagerAdapter
import com.adt.vpm.fragment.FeedFragment
import com.adt.vpm.fragment.VideosFragment
import com.adt.vpm.interfaces.ActivityListener
import com.adt.vpm.model.Feed
import com.adt.vpm.util.Log
import com.adt.vpm.util.Utils
import com.adt.vpm.webrtc.service.Constants
import com.adt.vpm.webrtc.service.Constants.Companion.CAMERA_CREATE
import com.adt.vpm.webrtc.util.SessionManager
import com.adt.vpm.webrtc.util.UnhandledExceptionHandler
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.activity_home.*
import java.io.File

/**
 * This class is the home page to show feeds and videos.
 */
class HomeActivity : AppCompatActivity(), ActivityListener, View.OnClickListener,
    ViewPager.OnPageChangeListener {

    private var dialog: BottomSheetDialog? = null
    private var enableLocalVideo: Boolean = true
    private var enableLocalAudio: Boolean = true
    private var enableRemoteVideo: Boolean = true
    private var enableRemoteAudio: Boolean = true
    private var feedFragment: FeedFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread.setDefaultUncaughtExceptionHandler(UnhandledExceptionHandler(this))

        setContentView(R.layout.activity_home)
        supportActionBar?.hide()

        setupViewPager(viewPager)
        tabLayout.setupWithViewPager(viewPager)

        requestPermissions()

        SessionManager.instance?.init(this)
        SessionManager.instance?.resetFeedListStatus()

        initListener()

        Log.enable(SessionManager.instance?.isLoggingEnabled(this)!!)

    }

    /**
     * This method will be invoked from onCreate method to set click listener for views.
     */
    private fun initListener() {
        tvClear.setTextColor(ContextCompat.getColor(this, R.color.bgGrey))
        ivSettingIcon.setOnClickListener(this)
        ivVideoIcon.setOnClickListener(this)
        ivDelete.setOnClickListener(this)
        tvClear.setOnClickListener(this)
        tvSelectAll.setOnClickListener(this)
    }

    /**
     * This method will be invoked from onCreate method to add pages for view pager.
     */
    private fun setupViewPager(viewPager: ViewPager) {
        feedFragment = FeedFragment()
        val adapter = MyPagerAdapter(supportFragmentManager)
        adapter.addFrag(VideosFragment(), getString(R.string.videos))
        adapter.addFrag(feedFragment!!, getString(R.string.feeds))
        viewPager.adapter = adapter
        viewPager.currentItem = 0
    }

    /**
     * This method will be invoked when click video icon or feed plus icon to initiate or join in video call.
     *
     * @param isFrom isFrom represents where it is called from.
     */
    @SuppressLint("InflateParams")
    private fun showBottomDialog(isFrom: Int) {
        val view: View = layoutInflater.inflate(R.layout.bottom_sheet_dialog, null)
        dialog = BottomSheetDialog(this, R.style.BottomSheetDialog)

        val btnCancel = view.findViewById(R.id.btnCancel) as AppCompatButton
        val btnStart = view.findViewById(R.id.btnStart) as AppCompatButton
        val edtRoomName = view.findViewById(R.id.edtRoomName) as AppCompatEditText

        val cbLocalVideo = view.findViewById(R.id.cbLocalVideo) as AppCompatCheckBox
        val cbLocalAudio = view.findViewById(R.id.cbLocalAudio) as AppCompatCheckBox
        val cbRemoteVideo = view.findViewById(R.id.cbRemoteVideo) as AppCompatCheckBox
        val cbRemoteAudio = view.findViewById(R.id.cbRemoteAudio) as AppCompatCheckBox

        val isInitiator = isFrom == CAMERA_CREATE

        enableLocalAudio = true
        enableRemoteAudio = true
        enableLocalVideo = isInitiator
        enableRemoteVideo = !isInitiator

        btnStart.text = if (isInitiator) getString(R.string.create) else getString(R.string.start)

        cbLocalVideo.isChecked = isInitiator
        cbLocalAudio.isChecked = true
        cbRemoteAudio.isChecked = true
        cbRemoteVideo.isChecked = !isInitiator

        dialog?.setCanceledOnTouchOutside(false)
        dialog?.setContentView(view)

        btnCancel.setOnClickListener {
            hideCreateRoomDialog()
        }

        btnStart.setOnClickListener {
            val roomName = edtRoomName.text.toString()
            connectToRoom(roomName, isFrom)
        }
        dialog?.setOnCancelListener {
            hideCreateRoomDialog()
        }
        dialog?.show()
    }

    /**
     * This method will be invoked when click on create or start button in room creation dialog.
     *
     * @param roomName which represents name of the room to connect video call.
     * @param isFrom which represents where it is called from.
     */
    private fun connectToRoom(roomName: String, isFrom: Int) {
        if (roomName.isNotEmpty()) {
            hideCreateRoomDialog()
            val feed = Feed()
            feed.roomId = Constants.ROOM_NAME_PREFIX.plus(roomName)
            feed.roomName = roomName
            feed.initializer = isFrom == CAMERA_CREATE
            feed.enableLocalVideo = enableLocalVideo
            feed.enableLocalAudio = enableLocalAudio
            feed.enableRemoteVideo = enableRemoteVideo
            feed.enableRemoteAudio = enableRemoteAudio
            SessionManager.instance?.connectToRoom(this, feed, isFrom)
        } else {
            Toast.makeText(this, getString(R.string.room_name_alert), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * This method will be invoked to check whether any one room has live room and show room creation dialog.
     *
     * @param isFrom which represents where it is called from.
     */
    override fun showCreateRoomDialog(isFrom: Int) {
        if (canStartSession(isFrom)) {
            rltDashBoard.visibility = View.GONE
            rootLayout?.setBackgroundColor(ContextCompat.getColor(this, R.color.bgGrey))
            showBottomDialog(isFrom)
        }
    }

    /**
     * This method will be invoked from room creation dialog which is used to hide that dialog.
     */
    override fun hideCreateRoomDialog() {
        rootLayout?.setBackgroundColor(ContextCompat.getColor(this, R.color.bgColor))
        rltDashBoard.visibility = View.VISIBLE
        if (dialog != null && dialog!!.isShowing) dialog?.dismiss()
    }

    /**
     * This method will be invoked from feed whenever user try to delete feed items.
     *
     * @param isShow which is true to show delete, select all and clear icons otherwise hide.
     */
    override fun showDeleteIcon(isShow: Boolean) {
        ivDelete?.visibility = if (isShow) View.VISIBLE else View.GONE
        tvSelectAll?.visibility = if (isShow) View.VISIBLE else View.GONE
        tvClear?.visibility = if (isShow) View.VISIBLE else View.GONE
        lltSelectLayout?.visibility = if (isShow) View.VISIBLE else View.GONE
        tvClear.setTextColor(
            ContextCompat.getColor(
                this,
                if (isShow) R.color.white else R.color.bgGrey
            )
        )
    }

    /**
     * This method will be invoked when click on feed list item to connect video call with same room name.
     *
     * @param feed which contains data about room, audio, video stream controls detail.
     */
    override fun onVideoSelected(feed: Feed) {
        val isFrom = if (feed.isLive) Constants.FEED_REJOIN else {
            if (feed.initializer) Constants.CAMERA_CREATE else Constants.FEED_JOIN
        }

        val isInitiator = feed.initializer
        feed.enableLocalAudio = true
        feed.enableLocalVideo = isInitiator
        feed.enableRemoteAudio = true
        feed.enableRemoteVideo = !isInitiator

        if (canStartSession(isFrom)) {
            SessionManager.instance?.connectToRoom(this, feed, isFrom)
        }
    }


    /**
     * This method will be invoked when click on checkbox views of video and audio controls for local and remote.
     *
     * @param view which represents view of the clicked item
     */
    fun onCheckboxClicked(view: View) {
        if (view is CheckBox) {
            val checked: Boolean = view.isChecked
            when (view.id) {
                R.id.cbLocalVideo -> {
                    enableLocalVideo = checked
                }
                R.id.cbLocalAudio -> {
                    enableLocalAudio = checked
                }
                R.id.cbRemoteVideo -> {
                    enableRemoteVideo = checked
                }
                R.id.cbRemoteAudio -> {
                    enableRemoteAudio = checked
                }
            }
        }
    }

    override fun onPageScrollStateChanged(state: Int) {}

    override fun onPageScrolled(
        position: Int,
        positionOffset: Float,
        positionOffsetPixels: Int
    ) {
    }

    /**
     * This method will be invoked when a new page becomes selected. Animation is not
     * necessarily complete.
     *
     * @param position Position index of the new selected page.
     */
    override fun onPageSelected(position: Int) {
        ivSettingIcon?.visibility = if (position == 1) View.VISIBLE else View.GONE
        ivVideoIcon?.visibility = if (position == 1) View.VISIBLE else View.GONE
        ivDelete?.visibility = if (position == 1) View.VISIBLE else View.GONE
    }

    /**
     * This method will be invoked when click on view settings icon, video icon, select all and clear
     *
     * @param view which represents view of the clicked item
     */
    override fun onClick(view: View?) {
        val isTaped = SessionManager.instance?.isTaped() ?: false
        if (isTaped) return

        when (view?.id) {
            R.id.ivSettingIcon -> {
                if (canStartSession(CAMERA_CREATE)) {
                    startActivity(Intent(this, SettingsActivity::class.java))
                }
            }
            R.id.ivVideoIcon -> {
                showCreateRoomDialog(CAMERA_CREATE)
            }
            R.id.tvSelectAll -> {
                feedFragment?.selectAllRoom()
            }
            R.id.ivDelete -> {
                showAlert(false, R.string.alert_for_remove_room)
            }
            R.id.tvClear -> {
                feedFragment?.clearAll()
            }
        }
    }

    /**
     * This method will be invoked to check whether any one room is already in live or not.
     *
     * @param isFrom which represents where it is called from.
     *
     * @return which returns true if any one room is live otherwise false.
     */
    private fun canStartSession(isFrom: Int): Boolean {
        val hasLiveRoom = SessionManager.instance?.hasLiveRoom() ?: false
        if (hasLiveRoom && (isFrom == CAMERA_CREATE)) {
            showAlert(
                R.string.already_live_title,
                R.string.already_live_msg
            )
            return false
        }
        return true
    }

    /**
     * This method will be triggered when user click on back press.
     */
    override fun onBackPressed() {
        if (dialog != null && dialog!!.isShowing) hideCreateRoomDialog()
        else super.onBackPressed()
    }

    /**
     * This method will be triggered when user close the app.
     */
    override fun onDestroy() {
        Thread.setDefaultUncaughtExceptionHandler(null)
        if (dialog != null && dialog!!.isShowing) dialog?.dismiss()
        super.onDestroy()
    }

    /**
     * This method will be invoked to get needed user permission to access the app.
     */
    private fun requestPermissions() {
        val isPermissionGranted = Utils.isPermissionsGranted(this)
        if (isPermissionGranted) onPermissionsGranted()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST) {
            val missingPermissions = Utils.getMissingPermissions(this)
            if (missingPermissions.isNotEmpty() && missingPermissions[0] != "android.permission.FOREGROUND_SERVICE") {
                // User didn't grant all the permissions. Warn that the application might not work
                // correctly.
                showAlert(true, R.string.missing_permissions_try_again)
            } else {
                // All permissions granted.
                onPermissionsGranted()
            }
        }
    }

    /**
     * This method will be invoked to show alert message.
     *
     * @param title which is title
     * @param message which is message to show
     */
    override fun showAlert(title: Int, message: Int) {
        android.app.AlertDialog.Builder(this)
            .setTitle(getText(title))
            .setMessage(getText(message))
            .setCancelable(false)
            .setNeutralButton(
                R.string.ok
            ) { dialog, _ ->
                dialog.cancel()
            }
            .create()
            .show()
    }

    /**
     * This method will be invoked to show alert message when get permission and delete feed item.
     *
     * @param isPermission which is true if it comes from permission
     * @param message which is a description of message
     */
    override fun showAlert(isPermission: Boolean, message: Int) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(
                R.string.yes
            ) { dialog: DialogInterface, _: Int ->
                // User wants to try giving the permissions again.
                dialog.cancel()
                if (isPermission) requestPermissions() else feedFragment?.deleteRoom()
            }
            .setNegativeButton(
                R.string.no
            ) { dialog: DialogInterface, _: Int ->
                // User doesn't want to give the permissions.
                dialog.cancel()
                if (isPermission) onPermissionsGranted()
            }
            .show()
    }

    private fun onPermissionsGranted() {
        val mediaStorageDir: File = File(getExternalFilesDir(null), "vpm")
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "Failed to create directory")
            } else {
                createVideoFolder(mediaStorageDir)
            }
        } else {
            createVideoFolder(mediaStorageDir)
        }


    }

    private fun createVideoFolder(mediaStorageDir: File) {
        val fileWithinMyDir: File =
            File(mediaStorageDir, "videos") //Getting a file within the dir.
        fileWithinMyDir.mkdir()
    }

    companion object {
        private const val TAG = "HomeActivity"
        private const val PERMISSION_REQUEST = 1
    }
}
