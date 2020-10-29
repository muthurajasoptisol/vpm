/*
 * Created by ADT author on 9/14/20 11:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/14/20 10:51 PM
 */
package com.adt.vpm.fragment

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.adt.vpm.R

/**
 * Settings fragment for AppRTC.
 */
class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
    }
}