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

package org.nd4j.linalg.indexing.conditions;

import org.nd4j.linalg.api.complex.IComplexNumber;
import org.nd4j.linalg.factory.Nd4j;

/**
 * Created by agibsonccc on 10/8/14.
 */
public abstract class BaseCondition implements Condition {
    protected Number value;
    protected IComplexNumber complexNumber;

    public BaseCondition(Number value) {
        this.value = value;
        this.complexNumber = Nd4j.createComplexNumber(value, 0);
    }

    @Override
    public double getValue() {
        return value.doubleValue();
    }

    public BaseCondition(IComplexNumber complexNumber) {
        this.complexNumber = complexNumber;
        this.value = complexNumber.absoluteValue();
    }


}
