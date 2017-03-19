/*-
 *
 *  * Copyright 2015 Skymind,Inc.
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
 *
 */

package org.nd4j.linalg.api.ops.factory;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.*;

/**
 * Op factory
 *
 * @author Adam Gibson
 */
public interface OpFactory {

    /**
     * Create a loss function with the given inputs and outputs
     * @param name the name of the function
     * @param x the input
     * @param y the output
     * @return a loss function representing the delta between the 2
     */
    LossFunction createLossFunction(String name, INDArray x, INDArray y);

    /**
     * Accumulation operation
     *
     * @param name the name of the function to create
     * @param x    the input to the function
     * @return the operation
     */
    Accumulation createAccum(String name, INDArray x);

    /**
     * Accumulation operation
     *
     * @param name the name of the function
     * @param x    the input
     * @param y    the pairwise transformation
     * @param z    the output
     * @return the operation
     */
    Accumulation createAccum(String name, INDArray x, INDArray y, INDArray z);

    /**
     * @param name
     * @param x
     * @param y
     * @return
     */
    Accumulation createAccum(String name, INDArray x, INDArray y);

    /**
     * Index accumulation operation
     *
     * @param name the name of the function to create
     * @param x    the input to the function
     * @return the operation
     */
    IndexAccumulation createIndexAccum(String name, INDArray x);

    /**Index accumulation operation
     * @param name
     * @param x
     * @param y
     * @return
     */
    IndexAccumulation createIndexAccum(String name, INDArray x, INDArray y);

    /**
     * @param name
     * @param x
     * @param y
     * @return
     */
    TransformOp createTransform(String name, INDArray x, INDArray y);


    /**
     * @param name
     * @param x
     * @return
     */
    TransformOp createTransform(String name, INDArray x);

    /**
     * @param name
     * @param x
     * @param extraArgs
     * @return
     */
    TransformOp createTransform(String name, INDArray x, Object[] extraArgs);

    /**
     * @param name
     * @param x
     * @param y
     * @param z
     * @return
     */
    TransformOp createTransform(String name, INDArray x, INDArray y, INDArray z);

    /** Create a vector operation
     *
     * @param name Name of the vector op
     * @param x NDArray to operate on
     * @param y Vector
     * @param z Result NDArray
     * @param dimension Dimension to do op along. 0 for row, 1 for column, etc
     * @return VectorOp
     */
    BroadcastOp createBroadcastOp(String name, INDArray x, INDArray y, INDArray z, int... dimension);

    /** Create a vector operation
     *
     * @param name Name of the vector op
     * @param x NDArray to operate on
     * @param z Result NDArray
     * @param dimension Dimension to do op along. 0 for row, 1 for column, etc
     * @return VectorOp
     */
    BroadcastOp createBroadcastOp(String name, INDArray x, INDArray z, int... dimension);

}
