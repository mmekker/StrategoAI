package org.deeplearning4j.nn.conf.layers;

import lombok.*;
import org.deeplearning4j.nn.conf.ConvolutionMode;
import org.deeplearning4j.nn.conf.InputPreProcessor;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.optimize.api.IterationListener;
import org.deeplearning4j.util.ConvolutionUtils;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.Collection;
import java.util.Map;

/**
 * 1D (temporal) subsampling layer. Currently, we just subclass off the
 * SubsamplingLayer and hard code the "width" dimension to 1. Also, this
 * layer accepts RNN InputTypes instead of CNN InputTypes.
 *
 * This approach treats a multivariate time series with L timesteps and
 * P variables as an L x 1 x P image (L rows high, 1 column wide, P
 * channels deep). The kernel should be H<L pixels high and W=1 pixels
 * wide.
 *
 * TODO: We will eventually want to NOT subclass off of SubsamplingLayer.
 *
 * @author dave@skymind.io
 */
@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Subsampling1DLayer extends SubsamplingLayer {

    private Subsampling1DLayer(Builder builder) {
        super(builder);
    }

    @Override
    public org.deeplearning4j.nn.api.Layer instantiate(NeuralNetConfiguration conf, Collection<IterationListener> iterationListeners, int layerIndex, INDArray layerParamsView, boolean initializeParams) {
        org.deeplearning4j.nn.layers.convolution.subsampling.Subsampling1DLayer ret
                = new org.deeplearning4j.nn.layers.convolution.subsampling.Subsampling1DLayer(conf);
        ret.setListeners(iterationListeners);
        ret.setIndex(layerIndex);
        ret.setParamsViewArray(layerParamsView);
        Map<String, INDArray> paramTable = initializer().init(conf, layerParamsView, initializeParams);
        ret.setParamTable(paramTable);
        ret.setConf(conf);
        return ret;
    }

    @Override
    public InputType getOutputType(int layerIndex, InputType inputType) {
        if(inputType == null || inputType.getType() != InputType.Type.RNN){
            throw new IllegalStateException("Invalid input for Subsampling layer (layer name=\"" + getLayerName() + "\"): Expected RNN input, got " + inputType);
        }
        return inputType;
    }

    @Override
    public void setNIn(InputType inputType, boolean override){
        //No op: subsampling layer doesn't have nIn value
    }

    @Override
    public InputPreProcessor getPreProcessorForInputType(InputType inputType) {
        if(inputType == null ){
            throw new IllegalStateException("Invalid input for Subsampling1D layer (layer name=\"" + getLayerName() + "\"): input is null");
        }

        return InputTypeUtil.getPreprocessorForInputTypeRnnLayers(inputType, getLayerName());
    }

    @Override
    public Subsampling1DLayer clone() {
        Subsampling1DLayer clone = (Subsampling1DLayer) super.clone();

        if(clone.kernelSize != null) clone.kernelSize = clone.kernelSize.clone();
        if(clone.stride != null) clone.stride = clone.stride.clone();
        if(clone.padding != null) clone.padding = clone.padding.clone();
        return clone;
    }

    public static class Builder extends SubsamplingLayer.BaseSubsamplingBuilder<Builder> {
        private static final org.deeplearning4j.nn.conf.layers.PoolingType DEFAULT_POOLING = org.deeplearning4j.nn.conf.layers.PoolingType.MAX;
        private static final int DEFAULT_KERNEL = 2;
        private static final int DEFAULT_STRIDE = 1;
        private static final int DEFAULT_PADDING = 0;

        public Builder(PoolingType poolingType, int kernelSize, int stride) {
            this(poolingType, kernelSize, stride, DEFAULT_PADDING);
        }

        public Builder(PoolingType poolingType, int kernelSize) {
            this(poolingType, kernelSize, DEFAULT_STRIDE, DEFAULT_PADDING);
        }

        public Builder(org.deeplearning4j.nn.conf.layers.PoolingType poolingType, int kernelSize) {
            this(poolingType, kernelSize, DEFAULT_STRIDE, DEFAULT_PADDING);
        }

        public Builder(int kernelSize, int stride, int padding) {
            this(DEFAULT_POOLING, kernelSize, stride, padding);
        }

        public Builder(int kernelSize, int stride) {
            this(DEFAULT_POOLING, kernelSize, stride, DEFAULT_PADDING);
        }

        public Builder(int kernelSize) {
            this(DEFAULT_POOLING, kernelSize, DEFAULT_STRIDE, DEFAULT_PADDING);
        }

        public Builder(PoolingType poolingType) {
            this(poolingType, DEFAULT_KERNEL, DEFAULT_STRIDE, DEFAULT_PADDING);
        }

        public Builder(org.deeplearning4j.nn.conf.layers.PoolingType poolingType){
            this(poolingType, DEFAULT_KERNEL, DEFAULT_STRIDE, DEFAULT_PADDING);
        }

        public Builder() {
            this(DEFAULT_POOLING, DEFAULT_KERNEL, DEFAULT_STRIDE, DEFAULT_PADDING);
        }

        public Builder(org.deeplearning4j.nn.conf.layers.PoolingType poolingType, int kernelSize, int stride, int padding){
            super(poolingType, new int[]{kernelSize, 1}, new int[]{stride, 1}, new int[]{padding, 0});
        }

        public Builder(PoolingType poolingType, int kernelSize, int stride, int padding){
            super(poolingType, new int[]{kernelSize, 1}, new int[]{stride, 1}, new int[]{padding, 0});
        }

        @SuppressWarnings("unchecked")
        public Subsampling1DLayer build() {
            if(poolingType == org.deeplearning4j.nn.conf.layers.PoolingType.PNORM && pnorm <= 0) throw new IllegalStateException("Incorrect Subsampling config: p-norm must be set when using PoolingType.PNORM");
            ConvolutionUtils.validateCnnKernelStridePadding(kernelSize, stride, padding);

            return new Subsampling1DLayer(this);
        }

        /**
         * Kernel size
         *
         * @param kernelSize    kernel size in height and width dimensions
         */
        public Subsampling1DLayer.Builder kernelSize(int kernelSize){
            this.kernelSize[0] = kernelSize;
            return this;
        }

        /**
         * Stride
         *
         * @param stride    stride value
         */
        public Subsampling1DLayer.Builder stride(int stride){
            this.stride[0] = stride;
            return this;
        }

        /**
         * Padding
         *
         * @param padding    padding value
         */
        public Subsampling1DLayer.Builder padding(int padding){
            this.padding[0] = padding;
            return this;
        }
    }
}
