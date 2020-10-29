/*
 * Created by ADT author on 9/14/20 11:00 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/14/20 10:49 PM
 */

package com.adt.vpm.util

class LogMsg {
    companion object {
        const val app_name = "VideoPlayerModule"
        const val msg_peer_factory_created = "Peer connection factory created."
        const val msg_peer_connected = "Peer connection created successfully"
        const val msg_create_offer = "Offer created successfully"
        const val msg_create_answer = "Answer created successfully"
        const val msg_add_candidates = "Candidate added successfully"
        const val msg_stream_connected = "Video stream connected"
        const val msg_creator = "Creator"
        const val msg_joiner = "Joiner"
        const val msg_remove_candidates = "Candidates removed from peer"
        const val msg_room_connected = "Room %s connected successfully"
        const val msg_room_created = "Room created"
        const val msg_room_joined = "Room joined"
        const val msg_received = "Received message.."
        const val msg_room_joined_resp = "Room joined response : %s"
        const val msg_set_local_sdp = "Set Local session description successfully"
        const val msg_set_remote_sdp = "Set remote session description successfully"

        const val sdp_type_offer = "offer"
        const val sdp_type_answer = "answer"

        const val msg_room_disconnected = "Room disconnected"
        const val msg_disconnect_remote_end = "Remote end hung up, dropping PeerConnection"

        const val msg_mute_local_video = "Local video enabled : %s"
        const val msg_mute_local_audio = "Local audio enabled : %s"
        const val msg_mute_remote_audio = "Remote audio enabled : %s"
        const val msg_room_params = "Room connection params : %s"
        const val msg_signaling_param = "Signalling params : %s"

        const val msg_peer_connection_param = "Peer connection params : %s"
        const val msg_remote_offer_receive_video = "OfferToReceiveVideo : %s"
        const val msg_remote_offer_receive_audio = "OfferToReceiveAudio : %s"
        const val msg_send_offer = "Send Offer Sdp : %s"
        const val msg_send_answer = "Send Answer Sdp : %s"
        const val msg_received_resp = "On Message received : %s"
        const val msg_bye_resp = "Bye event received from roomId : %s"
        const val msg_peer_connection_fail = "Peer connection failed"
        const val msg_sdp_create_fail = "Session description creation failed %s"
        const val msg_sdp_set_fail = "Session description set failed %s"
        const val msg_peer_connection_state = "Peer Connection state : %s"
        const val msg_ice_connection_state = "Ice Connection state : %s"
        const val msg_set_remote_audio_track = "Remote audio track set successfully"
        const val msg_set_remote_video_track = "Remote video track set successfully"
        const val msg_bye = "Received bye"

        const val msg_closing_peer = "Closing peer connection."
        const val msg_max_video_bitrate = "Configured max video bitrate to: %s"
        const val msg_sender_not_ready = "Sender is not ready."
        const val msg_rtp_param_not_ready = "RtpParameters are not ready."
        const val msg_rtp_send_param_failed = "RtpSender.setParameters failed."

        const val error_peer_connection_not_created = "Peer connection factory is not created"
        const val error_peer_failed_to_create = "Failed to create peer connection: %s"
        const val error_audio_record = "onWebRtcAudioRecordError: %s"
        const val error_audio_record_init = "onWebRtcAudioRecordInitError: %s"
        const val error_audio_record_start = "onWebRtcAudioRecordStartError: %s"
        const val error_audio_record_track = "onWebRtcAudioRecordTrackError: %s"
        const val error_audio_record_track_init = "onWebRtcAudioTrackInitError: %s"
        const val error_audio_record_track_start = "onWebRtcAudioTrackStartError: %s"
        const val error_ice_connect_failed = "ICE connection failed."
        const val error_multiple_sdp_create = "Multiple sdp create"
        const val error_send_offer_failed = "ERROR : Room not connected for sendOfferSdp"
        const val error_send_answer_failed = "ERROR : Room not connected for sendAnswerSdp"

        const val camera2_texture_only_error =
            "Camera2 only supports capturing to texture. Either disable Camera2 or enable capturing to texture in the options."

        const val data_file_name = "File name"
        const val data_location = "Location"
        const val data_width = "Width"
        const val data_height = "Height"
        const val data_duration = "Duration"
        const val data_frame_rate = "FrameRate"
        const val data_mime_type = "MimeType"
        const val txt_duration = "/ %s"

        const val msg_playback_url = "Video playback url : %s"
        const val msg_playback_type = "Video playback type : %s"
        const val msg_playback_state_changed = "Playback state changed : %s"
        const val msg_play_when_ready_changed = "onPlayWhenReadyChanged : %s"
        const val msg_on_video_size_changed = "onVideoSizeChanged : %s"
        const val msg_player_open = "Video player open called"
        const val msg_player_play = "Video player play called"
        const val msg_player_pause = "Video player pause called"
        const val msg_player_resume = "Video player resume called"
        const val msg_player_stop = "Video player open called"
        const val msg_player_close = "Video player close called"
        const val msg_player_seek_to_position = "Video Player seekToPosition : %s"
        const val msg_player_resize_mode = "Set Resize Mode : %s"
        const val msg_metadata_list = "Metadata : %s"
        const val msg_player_error_type = "onPlayerError Type : %s"
        const val msg_player_error_msg = "onPlayerError Message : %s"
        const val msg_controller_visibility_change = "onVisibilityChange : %s"
        const val msg_video_mode = "isPortraitVideo : %s"
        const val msg_orientation_change = "Orientation Changed: %s"
        const val msg_invalid_url_title = "Invalid Url"
        const val msg_invalid_url_msg = "Please check your selected playback source"
        const val msg_internet_failure_title = "Network Failure"
        const val msg_internet_failure_msg = "Please check your internet connection and try again."
        const val msg_invalid_url_unsuccess = "UnSuccess. Please check your selected playback url"
        const val msg_invalid_url_no_response = "No Response from server. Please check your selected playback url"
        const val msg_invalid_url_unauthorized = "UnAuthorized. Please check your selected playback url"
        const val msg_invalid_url_malfunction = "Malfunction. Please check your selected playback url"
        const val msg_invalid_url_request_timeout = "Request Timeout. Please check your selected playback url and try again"
        const val msg_invalid_url_io_error = "Couldn't read data. Please try again"
        const val msg_un_expected_error = "Internal server error. Please try again"

        const val msg_cast_title = "Cast Connection Error"
        const val msg_cast_context_error = "Failed to get Cast context. Try updating Google Play Services and restart the app."

        const val error_invalid_state = "Invalid Playback State"
    }
}