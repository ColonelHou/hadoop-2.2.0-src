/**
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.apache.hadoop.yarn.server.resourcemanager.scheduler;

import java.io.IOException;

import org.apache.hadoop.classification.InterfaceAudience.LimitedPrivate;
import org.apache.hadoop.classification.InterfaceStability.Evolving;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.server.resourcemanager.RMContext;
import org.apache.hadoop.yarn.server.resourcemanager.recovery.Recoverable;

/**
 * This interface is the one implemented by the schedulers. It mainly extends 
 * 
 * 资源调度器
 * 它按照一定的约束条件（比如队列容量限制等）将集群中的资源分配给各个应用程序，当前主要考虑内存资源，
 * 在3.0版本中将会考虑CPU（https://issues.apache.org/jira/browse/YARN-2）。
 * ResourceScheduler是一个插拔式模块，默认是FIFO实现，
 * YARN还提供了Fair Scheduler和Capacity Scheduler两个多租户调度器。
 * 
 * {@link YarnScheduler}. 
 *
 */
@LimitedPrivate("yarn")
@Evolving
public interface ResourceScheduler extends YarnScheduler, Recoverable {
  /**
   * Re-initialize the <code>ResourceScheduler</code>.
   * @param conf configuration
   * @throws IOException
   */
  void reinitialize(Configuration conf, RMContext rmContext) throws IOException;
}
