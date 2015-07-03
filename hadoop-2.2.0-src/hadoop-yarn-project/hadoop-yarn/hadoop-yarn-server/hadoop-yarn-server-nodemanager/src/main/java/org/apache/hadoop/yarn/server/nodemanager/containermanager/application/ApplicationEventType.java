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

package org.apache.hadoop.yarn.server.nodemanager.containermanager.application;

public enum ApplicationEventType {

  // Source: ContainerManager
	/**
	 * NodeManager收到来自某个Application的第一个container，则会触发一个INIT_APPLICATION事件，
	 * 同时使Application状态由初始状态NEW转换为INITING。
	 */
  INIT_APPLICATION,
  
  /**
   * NodeManager收到一个ApplicationMaster启动container的请求（通过RPC函数ContainerManager.startContainer()）后，会触发一个INIT_CONTAINER事件。
   */
  INIT_CONTAINER,
  
  /**
   * NodeManager通过心跳机制收到ResourceManager发送的待清理的Application列表后，
   * 会为这些application发送一个FINISH_APPLICATION事件。
   */
  FINISH_APPLICATION, // Source: LogAggregationService if init fails

  // Source: ResourceLocalizationService
  /**
   * Application本地化完成（在每个NodeManager上，对于同一个Application，
   * 由第一个container负责Application级别的本地化工作，后续的container只需负责自己的本地化工作。
   * 本地化涉及到的主要工作是准备执行环境，包括准备各种jar包，二进制文件，外部文件等。）
   */
  APPLICATION_INITED,
  
  /**
   * NodeManager清理Application占用的临时目录，该过程与Application本地化是一对逆过程。
   */
  APPLICATION_RESOURCES_CLEANEDUP,

  /**
   * 该Application的一个container退出（可能运行失败，也可能运行成功。）
   */
  // Source: Container
  APPLICATION_CONTAINER_FINISHED,

  // Source: Log Handler
  /**
   * Application触发INIT_APPLICATION事件的同时，会执行一个函数，
   * 该函数会进一步触发APPLICATION_LOG_HANDLING_INITED事件。
   */
  APPLICATION_LOG_HANDLING_INITED,
  /**
   * Application运行完成，资源得到回收后，会触发一个APPLICATION_LOG_HANDLING_FINISHED事件，销毁log句柄。
下图描述了以上各个事件的来源：
   */
  APPLICATION_LOG_HANDLING_FINISHED,
  APPLICATION_LOG_HANDLING_FAILED
}
