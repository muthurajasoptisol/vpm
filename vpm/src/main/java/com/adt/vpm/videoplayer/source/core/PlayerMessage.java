/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core;

import android.os.Handler;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.adt.vpm.videoplayer.source.common.C;
import com.adt.vpm.videoplayer.source.common.util.Assertions;
import com.adt.vpm.videoplayer.source.core.util.Clock;

import java.util.concurrent.TimeoutException;

/**
 * Defines a player message which can be sent with a {@link Sender} and received by a {@link
 * Target}.
 */
public final class PlayerMessage {

  /** A target for messages. */
  public interface Target {

    /**
     * Handles a message delivered to the target.
     *
     * @param messageType The message type.
     * @param payload The message payload.
     * @throws ExoPlaybackException If an error occurred whilst handling the message. Should only be
     *     thrown by targets that handle messages on the playback thread.
     */
    void handleMessage(int messageType, @Nullable Object payload) throws ExoPlaybackException;
  }

  /** A sender for messages. */
  public interface Sender {

    /**
     * Sends a message.
     *
     * @param message The message to be sent.
     */
    void sendMessage(PlayerMessage message);
  }

  private final Target target;
  private final Sender sender;
  private final Timeline timeline;

  private int type;
  @Nullable private Object payload;
  private Handler handler;
  private int windowIndex;
  private long positionMs;
  private boolean deleteAfterDelivery;
  private boolean isSent;
  private boolean isDelivered;
  private boolean isProcessed;
  private boolean isCanceled;

  /**
   * Creates a new message.
   *
   * @param sender The {@link Sender} used to send the message.
   * @param target The {@link Target} the message is sent to.
   * @param timeline The timeline used when setting the position with {@link #setPosition(long)}. If
   *     set to {@link Timeline#EMPTY}, any position can be specified.
   * @param defaultWindowIndex The default window index in the {@code timeline} when no other window
   *     index is specified.
   * @param defaultHandler The default handler to send the message on when no other handler is
   *     specified.
   */
  public PlayerMessage(
      Sender sender,
      Target target,
      Timeline timeline,
      int defaultWindowIndex,
      Handler defaultHandler) {
    this.sender = sender;
    this.target = target;
    this.timeline = timeline;
    this.handler = defaultHandler;
    this.windowIndex = defaultWindowIndex;
    this.positionMs = C.TIME_UNSET;
    this.deleteAfterDelivery = true;
  }

  /** Returns the timeline used for setting the position with {@link #setPosition(long)}. */
  public Timeline getTimeline() {
    return timeline;
  }

  /** Returns the target the message is sent to. */
  public Target getTarget() {
    return target;
  }

  /**
   * Sets the message type forwarded to {@link Target#handleMessage(int, Object)}.
   *
   * @param messageType The message type.
   * @return This message.
   * @throws IllegalStateException If {@link #send()} has already been called.
   */
  public PlayerMessage setType(int messageType) {
    Assertions.checkState(!isSent);
    this.type = messageType;
    return this;
  }

  /** Returns the message type forwarded to {@link Target#handleMessage(int, Object)}. */
  public int getType() {
    return type;
  }

  /**
   * Sets the message payload forwarded to {@link Target#handleMessage(int, Object)}.
   *
   * @param payload The message payload.
   * @return This message.
   * @throws IllegalStateException If {@link #send()} has already been called.
   */
  public PlayerMessage setPayload(@Nullable Object payload) {
    Assertions.checkState(!isSent);
    this.payload = payload;
    return this;
  }

  /** Returns the message payload forwarded to {@link Target#handleMessage(int, Object)}. */
  @Nullable
  public Object getPayload() {
    return payload;
  }

  /**
   * Sets the handler the message is delivered on.
   *
   * @param handler A {@link Handler}.
   * @return This message.
   * @throws IllegalStateException If {@link #send()} has already been called.
   */
  public PlayerMessage setHandler(Handler handler) {
    Assertions.checkState(!isSent);
    this.handler = handler;
    return this;
  }

  /** Returns the handler the message is delivered on. */
  public Handler getHandler() {
    return handler;
  }

  /**
   * Returns position in window at {@link #getWindowIndex()} at which the message will be delivered,
   * in milliseconds. If {@link C#TIME_UNSET}, the message will be delivered immediately. If {@link
   * C#TIME_END_OF_SOURCE}, the message will be delivered at the end of the window at {@link
   * #getWindowIndex()}.
   */
  public long getPositionMs() {
    return positionMs;
  }

  /**
   * Sets a position in the current window at which the message will be delivered.
   *
   * @param positionMs The position in the current window at which the message will be sent, in
   *     milliseconds, or {@link C#TIME_END_OF_SOURCE} to deliver the message at the end of the
   *     current window.
   * @return This message.
   * @throws IllegalStateException If {@link #send()} has already been called.
   */
  public PlayerMessage setPosition(long positionMs) {
    Assertions.checkState(!isSent);
    this.positionMs = positionMs;
    return this;
  }

  /**
   * Sets a position in a window at which the message will be delivered.
   *
   * @param windowIndex The index of the window at which the message will be sent.
   * @param positionMs The position in the window with index {@code windowIndex} at which the
   *     message will be sent, in milliseconds, or {@link C#TIME_END_OF_SOURCE} to deliver the
   *     message at the end of the window with index {@code windowIndex}.
   * @return This message.
   * @throws IllegalSeekPositionException If the timeline returned by {@link #getTimeline()} is not
   *     empty and the provided window index is not within the bounds of the timeline.
   * @throws IllegalStateException If {@link #send()} has already been called.
   */
  public PlayerMessage setPosition(int windowIndex, long positionMs) {
    Assertions.checkState(!isSent);
    Assertions.checkArgument(positionMs != C.TIME_UNSET);
    if (windowIndex < 0 || (!timeline.isEmpty() && windowIndex >= timeline.getWindowCount())) {
      throw new IllegalSeekPositionException(timeline, windowIndex, positionMs);
    }
    this.windowIndex = windowIndex;
    this.positionMs = positionMs;
    return this;
  }

  /** Returns window index at which the message will be delivered. */
  public int getWindowIndex() {
    return windowIndex;
  }

  /**
   * Sets whether the message will be deleted after delivery. If false, the message will be resent
   * if playback reaches the specified position again. Only allowed to be false if a position is set
   * with {@link #setPosition(long)}.
   *
   * @param deleteAfterDelivery Whether the message is deleted after delivery.
   * @return This message.
   * @throws IllegalStateException If {@link #send()} has already been called.
   */
  public PlayerMessage setDeleteAfterDelivery(boolean deleteAfterDelivery) {
    Assertions.checkState(!isSent);
    this.deleteAfterDelivery = deleteAfterDelivery;
    return this;
  }

  /** Returns whether the message will be deleted after delivery. */
  public boolean getDeleteAfterDelivery() {
    return deleteAfterDelivery;
  }

  /**
   * Sends the message. If the target throws an {@link ExoPlaybackException} then it is propagated
   * out of the player as an error using {@link
   * Player.EventListener#onPlayerError(ExoPlaybackException)}.
   *
   * @return This message.
   * @throws IllegalStateException If this message has already been sent.
   */
  public PlayerMessage send() {
    Assertions.checkState(!isSent);
    if (positionMs == C.TIME_UNSET) {
      Assertions.checkArgument(deleteAfterDelivery);
    }
    isSent = true;
    sender.sendMessage(this);
    return this;
  }

  /**
   * Cancels the message delivery.
   *
   * @return This message.
   * @throws IllegalStateException If this method is called before {@link #send()}.
   */
  public synchronized PlayerMessage cancel() {
    Assertions.checkState(isSent);
    isCanceled = true;
    markAsProcessed(/* isDelivered= */ false);
    return this;
  }

  /** Returns whether the message delivery has been canceled. */
  public synchronized boolean isCanceled() {
    return isCanceled;
  }

  /**
   * Blocks until after the message has been delivered or the player is no longer able to deliver
   * the message.
   *
   * <p>Note that this method can't be called if the current thread is the same thread used by the
   * message handler set with {@link #setHandler(Handler)} as it would cause a deadlock.
   *
   * @return Whether the message was delivered successfully.
   * @throws IllegalStateException If this method is called before {@link #send()}.
   * @throws IllegalStateException If this method is called on the same thread used by the message
   *     handler set with {@link #setHandler(Handler)}.
   * @throws InterruptedException If the current thread is interrupted while waiting for the message
   *     to be delivered.
   */
  public synchronized boolean blockUntilDelivered() throws InterruptedException {
    Assertions.checkState(isSent);
    Assertions.checkState(handler.getLooper().getThread() != Thread.currentThread());
    while (!isProcessed) {
      wait();
    }
    return isDelivered;
  }

  /**
   * Marks the message as processed. Should only be called by a {@link Sender} and may be called
   * multiple times.
   *
   * @param isDelivered Whether the message has been delivered to its target. The message is
   *     considered as being delivered when this method has been called with {@code isDelivered} set
   *     to true at least once.
   */
  public synchronized void markAsProcessed(boolean isDelivered) {
    this.isDelivered |= isDelivered;
    isProcessed = true;
    notifyAll();
  }

  /**
   * Blocks until after the message has been delivered or the player is no longer able to deliver
   * the message or the specified waiting time elapses.
   *
   * <p>Note that this method can't be called if the current thread is the same thread used by the
   * message handler set with {@link #setHandler(Handler)} as it would cause a deadlock.
   *
   * @param timeoutMs the maximum time to wait in milliseconds.
   * @return Whether the message was delivered successfully.
   * @throws IllegalStateException If this method is called before {@link #send()}.
   * @throws IllegalStateException If this method is called on the same thread used by the message
   *     handler set with {@link #setHandler(Handler)}.
   * @throws TimeoutException If the waiting time elapsed and this message has not been delivered
   *     and the player is still able to deliver the message.
   * @throws InterruptedException If the current thread is interrupted while waiting for the message
   *     to be delivered.
   */
  public synchronized boolean experimentalBlockUntilDelivered(long timeoutMs)
      throws InterruptedException, TimeoutException {
    return experimentalBlockUntilDelivered(timeoutMs, Clock.DEFAULT);
  }

  @VisibleForTesting()
  /* package */ synchronized boolean experimentalBlockUntilDelivered(long timeoutMs, Clock clock)
      throws InterruptedException, TimeoutException {
    Assertions.checkState(isSent);
    Assertions.checkState(handler.getLooper().getThread() != Thread.currentThread());

    long deadlineMs = clock.elapsedRealtime() + timeoutMs;
    long remainingMs = timeoutMs;
    while (!isProcessed && remainingMs > 0) {
      wait(remainingMs);
      remainingMs = deadlineMs - clock.elapsedRealtime();
    }

    if (!isProcessed) {
      throw new TimeoutException("Message delivery timed out.");
    }

    return isDelivered;
  }
}
