/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.trackselection;

import androidx.annotation.Nullable;

import com.adt.vpm.videoplayer.source.core.source.TrackGroupArray;
import com.adt.vpm.videoplayer.source.core.trackselection.TrackSelection.Definition;

import org.checkerframework.checker.nullness.compatqual.NullableType;

/** Track selection related utility methods. */
public final class TrackSelectionUtil {

  private TrackSelectionUtil() {}

  /** Functional interface to create a single adaptive track selection. */
  public interface AdaptiveTrackSelectionFactory {

    /**
     * Creates an adaptive track selection for the provided track selection definition.
     *
     * @param trackSelectionDefinition A {@link Definition} for the track selection.
     * @return The created track selection.
     */
    TrackSelection createAdaptiveTrackSelection(Definition trackSelectionDefinition);
  }

  /**
   * Creates track selections for an array of track selection definitions, with at most one
   * multi-track adaptive selection.
   *
   * @param definitions The list of track selection {@link Definition definitions}. May include null
   *     values.
   * @param adaptiveTrackSelectionFactory A factory for the multi-track adaptive track selection.
   * @return The array of created track selection. For null entries in {@code definitions} returns
   *     null values.
   */
  public static @NullableType TrackSelection[] createTrackSelectionsForDefinitions(
      @NullableType Definition[] definitions,
      AdaptiveTrackSelectionFactory adaptiveTrackSelectionFactory) {
    TrackSelection[] selections = new TrackSelection[definitions.length];
    boolean createdAdaptiveTrackSelection = false;
    for (int i = 0; i < definitions.length; i++) {
      Definition definition = definitions[i];
      if (definition == null) {
        continue;
      }
      if (definition.tracks.length > 1 && !createdAdaptiveTrackSelection) {
        createdAdaptiveTrackSelection = true;
        selections[i] = adaptiveTrackSelectionFactory.createAdaptiveTrackSelection(definition);
      } else {
        selections[i] =
            new FixedTrackSelection(
                definition.group, definition.tracks[0], definition.reason, definition.data);
      }
    }
    return selections;
  }

  /**
   * Updates {@link DefaultTrackSelector.Parameters} with an override.
   *
   * @param parameters The current {@link DefaultTrackSelector.Parameters} to build upon.
   * @param rendererIndex The renderer index to update.
   * @param trackGroupArray The {@link TrackGroupArray} of the renderer.
   * @param isDisabled Whether the renderer should be set disabled.
   * @param override An optional override for the renderer. If null, no override will be set and an
   *     existing override for this renderer will be cleared.
   * @return The updated {@link DefaultTrackSelector.Parameters}.
   */
  public static DefaultTrackSelector.Parameters updateParametersWithOverride(
      DefaultTrackSelector.Parameters parameters,
      int rendererIndex,
      TrackGroupArray trackGroupArray,
      boolean isDisabled,
      @Nullable DefaultTrackSelector.SelectionOverride override) {
    DefaultTrackSelector.ParametersBuilder builder =
        parameters
            .buildUpon()
            .clearSelectionOverrides(rendererIndex)
            .setRendererDisabled(rendererIndex, isDisabled);
    if (override != null) {
      builder.setSelectionOverride(rendererIndex, trackGroupArray, override);
    }
    return builder.build();
  }
}
