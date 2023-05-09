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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.github.inpefess.tptp_grpc.tptp_proto.Function;
import com.github.inpefess.tptp_grpc.tptp_proto.Term;
import com.github.inpefess.tptp_grpc.tptp_proto.Variable;
import com.google.common.graph.Graph;
import com.google.common.graph.Graphs;
import com.google.common.graph.MutableGraph;

class Graph2TPTPProto<NodeType> {
  public Function toProto(Graph<NodeType> graph) throws GraphTraversalException, IOException {
    NodeType startingNode = getTheStartingNode(graph);
    return applicationNode(graph, startingNode);
  }

  private String getFunctionOrVariableName(Graph<NodeType> graph, NodeType node, NodeType nextNode)
      throws GraphTraversalException {
    ArrayList<NodeType> successors = new ArrayList<NodeType>();
    successors.addAll(graph.successors(node));
    if (nextNode != null) {
      successors.remove(nextNode);
    }
    if (successors.size() != 1) {
      throw new GraphTraversalException("UniqueName node must have exactly one successor, but "
          + node.toString() + " has " + successors.size());
    }
    NodeType nameNode = successors.iterator().next();
    if (graph.outDegree(nameNode) != 0) {
      throw new GraphTraversalException("Name node must be terminal, but " + nameNode + " has "
          + graph.outDegree(nameNode) + " successors");
    }
    return nameNode.toString();
  }

  private List<Term> argumentsNode(Graph<NodeType> graph, NodeType node)
      throws GraphTraversalException {
    MutableGraph<NodeType> argumentsGraph = Graphs.inducedSubgraph(graph, graph.successors(node));
    List<Term> terms = new ArrayList<>();
    if (argumentsGraph.nodes().size() == 0) {
      return terms;
    }
    NodeType startingNode = getAnyStartingNode(argumentsGraph);
    List<NodeType> arguments = new ArrayList<>();
    if (argumentsGraph.outDegree(startingNode) == 0) {
      arguments = parseIsolatedArguments(argumentsGraph, startingNode);
    } else {
      arguments = parseLinkedListArguments(argumentsGraph, startingNode);
    }
    for (NodeType argumentNode : arguments) {
      terms.add(graphTerm2Proto(graph, argumentsGraph, argumentNode));
    }
    return terms;
  }

  private Term graphTerm2Proto(Graph<NodeType> graph, Graph<NodeType> argumentsGraph,
      NodeType argumentNode) {
    Term.Builder term = Term.newBuilder();
    try {
      Variable.Builder variable = Variable.newBuilder();
      Iterator<NodeType> localSuccessors = argumentsGraph.successors(argumentNode).iterator();
      NodeType localSuccessor = localSuccessors.hasNext() ? localSuccessors.next() : null;
      variable.setName(getFunctionOrVariableName(graph, argumentNode, localSuccessor));
      term.setVariable(variable.build());
    } catch (GraphTraversalException e) {
      term.setFunction(applicationNode(graph, argumentNode));
    }
    return term.build();
  }

  private List<NodeType> parseLinkedListArguments(Graph<NodeType> argumentsGraph,
      NodeType startingNode) {
    List<NodeType> arguments = new ArrayList<>();
    arguments.add(startingNode);
    NodeType currentNode = startingNode;
    while (argumentsGraph.outDegree(currentNode) > 0) {
      if (argumentsGraph.outDegree(currentNode) != 1) {
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

  private List<NodeType> parseIsolatedArguments(Graph<NodeType> argumentsGraph,
      NodeType startingNode) {
    List<NodeType> arguments = new ArrayList<>();
    if (argumentsGraph.nodes().size() != 1) {
      for (NodeType argumentNode : argumentsGraph.nodes()) {
        if (argumentsGraph.outDegree(argumentNode) == 0) {
          arguments.add(argumentNode);
        } else {
          throw new GraphTraversalException(
              "Arguments must be either isolated or forming a linked list.");
        }
      }
    } else {
      arguments.add(startingNode);
    }
    return arguments;
  }

  private Function applicationNode(Graph<NodeType> graph, NodeType node)
      throws GraphTraversalException {
    Function.Builder tptpProto = Function.newBuilder();
    if (graph.outDegree(node) != 2) {
      throw new GraphTraversalException("Application node must have exactly two successors, but "
          + node.toString() + " has " + graph.outDegree(node));
    }
    Iterator<NodeType> successors = graph.successors(node).iterator();
    NodeType firstSuccessor = successors.next();
    NodeType secondSuccessor = successors.next();
    try {
      tptpProto.setName(getFunctionOrVariableName(graph, firstSuccessor, null));
      tptpProto.addAllArgument(argumentsNode(graph, secondSuccessor));
    } catch (GraphTraversalException e) {
      tptpProto.setName(getFunctionOrVariableName(graph, secondSuccessor, null));
      tptpProto.addAllArgument(argumentsNode(graph, firstSuccessor));
    }
    return tptpProto.build();
  }

  private NodeType getTheStartingNode(Graph<NodeType> graph) throws GraphTraversalException {
    NodeType startingNode = null;
    for (NodeType node : graph.nodes()) {
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

  private NodeType getAnyStartingNode(Graph<NodeType> graph) throws GraphTraversalException {
    for (NodeType node : graph.nodes()) {
      if (graph.inDegree(node) == 0) {
        return node;
      }
    }
    throw new GraphTraversalException("no starting node");
  }
}
