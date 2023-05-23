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
import java.io.Writer;
import com.google.common.graph.EndpointPair;
import com.google.common.graph.ValueGraph;

public class GraphWriter {
  public static void writeDot(ValueGraph<LabeledNode, EdgeKind> graph, Writer writer)
      throws IOException {
    writer.write("digraph {\n");
    for (EndpointPair<LabeledNode> edge : graph.edges()) {
      writer.write("\"" + edge.source().label + edge.source().index + "\" -> \""
          + edge.target().label + edge.target().index + "\"\n");
    }
    writer.write("}");
    writer.close();
  }
}
