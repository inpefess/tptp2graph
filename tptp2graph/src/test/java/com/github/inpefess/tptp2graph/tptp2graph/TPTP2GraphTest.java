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
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import com.github.inpefess.tptp_grpc.tptp2proto.TPTP2Proto;
import com.github.inpefess.tptp_grpc.tptp_proto.Function;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TPTP2GraphTest {
  private static TPTPProto2Graph tptp2Graph;
  private static Function tptpProto;

  @BeforeAll
  static void setUp() throws FileNotFoundException {
    StringReader testProblem = new StringReader("cnf(test, axiom, ~ p(f(X, f(Y, X))) | p(X, Y)).");
    tptpProto = (new TPTP2Proto("")).tptp2Proto(testProblem);
    tptp2Graph = new TPTPProto2Graph(tptpProto);
  }

  @Test
  void testWrite2Dot() throws IOException {
    GraphWriter.<String>writeDot(tptp2Graph.getTptpGraph(), new FileWriter("graph.dot"));
  }

  @Test
  void testWrite2DGL() throws IOException {
    Graph2DGLProto<String> graph2DGLProto =
        new Graph2DGLProto<String>(tptp2Graph.getTptpGraph(), tptp2Graph.getNodeKinds());
    InputStream testGraph = this.getClass().getResourceAsStream("/test_dgl.pb");
    // assertEquals(graph2DGLProto.toDGLProto(), DGLGraph.parseFrom(testGraph));
  }

  @Test
  void testGraph2Proto() throws GraphTraversalException, IOException {
    assertEquals((new Graph2TPTPProto<String>()).toProto(tptp2Graph.getTptpGraph()), tptpProto);
  }
}
