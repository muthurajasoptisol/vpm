package com.adt.vpm.videoplayer.source.rtsp;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import com.adt.vpm.videoplayer.source.extractor.UnsupportedFormatException;
import com.adt.vpm.videoplayer.source.common.util.Assertions;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.util.concurrent.CancellationException;

/**
 * Thrown when a rtsp playback failure occurs.
 */
public final class RtspMediaException extends Exception {

  @Retention(RetentionPolicy.SOURCE)
  @IntDef(value = {UNSUPPORTED_PROTOCOL, UNSUPPORTED_FORMAT, LOAD_CANCELLED, LOAD_FAILED,
      LOAD_TIMEOUT})
  @interface Type {}
  /**
   * The error occurred opening media from a {@link RtspSampleStreamWrapper}.
   * <p>
   * Call {@link #getUnsupportedProtocolException()} ()} to retrieve the underlying cause.
   */
  static final int UNSUPPORTED_PROTOCOL = 1;
  /**
   * The error occurred creating extractor from a {@link RtspSampleStreamWrapper}.
   * <p>
   * Call {@link #getUnsupportedFormatException()} ()} to retrieve the underlying cause.
   */
  static final int UNSUPPORTED_FORMAT = 2;
  /**
   * The error occurred when loading is canceled from a {@link RtspSampleStreamWrapper}.
   * <p>
   * Call {@link #getLoadCancelledException()} ()} to retrieve the underlying cause.
   */
  static final int LOAD_CANCELLED = 3;
  /**
   * The error occurred loading data from a {@link RtspSampleStreamWrapper}.
   * <p>
   * Call {@link #getLoadFailedException()} to retrieve the underlying cause.
   */
  static final int LOAD_FAILED = 4;
  /**
   * The error occurred loading data from a {@link RtspSampleStreamWrapper}.
   * <p>
   * Call {@link #getLoadTimeoutException()} to retrieve the underlying cause.
   */
  static final int LOAD_TIMEOUT = 5;


  /** The {@link Type} of the playback failure. */
  @Type public final int type;

  @Nullable
  private final Throwable cause;

  /**
   * Creates an instance of type {@link #UNSUPPORTED_PROTOCOL}.
   *
   * @param cause The cause of the failure.
   * @return The created instance.
   */
  static RtspMediaException createForUnsupportedProtocol(ProtocolException cause) {
    return new RtspMediaException(UNSUPPORTED_PROTOCOL, cause);
  }

  /**
   * Creates an instance of type {@link #UNSUPPORTED_PROTOCOL}.
   *
   * @param cause The cause of the failure.
   * @return The created instance.
   */
  static RtspMediaException createForUnsupportedFormat(NullPointerException cause) {
    return new RtspMediaException(LOAD_FAILED, cause);
  }

  /**
   * Creates an instance of type {@link #UNSUPPORTED_PROTOCOL}.
   *
   * @param cause The cause of the failure.
   * @return The created instance.
   */
  static RtspMediaException createForCancellation(CancellationException cause) {
    return new RtspMediaException(LOAD_CANCELLED, cause);
  }

  /**
   * Creates an instance of type {@link #LOAD_TIMEOUT}.
   *
   * @param cause The cause of the failure.
   * @return The created instance.
   */
  static RtspMediaException createForReadDataTimeout(SocketTimeoutException cause) {
    return new RtspMediaException(LOAD_TIMEOUT, cause);
  }

  /**
   * Creates an instance of type {@link #LOAD_FAILED}.
   *
   * @param cause The cause of the failure.
   * @return The created instance.
   */
  static RtspMediaException createForReadDataFailed(IOException cause) {
    return new RtspMediaException(LOAD_FAILED, cause);
  }

  /**
   * Creates an instance of type {@link #UNSUPPORTED_PROTOCOL}.
   *
   * @param cause The cause of the failure.
   * @return The created instance.
   */
  static RtspMediaException createForUnsupportedFormat(UnsupportedFormatException cause) {
    return new RtspMediaException(UNSUPPORTED_FORMAT, cause);
  }

  private RtspMediaException(int type, @Nullable Throwable cause) {
    this.type = type;
    this.cause = cause;
  }

  /**
   * Retrieves the underlying error when {@link #type} is {@link #UNSUPPORTED_PROTOCOL}.
   *
   * @throws IllegalStateException If {@link #type} is not {@link #UNSUPPORTED_PROTOCOL}.
   */
  public ProtocolException getUnsupportedProtocolException() {
    Assertions.checkState(type == UNSUPPORTED_PROTOCOL);
    return (ProtocolException) Assertions.checkNotNull(cause);
  }

  /**
   * Retrieves the underlying error when {@link #type} is {@link #UNSUPPORTED_FORMAT}.
   *
   * @throws IllegalStateException If {@link #type} is not {@link #UNSUPPORTED_FORMAT}.
   */
  public ProtocolException getUnsupportedFormatException() {
    Assertions.checkState(type == UNSUPPORTED_FORMAT);
    return (ProtocolException) Assertions.checkNotNull(cause);
  }

  /**
   * Retrieves the underlying error when {@link #type} is {@link #LOAD_CANCELLED}.
   *
   * @throws IllegalStateException If {@link #type} is not {@link #LOAD_CANCELLED}.
   */
  public IOException getLoadCancelledException() {
    Assertions.checkState(type == LOAD_CANCELLED);
    return (IOException) Assertions.checkNotNull(cause);
  }

  /**
   * Retrieves the underlying error when {@link #type} is {@link #LOAD_FAILED}.
   *
   * @throws IllegalStateException If {@link #type} is not {@link #LOAD_FAILED}.
   */
  public IOException getLoadFailedException() {
    Assertions.checkState(type == LOAD_FAILED);
    return (IOException) Assertions.checkNotNull(cause);
  }

  /**
   * Retrieves the underlying error when {@link #type} is {@link #LOAD_TIMEOUT}.
   *
   * @throws IllegalStateException If {@link #type} is not {@link #LOAD_TIMEOUT}.
   */
  public SocketTimeoutException getLoadTimeoutException() {
    Assertions.checkState(type == LOAD_TIMEOUT);
    return (SocketTimeoutException) Assertions.checkNotNull(cause);
  }
}
