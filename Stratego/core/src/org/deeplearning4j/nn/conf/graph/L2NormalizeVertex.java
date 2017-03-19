/*-
 *
 *  * Copyright 2016 Skymind,Inc.
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */

package org.deeplearning4j.nn.conf.graph;

import lombok.Data;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.inputs.InvalidInputTypeException;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.shade.jackson.annotation.JsonProperty;

/**
 * L2NormalizeVertex performs L2 normalization on a single input.
 *
 * Can be configured to normalize a single dimension, or normalize across
 * all dimensions except zero by leaving dimension blank or setting it to -1.
 *
 * @author Justin Long (crockpotveggies)
 * @author Alex Black (AlexDBlack)
 */
@Data
public class L2NormalizeVertex extends GraphVertex {
    public static final double DEFAULT_EPS = 1e-8;

    protected int[] dimension;
    protected double eps;

    public L2NormalizeVertex() {
        this(null, DEFAULT_EPS);
    }

    public L2NormalizeVertex(@JsonProperty("dimension") int[] dimension, @JsonProperty("eps") double eps) {
        this.dimension = dimension;
        this.eps = eps;
    }



    @Override
    public L2NormalizeVertex clone() {
        return new L2NormalizeVertex(dimension, eps);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof L2NormalizeVertex))
            return false;
        return ((L2NormalizeVertex) o).dimension == dimension;
    }

    @Override
    public int hashCode() {
        return 123081189;
    }

    @Override
    public int numParams(boolean backprop) {
        return 0;
    }

    @Override
    public org.deeplearning4j.nn.graph.vertex.GraphVertex instantiate(ComputationGraph graph, String name, int idx,
                    INDArray paramsView, boolean initializeParams) {

        return new org.deeplearning4j.nn.graph.vertex.impl.L2NormalizeVertex(graph, name, idx, dimension, eps);
    }

    @Override
    public InputType getOutputType(int layerIndex, InputType... vertexInputs) throws InvalidInputTypeException {
        if (vertexInputs.length == 1)
            return vertexInputs[0];
        InputType first = vertexInputs[0];

        return first; //Same output shape/size as
    }
}
