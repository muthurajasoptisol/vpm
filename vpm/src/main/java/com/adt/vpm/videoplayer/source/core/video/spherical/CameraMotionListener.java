/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.video.spherical;

/** Listens camera motion. */
public interface CameraMotionListener {

  /**
   * Called when a new camera motion is read. This method is called on the playback thread.
   *
   * @param timeUs The presentation time of the data.
   * @param rotation Angle axis orientation in radians representing the rotation from camera
   *     coordinate system to world coordinate system.
   */
  void onCameraMotion(long timeUs, float[] rotation);

  /** Called when the camera motion track position is reset or the track is disabled. */
  void onCameraMotionReset();
}
