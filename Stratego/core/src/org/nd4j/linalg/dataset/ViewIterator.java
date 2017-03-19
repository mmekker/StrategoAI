package org.nd4j.linalg.dataset;

import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;

import java.util.List;

/**
 * Iterate over a dataset
 * with views
 *
 * @author Adam Gibson
 */
public class ViewIterator implements DataSetIterator {
    private int batchSize = -1;
    private int cursor = 0;
    private DataSet data;
    private DataSetPreProcessor preProcessor;

    public ViewIterator(DataSet data, int batchSize) {
        this.batchSize = batchSize;
        this.data = data;
    }

    @Override
    public DataSet next(int num) {
        throw new UnsupportedOperationException("Only allowed to retrieve dataset based on batch size");
    }

    @Override
    public int totalExamples() {
        return data.numExamples();
    }

    @Override
    public int inputColumns() {
        return data.numInputs();
    }

    @Override
    public int totalOutcomes() {
        return data.numOutcomes();
    }

    @Override
    public boolean resetSupported() {
        return true;
    }

    @Override
    public boolean asyncSupported() {
        //Already all in memory
        return false;
    }

    @Override
    public void reset() {
        cursor = 0;
    }

    @Override
    public int batch() {
        return batchSize;
    }

    @Override
    public int cursor() {
        return cursor;
    }

    @Override
    public int numExamples() {
        return data.numExamples();
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
        return null;
    }

    @Override
    public boolean hasNext() {
        return cursor < numExamples();
    }

    @Override
    public void remove() {}

    @Override
    public DataSet next() {
        //only loop to the end
        if (cursor + batch() > numExamples())
            cursor = cursor() + batch() - numExamples();
        DataSet next = (DataSet) data.getRange(cursor, cursor + batch());
        if (preProcessor != null)
            preProcessor.preProcess(next);
        cursor += batch();
        return next;
    }
}
