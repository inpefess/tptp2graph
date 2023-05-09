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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import com.github.inpefess.tptp2graph.graph_proto.DGLGraph;
import com.github.inpefess.tptp_grpc.tptp_proto.Function;
import com.github.inpefess.tptp_grpc.tptp_proto.Term;
import com.github.inpefess.tptp_grpc.tptp_proto.Variable;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;

public class TPTPProto2Graph {
  private ImmutableGraph.Builder<String> tptpGraphBuilder;
  private final List<String> permutativeFunctions = Arrays.asList("$$$or", "$$$and", "=", "!=");
  private final List<String> logicalOperators = Arrays.asList("$$$or", "$$$and", "$$$not");
  private int uniqueIndex;
  private ImmutableGraph<String> tptpGraph;
  private HashMap<String, NodeKind> nodeKinds;

  public ImmutableGraph<String> getTptpGraph() {
    return tptpGraph;
  }

  public HashMap<String, NodeKind> getNodeKinds() {
    return nodeKinds;
  }

  private void addFunction(String applicationNode, Function function, boolean parentIsLogical) {
    String nameNode = "$$$placeholder" + uniqueIndex++;
    nodeKinds.put(nameNode, NodeKind.PLACEHOLDER);
    tptpGraphBuilder.putEdge(applicationNode, nameNode);
    tptpGraphBuilder.putEdge(nameNode, function.getName());
    boolean currentIsLogical = predicateOrElse(parentIsLogical, function.getName());
    String argumentsNode = "$$$argument_list" + uniqueIndex++;
    nodeKinds.put(argumentsNode, NodeKind.ARGUMENT_LIST);
    tptpGraphBuilder.putEdge(applicationNode, argumentsNode);
    String previousNode = permutativeFunctions.contains(function.getName()) ? null : argumentsNode;
    for (Term argument : function.getArgumentList()) {
      previousNode = addArgument(argumentsNode, previousNode, argument, currentIsLogical);
    }
  }

  private boolean predicateOrElse(boolean parentIsLogical, String functionName) {
    boolean isLogical = logicalOperators.contains(functionName);
    if (isLogical) {
      nodeKinds.put(functionName, NodeKind.OR);
    } else if (functionName == "$$$and") {
      nodeKinds.put(functionName, NodeKind.AND);
    } else if (functionName == "$$$not") {
      nodeKinds.put(functionName, NodeKind.NOT);
    } else {
      nodeKinds.put(functionName, parentIsLogical ? NodeKind.PREDICATE : NodeKind.FUNCTION);
    }
    return isLogical;
  }

  private String addArgument(String argumentsNode, String previousNode, Term argument,
      boolean isLogical) {
    String currentNode = "";
    if (argument.hasVariable()) {
      currentNode = addVariable(argument.getVariable());
    } else {
      currentNode = "$$$application" + uniqueIndex++;
      nodeKinds.put(currentNode, NodeKind.APPLICATION);
      addFunction(currentNode, argument.getFunction(), isLogical);
    }
    tptpGraphBuilder.putEdge(argumentsNode, currentNode);
    if (previousNode != null) {
      tptpGraphBuilder.putEdge(previousNode, currentNode);
      return currentNode;
    } else {
      return null;
    }
  }

  private String addVariable(Variable variable) {
    String currentNode = "$$$placeholder" + uniqueIndex++;
    nodeKinds.put(currentNode, NodeKind.PLACEHOLDER);
    tptpGraphBuilder.putEdge(currentNode, variable.getName());
    nodeKinds.put(variable.getName(), NodeKind.VARIABLE);
    return currentNode;
  }

  public TPTPProto2Graph(Function tptpProto) {
    tptpGraphBuilder = GraphBuilder.directed().allowsSelfLoops(false).<String>immutable();
    nodeKinds = new HashMap<>();
    uniqueIndex = 0;
    String initialNode = "$$$application" + uniqueIndex++;
    nodeKinds.put(initialNode, NodeKind.APPLICATION);
    addFunction(initialNode, tptpProto, true);
    tptpGraph = tptpGraphBuilder.build();
  }

  public static void main(String[] args) throws IOException {
    Scanner problemList = new Scanner(new FileInputStream(args[0]));
    int fileIndex = 0;
    while (problemList.hasNextLine()) {
      String outputFilename = Paths.get(args[1], fileIndex++ + ".pb").toString();
      Function tptpProto = Function.parseFrom(new FileInputStream(problemList.nextLine()));
      TPTPProto2Graph tptpProto2Graph = new TPTPProto2Graph(tptpProto);
      DGLGraph dglGraph = (new Graph2DGLProto<String>(tptpProto2Graph.getTptpGraph(),
          tptpProto2Graph.getNodeKinds())).toDGLProto();
      dglGraph.writeTo(new FileOutputStream(outputFilename));
    }
  }
}
