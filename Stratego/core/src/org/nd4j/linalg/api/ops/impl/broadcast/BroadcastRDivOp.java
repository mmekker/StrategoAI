package org.nd4j.linalg.api.ops.impl.broadcast;

import org.nd4j.linalg.api.complex.IComplexNumber;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.BaseBroadcastOp;

/**
 * Broadcast reverse divide
 * @author Adam Gibson
 */
public class BroadcastRDivOp extends BaseBroadcastOp {

    public BroadcastRDivOp() {}

    public BroadcastRDivOp(INDArray x, INDArray y, INDArray z, int... dimension) {
        super(x, y, z, dimension);
    }


    @Override
    public int opNum() {
        return 4;
    }

    @Override
    public String name() {
        return "broadcastrdiv";
    }

    @Override
    public IComplexNumber op(IComplexNumber origin, double other) {
        return origin.rdiv(other);
    }

    @Override
    public IComplexNumber op(IComplexNumber origin, float other) {
        return origin.rdiv(other);
    }

    @Override
    public IComplexNumber op(IComplexNumber origin, IComplexNumber other) {
        return origin.rdiv(other);
    }

    @Override
    public float op(float origin, float other) {
        return other / origin;
    }

    @Override
    public double op(double origin, double other) {
        return other / origin;
    }

    @Override
    public double op(double origin) {
        return origin;
    }

    @Override
    public float op(float origin) {
        return origin;
    }

    @Override
    public IComplexNumber op(IComplexNumber origin) {
        return origin;
    }


}
