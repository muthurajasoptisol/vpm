/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core;

import android.os.Handler;
import com.adt.vpm.videoplayer.source.core.audio.AudioRendererEventListener;
import com.adt.vpm.videoplayer.source.core.metadata.MetadataOutput;
import com.adt.vpm.videoplayer.source.core.text.TextOutput;
import com.adt.vpm.videoplayer.source.core.video.VideoRendererEventListener;

/**
 * Builds {@link Renderer} instances for use by a {@link SimpleExoPlayer}.
 */
public interface RenderersFactory {

  /**
   * Builds the {@link Renderer} instances for a {@link SimpleExoPlayer}.
   *
   * @param eventHandler A handler to use when invoking event listeners and outputs.
   * @param videoRendererEventListener An event listener for video renderers.
   * @param audioRendererEventListener An event listener for audio renderers.
   * @param textRendererOutput An output for text renderers.
   * @param metadataRendererOutput An output for metadata renderers.
   * @return The {@link Renderer instances}.
   */
  Renderer[] createRenderers(
          Handler eventHandler,
          VideoRendererEventListener videoRendererEventListener,
          AudioRendererEventListener audioRendererEventListener,
          TextOutput textRendererOutput,
          MetadataOutput metadataRendererOutput);
}