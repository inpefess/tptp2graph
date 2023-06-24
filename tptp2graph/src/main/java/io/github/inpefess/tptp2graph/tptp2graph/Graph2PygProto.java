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

package io.github.inpefess.tptp2graph.tptp2graph;

import io.github.inpefess.tptp2graph.pygproto.Data;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.ValueGraph;

final class Graph2PygProto {
  public static final Data toPygProto(final ValueGraph<LabeledNode, EdgeKind> graph) {
    final Data.Builder pygData = Data.newBuilder();
    for (final EndpointPair<LabeledNode> edge : graph.edges()) {
      pygData.addEdgeIndexSource(edge.source().index);
      pygData.addEdgeIndexTarget(edge.target().index);
      pygData.addEdgeAttr(graph.edgeValue(edge).get().ordinal());
    }
    for (final LabeledNode node : graph.nodes()) {
      pygData.addX(node.kind.ordinal());
    }
    return pygData.build();
  }
}
