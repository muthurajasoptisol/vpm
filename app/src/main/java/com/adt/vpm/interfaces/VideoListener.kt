/*
 * Created by ADT author on 9/23/20 12:57 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/23/20 12:57 PM
 */

package com.adt.vpm.interfaces

import com.adt.vpm.model.VideoFile

interface VideoListener {

    fun onVideoSelected(video: VideoFile)

}