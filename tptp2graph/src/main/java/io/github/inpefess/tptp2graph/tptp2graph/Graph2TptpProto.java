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

import java.util.ArrayList;
import java.util.List;
import io.github.inpefess.tptpgrpc.tptpproto.Node;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.Graphs;
import com.google.common.graph.ImmutableValueGraph;
import com.google.common.graph.ValueGraph;

final class Graph2TptpProto {
  private final ImmutableValueGraph<LabeledNode, EdgeKind> graph;

  public Graph2TptpProto(final ValueGraph<LabeledNode, EdgeKind> graph) {
    this.graph = ImmutableValueGraph.copyOf(graph);
  }

  public final Node toProto() throws GraphTraversalException {
    return parseNode(getTheStartingNode(graph));
  }

  private final Node parseNode(final LabeledNode labeledNode) {
    final Node.Builder tptpProto = Node.newBuilder();
    tptpProto.setValue(labeledNode.label);
    final List<LabeledNode> childNodes = new ArrayList<>();
    for (final EndpointPair<LabeledNode> edge : graph.incidentEdges(labeledNode)) {
      if (graph.edgeValue(edge).get() == EdgeKind.AST && edge.target() != labeledNode) {
        childNodes.add(edge.target());
      }
    }
    final List<LabeledNode> sortedChildNodes = getSortedChildNodes(childNodes);
    for (final LabeledNode childNode : sortedChildNodes) {
      tptpProto.addChild(parseNode(childNode));
    }
    return tptpProto.build();
  }

  private final List<LabeledNode> getSortedChildNodes(final List<LabeledNode> childNodes) {
    final ValueGraph<LabeledNode, EdgeKind> childNodesGraph =
        Graphs.inducedSubgraph(graph, childNodes);
    List<LabeledNode> sortedChildNodes = List.copyOf(childNodesGraph.nodes());
    if (childNodesGraph.nodes().size() > 1) {
      final LabeledNode anyStartingNode = getAnyStartingNode(childNodesGraph);
      if (childNodesGraph.outDegree(anyStartingNode) > 0) {
        sortedChildNodes = parseLinkedListArguments(childNodesGraph, anyStartingNode);
      }
    }
    return sortedChildNodes;
  }

  private static final LabeledNode getTheStartingNode(final ValueGraph<LabeledNode, EdgeKind> graph)
      throws GraphTraversalException {
    LabeledNode startingNode = null;
    for (final LabeledNode node : graph.nodes()) {
      if (graph.inDegree(node) == 0) {
        if (startingNode != null) {
          throw new GraphTraversalException("There are at least two starting nodes: "
              + startingNode.toString() + " and " + node.toString());
        }
        startingNode = node;
      }
    }
    if (startingNode == null) {
      throw new GraphTraversalException("no starting node");
    }
    return startingNode;
  }

  private static final LabeledNode getAnyStartingNode(final ValueGraph<LabeledNode, EdgeKind> graph)
      throws GraphTraversalException {
    for (final LabeledNode node : graph.nodes()) {
      if (graph.inDegree(node) == 0) {
        return node;
      }
    }
    throw new GraphTraversalException("no starting node");
  }

  private List<LabeledNode> parseLinkedListArguments(
      final ValueGraph<LabeledNode, EdgeKind> argumentsGraph, final LabeledNode startingNode) {
    final List<LabeledNode> arguments = new ArrayList<>();
    arguments.add(startingNode);
    LabeledNode currentNode = startingNode;
    while (argumentsGraph.outDegree(currentNode) > 0) {
      if (argumentsGraph.outDegree(currentNode) != 1
          || argumentsGraph.edgeValue(argumentsGraph.incidentEdges(currentNode).iterator().next())
              .get() != EdgeKind.NCS) {
        throw new GraphTraversalException(
            "Arguments must be either isolated or forming a linked list.");
      }
      currentNode = argumentsGraph.successors(currentNode).iterator().next();
      arguments.add(currentNode);
    }
    if (arguments.size() != argumentsGraph.nodes().size()) {
      throw new GraphTraversalException(
          "Arguments must be either isolated or forming a linked list.");
    }
    return arguments;
  }
}
