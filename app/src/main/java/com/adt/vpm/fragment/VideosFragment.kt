/*
 * Created by ADT author on 9/14/20 11:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/14/20 10:51 PM
 */

package com.adt.vpm.fragment


import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.adt.vpm.BuildConfig
import com.adt.vpm.R
import com.adt.vpm.adpater.VideoAdapter
import com.adt.vpm.interfaces.VideoListener
import com.adt.vpm.model.AssetFile
import com.adt.vpm.model.AssetModel
import com.adt.vpm.model.UriSerializer
import com.adt.vpm.model.VideoFile
import com.adt.vpm.videoplayer.VideoPlayer.StreamType
import com.adt.vpm.videoplayer.VideoPlayerActivity
import com.adt.vpm.videoplayer.source.core.upstream.RawResourceDataSource
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.io.IOException


class VideosFragment : Fragment(), VideoListener {

    private val videoList: MutableList<VideoFile> = mutableListOf()
    private var rvVideoList: RecyclerView? = null
    private var tvNoRecord: AppCompatTextView? = null
    private var adapter: VideoAdapter? = null
    private var videoListItem: List<VideoFile> = mutableListOf()
    private var isLoadLocal: Boolean = false
    private var isLoadHttp: Boolean = false
    private var isLoadRtsp: Boolean = false
    private var sharedPref: SharedPreferences? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val controlView = inflater.inflate(R.layout.fragment_videos, container, false)

        rvVideoList = controlView.findViewById(R.id.rvVideoList)
        tvNoRecord = controlView.findViewById(R.id.tvNoRecord)

        PreferenceManager.setDefaultValues(context, R.xml.preferences, false)
        sharedPref = PreferenceManager.getDefaultSharedPreferences(context)

        isLoadLocal = loadLocalPlayback()
        isLoadHttp = loadHttpPlayback()
        isLoadRtsp = loadRtspPlayback()

        refreshVideoList()

        return controlView
    }

    override fun onResume() {
        super.onResume()
        val isLocalChanged = loadLocalPlayback()
        val isHttpChanged = loadHttpPlayback()
        val isRtspChanged = loadRtspPlayback()
        if (isLocalChanged != isLoadLocal || isHttpChanged != isLoadHttp || isRtspChanged != isLoadRtsp) {
            isLoadLocal = isLocalChanged
            isLoadHttp = isHttpChanged
            isLoadRtsp = isRtspChanged
            refreshVideoList()
        }
    }

    /**
     * This method is used to load video file to refresh list under video tab
     */
    private fun refreshVideoList() {
        videoListItem = openVideoFile()

        val isEmptyVideo = videoListItem.isNullOrEmpty()
        tvNoRecord?.visibility = if (isEmptyVideo) View.VISIBLE else View.GONE

        if (!isEmptyVideo) {
            rvVideoList?.layoutManager = LinearLayoutManager(activity)
            adapter = activity?.let { VideoAdapter(videoListItem, this, it) }
            rvVideoList?.adapter = adapter
        }
    }

    /**
     * This method is used to add file into list
     */
    private fun openVideoFile(): List<VideoFile> {
        videoList.clear()
        videoList.add(
            VideoFile(
                "foreman_assest.mp4",
                RawResourceDataSource.buildRawResourceUri(R.raw.foreman),
                StreamType.LOCAL
            )
        )

        if (isLoadLocal) loadFromLocalStorage()

        if (isLoadHttp || isLoadRtsp) loadFileFromAsset()

        return videoList
    }

    /**
     * This method is used to load files from local storage under path vpm/videos folder
     */
    private fun loadFromLocalStorage() {
        val path = activity?.getExternalFilesDir(null).toString() + "/vpm/videos"
        val directory = File(path)
        val files = directory.listFiles()
        if (files != null && activity != null) {
            for (i in files.indices) {
                val fileName = files[i].name
                val imageUri = activity?.baseContext?.let {
                    FileProvider.getUriForFile(
                        it,
                        BuildConfig.APPLICATION_ID + ".provider",
                        files[i]
                    )
                }
                imageUri?.let { VideoFile(fileName, it, StreamType.LOCAL) }
                    ?.let { videoList.add(it) }
            }
        }
    }

    /**
     * This method is used to load json urls from asset folder
     */
    private fun loadFileFromAsset() {
        var jsonString = ""
        try {
            jsonString = requireActivity().applicationContext.assets.open("videos.json")
                .bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        if (jsonString.isNotEmpty()) {
            val assetModel: AssetModel = Gson().fromJson(jsonString, AssetModel::class.java)
            if (assetModel.rtsp.isNotEmpty() && isLoadRtsp) {
                loadFromJsonList(assetModel.rtsp)
            }
            if (assetModel.http.isNotEmpty() && isLoadHttp) {
                loadFromJsonList(assetModel.http)
            }
        }
    }

    /**
     * This method is used to convert json list to video file list.
     *
     * @param list List of asset files
     */
    private fun loadFromJsonList(list: ArrayList<AssetFile>) {
        for (i in list.indices) {
            val type = when (list[i].type) {
                1 -> StreamType.RTSP
                2 -> StreamType.HLS
                3 -> StreamType.HTTP
                else -> StreamType.OTHERS
            }
            videoList.add(
                VideoFile(
                    list[i].name,
                    Uri.parse(list[i].uri),
                    type
                )
            )
        }
    }

    /**
     * This method will be triggered when video files clicked on list.
     *
     * @param video Video that contains file detail
     */
    override fun onVideoSelected(video: VideoFile) {
        val gson = GsonBuilder()
            .registerTypeAdapter(Uri::class.java, UriSerializer())
            .create()
        startActivity(
            Intent(activity, VideoPlayerActivity::class.java)
                .putExtra("content", gson.toJson(video))
        )
    }

    /**
     * This method is used to get state of load local content url from vpm/video folder based on which is enabled or not.
     */
    private fun loadLocalPlayback(): Boolean {
        return sharedPref?.getBoolean(
            activity?.getString(R.string.pref_load_vpm_video_url_key),
            false
        ) ?: false
    }

    /**
     * This method is used to get state of load http url list from asset based on which is enabled or not.
     */
    private fun loadHttpPlayback(): Boolean {
        return sharedPref?.getBoolean(activity?.getString(R.string.pref_load_http_url_key), false)
            ?: false
    }

    /**
     * This method is used to get state of load Rtsp url list from asset based on which is enabled or not.
     */
    private fun loadRtspPlayback(): Boolean {
        return sharedPref?.getBoolean(activity?.getString(R.string.pref_load_rtsp_url_key), false)
            ?: false
    }

}