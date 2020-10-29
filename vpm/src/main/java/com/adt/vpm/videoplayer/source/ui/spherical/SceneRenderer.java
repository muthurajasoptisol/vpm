/*
 * Created by ADT author on 9/29/20 5:37 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/22/20 6:23 PM
 */
package com.adt.vpm.videoplayer.source.ui.spherical;

import android.graphics.SurfaceTexture;
import android.media.MediaFormat;
import android.opengl.GLES20;
import android.opengl.Matrix;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.Format;
import com.adt.vpm.videoplayer.source.common.util.Assertions;
import com.adt.vpm.videoplayer.source.core.util.GlUtil;
import com.adt.vpm.videoplayer.source.core.util.TimedValueQueue;
import com.adt.vpm.videoplayer.source.core.video.VideoFrameMetadataListener;
import com.adt.vpm.videoplayer.source.core.video.spherical.CameraMotionListener;
import com.adt.vpm.videoplayer.source.core.video.spherical.FrameRotationQueue;
import com.adt.vpm.videoplayer.source.core.video.spherical.Projection;
import com.adt.vpm.videoplayer.source.core.video.spherical.ProjectionDecoder;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.adt.vpm.videoplayer.source.core.util.GlUtil.checkGlError;


/** Renders a GL Scene. */
/* package */ final class SceneRenderer
    implements VideoFrameMetadataListener, CameraMotionListener {

  private final AtomicBoolean frameAvailable;
  private final AtomicBoolean resetRotationAtNextFrame;
  private final ProjectionRenderer projectionRenderer;
  private final FrameRotationQueue frameRotationQueue;
  private final TimedValueQueue<Long> sampleTimestampQueue;
  private final TimedValueQueue<Projection> projectionQueue;
  private final float[] rotationMatrix;
  private final float[] tempMatrix;

  // Used by GL thread only
  private int textureId;
  private @MonotonicNonNull SurfaceTexture surfaceTexture;

  // Used by other threads only
  private volatile @C.StreamType int defaultStereoMode;
  private @C.StreamType int lastStereoMode;
  @Nullable private byte[] lastProjectionData;

  // Methods called on any thread.

  public SceneRenderer() {
    frameAvailable = new AtomicBoolean();
    resetRotationAtNextFrame = new AtomicBoolean(true);
    projectionRenderer = new ProjectionRenderer();
    frameRotationQueue = new FrameRotationQueue();
    sampleTimestampQueue = new TimedValueQueue<>();
    projectionQueue = new TimedValueQueue<>();
    rotationMatrix = new float[16];
    tempMatrix = new float[16];
    defaultStereoMode = C.STEREO_MODE_MONO;
    lastStereoMode = Format.NO_VALUE;
  }

  /**
   * Sets the default stereo mode. If the played video doesn't contain a stereo mode the default one
   * is used.
   *
   * @param stereoMode A {@link C.StereoMode} value.
   */
  public void setDefaultStereoMode(@C.StereoMode int stereoMode) {
    defaultStereoMode = stereoMode;
  }

  // Methods called on GL thread.

  /** Initializes the renderer. */
  public SurfaceTexture init() {
    // Set the background frame color. This is only visible if the display mesh isn't a full sphere.
    GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
    checkGlError();

    projectionRenderer.init();
    checkGlError();

    textureId = GlUtil.createExternalTexture();
    surfaceTexture = new SurfaceTexture(textureId);
    surfaceTexture.setOnFrameAvailableListener(surfaceTexture -> frameAvailable.set(true));
    return surfaceTexture;
  }

  /**
   * Draws the scene with a given eye pose and type.
   *
   * @param viewProjectionMatrix 16 element GL matrix.
   * @param rightEye Whether the right eye view should be drawn. If {@code false}, the left eye view
   *     is drawn.
   */
  public void drawFrame(float[] viewProjectionMatrix, boolean rightEye) {
    // glClear isn't strictly necessary when rendering fully spherical panoramas, but it can improve
    // performance on tiled renderers by causing the GPU to discard previous data.
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
    checkGlError();

    if (frameAvailable.compareAndSet(true, false)) {
      Assertions.checkNotNull(surfaceTexture).updateTexImage();
      checkGlError();
      if (resetRotationAtNextFrame.compareAndSet(true, false)) {
        Matrix.setIdentityM(rotationMatrix, 0);
      }
      long lastFrameTimestampNs = surfaceTexture.getTimestamp();
      Long sampleTimestampUs = sampleTimestampQueue.poll(lastFrameTimestampNs);
      if (sampleTimestampUs != null) {
        frameRotationQueue.pollRotationMatrix(rotationMatrix, sampleTimestampUs);
      }
      Projection projection = projectionQueue.pollFloor(lastFrameTimestampNs);
      if (projection != null) {
        projectionRenderer.setProjection(projection);
      }
    }
    Matrix.multiplyMM(tempMatrix, 0, viewProjectionMatrix, 0, rotationMatrix, 0);
    projectionRenderer.draw(textureId, tempMatrix, rightEye);
  }

  /** Cleans up the GL resources. */
  public void shutdown() {
    projectionRenderer.shutdown();
  }

  // Methods called on playback thread.

  // VideoFrameMetadataListener implementation.

  @Override
  public void onVideoFrameAboutToBeRendered(
      long presentationTimeUs,
      long releaseTimeNs,
      Format format,
      @Nullable MediaFormat mediaFormat) {
    sampleTimestampQueue.add(releaseTimeNs, presentationTimeUs);
    setProjection(format.projectionData, format.stereoMode, releaseTimeNs);
  }

  // CameraMotionListener implementation.

  @Override
  public void onCameraMotion(long timeUs, float[] rotation) {
    frameRotationQueue.setRotation(timeUs, rotation);
  }

  @Override
  public void onCameraMotionReset() {
    sampleTimestampQueue.clear();
    frameRotationQueue.reset();
    resetRotationAtNextFrame.set(true);
  }

  /**
   * Sets projection data and stereo mode of the media to be played.
   *
   * @param projectionData Contains the projection data to be rendered.
   * @param stereoMode A {@link C.StereoMode} value.
   * @param timeNs When then new projection should be used.
   */
  private void setProjection(
      @Nullable byte[] projectionData, @C.StereoMode int stereoMode, long timeNs) {
    byte[] oldProjectionData = lastProjectionData;
    int oldStereoMode = lastStereoMode;
    lastProjectionData = projectionData;
    lastStereoMode = stereoMode == Format.NO_VALUE ? defaultStereoMode : stereoMode;
    if (oldStereoMode == lastStereoMode && Arrays.equals(oldProjectionData, lastProjectionData)) {
      return;
    }

    Projection projectionFromData = null;
    if (lastProjectionData != null) {
      projectionFromData = ProjectionDecoder.decode(lastProjectionData, lastStereoMode);
    }
    Projection projection =
        projectionFromData != null && ProjectionRenderer.isSupported(projectionFromData)
            ? projectionFromData
            : Projection.createEquirectangular(lastStereoMode);
    projectionQueue.add(timeNs, projection);
  }
}
