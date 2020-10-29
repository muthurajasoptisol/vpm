/*
 * Created by ADT author on 9/14/20 11:00 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/14/20 10:49 PM
 */

package com.adt.vpm.util

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

/**
 * This class is used to get utility functionality
 */
object Utils {

    private const val TAG = "VPMUtils"
    private const val PERMISSION_REQUEST = 1

    /**
     * This method is used to check network connection state
     *
     * @param activity Context of the activity
     */
    fun haveNetworkConnection(activity: Context): Boolean {
        var result = false
        val connectivityManager =
            activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connectivityManager.activeNetwork ?: return false
            val actNw =
                connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
            result = when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            connectivityManager.run {
                @Suppress("DEPRECATION")
                connectivityManager.activeNetworkInfo?.run {
                    result = when (type) {
                        ConnectivityManager.TYPE_WIFI -> true
                        ConnectivityManager.TYPE_MOBILE -> true
                        ConnectivityManager.TYPE_ETHERNET -> true
                        else -> false
                    }

                }
            }
        }

        return result
    }

    /**
     * This method will be invoked to get needed user permission to access the app.
     *
     * @param appContext Context of the app
     */
    fun isPermissionsGranted(appContext: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // Dynamic permissions are not required before Android M.
            return true
        }
        val missingPermissions = getMissingPermissions(appContext)
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                appContext as AppCompatActivity,
                missingPermissions,
                PERMISSION_REQUEST
            )
        }
        return missingPermissions.isEmpty()
    }

    fun getMissingPermissions(appContext: Context): Array<String?> {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return arrayOfNulls(0)
        }

        val info: PackageInfo = try {
            appContext.packageManager.getPackageInfo(
                appContext.packageName,
                PackageManager.GET_PERMISSIONS
            )
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "Failed to retrieve permissions.")
            return arrayOfNulls(0)
        }
        if (info.requestedPermissions == null) {
            Log.w(TAG, "No requested permissions.")
            return arrayOfNulls(0)
        }

        val missingPermissions = ArrayList<String?>()
        for (i in info.requestedPermissions.indices) {
            if (info.requestedPermissionsFlags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED == 0) {
                missingPermissions.add(info.requestedPermissions[i])
            }
        }
        Log.d(TAG, "Missing permissions: $missingPermissions")

        return missingPermissions.toTypedArray()
    }
}