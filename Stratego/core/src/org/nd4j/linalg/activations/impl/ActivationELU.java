package org.nd4j.linalg.activations.impl;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.math3.util.Pair;
import org.nd4j.linalg.activations.BaseActivationFunction;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.impl.transforms.ELU;
import org.nd4j.linalg.api.ops.impl.transforms.ELUDerivative;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.BooleanIndexing;
import org.nd4j.linalg.indexing.conditions.Conditions;

/**
 *  f(x) = alpha * (exp(x) - 1.0); x < 0
 *       = x ; x>= 0
 *
 *  alpha defaults to 1, if not specified
 */
@EqualsAndHashCode
@Getter
public class ActivationELU extends BaseActivationFunction {
    public static final double DEFAULT_ALPHA = 1.0;

    private double alpha = DEFAULT_ALPHA;

    public ActivationELU() {
        this(DEFAULT_ALPHA);
    }

    public ActivationELU(double alpha) {
        this.alpha = alpha;
    }

    /*
             = alpha * (exp(x) - 1.0); x < 0
       f(x)
             = x ; x >= 0
     */
    @Override
    public INDArray getActivation(INDArray in, boolean training) {
        // no support in ELU native to override alpha
        if (this.alpha != 1.00) {
            INDArray alphaMultiple = Nd4j.getExecutioner().execAndReturn(new ELU(in.dup()));
            alphaMultiple.muli(alpha);
            BooleanIndexing.replaceWhere(in, alphaMultiple, Conditions.lessThan(0));
        } else {
            Nd4j.getExecutioner().execAndReturn(new ELU(in));
        }
        return in;
    }

    /*
             = alpha * exp(x) ; x < 0
       f'(x)
             = 1 ; x >= 0
     */
    @Override
    public Pair<INDArray, INDArray> backprop(INDArray in, INDArray epsilon) {
        // no support in ELU native to override alpha
        if (alpha != 1.00) {
            INDArray dLdz = Nd4j.getExecutioner().execAndReturn(new ELUDerivative(in.dup()));
            dLdz.muli(alpha);
            BooleanIndexing.replaceWhere(dLdz, 1, Conditions.equals(alpha));

            dLdz.muli(epsilon);
            return new Pair<>(dLdz, null);
        }

        else {
            INDArray dLdz = Nd4j.getExecutioner().execAndReturn(new ELU(in).derivative());
            dLdz.muli(epsilon);
            return new Pair<>(dLdz, null);
        }
    }

    @Override
    public String toString() {
        return "elu(alpha=" + alpha + ")";
    }
}
