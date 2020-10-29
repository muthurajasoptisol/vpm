/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Handler;

/* package */ final class AudioBecomingNoisyManager {

  private final Context context;
  private final AudioBecomingNoisyReceiver receiver;
  private boolean receiverRegistered;

  public interface EventListener {
    void onAudioBecomingNoisy();
  }

  public AudioBecomingNoisyManager(Context context, Handler eventHandler, EventListener listener) {
    this.context = context.getApplicationContext();
    this.receiver = new AudioBecomingNoisyReceiver(eventHandler, listener);
  }

  /**
   * Enables the {@link AudioBecomingNoisyManager} which calls {@link
   * EventListener#onAudioBecomingNoisy()} upon receiving an intent of {@link
   * AudioManager#ACTION_AUDIO_BECOMING_NOISY}.
   *
   * @param enabled True if the listener should be notified when audio is becoming noisy.
   */
  public void setEnabled(boolean enabled) {
    if (enabled && !receiverRegistered) {
      context.registerReceiver(
              receiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
      receiverRegistered = true;
    } else if (!enabled && receiverRegistered) {
      context.unregisterReceiver(receiver);
      receiverRegistered = false;
    }
  }

  private final class AudioBecomingNoisyReceiver extends BroadcastReceiver implements Runnable {
    private final EventListener listener;
    private final Handler eventHandler;

    public AudioBecomingNoisyReceiver(Handler eventHandler, EventListener listener) {
      this.eventHandler = eventHandler;
      this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
      if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
        eventHandler.post(this);
      }
    }

    @Override
    public void run() {
      if (receiverRegistered) {
        listener.onAudioBecomingNoisy();
      }
    }
  }
}
