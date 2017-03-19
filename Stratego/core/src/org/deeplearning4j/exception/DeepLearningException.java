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
 */

package org.deeplearning4j.exception;

public class DeepLearningException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = -7973589163269627293L;

    public DeepLearningException() {
        super();
    }

    public DeepLearningException(String message, Throwable cause, boolean enableSuppression,
                    boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public DeepLearningException(String message, Throwable cause) {
        super(message, cause);
    }

    public DeepLearningException(String message) {
        super(message);
    }

    public DeepLearningException(Throwable cause) {
        super(cause);
    }



}
