/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.audio;

import com.adt.vpm.videoplayer.source.common.audio.AudioAttributes;

/** A listener for changes in audio configuration. */
public interface AudioListener {

  /**
   * Called when the audio session is set.
   *
   * @param audioSessionId The audio session id.
   */
  default void onAudioSessionId(int audioSessionId) {}

  /**
   * Called when the audio attributes change.
   *
   * @param audioAttributes The audio attributes.
   */
  default void onAudioAttributesChanged(AudioAttributes audioAttributes) {}

  /**
   * Called when the volume changes.
   *
   * @param volume The new volume, with 0 being silence and 1 being unity gain.
   */
  default void onVolumeChanged(float volume) {}

  /**
   * Called when skipping silences is enabled or disabled in the audio stream.
   *
   * @param skipSilenceEnabled Whether skipping silences in the audio stream is enabled.
   */
  default void onSkipSilenceEnabledChanged(boolean skipSilenceEnabled) {}
}
