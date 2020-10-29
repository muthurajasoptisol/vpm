/*
 * Created by ADT author on 9/14/20 11:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/14/20 10:51 PM
 */
package com.adt.vpm.activity

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import com.adt.vpm.R
import com.adt.vpm.fragment.SettingsFragment
import com.adt.vpm.util.Log
import com.adt.vpm.webrtc.util.SessionManager
import kotlinx.android.synthetic.main.activity_setting.*

/**
 * Settings activity for AppRTC.
 */
class SettingsActivity : AppCompatActivity(), OnSharedPreferenceChangeListener {
    private var keyPrefSlServerUrl: String? = null
    private var keyPrefLogEnable: String? = null
    private var settingsFragment: SettingsFragment? = null
    private var keyPrefVideoCall: String? = null
    private var keyPrefScreenCapture: String? = null
    private var keyPrefCamera2: String? = null
    private var keyPrefResolution: String? = null
    private var keyPrefFps: String? = null
    private var keyPrefCaptureQualitySlider: String? = null
    private var keyPrefMaxVideoBitrateType: String? = null
    private var keyPrefMaxVideoBitrateValue: String? = null
    private var keyPrefVideoCodec: String? = null
    private var keyPrefHwCodec: String? = null
    private var keyPrefCaptureToTexture: String? = null
    private var keyPrefFlexFec: String? = null
    private var keyPrefStartAudioBitrateType: String? = null
    private var keyPrefStartAudioBitrateValue: String? = null
    private var keyPrefAudioCodec: String? = null
    private var keyPrefNoAudioProcessing: String? = null
    private var keyPrefAecDump: String? = null
    private var keyPrefEnableSaveInputAudioToFile: String? = null
    private var keyPrefOpenSLES: String? = null
    private var keyPrefDisableBuiltInAEC: String? = null
    private var keyPrefDisableBuiltInAGC: String? = null
    private var keyPrefDisableBuiltInNS: String? = null
    private var keyPrefDisableWebRtcAGCAndHPF: String? = null
    private var keyPrefSpeakerphone: String? = null
    private var keyPrefRoomServerUrl: String? = null
    private var keyPrefDisplayHud: String? = null
    private var keyPrefTracing: String? = null
    private var keyPrefEnabledRtcEventLog: String? = null
    private var keyPrefEnableDataChannel: String? = null
    private var keyPrefOrdered: String? = null
    private var keyPrefMaxRetransmitTimeMs: String? = null
    private var keyPrefMaxRetransmits: String? = null
    private var keyPrefDataProtocol: String? = null
    private var keyPrefNegotiated: String? = null
    private var keyPrefDataId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initKeyPrefKey()
        setContentView(R.layout.activity_setting)
        supportActionBar?.hide()

        ivBackBtn?.setOnClickListener { onBackPressed() }

        // Display the fragment as the main content.
        settingsFragment = SettingsFragment()

        val aFragmentTrans = supportFragmentManager.beginTransaction()
        aFragmentTrans.add(R.id.flCallSettingsFragment, settingsFragment!!)
        aFragmentTrans.commit()

    }

    override fun onResume() {
        super.onResume()
        setSummary()
        val disableBuiltInAGCPreference: Preference? =
            keyPrefDisableBuiltInAGC?.let { settingsFragment?.findPreference(it) }

        disableBuiltInAGCPreference?.summary = getString(R.string.pref_built_in_agc_not_available)
        disableBuiltInAGCPreference?.isEnabled = false
    }

    override fun onPause() {
        super.onPause()
        val sharedPreferences =
            settingsFragment!!.preferenceScreen.sharedPreferences
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        // clang-format off
        if (key == keyPrefResolution || key == keyPrefFps || key == keyPrefMaxVideoBitrateType || key == keyPrefVideoCodec || key == keyPrefStartAudioBitrateType || key == keyPrefAudioCodec || key == keyPrefRoomServerUrl || key == keyPrefMaxRetransmitTimeMs || key == keyPrefMaxRetransmits || key == keyPrefDataProtocol || key == keyPrefDataId) {
            updateSummary(sharedPreferences, key)
        } else if (key == keyPrefMaxVideoBitrateValue || key == keyPrefStartAudioBitrateValue) {
            updateSummaryBitrate(sharedPreferences, key)
        } else if (key == keyPrefVideoCall || key == keyPrefScreenCapture || key == keyPrefCamera2 || key == keyPrefTracing || key == keyPrefCaptureQualitySlider || key == keyPrefHwCodec || key == keyPrefCaptureToTexture || key == keyPrefFlexFec || key == keyPrefNoAudioProcessing || key == keyPrefAecDump || key == keyPrefEnableSaveInputAudioToFile || key == keyPrefOpenSLES || key == keyPrefDisableBuiltInAEC || key == keyPrefDisableBuiltInAGC || key == keyPrefDisableBuiltInNS || key == keyPrefDisableWebRtcAGCAndHPF || key == keyPrefDisplayHud || key == keyPrefEnableDataChannel || key == keyPrefOrdered || key == keyPrefNegotiated || key == keyPrefEnabledRtcEventLog) {
            updateSummaryB(sharedPreferences, key)
        } else if (key == keyPrefSpeakerphone) {
            updateSummaryList(key)
        }
        // clang-format on
        if (key == keyPrefMaxVideoBitrateType) {
            setVideoBitrateEnable(sharedPreferences)
        }
        if (key == keyPrefStartAudioBitrateType) {
            setAudioBitrateEnable(sharedPreferences)
        }
        if (key == keyPrefEnableDataChannel) {
            setDataChannelEnable(sharedPreferences)
        }

        if (key == keyPrefSlServerUrl) {
            updateSlServerUrl(sharedPreferences, key)
        }

        if (key == keyPrefLogEnable) {
            updateLogEnable()
        }
    }

    private fun updateLogEnable() {
        Log.enable(SessionManager.instance?.isLoggingEnabled(this)!!)
    }

    private fun updateSlServerUrl(
        sharedPreferences: SharedPreferences,
        key: String
    ) {
        val updatedPref: EditTextPreference? = key.let { settingsFragment?.findPreference(it) }
        updatedPref?.onPreferenceChangeListener =
            CustomPreferenceClickListener(
                this.applicationContext,
                sharedPreferences,
                updatedPref,
                key
            )
    }


    class CustomPreferenceClickListener(
        var context: Context,
        private var sharedPreferences: SharedPreferences,
        private var updatedPref: EditTextPreference?,
        private val key: String
    ) : Preference.OnPreferenceChangeListener {

        override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {

            newValue?.let {
                updatedPref?.text = newValue as String
                val validateFlag = checkValidURL(it as String)
                if (validateFlag) sharedPreferences.edit().putString(key, it).apply() else {
                    Toast.makeText(
                        context,
                        "Enter Valid URL!!",
                        Toast.LENGTH_LONG
                    ).show()
                    sharedPreferences.edit()
                        .putString(key, context.getString(R.string.pref_default_sl_server_url_demo))
                        .apply()
                }
            }
            return false
        }

        private fun checkValidURL(s: String): Boolean {
            return Patterns.WEB_URL.matcher(s).matches()
        }

    }


    private fun updateSummary(sharedPreferences: SharedPreferences, key: String?) {
        val updatedPref: Preference? = key?.let { settingsFragment?.findPreference(it) }
        // Set summary to be the user-description for the selected value
        updatedPref?.summary = sharedPreferences.getString(key, "")
    }

    private fun updateSummaryBitrate(sharedPreferences: SharedPreferences, key: String?) {
        val updatedPref: Preference? = key?.let { settingsFragment?.findPreference(it) }
        updatedPref?.summary = sharedPreferences.getString(key, "") + " kbps"
    }

    private fun updateSummaryB(sharedPreferences: SharedPreferences, key: String?) {
        val updatedPref: Preference? = key?.let { settingsFragment?.findPreference(it) }
        updatedPref?.summary = if (sharedPreferences.getBoolean(
                key,
                true
            )
        ) getString(R.string.pref_value_enabled) else getString(R.string.pref_value_disabled)
    }

    private fun updateSummaryList(key: String?) {
        val updatedPref: androidx.preference.ListPreference? =
            key?.let { settingsFragment?.findPreference(it) }
        updatedPref?.summary = updatedPref?.entry
    }

    private fun setVideoBitrateEnable(sharedPreferences: SharedPreferences) {
        val bitratePreferenceValue: Preference? =
            keyPrefMaxVideoBitrateValue?.let { settingsFragment?.findPreference(it) }
        val bitrateTypeDefault = getString(R.string.pref_maxvideobitrate_default)
        val bitrateType =
            sharedPreferences.getString(keyPrefMaxVideoBitrateType, bitrateTypeDefault)
        bitratePreferenceValue?.isEnabled = bitrateType != bitrateTypeDefault
    }

    private fun setAudioBitrateEnable(sharedPreferences: SharedPreferences) {
        val bitratePreferenceValue: Preference? =
            keyPrefStartAudioBitrateValue?.let { settingsFragment?.findPreference(it) }
        val bitrateTypeDefault = getString(R.string.pref_startaudiobitrate_default)
        val bitrateType =
            sharedPreferences.getString(keyPrefStartAudioBitrateType, bitrateTypeDefault)
        bitratePreferenceValue?.isEnabled = bitrateType != bitrateTypeDefault
    }

    private fun setDataChannelEnable(sharedPreferences: SharedPreferences) {
        val enabled = sharedPreferences.getBoolean(keyPrefEnableDataChannel, true)
        setEnable(keyPrefOrdered, enabled)
        setEnable(keyPrefMaxRetransmitTimeMs, enabled)
        setEnable(keyPrefMaxRetransmits, enabled)
        setEnable(keyPrefDataProtocol, enabled)
        setEnable(keyPrefNegotiated, enabled)
        setEnable(keyPrefDataId, enabled)
    }

    private fun setEnable(aVal: String?, enable: Boolean) {
        val aPref: Preference? = aVal?.let { settingsFragment?.findPreference(it) }
        aPref?.isEnabled = enable
    }

    private fun initKeyPrefKey() {
        keyPrefSlServerUrl = getString(R.string.pref_sl_server_url_key)
        keyPrefLogEnable = getString(R.string.pref_enable_logging_key)
        keyPrefVideoCall = getString(R.string.pref_videocall_key)
        keyPrefScreenCapture = getString(R.string.pref_screencapture_key)
        keyPrefCamera2 = getString(R.string.pref_camera2_key)
        keyPrefResolution = getString(R.string.pref_resolution_key)
        keyPrefFps = getString(R.string.pref_fps_key)
        keyPrefCaptureQualitySlider = getString(R.string.pref_capturequalityslider_key)
        keyPrefMaxVideoBitrateType = getString(R.string.pref_maxvideobitrate_key)
        keyPrefMaxVideoBitrateValue = getString(R.string.pref_maxvideobitratevalue_key)
        keyPrefVideoCodec = getString(R.string.pref_videocodec_key)
        keyPrefHwCodec = getString(R.string.pref_hwcodec_key)
        keyPrefCaptureToTexture = getString(R.string.pref_capturetotexture_key)
        keyPrefFlexFec = getString(R.string.pref_flexfec_key)
        keyPrefStartAudioBitrateType = getString(R.string.pref_startaudiobitrate_key)
        keyPrefStartAudioBitrateValue = getString(R.string.pref_startaudiobitratevalue_key)
        keyPrefAudioCodec = getString(R.string.pref_audiocodec_key)
        keyPrefNoAudioProcessing = getString(R.string.pref_noaudioprocessing_key)
        keyPrefAecDump = getString(R.string.pref_aecdump_key)
        keyPrefEnableSaveInputAudioToFile =
            getString(R.string.pref_enable_save_input_audio_to_file_key)
        keyPrefOpenSLES = getString(R.string.pref_opensles_key)
        keyPrefDisableBuiltInAEC = getString(R.string.pref_disable_built_in_aec_key)
        keyPrefDisableBuiltInAGC = getString(R.string.pref_disable_built_in_agc_key)
        keyPrefDisableBuiltInNS = getString(R.string.pref_disable_built_in_ns_key)
        keyPrefDisableWebRtcAGCAndHPF = getString(R.string.pref_disable_webrtc_agc_and_hpf_key)
        keyPrefSpeakerphone = getString(R.string.pref_speakerphone_key)
        keyPrefEnableDataChannel = getString(R.string.pref_enable_datachannel_key)
        keyPrefOrdered = getString(R.string.pref_ordered_key)
        keyPrefMaxRetransmitTimeMs = getString(R.string.pref_max_retransmit_time_ms_key)
        keyPrefMaxRetransmits = getString(R.string.pref_max_retransmits_key)
        keyPrefDataProtocol = getString(R.string.pref_data_protocol_key)
        keyPrefNegotiated = getString(R.string.pref_negotiated_key)
        keyPrefDataId = getString(R.string.pref_data_id_key)
        keyPrefRoomServerUrl = getString(R.string.pref_room_server_url_key)
        keyPrefDisplayHud = getString(R.string.pref_displayhud_key)
        keyPrefTracing = getString(R.string.pref_tracing_key)
        keyPrefEnabledRtcEventLog = getString(R.string.pref_enable_rtceventlog_key)
    }

    private fun setSummary() {
        // Set summary to be the user-description for the selected value
        val sharedPreferences = settingsFragment!!.preferenceScreen.sharedPreferences
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        updateSummaryB(sharedPreferences, keyPrefVideoCall)
        updateSummaryB(sharedPreferences, keyPrefScreenCapture)
        updateSummaryB(sharedPreferences, keyPrefCamera2)
        updateSummary(sharedPreferences, keyPrefResolution)
        updateSummary(sharedPreferences, keyPrefFps)
        updateSummaryB(sharedPreferences, keyPrefCaptureQualitySlider)
        updateSummary(sharedPreferences, keyPrefMaxVideoBitrateType)
        updateSummaryBitrate(sharedPreferences, keyPrefMaxVideoBitrateValue)
        setVideoBitrateEnable(sharedPreferences)
        updateSummary(sharedPreferences, keyPrefVideoCodec)
        updateSummaryB(sharedPreferences, keyPrefHwCodec)
        updateSummaryB(sharedPreferences, keyPrefCaptureToTexture)
        updateSummaryB(sharedPreferences, keyPrefFlexFec)
        updateSummary(sharedPreferences, keyPrefStartAudioBitrateType)
        updateSummaryBitrate(sharedPreferences, keyPrefStartAudioBitrateValue)
        setAudioBitrateEnable(sharedPreferences)
        updateSummary(sharedPreferences, keyPrefAudioCodec)
        updateSummaryB(sharedPreferences, keyPrefNoAudioProcessing)
        updateSummaryB(sharedPreferences, keyPrefAecDump)
        updateSummaryB(sharedPreferences, keyPrefEnableSaveInputAudioToFile)
        updateSummaryB(sharedPreferences, keyPrefOpenSLES)
        updateSummaryB(sharedPreferences, keyPrefDisableBuiltInAEC)
        updateSummaryB(sharedPreferences, keyPrefDisableBuiltInAGC)
        updateSummaryB(sharedPreferences, keyPrefDisableBuiltInNS)
        updateSummaryB(sharedPreferences, keyPrefDisableWebRtcAGCAndHPF)
        updateSummaryList(keyPrefSpeakerphone)
        updateSummaryB(sharedPreferences, keyPrefEnableDataChannel)
        updateSummaryB(sharedPreferences, keyPrefOrdered)
        updateSummary(sharedPreferences, keyPrefMaxRetransmitTimeMs)
        updateSummary(sharedPreferences, keyPrefMaxRetransmits)
        updateSummary(sharedPreferences, keyPrefDataProtocol)
        updateSummaryB(sharedPreferences, keyPrefNegotiated)
        updateSummary(sharedPreferences, keyPrefDataId)
        setDataChannelEnable(sharedPreferences)
        updateSummary(sharedPreferences, keyPrefRoomServerUrl)
        updateSummaryB(sharedPreferences, keyPrefDisplayHud)
        updateSummaryB(sharedPreferences, keyPrefTracing)
        updateSummaryB(sharedPreferences, keyPrefEnabledRtcEventLog)
    }

}