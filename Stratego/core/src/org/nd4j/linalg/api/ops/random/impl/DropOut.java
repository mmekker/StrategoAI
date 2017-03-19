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

package org.nd4j.linalg.api.ops.random.impl;

import lombok.NonNull;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.random.BaseRandomOp;

/**
 * Inverted DropOut implementation as Op
 *
 * @author raver119@gmail.com
 */
public class DropOut extends BaseRandomOp {

    private double p;

    public DropOut() {

    }

    public DropOut(@NonNull INDArray x, double p) {
        this(x, x, p, x.lengthLong());
    }

    public DropOut(@NonNull INDArray x, @NonNull INDArray z, double p) {
        this(x, z, p, x.lengthLong());
    }

    public DropOut(@NonNull INDArray x, @NonNull INDArray z, double p, long n) {
        this.p = p;
        init(x, null, z, n);
    }


    @Override
    public int opNum() {
        return 1;
    }

    @Override
    public String name() {
        return "dropout";
    }

    @Override
    public void init(INDArray x, INDArray y, INDArray z, long n) {
        super.init(x, y, z, n);
        this.extraArgs = new Object[] {p, (double) n};
    }
}
