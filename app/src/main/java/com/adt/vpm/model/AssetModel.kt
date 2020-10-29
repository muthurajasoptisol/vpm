/*
 * Created by ADT author on 9/23/20 12:27 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/23/20 12:27 PM
 */

package com.adt.vpm.model

data class AssetModel(
    val http: ArrayList<AssetFile>,
    val rtsp: ArrayList<AssetFile>,
)