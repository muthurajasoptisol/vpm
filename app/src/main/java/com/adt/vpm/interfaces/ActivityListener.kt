/*
 * Created by ADT author on 9/14/20 11:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/14/20 10:51 PM
 */

package com.adt.vpm.interfaces

import com.adt.vpm.model.Feed

interface ActivityListener {

    fun showCreateRoomDialog(isFrom: Int)

    fun hideCreateRoomDialog()

    fun showDeleteIcon(isShow: Boolean)

    fun onVideoSelected(feed: Feed)

    fun showAlert(title: Int, message: Int)

    fun showAlert(isPermission: Boolean, message: Int)

}