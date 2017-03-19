package org.nd4j.linalg.dataset.api.iterator;

import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a dataset into k folds.
 * DataSet is duplicated in memory once
 * call .next() to get the k-1 folds to train on and call .testfold() to get the corresponding kth fold for testing
 * @author Susan Eraly
 */
public class KFoldIterator implements DataSetIterator {
    private DataSet singleFold;
    private int k;
    private int batch;
    private int lastBatch;
    private int kCursor = 0;
    private DataSet test;
    private DataSet train;
    protected DataSetPreProcessor preProcessor;

    public KFoldIterator(DataSet singleFold) {
        this(10, singleFold);
    }

    /**Create an iterator given the dataset and a value of k (optional, defaults to 10)
     * If number of samples in the dataset is not a multiple of k, the last fold will have less samples with the rest having the same number of samples.
     *
     * @param k number of folds (optional, defaults to 10)
     * @param singleFold DataSet to split into k folds
     */

    public KFoldIterator(int k, DataSet singleFold) {
        this.k = k;
        this.singleFold = singleFold.copy();
        if (k <= 1)
            throw new IllegalArgumentException();
        if (singleFold.numExamples() % k != 0) {
            if (k != 2) {
                this.batch = singleFold.numExamples() / (k - 1);
                this.lastBatch = singleFold.numExamples() % (k - 1);
            } else {
                this.lastBatch = singleFold.numExamples() / 2;
                this.batch = this.lastBatch + 1;
            }
        } else {
            this.batch = singleFold.numExamples() / k;
            this.lastBatch = singleFold.numExamples() / k;
        }
    }

    @Override
    public DataSet next(int num) throws UnsupportedOperationException {
        return null;
    }

    /**
     * Returns total number of examples in the dataset (all k folds)
     *
     * @return total number of examples in the dataset including all k folds
     */
    @Override
    public int totalExamples() {
        return singleFold.getLabels().size(0);
    }

    @Override
    public int inputColumns() {
        return singleFold.getFeatures().size(1);
    }

    @Override
    public int totalOutcomes() {
        return singleFold.getLabels().size(1);
    }

    @Override
    public boolean resetSupported() {
        return true;
    }

    @Override
    public boolean asyncSupported() {
        return false;
    }

    /**
     * Shuffles the dataset and resets to the first fold
     *
     * @return void
     */
    @Override
    public void reset() {
        //shuffle and return new k folds
        singleFold.shuffle();
        kCursor = 0;
    }


    /**
     * The number of examples in every fold, except the last if totalexamples % k !=0
     *
     * @return examples in a fold
     */
    @Override
    public int batch() {
        return batch;
    }

    /**
     * The number of examples in the last fold
     * if totalexamples % k == 0 same as the number of examples in every other fold
     *
     * @return examples in the last fold
     */
    public int lastBatch() {
        return lastBatch;
    }

    /**
     * cursor value of zero indicates no iterations and the very first fold will be held out as test
     * cursor value of 1 indicates the the next() call will return all but second fold which will be held out as test
     * curson value of k-1 indicates the next() call will return all but the last fold which will be held out as test
     *
     * @return the value of the cursor which indicates which fold is held out as a test set
     */

    @Override
    public int cursor() {
        return kCursor;
    }

    @Override
    public int numExamples() {
        return totalExamples();
    }

    @Override
    public void setPreProcessor(DataSetPreProcessor preProcessor) {
        this.preProcessor = preProcessor;
    }

    @Override
    public DataSetPreProcessor getPreProcessor() {
        return preProcessor;
    }

    @Override
    public List<String> getLabels() {
        return singleFold.getLabelNamesList();
    }

    @Override
    public boolean hasNext() {
        return kCursor < k;
    }

    @Override
    public DataSet next() {
        nextFold();
        return train;
    }

    @Override
    public void remove() {
        // no-op
    }

    private void nextFold() {
        int left;
        int right;
        if (kCursor == k - 1) {
            left = totalExamples() - lastBatch;
            right = totalExamples();
        } else {
            left = kCursor * batch;
            right = left + batch;
        }

        List<DataSet> kMinusOneFoldList = new ArrayList<DataSet>();
        if (right < totalExamples()) {
            if (left > 0) {
                kMinusOneFoldList.add((DataSet) singleFold.getRange(0, left));
            }
            kMinusOneFoldList.add((DataSet) singleFold.getRange(right, totalExamples()));
            train = DataSet.merge(kMinusOneFoldList);
        } else {
            train = (DataSet) singleFold.getRange(0, left);
        }
        test = (DataSet) singleFold.getRange(left, right);

        kCursor++;

    }

    /**
     * @return the held out fold as a dataset
     */
    public DataSet testFold() {
        return test;
    }
}
