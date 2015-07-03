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

package org.apache.hadoop.yarn.server.resourcemanager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.classification.InterfaceAudience.Private;
import org.apache.hadoop.yarn.api.ContainerManagementProtocol;
import org.apache.hadoop.yarn.api.protocolrecords.GetContainerStatusesRequest;
import org.apache.hadoop.yarn.api.protocolrecords.GetContainerStatusesResponse;
import org.apache.hadoop.yarn.api.protocolrecords.StartContainerRequest;
import org.apache.hadoop.yarn.api.protocolrecords.StartContainersRequest;
import org.apache.hadoop.yarn.api.protocolrecords.StartContainersResponse;
import org.apache.hadoop.yarn.api.protocolrecords.StopContainersRequest;
import org.apache.hadoop.yarn.api.protocolrecords.StopContainersResponse;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.api.records.ContainerState;
import org.apache.hadoop.yarn.api.records.ContainerStatus;
import org.apache.hadoop.yarn.api.records.NodeId;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.api.records.Token;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.factories.RecordFactory;
import org.apache.hadoop.yarn.factory.providers.RecordFactoryProvider;
import org.apache.hadoop.yarn.ipc.RPCUtil;
import org.apache.hadoop.yarn.security.ContainerTokenIdentifier;
import org.apache.hadoop.yarn.server.api.protocolrecords.NodeHeartbeatRequest;
import org.apache.hadoop.yarn.server.api.protocolrecords.NodeHeartbeatResponse;
import org.apache.hadoop.yarn.server.api.protocolrecords.RegisterNodeManagerRequest;
import org.apache.hadoop.yarn.server.api.records.NodeHealthStatus;
import org.apache.hadoop.yarn.server.api.records.NodeStatus;
import org.apache.hadoop.yarn.server.resourcemanager.scheduler.common.fica.FiCaSchedulerNode;
import org.apache.hadoop.yarn.server.utils.BuilderUtils;
import org.apache.hadoop.yarn.util.resource.Resources;


/**
 * ResourceManager将Container分配给ApplicationMaster，ApplicationMaster再进一步要求对应的NodeManager启动container
 * nodemanager根据ApplicationMaster的要求启动相应的container (yarnchild). yarnchild再向am汇报.
 * 为防止ApplicationMaster未经授权随意要求NodeManager启动container，
 * ResourceManager一般会为每个container分配一个令牌（ApplicationMaster无法伪造），
 * 而NodeManager启动任何container之前均会对令牌的合法性进行验证，一旦通过验证后，NodeManager才会按照一定的流程启动该container
 * 
 * 
 * 定时向RM汇报(汇报给调度器)本节点上的资源使用情况和各个Container的运行状态
 * 接收并处理来自AM的Container启动/停止等各种要求
 * Container是YARN中的资源抽象，它封装了某个节点上的多维度资源
 * YARN会为每个任务分配一个Container，且改任务只能使用该Container中描述的资源
 * Container不同于MRv1的slot，它是一个动态资源划分单位，是根据应用程序的需求动态产生的
 * 
 * 
 * 用户提交的任何一个应用程序，在YARN中被称为Application。
 * 一个Application通常会被分解成多个任务并行执行，其中，每个任务要使用一定量的资源，这些资源被封装成container。
 * 详细说来，container不仅包含一个任务的资源说明，还包含很多其他信息，
 * 比如Container对应的节点、启动container所需的文件资源、环境变量和命令等信息。
 * 
 * NM管理:
 *     NMLivelinessMonitor
 *     NodesListManager
 *     ResourceTrackerService
 * @author John
 *
 */
@Private
public class NodeManager implements ContainerManagementProtocol {
  private static final Log LOG = LogFactory.getLog(NodeManager.class);
  private static final RecordFactory recordFactory = RecordFactoryProvider.getRecordFactory(null);
  
  final private String containerManagerAddress;
  final private String nodeHttpAddress;
  final private String rackName;
  final private NodeId nodeId;
  final private Resource capability;
  Resource available = recordFactory.newRecordInstance(Resource.class);
  Resource used = recordFactory.newRecordInstance(Resource.class);

  final ResourceTrackerService resourceTrackerService;
  final FiCaSchedulerNode schedulerNode;
  final Map<ApplicationId, List<Container>> containers = 
    new HashMap<ApplicationId, List<Container>>();
  
  final Map<Container, ContainerStatus> containerStatusMap =
      new HashMap<Container, ContainerStatus>();

  public NodeManager(String hostName, int containerManagerPort, int httpPort,
      String rackName, Resource capability,
      ResourceTrackerService resourceTrackerService, RMContext rmContext)
      throws IOException, YarnException {
    this.containerManagerAddress = hostName + ":" + containerManagerPort;
    this.nodeHttpAddress = hostName + ":" + httpPort;
    this.rackName = rackName;
    this.resourceTrackerService = resourceTrackerService;
    this.capability = capability;
    Resources.addTo(available, capability);

    this.nodeId = NodeId.newInstance(hostName, containerManagerPort);
    RegisterNodeManagerRequest request = recordFactory
        .newRecordInstance(RegisterNodeManagerRequest.class);
    request.setHttpPort(httpPort);
    request.setNodeId(this.nodeId);
    request.setResource(capability);
    request.setNodeId(this.nodeId);
    resourceTrackerService.registerNodeManager(request);
    this.schedulerNode = new FiCaSchedulerNode(rmContext.getRMNodes().get(
        this.nodeId), false);
   
    // Sanity check
    Assert.assertEquals(capability.getMemory(), 
       schedulerNode.getAvailableResource().getMemory());
    Assert.assertEquals(capability.getVirtualCores(), 
        schedulerNode.getAvailableResource().getVirtualCores());
  }
  
  public String getHostName() {
    return containerManagerAddress;
  }

  public String getRackName() {
    return rackName;
  }

  public NodeId getNodeId() {
    return nodeId;
  }

  public Resource getCapability() {
    return capability;
  }

  public Resource getAvailable() {
    return available;
  }
  
  public Resource getUsed() {
    return used;
  }
  
  int responseID = 0;
  
  private List<ContainerStatus> getContainerStatuses(Map<ApplicationId, List<Container>> containers) {
    List<ContainerStatus> containerStatuses = new ArrayList<ContainerStatus>();
    for (List<Container> appContainers : containers.values()) {
      for (Container container : appContainers) {
        containerStatuses.add(containerStatusMap.get(container));
      }
    }
    return containerStatuses;
  }
  public void heartbeat() throws IOException, YarnException {
    NodeStatus nodeStatus = 
      org.apache.hadoop.yarn.server.resourcemanager.NodeManager.createNodeStatus(
          nodeId, getContainerStatuses(containers));
    nodeStatus.setResponseId(responseID);
    NodeHeartbeatRequest request = recordFactory
        .newRecordInstance(NodeHeartbeatRequest.class);
    request.setNodeStatus(nodeStatus);
    NodeHeartbeatResponse response = resourceTrackerService
        .nodeHeartbeat(request);
    responseID = response.getResponseId();
  }

  @Override
  synchronized public StartContainersResponse startContainers(
      StartContainersRequest requests) 
  throws YarnException {

    for (StartContainerRequest request : requests.getStartContainerRequests()) {
      Token containerToken = request.getContainerToken();
      ContainerTokenIdentifier tokenId = null;

      try {
        tokenId = BuilderUtils.newContainerTokenIdentifier(containerToken);
      } catch (IOException e) {
        throw RPCUtil.getRemoteException(e);
      }

      ContainerId containerID = tokenId.getContainerID();
      ApplicationId applicationId =
          containerID.getApplicationAttemptId().getApplicationId();

      List<Container> applicationContainers = containers.get(applicationId);
      if (applicationContainers == null) {
        applicationContainers = new ArrayList<Container>();
        containers.put(applicationId, applicationContainers);
      }

      // Sanity check
      for (Container container : applicationContainers) {
        if (container.getId().compareTo(containerID) == 0) {
          throw new IllegalStateException("Container " + containerID
              + " already setup on node " + containerManagerAddress);
        }
      }

      Container container =
          BuilderUtils.newContainer(containerID, this.nodeId, nodeHttpAddress,
            tokenId.getResource(), null, null // DKDC - Doesn't matter
            );

      ContainerStatus containerStatus =
          BuilderUtils.newContainerStatus(container.getId(),
            ContainerState.NEW, "", -1000);
      applicationContainers.add(container);
      containerStatusMap.put(container, containerStatus);
      Resources.subtractFrom(available, tokenId.getResource());
      Resources.addTo(used, tokenId.getResource());

      if (LOG.isDebugEnabled()) {
        LOG.debug("startContainer:" + " node=" + containerManagerAddress
            + " application=" + applicationId + " container=" + container
            + " available=" + available + " used=" + used);
      }

    }
    StartContainersResponse response =
        StartContainersResponse.newInstance(null, null, null);
    return response;
  }

  synchronized public void checkResourceUsage() {
    LOG.info("Checking resource usage for " + containerManagerAddress);
    Assert.assertEquals(available.getMemory(), 
        schedulerNode.getAvailableResource().getMemory());
    Assert.assertEquals(used.getMemory(), 
        schedulerNode.getUsedResource().getMemory());
  }
  
  @Override
  synchronized public StopContainersResponse stopContainers(StopContainersRequest request) 
  throws YarnException {
    for (ContainerId containerID : request.getContainerIds()) {
      String applicationId =
          String.valueOf(containerID.getApplicationAttemptId()
            .getApplicationId().getId());

      // Mark the container as COMPLETE
      List<Container> applicationContainers = containers.get(applicationId);
      for (Container c : applicationContainers) {
        if (c.getId().compareTo(containerID) == 0) {
          ContainerStatus containerStatus = containerStatusMap.get(c);
          containerStatus.setState(ContainerState.COMPLETE);
          containerStatusMap.put(c, containerStatus);
        }
      }

      // Send a heartbeat
      try {
        heartbeat();
      } catch (IOException ioe) {
        throw RPCUtil.getRemoteException(ioe);
      }

      // Remove container and update status
      int ctr = 0;
      Container container = null;
      for (Iterator<Container> i = applicationContainers.iterator(); i
        .hasNext();) {
        container = i.next();
        if (container.getId().compareTo(containerID) == 0) {
          i.remove();
          ++ctr;
        }
      }

      if (ctr != 1) {
        throw new IllegalStateException("Container " + containerID
            + " stopped " + ctr + " times!");
      }

      Resources.addTo(available, container.getResource());
      Resources.subtractFrom(used, container.getResource());

      if (LOG.isDebugEnabled()) {
        LOG.debug("stopContainer:" + " node=" + containerManagerAddress
            + " application=" + applicationId + " container=" + containerID
            + " available=" + available + " used=" + used);
      }
    }
    return StopContainersResponse.newInstance(null,null);
  }

  @Override
  synchronized public GetContainerStatusesResponse getContainerStatuses(
      GetContainerStatusesRequest request) throws YarnException {
    List<ContainerStatus> statuses = new ArrayList<ContainerStatus>();
    for (ContainerId containerId : request.getContainerIds()) {
      List<Container> appContainers =
          containers.get(containerId.getApplicationAttemptId()
            .getApplicationId());
      Container container = null;
      for (Container c : appContainers) {
        if (c.getId().equals(containerId)) {
          container = c;
        }
      }
      if (container != null
          && containerStatusMap.get(container).getState() != null) {
        statuses.add(containerStatusMap.get(container));
      }
    }
    return GetContainerStatusesResponse.newInstance(statuses, null);
  }

  public static org.apache.hadoop.yarn.server.api.records.NodeStatus 
  createNodeStatus(NodeId nodeId, List<ContainerStatus> containers) {
    RecordFactory recordFactory = RecordFactoryProvider.getRecordFactory(null);
    org.apache.hadoop.yarn.server.api.records.NodeStatus nodeStatus = 
        recordFactory.newRecordInstance(org.apache.hadoop.yarn.server.api.records.NodeStatus.class);
    nodeStatus.setNodeId(nodeId);
    nodeStatus.setContainersStatuses(containers);
    NodeHealthStatus nodeHealthStatus = 
      recordFactory.newRecordInstance(NodeHealthStatus.class);
    nodeHealthStatus.setIsNodeHealthy(true);
    nodeStatus.setNodeHealthStatus(nodeHealthStatus);
    return nodeStatus;
  }
}