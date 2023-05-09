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
import com.github.inpefess.tptp2graph.graph_proto.DGLGraph;
import com.github.inpefess.tptp2graph.graph_proto.NodeData;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.Graph;

class Graph2DGLProto<NodeType> {
  private Graph<NodeType> graph;
  private HashMap<NodeType, NodeKind> nodeKinds;
  DGLGraph.Builder dglGraph;

  public Graph2DGLProto(Graph<NodeType> graph, HashMap<NodeType, NodeKind> nodeKinds) {
    this.graph = graph;
    this.nodeKinds = nodeKinds;
  }

  public DGLGraph toDGLProto() {
    dglGraph = DGLGraph.newBuilder();
    HashMap<NodeType, Integer> nodeIndex = addNodes();
    for (EndpointPair<NodeType> edge : graph.edges()) {
      dglGraph.addSource(nodeIndex.get(edge.source()));
      dglGraph.addTarget(nodeIndex.get(edge.target()));
    }
    return dglGraph.build();
  }

  private HashMap<NodeType, Integer> addNodes() {
    HashMap<NodeType, Integer> nodeIndex = new HashMap<>();
    int index = 0;
    for (NodeType node : graph.nodes()) {
      nodeIndex.put(node, index++);
      dglGraph.addNdata(getNodeData(index, nodeKinds.get(node)));
    }
    return nodeIndex;
  }

  private NodeData getNodeData(int index, NodeKind nodeKind) {
    NodeData.Builder nData = NodeData.newBuilder();
    for (NodeKind possibleNodeKind : NodeKind.values()) {
      nData.addFeature(possibleNodeKind == nodeKind ? 1 : 0);
    }
    return nData.build();
  }
}
