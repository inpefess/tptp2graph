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
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import com.github.inpefess.tptpgrpc.tptpproto.Node;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;

public final class TPTPProto2Graph {
  public final MutableValueGraph<LabeledNode, EdgeKind> tptpGraph;
  private int uniqueIndex;
  private static final Map<String, NodeKind> symbolToNodeKind =
      Map.of("&", NodeKind.AND, "~", NodeKind.NOT, "|", NodeKind.OR, "!", NodeKind.FOR_ALL, "?",
          NodeKind.EXISTS, "=", NodeKind.EQUALITY, "!=", NodeKind.INEQUALITY);

  public final LabeledNode addNode(Node node, LabeledNode parentNode, LabeledNode previousNode,
      List<LabeledNode> dataNodes) {
    NodeKind nodeKind = NodeKind.PREDICATE_OR_FUNCTION;
    if (symbolToNodeKind.containsKey(node.getValue())) {
      nodeKind = symbolToNodeKind.get(node.getValue());
    } else {
      if (Character.isUpperCase(node.getValue().charAt(0))) {
        nodeKind = NodeKind.VARIABLE;
      }
    }
    final LabeledNode currentNode = LabeledNode.build(uniqueIndex++, nodeKind, node.getValue());
    if (parentNode != null) {
      tptpGraph.putEdgeValue(parentNode, currentNode, EdgeKind.AST);
    }
    if (previousNode != null) {
      tptpGraph.putEdgeValue(previousNode, currentNode, EdgeKind.NCS);
    }
    for (final LabeledNode dataNode : dataNodes) {
      if (dataNode.label.equals(currentNode.label)) {
        tptpGraph.putEdgeValue(dataNode, currentNode, EdgeKind.DDG);
      }
    }
    switch (node.getValue()) {
      case "!":
      case "?":
        final int variableCount = node.getChildCount() - 1;
        final List<LabeledNode> newDataNodes = new ArrayList<>(dataNodes);
        for (int i = 0; i < variableCount; i++) {
          newDataNodes.add(addNode(node.getChild(i), currentNode, null, Collections.emptyList()));
        }
        addNode(node.getChild(variableCount), currentNode, null, newDataNodes);
        break;
      case "|":
      case "&":
      case "~":
      case "=":
      case "!=":
        for (final Node newNode : node.getChildList()) {
          addNode(newNode, currentNode, null, dataNodes);
        }
        break;
      default:
        if (node.getChildCount() > 0) {
          LabeledNode newPreviousNode = addNode(node.getChild(0), currentNode, null, dataNodes);
          for (int i = 1; i < node.getChildCount(); i++) {
            newPreviousNode = addNode(node.getChild(i), currentNode, newPreviousNode, dataNodes);
          }
        }
    }
    return currentNode;
  }

  public TPTPProto2Graph() {
    tptpGraph = ValueGraphBuilder.directed().allowsSelfLoops(false).build();
    uniqueIndex = 0;
  }

  public final static void main(String[] args) throws IOException {
    final Scanner problemList = new Scanner(new FileInputStream(args[0]));
    int fileIndex = 0;
    while (problemList.hasNextLine()) {
      String outputFilename = Paths.get(args[1], fileIndex++ + ".pb").toString();
      Node tptpProto = Node.parseFrom(new FileInputStream(problemList.nextLine()));
      TPTPProto2Graph tptpProto2Graph = new TPTPProto2Graph();
      // DGLGraph dglGraph = (new Graph2DGLProto<String>(tptpProto2Graph.getTptpGraph(),
      //     tptpProto2Graph.getNodeKinds())).toDGLProto();
      // dglGraph.writeTo(new FileOutputStream(outputFilename));
    }
  }
}
