/**
 * Copyright (c) 2019. Qubole Inc
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an AS IS BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. See accompanying LICENSE file.
 */
package com.qubole.rubix.core.utils;

import com.qubole.rubix.spi.ClusterManager;
import com.qubole.rubix.spi.ClusterType;
import com.qubole.rubix.spi.thrift.ClusterNode;
import com.qubole.rubix.spi.thrift.NodeState;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Abhishek on 6/8/18.
 */
public class DummyClusterManager extends ClusterManager
{
  private static long splitSize = 64 * 1024 * 1024;
  @Override
  public List<ClusterNode> getNodes()
  {
    List<ClusterNode> list = new ArrayList<>();
    String hostName = "";
    try {
      hostName = InetAddress.getLocalHost().getHostAddress();
    }
    catch (UnknownHostException e) {
      hostName = "localhost";
    }

    list.add(new ClusterNode(hostName, NodeState.ACTIVE));

    return list;
  }

  @Override
  public ClusterType getClusterType()
  {
    return ClusterType.TEST_CLUSTER_MANAGER;
  }
}
