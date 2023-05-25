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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import com.github.inpefess.tptpgrpc.tptpproto.Node;
import com.google.common.graph.ElementOrder;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;

public final class TptpProto2Graph {
  public final MutableValueGraph<LabeledNode, EdgeKind> tptpGraph;
  private int uniqueIndex;
  private static final Map<String, NodeKind> symbolToNodeKind =
      Map.of("&", NodeKind.AND, "~", NodeKind.NOT, "|", NodeKind.OR, "!", NodeKind.FOR_ALL, "?",
          NodeKind.EXISTS, "=", NodeKind.EQUALITY, "!=", NodeKind.INEQUALITY);

  public final LabeledNode addNode(Node node, LabeledNode parentNode, LabeledNode previousNode,
      List<LabeledNode> dataNodes) {
    final NodeKind nodeKind = getNodeKind(node);
    final LabeledNode currentNode =
        processCurrentNode(node, parentNode, previousNode, dataNodes, nodeKind);
    switch (node.getValue()) {
      case "!":
      case "?":
        processQuantifier(node, dataNodes, currentNode);
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

  private final void processQuantifier(final Node node, final List<LabeledNode> dataNodes,
      final LabeledNode currentNode) {
    final int variableCount = node.getChildCount() - 1;
    final List<LabeledNode> newDataNodes = new ArrayList<>(dataNodes);
    for (int i = 0; i < variableCount; i++) {
      newDataNodes.add(addNode(node.getChild(i), currentNode, null, Collections.emptyList()));
    }
    addNode(node.getChild(variableCount), currentNode, null, newDataNodes);
  }

  private final LabeledNode processCurrentNode(final Node node, final LabeledNode parentNode,
      final LabeledNode previousNode, final List<LabeledNode> dataNodes, final NodeKind nodeKind) {
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
    return currentNode;
  }

  private final NodeKind getNodeKind(final Node node) {
    NodeKind nodeKind = NodeKind.PREDICATE_OR_FUNCTION;
    if (symbolToNodeKind.containsKey(node.getValue())) {
      nodeKind = symbolToNodeKind.get(node.getValue());
    } else {
      if (Character.isUpperCase(node.getValue().charAt(0))) {
        nodeKind = NodeKind.VARIABLE;
      }
    }
    return nodeKind;
  }

  public TptpProto2Graph() {
    tptpGraph = ValueGraphBuilder.directed().allowsSelfLoops(false)
        .incidentEdgeOrder(ElementOrder.stable()).nodeOrder(ElementOrder.insertion()).build();
    uniqueIndex = 0;
  }

  public static final void main(final String[] args) throws IOException {
    try (FileInputStream problemListFile = new FileInputStream(args[0]);
        Scanner problemList = new Scanner(problemListFile)) {
      int fileIndex = 0;
      while (problemList.hasNextLine()) {
        final TptpProto2Graph tptpProto2Graph = new TptpProto2Graph();
        try (FileInputStream protobufFile = new FileInputStream(problemList.nextLine())) {
          final Node tptpProto = Node.parseFrom(protobufFile);
          tptpProto2Graph.addNode(tptpProto, null, null, new ArrayList<>());
        }
        final String outputFilename = Paths.get(args[1], fileIndex++ + ".pb").toString();
        try (FileOutputStream pygProtobufFile = new FileOutputStream(outputFilename)) {
          Graph2PygProto.toPygProto(tptpProto2Graph.tptpGraph).writeTo(pygProtobufFile);
        }
      }
    }
  }
}
