/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Calculate any percentile over a sliding window of weighted values. A maximum weight is
 * configured. Once the total weight of the values reaches the maximum weight, the oldest value is
 * reduced in weight until it reaches zero and is removed. This maintains a constant total weight,
 * equal to the maximum allowed, at the steady state.
 * <p>
 * This class can be used for bandwidth estimation based on a sliding window of past transfer rate
 * observations. This is an alternative to sliding mean and exponential averaging which suffer from
 * susceptibility to outliers and slow adaptation to step functions.
 *
 * @see <a href="http://en.wikipedia.org/wiki/Moving_average">Wiki: Moving average</a>
 * @see <a href="http://en.wikipedia.org/wiki/Selection_algorithm">Wiki: Selection algorithm</a>
 */
public class SlidingPercentile {

  // Orderings.
  private static final Comparator<Sample> INDEX_COMPARATOR = (a, b) -> a.index - b.index;
  private static final Comparator<Sample> VALUE_COMPARATOR =
      (a, b) -> Float.compare(a.value, b.value);

  private static final int SORT_ORDER_NONE = -1;
  private static final int SORT_ORDER_BY_VALUE = 0;
  private static final int SORT_ORDER_BY_INDEX = 1;

  private static final int MAX_RECYCLED_SAMPLES = 5;

  private final int maxWeight;
  private final ArrayList<Sample> samples;

  private final Sample[] recycledSamples;

  private int currentSortOrder;
  private int nextSampleIndex;
  private int totalWeight;
  private int recycledSampleCount;

  /**
   * @param maxWeight The maximum weight.
   */
  public SlidingPercentile(int maxWeight) {
    this.maxWeight = maxWeight;
    recycledSamples = new Sample[MAX_RECYCLED_SAMPLES];
    samples = new ArrayList<>();
    currentSortOrder = SORT_ORDER_NONE;
  }

  /** Resets the sliding percentile. */
  public void reset() {
    samples.clear();
    currentSortOrder = SORT_ORDER_NONE;
    nextSampleIndex = 0;
    totalWeight = 0;
  }

  /**
   * Adds a new weighted value.
   *
   * @param weight The weight of the new observation.
   * @param value The value of the new observation.
   */
  public void addSample(int weight, float value) {
    ensureSortedByIndex();

    Sample newSample = recycledSampleCount > 0 ? recycledSamples[--recycledSampleCount]
        : new Sample();
    newSample.index = nextSampleIndex++;
    newSample.weight = weight;
    newSample.value = value;
    samples.add(newSample);
    totalWeight += weight;

    while (totalWeight > maxWeight) {
      int excessWeight = totalWeight - maxWeight;
      Sample oldestSample = samples.get(0);
      if (oldestSample.weight <= excessWeight) {
        totalWeight -= oldestSample.weight;
        samples.remove(0);
        if (recycledSampleCount < MAX_RECYCLED_SAMPLES) {
          recycledSamples[recycledSampleCount++] = oldestSample;
        }
      } else {
        oldestSample.weight -= excessWeight;
        totalWeight -= excessWeight;
      }
    }
  }

  /**
   * Computes a percentile by integration.
   *
   * @param percentile The desired percentile, expressed as a fraction in the range (0,1].
   * @return The requested percentile value or {@link Float#NaN} if no samples have been added.
   */
  public float getPercentile(float percentile) {
    ensureSortedByValue();
    float desiredWeight = percentile * totalWeight;
    int accumulatedWeight = 0;
    for (int i = 0; i < samples.size(); i++) {
      Sample currentSample = samples.get(i);
      accumulatedWeight += currentSample.weight;
      if (accumulatedWeight >= desiredWeight) {
        return currentSample.value;
      }
    }
    // Clamp to maximum value or NaN if no values.
    return samples.isEmpty() ? Float.NaN : samples.get(samples.size() - 1).value;
  }

  /**
   * Sorts the samples by index.
   */
  private void ensureSortedByIndex() {
    if (currentSortOrder != SORT_ORDER_BY_INDEX) {
      Collections.sort(samples, INDEX_COMPARATOR);
      currentSortOrder = SORT_ORDER_BY_INDEX;
    }
  }

  /**
   * Sorts the samples by value.
   */
  private void ensureSortedByValue() {
    if (currentSortOrder != SORT_ORDER_BY_VALUE) {
      Collections.sort(samples, VALUE_COMPARATOR);
      currentSortOrder = SORT_ORDER_BY_VALUE;
    }
  }

  private static class Sample {

    public int index;
    public int weight;
    public float value;

  }

}
