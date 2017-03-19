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

package org.deeplearning4j.nn.layers;


import org.deeplearning4j.berkeley.Pair;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.api.Updater;
import org.deeplearning4j.nn.api.layers.IOutputLayer;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.gradient.DefaultGradient;
import org.deeplearning4j.nn.gradient.Gradient;
import org.deeplearning4j.optimize.Solver;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.ILossFunction;
import org.nd4j.linalg.util.FeatureUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * LossLayer is a flexible output "layer" that performs a loss function on
 * an input without MLP logic.
 *
 * @author Justin Long (crockpotveggies)
 */
public class LossLayer extends BaseLayer<org.deeplearning4j.nn.conf.layers.LossLayer>
                implements Serializable, IOutputLayer {

    //current input and label matrices
    protected INDArray labels;

    private transient Solver solver;

    private double fullNetworkL1;
    private double fullNetworkL2;

    public LossLayer(NeuralNetConfiguration conf) {
        super(conf);
    }

    public LossLayer(NeuralNetConfiguration conf, INDArray input) {
        super(conf, input);
    }

    /** Compute score after labels and input have been set.
     * @param fullNetworkL1 L1 regularization term for the entire network
     * @param fullNetworkL2 L2 regularization term for the entire network
     * @param training whether score should be calculated at train or test time (this affects things like application of
     *                 dropout, etc)
     * @return score (loss function)
     */
    @Override
    public double computeScore(double fullNetworkL1, double fullNetworkL2, boolean training) {
        if (input == null || labels == null)
            throw new IllegalStateException("Cannot calculate score without input and labels");
        this.fullNetworkL1 = fullNetworkL1;
        this.fullNetworkL2 = fullNetworkL2;
        INDArray preOut = input;

        ILossFunction lossFunction = layerConf().getLossFn();

        //double score = lossFunction.computeScore(getLabels2d(), preOut, layerConf().getActivationFunction(), maskArray, false);
        double score = lossFunction.computeScore(getLabels2d(), preOut, layerConf().getActivationFn(), maskArray,
                        false);
        score += fullNetworkL1 + fullNetworkL2;
        score /= getInputMiniBatchSize();

        this.score = score;

        return score;
    }

    /**Compute the score for each example individually, after labels and input have been set.
     *
     * @param fullNetworkL1 L1 regularization term for the entire network (or, 0.0 to not include regularization)
     * @param fullNetworkL2 L2 regularization term for the entire network (or, 0.0 to not include regularization)
     * @return A column INDArray of shape [numExamples,1], where entry i is the score of the ith example
     */
    @Override
    public INDArray computeScoreForExamples(double fullNetworkL1, double fullNetworkL2) {
        if (input == null || labels == null)
            throw new IllegalStateException("Cannot calculate score without input and labels");
        INDArray preOut = input;

        ILossFunction lossFunction = layerConf().getLossFn();
        INDArray scoreArray =
                        lossFunction.computeScoreArray(getLabels2d(), preOut, layerConf().getActivationFn(), maskArray);
        double l1l2 = fullNetworkL1 + fullNetworkL2;
        if (l1l2 != 0.0) {
            scoreArray.addi(l1l2);
        }
        return scoreArray;
    }

    @Override
    public void computeGradientAndScore() {
        if (input == null || labels == null)
            return;

        INDArray preOut = input;
        Pair<Gradient, INDArray> pair = getGradientsAndDelta(preOut);
        this.gradient = pair.getFirst();

        score = computeScore(fullNetworkL1, fullNetworkL2, true);
    }

    @Override
    protected void setScoreWithZ(INDArray z) {
        throw new RuntimeException("Not yet implemented");
    }

    @Override
    public Pair<Gradient, Double> gradientAndScore() {
        return new Pair<>(gradient(), score());
    }

    @Override
    public Pair<Gradient, INDArray> backpropGradient(INDArray epsilon) {
        return getGradientsAndDelta(input);
    }


    /** Returns tuple: {Gradient,Delta,Output} given preOut */
    private Pair<Gradient, INDArray> getGradientsAndDelta(INDArray preOut) {
        // delta calculation
        ILossFunction lossFunction = layerConf().getLossFn();
        INDArray delta = lossFunction.computeGradient(getLabels2d(), preOut, layerConf().getActivationFn(), maskArray);

        // grab the empty gradient
        Gradient gradient = new DefaultGradient();

        return new Pair<>(gradient, delta);
    }

    /**
     * Gets the gradient from one training iteration
     * @return the gradient (bias and weight matrix)
     */
    @Override
    public Gradient gradient() {
        return gradient;
    }

    @Override
    public double calcL2(boolean backpropParamsOnly) {
        return 0;
    }

    @Override
    public double calcL1(boolean backpropParamsOnly) {
        return 0;
    }

    @Override
    public Type type() {
        return Type.FEED_FORWARD;
    }

    @Override
    public void fit(INDArray input) {
        // no-op
    }

    @Override
    public INDArray activate(boolean training) {
        INDArray z = input;
        //INDArray ret = Nd4j.getExecutioner().execAndReturn(Nd4j.getOpFactory().createTransform(
        //    conf.getLayer().getActivationFunction(), z.dup(), conf.getExtraArgs() ));
        INDArray ret = conf.getLayer().getActivationFn().getActivation(z.dup(), training);

        if (maskArray != null) {
            ret.muliColumnVector(maskArray);
        }

        return ret;
    }

    @Override
    public INDArray activate(INDArray input, boolean training) {
        setInput(input);
        return output(training);
    }

    @Override
    public INDArray activate(INDArray input) {
        setInput(input);
        return output(true);
    }

    @Override
    public INDArray activate() {
        return output(false);
    }

    public INDArray output(INDArray input, boolean training) {
        setInput(input);
        return output(training);
    }

    public INDArray output(INDArray input) {
        setInput(input);
        return output(false);
    }

    /**
     * Classify input
     * @param training determines if its training
     * the input (can either be a matrix or vector)
     * If it's a matrix, each row is considered an example
     * and associated rows are classified accordingly.
     * Each row will be the likelihood of a label given that example
     * @return a probability distribution for each row
     */
    public INDArray output(boolean training) {
        if (input == null)
            throw new IllegalArgumentException("No null input allowed");
        return activate(training);
    }

    @Override
    public Layer transpose() {
        throw new UnsupportedOperationException("Not applicable");
    }

    @Override
    public boolean isPretrainLayer() {
        return false;
    }


    @Override
    public Gradient calcGradient(Gradient layerError, INDArray indArray) {
        throw new UnsupportedOperationException("Not applicable");
    }

    @Override
    public void merge(Layer layer, int batchSize) {
        throw new UnsupportedOperationException();
    }

    @Override
    public INDArray params() {
        return null;
    }



    /**
     * Sets the input and labels and returns a score for the prediction
     * wrt true labels
     *
     * @param data the data to score
     * @return the score for the given input,label pairs
     */
    @Override
    public double f1Score(DataSet data) {
        return f1Score(data.getFeatures(), data.getLabels());
    }

    /**
     * Returns the f1 score for the given examples.
     * Think of this to be like a percentage right.
     * The higher the number the more it got right.
     * This is on a scale from 0 to 1.
     *
     * @param examples te the examples to classify (one example in each row)
     * @param labels   the true labels
     * @return the scores for each ndarray
     */
    @Override
    public double f1Score(INDArray examples, INDArray labels) {
        Evaluation eval = new Evaluation();
        eval.eval(labels, labelProbabilities(examples));
        return eval.f1();
    }

    /**
     * Returns the number of possible labels
     *
     * @return the number of possible labels for this classifier
     */
    @Override
    public int numLabels() {
        return labels.size(1);
    }

    @Override
    public void fit(DataSetIterator iter) {
        // no-op
    }

    /**
     * Returns the predictions for each example in the dataset
     * @param input the matrix to predict
     * @return the prediction for the dataset
     */
    @Override
    public int[] predict(INDArray input) {
        INDArray output = output(input);
        int[] ret = new int[input.rows()];
        for (int i = 0; i < ret.length; i++)
            ret[i] = Nd4j.getBlasWrapper().iamax(output.getRow(i));
        return ret;
    }

    /**
     * Return predicted label names
     *
     * @param dataSet to predict
     * @return the predicted labels for the dataSet
     */
    @Override
    public List<String> predict(DataSet dataSet) {
        int[] intRet = predict(dataSet.getFeatures());
        List<String> ret = new ArrayList<>();
        for (int i : intRet) {
            ret.add(i, dataSet.getLabelName(i));
        }
        return ret;
    }

    /**
     * Returns the probabilities for each label
     * for each example row wise
     *
     * @param examples the examples to classify (one example in each row)
     * @return the likelihoods of each example and each label
     */
    @Override
    public INDArray labelProbabilities(INDArray examples) {
        return output(examples);
    }

    /**
     * Fit the model
     *
     * @param input the examples to classify (one example in each row)
     * @param labels   the example labels(a binary outcome matrix)
     */
    @Override
    public void fit(INDArray input, INDArray labels) {
        setInput(input);
        setLabels(labels);
        applyDropOutIfNecessary(true);
        if (solver == null) {
            solver = new Solver.Builder().configure(conf()).listeners(getListeners()).model(this).build();
            //Set the updater state view array. For MLN and CG, this is done by MultiLayerUpdater and ComputationGraphUpdater respectively
            Updater updater = solver.getOptimizer().getUpdater();
            int updaterStateSize = updater.stateSizeForLayer(this);
            if (updaterStateSize > 0)
                updater.setStateViewArray(this, Nd4j.createUninitialized(new int[] {1, updaterStateSize}, Nd4j.order()),
                                true);
        }
        solver.optimize();
    }

    /**
     * Fit the model
     *
     * @param data the data to train on
     */
    @Override
    public void fit(DataSet data) {
        fit(data.getFeatures(), data.getLabels());
    }

    /**
     * Fit the model
     *
     * @param examples the examples to classify (one example in each row)
     * @param labels   the labels for each example (the number of labels must match
     */
    @Override
    public void fit(INDArray examples, int[] labels) {
        INDArray outcomeMatrix = FeatureUtil.toOutcomeMatrix(labels, numLabels());
        fit(examples, outcomeMatrix);

    }

    @Override
    public void clear() {
        super.clear();
        if (labels != null) {
            labels.data().destroy();
            labels = null;
        }
        solver = null;
    }

    @Override
    public void iterate(INDArray input) {
        throw new UnsupportedOperationException();
    }

    @Override
    public INDArray getLabels() {
        return labels;
    }

    public void setLabels(INDArray labels) {
        this.labels = labels;
    }

    protected INDArray getLabels2d() {
        if (labels.rank() > 2) {
            return labels.reshape(labels.size(2), labels.size(1));
        }
        return labels;
    }

}
