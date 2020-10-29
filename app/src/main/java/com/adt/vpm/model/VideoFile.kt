/*
 * Created by ADT author on 9/23/20 12:27 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/23/20 12:27 PM
 */

package com.adt.vpm.model

import android.net.Uri
import com.adt.vpm.videoplayer.VideoPlayer.StreamType

data class VideoFile(
    val filename: String,
    val fileUri: Uri,
    val fileType: StreamType
)