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

package org.nd4j.linalg.lossfunctions;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.lossfunctions.impl.*;


/**
 * Central class for loss functions
 * @author Adam Gibson
 */
public class LossFunctions {

    /**
     * Generic scoring function.
     * Note that an IllegalArgumentException is thrown if the given
     * loss function is custom. An alternative mechanism for scoring
     * (preferrably with a function name and the op factory) should be used instead.
     *
     * @param labels            the labels to score
     * @param lossFunction      the loss function to use
     * @param z                 the output function
     * @param l2                the l2 regularization term (0.5 * l2Coeff * sum w^2)
     * @param l1                the l1 regularization term (l1Coeff * sum |w|)
     * @param useRegularization whether to use regularization
     * @return the score for the given parameters
     */
    public static double score(INDArray labels, LossFunction lossFunction, INDArray z, double l2, double l1,
                    boolean useRegularization) {
        return LossCalculation.builder().l1(l1).lossFunction(lossFunction).l2(l2).labels(labels).z(z)
                        .useRegularization(useRegularization).build().score();
    }

    /**
     * MSE: Mean Squared Error: Linear Regression<br>
     * EXPLL: Exponential log likelihood: Poisson Regression<br>
     * XENT: Cross Entropy: Binary Classification<br>
     * MCXENT: Multiclass Cross Entropy<br>
     * RMSE_XENT: RMSE Cross Entropy<br>
     * SQUARED_LOSS: Squared Loss<br>
     * NEGATIVELOGLIKELIHOOD: Negative Log Likelihood<br>
     */
    public enum LossFunction {
        MSE, L1, @Deprecated EXPLL, XENT, MCXENT, @Deprecated RMSE_XENT, SQUARED_LOSS, RECONSTRUCTION_CROSSENTROPY, NEGATIVELOGLIKELIHOOD, @Deprecated CUSTOM, COSINE_PROXIMITY, HINGE, SQUARED_HINGE, KL_DIVERGENCE, MEAN_ABSOLUTE_ERROR, L2, MEAN_ABSOLUTE_PERCENTAGE_ERROR, MEAN_SQUARED_LOGARITHMIC_ERROR, POISSON;

        public ILossFunction getILossFunction() {
            switch (this) {
                case MSE:
                case SQUARED_LOSS:
                    return new LossMSE();
                case L1:
                    return new LossL1();
                case XENT:
                    return new LossBinaryXENT();
                case MCXENT:
                    return new LossMCXENT();
                case KL_DIVERGENCE:
                case RECONSTRUCTION_CROSSENTROPY:
                    return new LossKLD();
                case NEGATIVELOGLIKELIHOOD:
                    return new LossNegativeLogLikelihood();
                case COSINE_PROXIMITY:
                    return new LossCosineProximity();
                case HINGE:
                    return new LossHinge();
                case SQUARED_HINGE:
                    return new LossSquaredHinge();
                case MEAN_ABSOLUTE_ERROR:
                    return new LossMAE();
                case L2:
                    return new LossL2();
                case MEAN_ABSOLUTE_PERCENTAGE_ERROR:
                    return new LossMAPE();
                case MEAN_SQUARED_LOGARITHMIC_ERROR:
                    return new LossMSLE();
                case POISSON:
                case EXPLL:
                    return new LossPoisson();
                default:
                    //Custom, RMSE_XENT
                    throw new UnsupportedOperationException("Unknown or not supported loss function: " + this);
            }
        }
    }


}
