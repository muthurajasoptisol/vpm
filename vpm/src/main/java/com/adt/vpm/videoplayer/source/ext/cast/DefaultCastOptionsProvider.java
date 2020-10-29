/*
 * Created by ADT author on 10/19/20 4:50 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/22/20 6:23 PM
 */
package com.adt.vpm.videoplayer.source.ext.cast;

import android.content.Context;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.framework.CastOptions;
import com.google.android.gms.cast.framework.OptionsProvider;
import com.google.android.gms.cast.framework.SessionProvider;
import java.util.Collections;
import java.util.List;

/**
 * A convenience {@link OptionsProvider} to target the default cast receiver app.
 */
public final class DefaultCastOptionsProvider implements OptionsProvider {

  /**
   * App id of the Default Media Receiver app. Apps that do not require DRM support may use this
   * receiver receiver app ID.
   *
   * <p>See https://developers.google.com/cast/docs/caf_receiver/#default_media_receiver.
   */
  public static final String APP_ID_DEFAULT_RECEIVER =
      CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID;

  /**
   * App id for receiver app with rudimentary support for DRM.
   *
   * <p>This app id is only suitable for ExoPlayer's Cast Demo app, and it is not intended for
   * production use. In order to use DRM, custom receiver apps should be used. For environments that
   * do not require DRM, the default receiver app should be used (see {@link
   * #APP_ID_DEFAULT_RECEIVER}).
   */
  // TODO: Add a documentation resource link for DRM support in the receiver app [Internal ref:
  // b/128603245].
  public static final String APP_ID_DEFAULT_RECEIVER_WITH_DRM = "A12D4273";

  @Override
  public CastOptions getCastOptions(Context context) {
    return new CastOptions.Builder()
        .setReceiverApplicationId(APP_ID_DEFAULT_RECEIVER_WITH_DRM)
        .setStopReceiverApplicationWhenEndingSession(true)
        .build();
  }

  @Override
  public List<SessionProvider> getAdditionalSessionProviders(Context context) {
    return Collections.emptyList();
  }

}
