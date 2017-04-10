package com.game.stratego.core.ai;

import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.*;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.lossfunctions.LossFunctions;

/**
 * Created by user on 3/19/2017.
 */
public class BoardClassifier {


    public static MultiLayerNetwork getModel() {
        ConvolutionLayer layer0 = new ConvolutionLayer.Builder(5,5)
                .nIn(6)
                .nOut(20)
                .stride(1,1)
                .padding(2,2)
                .weightInit(WeightInit.XAVIER)
                .name("First convolution layer")
                .activation(Activation.RELU)
                .build();
        ConvolutionLayer layer1 = new ConvolutionLayer.Builder(5,5)
                .nOut(40)
                .stride(1,1)
                .padding(2,2)
                .weightInit(WeightInit.XAVIER)
                .name("First convolution layer")
                .activation(Activation.RELU)
                .build();
        DenseLayer layer2 = new DenseLayer.Builder().activation(Activation.RELU)
                .nOut(100)
                .build();
        DenseLayer layer3 = new DenseLayer.Builder().activation(Activation.RELU)
                .nOut(200)
                .build();
        OutputLayer layer4 = new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                .nOut(1)
                .activation(Activation.SOFTMAX)
                .build();




        MultiLayerConfiguration configuration = new NeuralNetConfiguration.Builder()
                .seed(12345)
                .iterations(1)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .learningRate(0.001)
                .regularization(true)
                .l2(0.0004)
                .updater(Updater.NESTEROVS)
                .momentum(0.9)
                .list()
                .layer(0, layer0)
                .layer(1, layer1)
                .layer(2, layer2)
                .layer(3, layer3)
                .layer(4, layer4)
                .pretrain(false)
                .backprop(true)
                .setInputType(InputType.convolutionalFlat(10,10,6))
                .build();

        MultiLayerNetwork network = new MultiLayerNetwork(configuration);
        network.init();
        return network;
    }
}
