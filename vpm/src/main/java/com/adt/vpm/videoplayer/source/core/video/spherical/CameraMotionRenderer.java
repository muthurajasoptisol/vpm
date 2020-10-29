/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.video.spherical;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.core.BaseRenderer;
import com.adt.vpm.videoplayer.source.core.ExoPlaybackException;
import com.adt.vpm.videoplayer.source.core.FormatHolder;
import com.adt.vpm.videoplayer.source.core.Renderer;
import com.adt.vpm.videoplayer.source.core.RendererCapabilities;
import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.Format;
import com.adt.vpm.videoplayer.source.common.decoder.DecoderInputBuffer;
import com.adt.vpm.videoplayer.source.core.source.SampleStream;
import com.adt.vpm.videoplayer.source.common.util.MimeTypes;
import com.adt.vpm.videoplayer.source.common.util.ParsableByteArray;
import com.adt.vpm.videoplayer.source.common.util.Util;
import java.nio.ByteBuffer;

/** A {@link Renderer} that parses the camera motion track. */
public final class CameraMotionRenderer extends BaseRenderer {

  private static final String TAG = "CameraMotionRenderer";
  // The amount of time to read samples ahead of the current time.
  private static final int SAMPLE_WINDOW_DURATION_US = 100_000;

  private final DecoderInputBuffer buffer;
  private final ParsableByteArray scratch;

  private long offsetUs;
  @Nullable private CameraMotionListener listener;
  private long lastTimestampUs;

  public CameraMotionRenderer() {
    super(C.TRACK_TYPE_CAMERA_MOTION);
    buffer = new DecoderInputBuffer(DecoderInputBuffer.BUFFER_REPLACEMENT_MODE_NORMAL);
    scratch = new ParsableByteArray();
  }

  @Override
  public String getName() {
    return TAG;
  }

  @Override
  @Capabilities
  public int supportsFormat(Format format) {
    return MimeTypes.APPLICATION_CAMERA_MOTION.equals(format.sampleMimeType)
        ? RendererCapabilities.create(FORMAT_HANDLED)
        : RendererCapabilities.create(FORMAT_UNSUPPORTED_TYPE);
  }

  @Override
  public void handleMessage(int messageType, @Nullable Object message) throws ExoPlaybackException {
    if (messageType == MSG_SET_CAMERA_MOTION_LISTENER) {
      listener = (CameraMotionListener) message;
    } else {
      super.handleMessage(messageType, message);
    }
  }

  @Override
  protected void onStreamChanged(Format[] formats, long startPositionUs, long offsetUs) {
    this.offsetUs = offsetUs;
  }

  @Override
  protected void onPositionReset(long positionUs, boolean joining) {
    lastTimestampUs = Long.MIN_VALUE;
    resetListener();
  }

  @Override
  protected void onDisabled() {
    resetListener();
  }

  @Override
  public void render(long positionUs, long elapsedRealtimeUs) {
    // Keep reading available samples as long as the sample time is not too far into the future.
    while (!hasReadStreamToEnd() && lastTimestampUs < positionUs + SAMPLE_WINDOW_DURATION_US) {
      buffer.clear();
      FormatHolder formatHolder = getFormatHolder();
      @SampleStream.ReadDataResult
      int result = readSource(formatHolder, buffer, /* formatRequired= */ false);
      if (result != C.RESULT_BUFFER_READ || buffer.isEndOfStream()) {
        return;
      }

      lastTimestampUs = buffer.timeUs;
      if (listener == null || buffer.isDecodeOnly()) {
        continue;
      }

      buffer.flip();
      @Nullable float[] rotation = parseMetadata(Util.castNonNull(buffer.data));
      if (rotation == null) {
        continue;
      }

      Util.castNonNull(listener).onCameraMotion(lastTimestampUs - offsetUs, rotation);
    }
  }

  @Override
  public boolean isEnded() {
    return hasReadStreamToEnd();
  }

  @Override
  public boolean isReady() {
    return true;
  }

  @Nullable
  private float[] parseMetadata(ByteBuffer data) {
    if (data.remaining() != 16) {
      return null;
    }
    scratch.reset(data.array(), data.limit());
    scratch.setPosition(data.arrayOffset() + 4); // skip reserved bytes too.
    float[] result = new float[3];
    for (int i = 0; i < 3; i++) {
      result[i] = Float.intBitsToFloat(scratch.readLittleEndianInt());
    }
    return result;
  }

  private void resetListener() {
    if (listener != null) {
      listener.onCameraMotionReset();
    }
  }
}
