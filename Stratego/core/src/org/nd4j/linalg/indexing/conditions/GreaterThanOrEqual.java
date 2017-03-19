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

/**
 * Created by agibsonccc on 10/8/14.
 */
public class GreaterThanOrEqual extends BaseCondition {

    /**
     * Special constructor for pairwise boolean operations.
     */
    public GreaterThanOrEqual() {
        super(0.0);
    }

    public GreaterThanOrEqual(Number value) {
        super(value);
    }

    public GreaterThanOrEqual(IComplexNumber complexNumber) {
        super(complexNumber);
    }

    /**
     * Returns condition ID for native side
     *
     * @return
     */
    @Override
    public int condtionNum() {
        return 5;
    }

    @Override
    public Boolean apply(Number input) {
        return input.floatValue() >= value.floatValue();
    }

    @Override
    public Boolean apply(IComplexNumber input) {
        return input.absoluteValue().floatValue() >= complexNumber.absoluteValue().floatValue();
    }
}
