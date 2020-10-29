/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.audio;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.common.Format;
import com.adt.vpm.videoplayer.source.common.audio.AudioAttributes;
import com.adt.vpm.videoplayer.source.core.PlaybackParameters;

import java.nio.ByteBuffer;

/** An overridable {@link AudioSink} implementation forwarding all methods to another sink. */
public class ForwardingAudioSink implements AudioSink {

  private final AudioSink sink;

  public ForwardingAudioSink(AudioSink sink) {
    this.sink = sink;
  }

  @Override
  public void setListener(Listener listener) {
    sink.setListener(listener);
  }

  @Override
  public boolean supportsFormat(Format format) {
    return sink.supportsFormat(format);
  }

  @Override
  @SinkFormatSupport
  public int getFormatSupport(Format format) {
    return sink.getFormatSupport(format);
  }

  @Override
  public long getCurrentPositionUs(boolean sourceEnded) {
    return sink.getCurrentPositionUs(sourceEnded);
  }

  @Override
  public void configure(Format inputFormat, int specifiedBufferSize, @Nullable int[] outputChannels)
      throws ConfigurationException {
    sink.configure(inputFormat, specifiedBufferSize, outputChannels);
  }

  @Override
  public void play() {
    sink.play();
  }

  @Override
  public void handleDiscontinuity() {
    sink.handleDiscontinuity();
  }

  @Override
  public boolean handleBuffer(
      ByteBuffer buffer, long presentationTimeUs, int encodedAccessUnitCount)
      throws InitializationException, WriteException {
    return sink.handleBuffer(buffer, presentationTimeUs, encodedAccessUnitCount);
  }

  @Override
  public void playToEndOfStream() throws WriteException {
    sink.playToEndOfStream();
  }

  @Override
  public boolean isEnded() {
    return sink.isEnded();
  }

  @Override
  public boolean hasPendingData() {
    return sink.hasPendingData();
  }

  @Override
  public void setPlaybackParameters(PlaybackParameters playbackParameters) {
    sink.setPlaybackParameters(playbackParameters);
  }

  @Override
  public PlaybackParameters getPlaybackParameters() {
    return sink.getPlaybackParameters();
  }

  @Override
  public void setSkipSilenceEnabled(boolean skipSilenceEnabled) {
    sink.setSkipSilenceEnabled(skipSilenceEnabled);
  }

  @Override
  public boolean getSkipSilenceEnabled() {
    return sink.getSkipSilenceEnabled();
  }

  @Override
  public void setAudioAttributes(AudioAttributes audioAttributes) {
    sink.setAudioAttributes(audioAttributes);
  }

  @Override
  public void setAudioSessionId(int audioSessionId) {
    sink.setAudioSessionId(audioSessionId);
  }

  @Override
  public void setAuxEffectInfo(AuxEffectInfo auxEffectInfo) {
    sink.setAuxEffectInfo(auxEffectInfo);
  }

  @Override
  public void enableTunnelingV21(int tunnelingAudioSessionId) {
    sink.enableTunnelingV21(tunnelingAudioSessionId);
  }

  @Override
  public void disableTunneling() {
    sink.disableTunneling();
  }

  @Override
  public void setVolume(float volume) {
    sink.setVolume(volume);
  }

  @Override
  public void pause() {
    sink.pause();
  }

  @Override
  public void flush() {
    sink.flush();
  }

  @Override
  public void experimentalFlushWithoutAudioTrackRelease() {
    sink.experimentalFlushWithoutAudioTrackRelease();
  }

  @Override
  public void reset() {
    sink.reset();
  }
}
