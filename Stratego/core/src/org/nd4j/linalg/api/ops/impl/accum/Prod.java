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

package org.nd4j.linalg.api.ops.impl.accum;

import org.nd4j.linalg.api.complex.IComplexNumber;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.BaseAccumulation;
import org.nd4j.linalg.api.ops.Op;
import org.nd4j.linalg.factory.Nd4j;

/**
 * Prod the components
 *
 * @author Adam Gibson
 */
public class Prod extends BaseAccumulation {
    public Prod() {}

    public Prod(INDArray x, INDArray y, INDArray z, long n) {
        super(x, y, z, n);
    }

    public Prod(INDArray x, INDArray y, long n) {
        super(x, y, n);
    }

    public Prod(INDArray x) {
        super(x);
    }

    public Prod(INDArray x, INDArray y) {
        super(x, y);
    }


    @Override
    public double op(double origin, double other) {
        return origin * other;
    }

    @Override
    public IComplexNumber op(IComplexNumber origin) {
        return null;
    }

    @Override
    public float op(float origin, float other) {
        return origin * other;
    }

    @Override
    public double update(double accum, double x) {
        return accum * x;
    }

    @Override
    public double update(double accum, double x, double y) {
        return accum * x;
    }

    @Override
    public float update(float accum, float x) {
        return accum * x;
    }

    @Override
    public float update(float accum, float x, float y) {
        return accum * x;
    }

    @Override
    public IComplexNumber update(IComplexNumber accum, double x) {
        return accum.mul(x);
    }

    @Override
    public IComplexNumber update(IComplexNumber accum, double x, double y) {
        return accum.mul(x);
    }

    @Override
    public IComplexNumber update(IComplexNumber accum, IComplexNumber x) {
        return accum.mul(x);
    }

    @Override
    public IComplexNumber update(IComplexNumber accum, IComplexNumber x, IComplexNumber y) {
        return accum.mul(x);
    }

    @Override
    public IComplexNumber update(IComplexNumber accum, IComplexNumber x, double y) {
        return accum.mul(x);
    }

    @Override
    public int opNum() {
        return 8;
    }

    @Override
    public String name() {
        return "prod";
    }

    @Override
    public IComplexNumber op(IComplexNumber origin, IComplexNumber other) {
        return null;
    }

    @Override
    public IComplexNumber op(IComplexNumber origin, float other) {
        return null;
    }

    @Override
    public IComplexNumber op(IComplexNumber origin, double other) {
        return null;
    }

    @Override
    public double zeroDouble() {
        return 1.0;
    }

    @Override
    public float zeroFloat() {
        return 1.0f;
    }

    @Override
    public float zeroHalf() {
        return zeroFloat();
    }

    @Override
    public IComplexNumber zeroComplex() {
        return Nd4j.createComplexNumber(1.0, 0.0);
    }

    @Override
    public Op opForDimension(int index, int dimension) {
        INDArray xAlongDimension = x.vectorAlongDimension(index, dimension);

        if (y() != null)
            return new Prod(xAlongDimension, y.vectorAlongDimension(index, dimension), xAlongDimension.length());
        else
            return new Prod(x.vectorAlongDimension(index, dimension));

    }

    @Override
    public Op opForDimension(int index, int... dimension) {
        INDArray xAlongDimension = x.tensorAlongDimension(index, dimension);

        if (y() != null)
            return new Prod(xAlongDimension, y.tensorAlongDimension(index, dimension), xAlongDimension.length());
        else
            return new Prod(x.tensorAlongDimension(index, dimension));
    }

}
