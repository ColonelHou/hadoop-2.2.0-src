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

package org.apache.hadoop.yarn.server.nodemanager.containermanager.container;

public enum ContainerEventType {

  // Producer: ContainerManager
	/**
	 * NodeManager收到来自某个ApplicationMaster的启动container请求
	 * （通过RPC函数ContainerManager.startContainer()），
	 * 则会触发一个INIT_CONTAINER事件，同时使container状态由初始状态NEW转换为LOCALIZING。
	 */
  INIT_CONTAINER,
  
  /**
   * 在多种场景下会触发产生KILL_CONTAINER事件，包括：
   * 1）ResourceManager要求NodeManager杀死一个container  
   * 2）Container使用的内存量超过约定值，被监控线程杀死 
   * 3）ApplicationMaster要求NodeManager杀死一个container（通过RPC函数ContainerManager.stopContainer()）
   * 4）Container正常运行结束（正如MRv1中Task结束后会收到KilledTaskAction一样，container结束也会收到KILL_CONTAINER）
   */
  KILL_CONTAINER,
  UPDATE_DIAGNOSTICS_MSG,
  CONTAINER_DONE,

  // DownloadManager
  CONTAINER_INITED,
  
  /**
   * Container完成本地化工作
   * （主要工作是从HDFS上下载各种文件，包括jar包，二进制文件和其他container执行过程中需用到的文件），
   * 会触发一个RESOURCE_LOCALIZED事件。
   */
  RESOURCE_LOCALIZED,
  
  /**
   * Container本地化过程中抛出异常，会触发一个RESOURCE_FAILED事件，导致Container失败。
   */
  RESOURCE_FAILED,
  
  /**
   * odeManager清理完成Container使用的各种临时目录，
   * 此时会触发一个CONTAINER_RESOURCES_CLEANEDUP事件，
   * 使得Container从EXITED_WITH_SUCCESS状态转换为DONE状态。
   */
  CONTAINER_RESOURCES_CLEANEDUP,

  // Producer: ContainersLauncher
  /**
   * Container成功启动后，会触发一个CONTAINER_LAUNCHED事件，
   * 使得Container从LOCALIZED状态转换为CONTAINER_LAUNCHED状态。
   */
  CONTAINER_LAUNCHED,
  
  /**
   * Container正常退出（Container实际上是一个shell命令，正常结束运行后会返回0），
   * 会触发一个CONTAINER_EXITED_WITH_SUCCESS事件。
   */
  CONTAINER_EXITED_WITH_SUCCESS,
  
  /**
   * Container异常退出（运行过程中抛出Throwable异常）
   */
  CONTAINER_EXITED_WITH_FAILURE,
  
  /**
   * Container运行过程中被强制杀死或者终止（返回码为137或者143）。
   */
  CONTAINER_KILLED_ON_REQUEST,
}
