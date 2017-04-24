package com.game.stratego.core.ai;

import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.*;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.stats.StatsListener;
import org.deeplearning4j.ui.storage.InMemoryStatsStorage;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.lossfunctions.LossFunctions;

/**
 * Created by user on 3/19/2017.
 */
public class BoardClassifier {


    public static MultiLayerNetwork getModel() {
        /*ConvolutionLayer layer0 = new ConvolutionLayer.Builder(2,2)
                .nIn(6)
                .nOut(20)
                .stride(1,1)
                .padding(0,0)
                .weightInit(WeightInit.XAVIER)
                .name("First convolution layer")
                .activation(Activation.RELU)
                .build();
        ConvolutionLayer layer1 = new ConvolutionLayer.Builder(2,2)
                .nIn(20)
                .nOut(40)
                .stride(1,1)
                .padding(0,0)
                .weightInit(WeightInit.XAVIER)
                .name("Second convolution layer")
                .activation(Activation.RELU)
                .build();
        DenseLayer dense1 = new DenseLayer.Builder().activation(Activation.RELU)
                .weightInit(WeightInit.XAVIER)
                .nIn(6)
                .nOut(40)
                .build();
        DenseLayer dense2 = new DenseLayer.Builder().activation(Activation.RELU)
                .nIn(40)
                .nOut(80)
                .build();
        OutputLayer output = new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                .nOut(1)
                .activation(Activation.SOFTMAX)
                .build();*/

        ConvolutionLayer layer0 = new ConvolutionLayer.Builder(2,2)
                .nIn(1)
                .nOut(20)
                .stride(1,1)
                .padding(0,0)
                .weightInit(WeightInit.XAVIER)
                .name("First convolution layer")
                .activation(Activation.RELU)
                .build();
        DenseLayer dense1 = new DenseLayer.Builder().activation(Activation.RELU)
                .weightInit(WeightInit.XAVIER)
                .nOut(40)
                .build();

        OutputLayer layer6 = new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                .activation(Activation.SOFTMAX)
                .weightInit(WeightInit.XAVIER)
                .name("Output")
                .nOut(2)
                .build();




        /*MultiLayerConfiguration configuration = new NeuralNetConfiguration.Builder()
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
                .layer(0, dense1)
                .layer(3, dense2)
                .layer(1, output)
                .pretrain(false)
                .backprop(true)
                .setInputType(InputType.convolutionalFlat(10,10,6))
                .build();*/

        MultiLayerConfiguration configuration = new NeuralNetConfiguration.Builder()
                .seed(12345)
                .iterations(1)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .learningRate(0.00001)
                .regularization(true)
                .l2(0.00004)
                .updater(Updater.NESTEROVS)
                .momentum(0.9)
                .list()
                .layer(0, layer0)
                .layer(1, dense1)
                .layer(2, layer6)
                .pretrain(false)
                .backprop(true)
                .setInputType(InputType.convolutionalFlat(10,10,1))
                .build();

        MultiLayerNetwork network = new MultiLayerNetwork(configuration);
        network.init();

        //Initialize the user interface backend
        UIServer uiServer = UIServer.getInstance();

        //Configure where the network information (gradients, score vs. time etc) is to be stored. Here: store in memory.
        StatsStorage statsStorage = new InMemoryStatsStorage();         //Alternative: new FileStatsStorage(File), for saving and loading later

        //Attach the StatsStorage instance to the UI: this allows the contents of the StatsStorage to be visualized
        uiServer.attach(statsStorage);

        //Then add the StatsListener to collect this information from the network, as it trains
        network.setListeners(new StatsListener(statsStorage));

        return network;
    }
}
