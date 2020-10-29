/*
 * Created by ADT author on 9/29/20 7:02 PM
 * Copyright (C) 2020 ADT. All rights reserved.
 * Last modified 9/29/20 7:02 PM
 */
package com.adt.vpm.videoplayer.source.core.upstream;

import com.adt.vpm.videoplayer.source.core.util.PriorityTaskManager;
import com.adt.vpm.videoplayer.source.common.upstream.DataSource.Factory;

/**
 * A {@link DataSource.Factory} that produces {@link PriorityDataSource} instances.
 */
public final class PriorityDataSourceFactory implements Factory {

  private final Factory upstreamFactory;
  private final PriorityTaskManager priorityTaskManager;
  private final int priority;

  /**
   * @param upstreamFactory A {@link DataSource.Factory} to be used to create an upstream {@link
   *     DataSource} for {@link PriorityDataSource}.
   * @param priorityTaskManager The priority manager to which PriorityDataSource task is registered.
   * @param priority The priority of PriorityDataSource task.
   */
  public PriorityDataSourceFactory(Factory upstreamFactory, PriorityTaskManager priorityTaskManager,
      int priority) {
    this.upstreamFactory = upstreamFactory;
    this.priorityTaskManager = priorityTaskManager;
    this.priority = priority;
  }

  @Override
  public PriorityDataSource createDataSource() {
    return new PriorityDataSource(upstreamFactory.createDataSource(), priorityTaskManager,
        priority);
  }

}
