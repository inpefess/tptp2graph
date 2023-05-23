/*
 *  Copyright 2023 Boris Shminke
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/

package com.github.inpefess.tptp2graph.tptp2graph;

import java.util.HashMap;
import com.github.inpefess.tptp2graph.graphproto.DglGraph;
import com.github.inpefess.tptp2graph.graphproto.NodeData;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.Graph;

class Graph2DglProto<NodeT> {
  private Graph<NodeT> graph;
  private HashMap<NodeT, NodeKind> nodeKinds;
  DglGraph.Builder dglGraph;

  public Graph2DglProto(Graph<NodeT> graph, HashMap<NodeT, NodeKind> nodeKinds) {
    this.graph = graph;
    this.nodeKinds = nodeKinds;
  }

  public DglGraph toDglProto() {
    dglGraph = DglGraph.newBuilder();
    HashMap<NodeT, Integer> nodeIndex = addNodes();
    for (EndpointPair<NodeT> edge : graph.edges()) {
      dglGraph.addSource(nodeIndex.get(edge.source()));
      dglGraph.addTarget(nodeIndex.get(edge.target()));
    }
    return dglGraph.build();
  }

  private HashMap<NodeT, Integer> addNodes() {
    HashMap<NodeT, Integer> nodeIndex = new HashMap<>();
    int index = 0;
    for (NodeT node : graph.nodes()) {
      nodeIndex.put(node, index++);
      dglGraph.addNdata(getNodeData(index, nodeKinds.get(node)));
    }
    return nodeIndex;
  }

  private NodeData getNodeData(int index, NodeKind nodeKind) {
    NodeData.Builder ndata = NodeData.newBuilder();
    for (NodeKind possibleNodeKind : NodeKind.values()) {
      ndata.addFeature(possibleNodeKind == nodeKind ? 1 : 0);
    }
    return ndata.build();
  }
}
