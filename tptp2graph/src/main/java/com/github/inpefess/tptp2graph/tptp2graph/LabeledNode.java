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

public final class LabeledNode {
  public final String label;
  public final int index;
  public final NodeKind kind;

  public LabeledNode(final int index, final NodeKind kind, final String label) {
    this.label = label;
    this.kind = kind;
    this.index = index;
  }

  public final static LabeledNode build(final int index, final NodeKind kind, final String label) {
    return new LabeledNode(index, kind, label);
  }

  public final String toString() {
    return "LabeledNode(" + index + ", " + kind + ", " + label + ")";
  }
}
