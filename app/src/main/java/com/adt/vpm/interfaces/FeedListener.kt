/*
 * Created by ADT author on 9/14/20 11:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/14/20 10:51 PM
 */

package com.adt.vpm.interfaces

import com.adt.vpm.model.Feed

interface FeedListener {

    fun onVideoSelected(feed: Feed)

    fun onShowDeleteIcon()

}