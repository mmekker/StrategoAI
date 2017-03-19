package org.deeplearning4j.nn.conf.layers;

import lombok.*;
import org.deeplearning4j.nn.api.ParamInitializer;
import org.deeplearning4j.nn.conf.InputPreProcessor;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.params.EmptyParamInitializer;
import org.deeplearning4j.optimize.api.IterationListener;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.Collection;
import java.util.Map;

@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class DropoutLayer extends FeedForwardLayer {
    private DropoutLayer(Builder builder) {
        super(builder);
    }

    @Override
    public DropoutLayer clone() {
        return (DropoutLayer) super.clone();
    }

    @Override
    public org.deeplearning4j.nn.api.Layer instantiate(NeuralNetConfiguration conf,
                    Collection<IterationListener> iterationListeners, int layerIndex, INDArray layerParamsView,
                    boolean initializeParams) {
        org.deeplearning4j.nn.layers.DropoutLayer ret = new org.deeplearning4j.nn.layers.DropoutLayer(conf);
        ret.setListeners(iterationListeners);
        ret.setIndex(layerIndex);
        ret.setParamsViewArray(layerParamsView);
        Map<String, INDArray> paramTable = initializer().init(conf, layerParamsView, initializeParams);
        ret.setParamTable(paramTable);
        ret.setConf(conf);
        return ret;
    }

    @Override
    public ParamInitializer initializer() {
        return EmptyParamInitializer.getInstance();
    }

    @Override
    public InputType getOutputType(int layerIndex, InputType inputType) {
        if (inputType == null)
            throw new IllegalStateException("Invalid input type: null for layer name \"" + getLayerName() + "\"");
        return inputType;
    }

    @Override
    public void setNIn(InputType inputType, boolean override) {
        //No op: dropout layer doesn't have a fixed nIn value
    }

    @Override
    public InputPreProcessor getPreProcessorForInputType(InputType inputType) {
        //No input preprocessor required; dropout applies to any input type
        return null;
    }

    @Override
    public double getL1ByParam(String paramName) {
        //Not applicable
        return 0;
    }

    @Override
    public double getL2ByParam(String paramName) {
        //Not applicable
        return 0;
    }

    @Override
    public double getLearningRateByParam(String paramName) {
        //Not applicable
        return 0;
    }

    @NoArgsConstructor
    public static class Builder extends FeedForwardLayer.Builder<DropoutLayer.Builder> {
        public Builder(double dropOut) {
            this.dropOut = dropOut;
        }

        @Override
        @SuppressWarnings("unchecked")
        public DropoutLayer build() {

            return new DropoutLayer(this);
        }
    }


}
