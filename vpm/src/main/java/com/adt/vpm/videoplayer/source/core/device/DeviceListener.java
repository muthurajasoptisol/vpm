/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.device;

import com.adt.vpm.videoplayer.source.common.device.DeviceInfo;
import com.adt.vpm.videoplayer.source.core.Player;

/** A listener for changes of {@link Player.DeviceComponent}. */
public interface DeviceListener {

  /** Called when the device information changes. */
  default void onDeviceInfoChanged(DeviceInfo deviceInfo) {}

  /** Called when the device volume or mute state changes. */
  default void onDeviceVolumeChanged(int volume, boolean muted) {}
}
