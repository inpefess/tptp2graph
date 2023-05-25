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

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import com.github.inpefess.tptp2graph.pygproto.Data;
import com.github.inpefess.tptpgrpc.tptp2proto.Tptp2Proto;
import com.github.inpefess.tptpgrpc.tptpproto.Node;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class Tptp2GraphTest {
  private static TptpProto2Graph tptp2Graph;
  private static Node tptpProto;

  @BeforeAll
  static final void setUp() throws IOException {
    try (final StringReader testProblem =
        new StringReader("cnf(test, axiom, ~ p(f(X, f(Y, X))) | p(X, Y)).")) {
      tptpProto = (new Tptp2Proto("")).tptp2Proto(testProblem);
    }
    tptp2Graph = new TptpProto2Graph();
    tptp2Graph.addNode(tptpProto, null, null, new ArrayList<>());
  }

  @Test
  final void testWrite2Dot() throws IOException {
    try (final FileWriter outputFile = new FileWriter("graph.dot")) {
      GraphWriter.writeDot(tptp2Graph.tptpGraph, outputFile);
    }
  }

  @Test
  final void testWrite2Pyg() throws IOException {
    try (final InputStream testGraph = this.getClass().getResourceAsStream("/test_pyg.pb")) {
      assertEquals(Graph2PygProto.toPygProto(tptp2Graph.tptpGraph), Data.parseFrom(testGraph));
    }
  }

  @Test
  final void testGraph2Proto() throws GraphTraversalException {
    assertEquals((new Graph2TptpProto(tptp2Graph.tptpGraph)).toProto(), tptpProto);
  }
}
