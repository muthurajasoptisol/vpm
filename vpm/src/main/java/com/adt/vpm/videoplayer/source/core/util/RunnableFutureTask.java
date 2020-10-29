/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.util;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.common.util.UnknownNull;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * A {@link RunnableFuture} that supports additional uninterruptible operations to query whether
 * execution has started and finished.
 *
 * @param <R> The type of the result.
 * @param <E> The type of any {@link ExecutionException} cause.
 */
public abstract class RunnableFutureTask<R, E extends Exception> implements RunnableFuture<R> {

  private final ConditionVariable started;
  private final ConditionVariable finished;
  private final Object cancelLock;

  @Nullable private Exception exception;
  @Nullable private R result;

  @Nullable private Thread workThread;
  private boolean canceled;

  protected RunnableFutureTask() {
    started = new ConditionVariable();
    finished = new ConditionVariable();
    cancelLock = new Object();
  }

  /** Blocks until the task has started, or has been canceled without having been started. */
  public final void blockUntilStarted() {
    started.blockUninterruptible();
  }

  /** Blocks until the task has finished, or has been canceled without having been started. */
  public final void blockUntilFinished() {
    finished.blockUninterruptible();
  }

  // Future implementation.

  @Override
  @UnknownNull
  public final R get() throws ExecutionException, InterruptedException {
    finished.block();
    return getResult();
  }

  @Override
  @UnknownNull
  public final R get(long timeout, TimeUnit unit)
      throws ExecutionException, InterruptedException, TimeoutException {
    long timeoutMs = MILLISECONDS.convert(timeout, unit);
    if (!finished.block(timeoutMs)) {
      throw new TimeoutException();
    }
    return getResult();
  }

  @Override
  public final boolean cancel(boolean interruptIfRunning) {
    synchronized (cancelLock) {
      if (canceled || finished.isOpen()) {
        return false;
      }
      canceled = true;
      cancelWork();
      @Nullable Thread workThread = this.workThread;
      if (workThread != null) {
        if (interruptIfRunning) {
          workThread.interrupt();
        }
      } else {
        started.open();
        finished.open();
      }
      return true;
    }
  }

  @Override
  public final boolean isDone() {
    return finished.isOpen();
  }

  @Override
  public final boolean isCancelled() {
    return canceled;
  }

  // Runnable implementation.

  @Override
  public final void run() {
    synchronized (cancelLock) {
      if (canceled) {
        return;
      }
      workThread = Thread.currentThread();
    }
    started.open();
    try {
      result = doWork();
    } catch (Exception e) {
      // Must be an instance of E or RuntimeException.
      exception = e;
    } finally {
      synchronized (cancelLock) {
        finished.open();
        workThread = null;
        // Clear the interrupted flag if set, to avoid it leaking into any subsequent tasks executed
        // using the calling thread.
        Thread.interrupted();
      }
    }
  }

  // Internal methods.

  /**
   * Performs the work or computation.
   *
   * @return The computed result.
   * @throws E If an error occurred.
   */
  @UnknownNull
  protected abstract R doWork() throws E;

  /**
   * Cancels any work being done by {@link #doWork()}. If {@link #doWork()} is currently executing
   * then the thread on which it's executing may be interrupted immediately after this method
   * returns.
   *
   * <p>The default implementation does nothing.
   */
  protected void cancelWork() {
    // Do nothing.
  }

  // The return value is guaranteed to be non-null if and only if R is a non-null type, but there's
  // no way to assert this. Suppress the warning instead.
  @SuppressWarnings("return.type.incompatible")
  @UnknownNull
  private R getResult() throws ExecutionException {
    if (canceled) {
      throw new CancellationException();
    } else if (exception != null) {
      throw new ExecutionException(exception);
    }
    return result;
  }
}
