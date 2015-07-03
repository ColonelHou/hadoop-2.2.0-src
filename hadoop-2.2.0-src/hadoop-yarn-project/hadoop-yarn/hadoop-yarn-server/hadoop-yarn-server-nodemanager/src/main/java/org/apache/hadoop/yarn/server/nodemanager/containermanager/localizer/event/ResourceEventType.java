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
package org.apache.hadoop.yarn.server.nodemanager.containermanager.localizer.event;

import org.apache.hadoop.yarn.server.nodemanager.containermanager.localizer.LocalizedResource;

/**
 * Events delivered to {@link LocalizedResource}. Each of these
 * events is a subclass of {@link ResourceEvent}.
 */
public enum ResourceEventType {
  /** See {@link ResourceRequestEvent} */
	/**
	 * 当需要为container下载某种资源时（比如jar包或者字典文件，
	 * 这些文件一般位于HDFS上，需要在container执行前下载到本地），会发出一个REQUEST事件。
	 */
  REQUEST,
  
  /** See {@link ResourceLocalizedEvent} */ 
  /**
   * 一种资源下载成功后，会触发一个LOCALIZED事件。
   */
  LOCALIZED,
  
  /**
   * 当Container执行完成（可能成功或者失败）后，会触发一个RELEASE事件，已清理各种存放在本地的资源。
   */
  /** See {@link ResourceReleaseEvent} */
  RELEASE,
  /** See {@link ResourceFailedLocalizationEvent} */
  LOCALIZATION_FAILED
}
