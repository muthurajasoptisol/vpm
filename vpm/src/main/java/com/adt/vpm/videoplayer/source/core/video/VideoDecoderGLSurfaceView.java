/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.video;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import androidx.annotation.Nullable;

/**
 * GLSurfaceView for rendering video output. To render video in this view, call {@link
 * #getVideoDecoderOutputBufferRenderer()} to get a {@link VideoDecoderOutputBufferRenderer} that
 * will render video decoder output buffers in this view.
 *
 * <p>This view is intended for use only with extension renderers. For other use cases a {@link
 * android.view.SurfaceView} or {@link android.view.TextureView} should be used instead.
 */
public class VideoDecoderGLSurfaceView extends GLSurfaceView {

  private final VideoDecoderGLFrameRenderer renderer;

  /** @param context A {@link Context}. */
  public VideoDecoderGLSurfaceView(Context context) {
    this(context, /* attrs= */ null);
  }

  /**
   * @param context A {@link Context}.
   * @param attrs Custom attributes.
   */
  @SuppressWarnings({
    "nullness:assignment.type.incompatible",
    "nullness:argument.type.incompatible",
    "nullness:method.invocation.invalid"
  })
  public VideoDecoderGLSurfaceView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    renderer = new VideoDecoderGLFrameRenderer(/* surfaceView= */ this);
    setPreserveEGLContextOnPause(true);
    setEGLContextClientVersion(2);
    setRenderer(renderer);
    setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
  }

  /** Returns the {@link VideoDecoderOutputBufferRenderer} that will render frames in this view. */
  public VideoDecoderOutputBufferRenderer getVideoDecoderOutputBufferRenderer() {
    return renderer;
  }
}
