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

package org.nd4j.linalg.api.ndarray;


import com.google.common.primitives.Ints;
import net.ericaro.neoitertools.Generator;
import org.apache.commons.math3.util.Pair;
import org.nd4j.linalg.api.blas.BlasBufferUtil;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.complex.IComplexNDArray;
import org.nd4j.linalg.api.complex.IComplexNumber;
import org.nd4j.linalg.api.instrumentation.Instrumentation;
import org.nd4j.linalg.api.iter.FirstAxisIterator;
import org.nd4j.linalg.api.ops.executioner.OpExecutioner;
import org.nd4j.linalg.api.ops.impl.accum.*;
import org.nd4j.linalg.api.ops.impl.accum.Max;
import org.nd4j.linalg.api.ops.impl.accum.Min;
import org.nd4j.linalg.api.ops.impl.accum.distances.EuclideanDistance;
import org.nd4j.linalg.api.ops.impl.accum.distances.ManhattanDistance;
import org.nd4j.linalg.api.ops.impl.broadcast.*;
import org.nd4j.linalg.api.ops.impl.scalar.*;
import org.nd4j.linalg.api.ops.impl.scalar.comparison.*;
import org.nd4j.linalg.api.ops.impl.transforms.Negative;
import org.nd4j.linalg.api.ops.impl.transforms.arithmetic.*;
import org.nd4j.linalg.api.ops.impl.transforms.comparison.*;
import org.nd4j.linalg.api.shape.Shape;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.*;
import org.nd4j.linalg.indexing.conditions.Condition;
import org.nd4j.linalg.profiler.OpProfiler;
import org.nd4j.linalg.string.NDArrayStrings;
import org.nd4j.linalg.util.ArrayUtil;
import org.nd4j.linalg.util.LinAlgExceptions;
import org.nd4j.linalg.util.NDArrayMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.IntBuffer;
import java.util.*;

import static org.nd4j.linalg.factory.Nd4j.*;


/**
 * NDArray: (think numpy)
 * <p/>
 * A few things of note.
 * <p/>
 * An NDArray can have any number of dimensions.
 * <p/>
 * An NDArray is accessed via strides.
 * <p/>
 * Strides are how to index over
 * a contiguous block of data.
 * <p/>
 * This block of data has 2 orders(as of right now):
 * fortran and c
 *
 * @author Adam Gibson
 */
public abstract class BaseNDArray implements INDArray, Iterable {


    protected static final Logger log = LoggerFactory.getLogger(BaseNDArray.class);
    /**
     *
     */
    private static final long serialVersionUID = 3285982317165542614L;

    protected transient volatile DataBuffer shapeInformation;
    protected transient volatile DataBuffer data;
    protected int rows, columns;
    protected long length = -1;
    protected int rank;
    protected boolean cleanedUp = false;
    protected int numLeadingOnes = -1;
    protected int numTrailingOnes = -1;
    protected Boolean isVector = null;
    protected Boolean isMatrix = null;
    protected Boolean isScalar = null;
    protected boolean isWrapAround = false;
    protected int linearStride = -1;
    protected boolean attemptedToFindElementWiseStride = false;
    protected transient DataBuffer shape;
    protected transient DataBuffer stride;
    protected transient boolean compressed = false;


    //Precalculate these arrays (like [3,2,1,0], [2,1,0], [1,0], [0] etc) for use in TAD, to avoid creating same int[]s over and over
    private static final int[][] tadFinalPermuteDimensions;
    static {
        tadFinalPermuteDimensions = new int[32][0];
        tadFinalPermuteDimensions[1] = new int[] {1, 0}; //Edge case for 1d tensors: selectively apply to column vectors
        for (int i = 2; i < 32; i++) {
            tadFinalPermuteDimensions[i] = new int[i];
            for (int k = i - 1, j = 0; k >= 0; k--, j++)
                tadFinalPermuteDimensions[i][j] = k;
        }
    }

    public BaseNDArray() {}

    /**
     * Returns true if this array is compressed, and false otherwise
     * @return
     */
    @Override
    public boolean isCompressed() {
        return compressed;
    }

    /**
     * This method marks INDArray instance as compressed
     * PLEASE NOTE: Do not use this method unless you 100% have to
     *
     * @param reallyCompressed
     */
    @Override
    public void markAsCompressed(boolean reallyCompressed) {
        this.compressed = reallyCompressed;
    }

    /**
     *
     * @param buffer
     */
    public BaseNDArray(DataBuffer buffer) {
        this.data = buffer;
        if (buffer.length() >= Integer.MAX_VALUE)
            throw new IllegalArgumentException("Length of buffer can not be >= Integer.MAX_VALUE");
        int[] shape = {1, (int) buffer.length()};
        int[] stride = Nd4j.getStrides(shape);
        this.shapeInformation = Nd4j.getShapeInfoProvider().createShapeInformation(shape, stride, 0, 1, Nd4j.order());
        init(shape, stride);
    }

    /**
     *
     * @param buffer
     * @param shape
     * @param stride
     * @param offset
     * @param ordering
     */
    public BaseNDArray(DataBuffer buffer, int[] shape, int[] stride, int offset, char ordering) {
        this.data = offset > 0 ? Nd4j.createBuffer(buffer, offset, ArrayUtil.prodLong(shape)) : buffer;
        this.shapeInformation = Nd4j.getShapeInfoProvider().createShapeInformation(shape, stride, offset,
                        Shape.elementWiseStride(shape, stride, ordering == 'f'), ordering);
        init(shape, stride);
        // Shape.setElementWiseStride(this.shapeInfo(),Shape.elementWiseStride(shape, stride, ordering == 'f'));

    }

    /**
     * Initialize the ndarray as a matrix
     * with the given data (indices preserved)
     * @param data
     */
    public BaseNDArray(double[][] data) {
        this(data, Nd4j.order());
    }

    /**
     *
     * @param data
     * @param ordering
     */
    public BaseNDArray(double[][] data, char ordering) {
        this(Nd4j.createBuffer(ordering == 'c' ? ArrayUtil.flatten(data) : ArrayUtil.flattenF(data)),
                        new int[] {data.length, data[0].length},
                        Nd4j.getStrides(new int[] {data.length, data[0].length}, ordering), 0, ordering);

        for (int r = 0; r < rows; r++) {
            assert (data[r].length == columns);
        }
        /*
        this.data = Nd4j.createBuffer(length);
        
        
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                putScalar(r, c, data[r][c]);
            }
        }*/
    }


    /**
     * Create with the specified shape and buffer
     *
     * @param shape  the shape
     * @param buffer the buffer
     */
    public BaseNDArray(int[] shape, DataBuffer buffer) {
        this.data = buffer;
        init(shape, Nd4j.getStrides(shape));
    }

    /**
     * Create this ndarray with the given data and shape and 0 offset
     *
     * @param data  the data to use
     * @param shape the shape of the ndarray
     */
    public BaseNDArray(float[] data, int[] shape, char ordering) {
        this(data, shape, 0, ordering);
    }

    /**
     * @param data     the data to use
     * @param shape    the shape of the ndarray
     * @param offset   the desired offset
     * @param ordering the ordering of the ndarray
     */
    public BaseNDArray(float[] data, int[] shape, int offset, char ordering) {
        this(data, shape, Nd4j.getStrides(shape, ordering), offset);

    }


    /**
     * Construct an ndarray of the specified shape
     * with an empty data array
     *
     * @param shape    the shape of the ndarray
     * @param stride   the stride of the ndarray
     * @param offset   the desired offset
     * @param ordering the ordering of the ndarray
     */
    public BaseNDArray(int[] shape, int[] stride, int offset, char ordering) {
        this(Nd4j.createBuffer(ArrayUtil.prodLong(shape)), shape, stride, offset, ordering);
    }

    /**
     * Construct an ndarray of the specified shape.
     *
     * @param shape    the shape of the ndarray
     * @param stride   the stride of the ndarray
     * @param offset   the desired offset
     * @param ordering the ordering of the ndarray
     * @param initialize Whether to initialize the INDArray. If true: initialize. If false: don't.
     */
    public BaseNDArray(int[] shape, int[] stride, int offset, char ordering, boolean initialize) {
        this(Nd4j.createBuffer(ArrayUtil.prodLong(shape), initialize), shape, stride, offset, ordering);
    }


    /**
     * Create the ndarray with
     * the specified shape and stride and an offset of 0
     *
     * @param shape    the shape of the ndarray
     * @param stride   the stride of the ndarray
     * @param ordering the ordering of the ndarray
     */
    public BaseNDArray(int[] shape, int[] stride, char ordering) {
        this(shape, stride, 0, ordering);
    }


    /**
     *
     * @param shape
     * @param offset
     * @param ordering
     */
    public BaseNDArray(int[] shape, int offset, char ordering) {
        this(shape, Nd4j.getStrides(shape, ordering), offset, ordering);
    }


    /**
     * Create an ndarray
     * with the given shape
     * @param shape
     */
    public BaseNDArray(int[] shape) {
        this(shape, 0, Nd4j.order());
    }


    /**
     * Creates a new <i>n</i> times <i>m</i> <tt>DoubleMatrix</tt>.
     *
     * @param newRows    the number of rows (<i>n</i>) of the new matrix.
     * @param newColumns the number of columns (<i>m</i>) of the new matrix.
     */
    public BaseNDArray(int newRows, int newColumns, char ordering) {
        this.data = Nd4j.createBuffer((long) newRows * newColumns);
        int[] shape = new int[] {newRows, newColumns};
        int[] stride = Nd4j.getStrides(shape, ordering);
        this.shapeInformation = Nd4j.getShapeInfoProvider().createShapeInformation(shape, stride, 0,
                        Shape.elementWiseStride(shape, stride, ordering == 'f'), ordering);
        init(shape, stride);
        //    Shape.setElementWiseStride(this.shapeInfo(),Shape.elementWiseStride(shape, stride, ordering == 'f'));

    }


    /**
     * Create an ndarray from the specified slices.
     * This will go through and merge all of the
     * data from each slice in to one ndarray
     * which will then take the specified shape
     *
     * @param slices the slices to merge
     * @param shape  the shape of the ndarray
     */
    public BaseNDArray(List<INDArray> slices, int[] shape, char ordering) {
        this(slices, shape, Nd4j.getStrides(shape, ordering), ordering);
    }


    /**
     * Create an ndarray from the specified slices.
     * This will go through and merge all of the
     * data from each slice in to one ndarray
     * which will then take the specified shape
     *
     * @param slices the slices to merge
     * @param shape  the shape of the ndarray
     */
    public BaseNDArray(List<INDArray> slices, int[] shape, int[] stride, char ordering) {
        DataBuffer ret = slices.get(0).data().dataType() == (DataBuffer.Type.FLOAT)
                        ? Nd4j.createBuffer(new float[ArrayUtil.prod(shape)])
                        : Nd4j.createBuffer(new double[ArrayUtil.prod(shape)]);
        this.data = ret;
        this.shapeInformation = Nd4j.getShapeInfoProvider().createShapeInformation(shape, stride, 0,
                        Shape.elementWiseStride(shape, stride, ordering == 'f'), ordering);
        init(shape, stride);
        //    Shape.setElementWiseStride(this.shapeInfo(),Shape.elementWiseStride(shape, stride, ordering == 'f'));

        if (slices.get(0).isScalar()) {
            for (int i = 0; i < length(); i++) {
                putScalar(i, slices.get(i).getDouble(0));
            }
        } else {
            for (int i = 0; i < slices(); i++) {
                putSlice(i, slices.get(i));
            }
        }

    }

    /**
     *
     * @param data
     * @param shape
     * @param stride
     * @param ordering
     */
    public BaseNDArray(float[] data, int[] shape, int[] stride, char ordering) {
        this(data, shape, stride, 0, ordering);
    }

    /**
     *
     * @param data
     * @param shape
     * @param stride
     * @param offset
     * @param ordering
     */
    public BaseNDArray(float[] data, int[] shape, int[] stride, int offset, char ordering) {
        this.shapeInformation = Nd4j.getShapeInfoProvider().createShapeInformation(shape, stride, offset,
                        Shape.elementWiseStride(shape, stride, ordering == 'f'), ordering);
        if (data != null && data.length > 0) {
            this.data = Nd4j.createBuffer(data, offset);
            if (offset >= data.length)
                throw new IllegalArgumentException("invalid offset: must be < data.length");
        }

        init(shape, stride);
        // Shape.setElementWiseStride(this.shapeInfo(),Shape.elementWiseStride(shape,stride,ordering == 'f'));

    }

    /**
     *
     * @param data
     * @param shape
     * @param stride
     * @param offset
     */
    public BaseNDArray(DataBuffer data, int[] shape, int[] stride, int offset) {
        this.data = Nd4j.createBuffer(data, offset, ArrayUtil.prodLong(shape));
        this.shapeInformation = Nd4j.getShapeInfoProvider().createShapeInformation(shape, stride, offset,
                        Shape.elementWiseStride(shape, stride, Nd4j.order() == 'f'), Nd4j.order());
        init(shape, stride);
        //  Shape.setElementWiseStride(this.shapeInfo(),Shape.elementWiseStride(shape, stride, Nd4j.order() == 'f'));


    }

    /**
     *
     * @param data
     * @param shape
     * @param strides
     */
    public BaseNDArray(int[] data, int[] shape, int[] strides) {
        this(Nd4j.createBuffer(data), shape, strides);
    }

    /**
     *
     * @param data
     * @param shape
     */
    public BaseNDArray(DataBuffer data, int[] shape) {
        this(data, shape, Nd4j.getStrides(shape, Nd4j.order()), 0, Nd4j.order());
    }


    /**
     *
     * @param buffer
     * @param shape
     * @param offset
     */
    public BaseNDArray(DataBuffer buffer, int[] shape, int offset) {
        this(Nd4j.createBuffer(buffer, offset, ArrayUtil.prodLong(shape)), shape, Nd4j.getStrides(shape), offset,
                        Nd4j.order());
    }

    /**
     *
     * @param buffer
     * @param shape
     * @param ordering
     */
    public BaseNDArray(DataBuffer buffer, int[] shape, char ordering) {
        this(buffer, shape, Nd4j.getStrides(shape, ordering), 0, ordering);
    }

    /**
     *
     * @param data
     * @param shape
     * @param ordering
     */
    public BaseNDArray(double[] data, int[] shape, char ordering) {
        this(Nd4j.createBuffer(data), shape, ordering);
    }

    /**
     *
     * @param data
     * @param shape
     * @param stride
     * @param offset
     * @param ordering
     */
    public BaseNDArray(double[] data, int[] shape, int[] stride, int offset, char ordering) {
        this(Nd4j.createBuffer(data, offset), shape, stride, offset, ordering);
    }

    /**
     *
     * @param data
     * @param order
     */
    public BaseNDArray(float[] data, char order) {
        this(Nd4j.createBuffer(data), order);
    }

    /**
     *
     * @param floatBuffer
     * @param order
     */
    public BaseNDArray(DataBuffer floatBuffer, char order) {
        this(floatBuffer, new int[] {(int) floatBuffer.length()},
                        Nd4j.getStrides(new int[] {(int) floatBuffer.length()}, order), 0, order);
        if (floatBuffer.length() >= Integer.MAX_VALUE)
            throw new IllegalArgumentException("Length of buffer can not be >= Integer.MAX_VALUE");
    }

    /**
     *
     * @param buffer
     * @param shape
     * @param strides
     */
    public BaseNDArray(DataBuffer buffer, int[] shape, int[] strides) {
        this(buffer, shape, strides, 0, Nd4j.order());
    }


    /**
     * Create this ndarray with the given data and shape and 0 offset
     *
     * @param data  the data to use
     * @param shape the shape of the ndarray
     */
    public BaseNDArray(float[] data, int[] shape) {
        this(data, shape, 0);
    }


    /**
     *
     * @param data
     * @param shape
     * @param offset
     */
    public BaseNDArray(float[] data, int[] shape, int offset) {
        this(data, shape, offset, Nd4j.order());

    }

    /**
     * Construct an ndarray of the specified shape
     * with an empty data array
     *
     * @param shape  the shape of the ndarray
     * @param stride the stride of the ndarray
     * @param offset the desired offset
     */
    public BaseNDArray(int[] shape, int[] stride, int offset) {
        this(new float[ArrayUtil.prod(shape)], shape, stride, offset, Nd4j.order());
    }

    /**
     * Create the ndarray with
     * the specified shape and stride and an offset of 0
     *
     * @param shape  the shape of the ndarray
     * @param stride the stride of the ndarray
     */
    public BaseNDArray(int[] shape, int[] stride) {
        this(shape, stride, 0);
    }

    /**
     *
     * @param shape
     * @param offset
     */
    public BaseNDArray(int[] shape, int offset) {
        this(shape, Nd4j.getStrides(shape), offset);
    }

    /**
     *
     * @param shape
     * @param ordering
     */
    public BaseNDArray(int[] shape, char ordering) {
        this(shape, 0, ordering);
    }


    /**
     * Creates a new <i>n</i> times <i>m</i> <tt>DoubleMatrix</tt>.
     *
     * @param newRows    the number of rows (<i>n</i>) of the new matrix.
     * @param newColumns the number of columns (<i>m</i>) of the new matrix.
     */
    public BaseNDArray(int newRows, int newColumns) {
        this(newRows, newColumns, Nd4j.order());
    }


    /**
     * Create an ndarray from the specified slices.
     * This will go through and merge all of the
     * data from each slice in to one ndarray
     * which will then take the specified shape
     *
     * @param slices the slices to merge
     * @param shape  the shape of the ndarray
     */
    public BaseNDArray(List<INDArray> slices, int[] shape) {
        this(slices, shape, Nd4j.order());
    }

    /**
     * Create an ndarray from the specified slices.
     * This will go through and merge all of the
     * data from each slice in to one ndarray
     * which will then take the specified shape
     *
     * @param slices the slices to merge
     * @param shape  the shape of the ndarray
     */
    public BaseNDArray(List<INDArray> slices, int[] shape, int[] stride) {
        this(slices, shape, stride, Nd4j.order());

    }

    /**
     *
     * @param data
     * @param shape
     * @param stride
     */
    public BaseNDArray(float[] data, int[] shape, int[] stride) {
        this(data, shape, stride, Nd4j.order());
    }


    /**
     *
     * @param data
     * @param shape
     * @param stride
     * @param offset
     */
    public BaseNDArray(float[] data, int[] shape, int[] stride, int offset) {
        this(data, shape, stride, offset, Nd4j.order());
    }

    /**
     *
     * @param data
     */
    public BaseNDArray(float[] data) {
        this(Nd4j.createBuffer(data));
    }


    /**
     * Initialize the ndarray
     * with the given data
     * @param data
     */
    public BaseNDArray(float[][] data) {
        this(data, Nd4j.order());
    }

    /**
     *
     * @param data
     * @param ordering
     */
    public BaseNDArray(float[][] data, char ordering) {
        this(Nd4j.createBuffer(ordering == 'c' ? ArrayUtil.flatten(data) : ArrayUtil.flattenF(data)),
                        new int[] {data.length, data[0].length},
                        Nd4j.getStrides(new int[] {data.length, data[0].length}, ordering), 0, ordering);

        for (int r = 0; r < rows; r++) {
            assert (data[r].length == columns);
        }
        /*
        this.data = Nd4j.createBuffer(length);
        
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < columns; c++) {
                putScalar(r, c, data[r][c]);
            }
        }
        */
    }



    /**
     * Constructor for stride and offset
     *
     * @param buffer
     * @param shape
     * @param offset
     * @param ordering
     */
    public BaseNDArray(DataBuffer buffer, int[] shape, int offset, char ordering) {
        this(buffer, shape, Nd4j.getStrides(shape, ordering), offset, ordering);
    }

    public BaseNDArray(double[] data, int[] shape, int[] stride, int offset) {
        this(Nd4j.createBuffer(data), shape, stride, offset);
    }


    @Override
    public void setWrapAround(boolean wrapAround) {
        this.isWrapAround = wrapAround;
    }

    @Override
    public boolean isWrapAround() {
        return isWrapAround;
    }

    /**
     * Returns whether the ndarray is valid or not
     * @return true if the ndarray is valid
     * false otherwise
     */
    public boolean isValid() {
        try {
            linearIndex(length() - 1);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public INDArray linearViewColumnOrder() {
        return this;
    }

    protected INDArray create(DataBuffer data, int[] shape, int offset) {
        if (this instanceof IComplexNDArray)
            return Nd4j.createComplex(data, shape, offset);
        else
            return Nd4j.create(data, shape, offset);
    }



    /**
     * Returns a linear view reference of shape
     * 1,length(ndarray)
     *
     * @return the linear view of this ndarray
     */
    @Override
    public INDArray linearView() {
        return this;
    }

    @Override
    public void resetLinearView() {

    }



    @Override
    public int elementWiseStride() {
        /*
        if(Shape.elementWiseStride(shapeInfo()) < 0 && !attemptedToFindElementWiseStride) {
            INDArray reshapeAttempt = Shape.newShapeNoCopy(this,new int[]{1,length()}, ordering() == 'f');
            if(reshapeAttempt != null && reshapeAttempt.elementWiseStride() > 0) {
               Shape.setElementWiseStride(shapeInfo(), reshapeAttempt.stride(-1));
               this.shapeInformation = Nd4j.getShapeInfoProvider().createShapeInformation(shape(), stride(), offset(),reshapeAttempt.stride(-1), ordering());
            }
            attemptedToFindElementWiseStride = true;
        
        }
        */
        return Shape.elementWiseStride(shapeInfoDataBuffer());
    }

    @Override
    public int elementStride() {
        return 1;
    }

    @Override
    public int majorStride() {
        setLinearStride();
        return stride(-1);
    }

    @Override
    @Deprecated
    public int secondaryStride() {
        return majorStride();
    }

    @Override
    public int tensorssAlongDimension(int... dimension) {
        if (dimension == null || dimension.length == 0)
            throw new IllegalArgumentException("Invalid input: dimensions not specified (null or length 0)");
        if (dimension.length >= rank())
            return 1;
        for (int i = 0; i < dimension.length; i++)
            if (dimension[i] < 0)
                dimension[i] += rank();
        int[] tensorShape = ArrayUtil.keep(shape(), dimension);
        long len = ArrayUtil.prodLong(tensorShape);
        if (len == 0)
            throw new IllegalStateException("Illegal length found after removing index");
        if (length / len >= Integer.MAX_VALUE)
            throw new IllegalArgumentException("Tensors along dimension can not be >= Integer.MAX_VALUE");
        return (int) (length / len);
    }

    @Override
    public INDArray tensorAlongDimension(int index, int... dimension) {
        if (dimension == null || dimension.length == 0)
            throw new IllegalArgumentException("Invalid input: dimensions not specified (null or length 0)");

        if (dimension.length >= rank())
            return this;
        for (int i = 0; i < dimension.length; i++)
            if (dimension[i] < 0)
                dimension[i] += rank();

        //dedup
        dimension = Ints.toArray(new ArrayList<>(new TreeSet<>(Ints.asList(dimension))));

        if (dimension.length > 1) {
            Arrays.sort(dimension);
        }

        int tads = tensorssAlongDimension(dimension);
        if (index >= tads)
            throw new IllegalArgumentException("Illegal index " + index + " out of tads " + tads);


        if (dimension.length == 1) {
            if (dimension[0] == 0 && isColumnVector()) {
                return this.transpose();
            } else if (dimension[0] == 1 && isRowVector()) {
                return this;
            }
        }

        Pair<DataBuffer, DataBuffer> tadInfo =
                        Nd4j.getExecutioner().getTADManager().getTADOnlyShapeInfo(this, dimension);
        DataBuffer shapeInfo = tadInfo.getFirst();
        int[] shape = Shape.shape(shapeInfo);
        int[] stride = Shape.stride(shapeInfo).asInt();
        int offset = offset() + tadInfo.getSecond().getInt(index);
        INDArray toTad = Nd4j.create(data(), shape, stride, offset);
        BaseNDArray baseNDArray = (BaseNDArray) toTad;

        //preserve immutability
        char newOrder = Shape.getOrder(shape, stride, 1);

        int ews = baseNDArray.shapeInfoDataBuffer().getInt(baseNDArray.shapeInfoDataBuffer().length() - 2);

        //TAD always calls permute. Permute EWS is always -1. This is not true for row vector shapes though.
        if (!Shape.isRowVectorShape(baseNDArray.shapeInfoDataBuffer()))
            ews = -1;

        // we create new shapeInfo with possibly new ews & order
        /**
         * NOTE HERE THAT ZERO IS PRESET FOR THE OFFSET AND SHOULD STAY LIKE THAT.
         * Zero is preset for caching purposes.
         * We don't actually use the offset defined in the
         * shape info data buffer.
         * We calculate and cache the offsets separately.
         *
         */
        baseNDArray.setShapeInformation(
                        Nd4j.getShapeInfoProvider().createShapeInformation(shape, stride, 0, ews, newOrder));

        return toTad;
    }

    /**
     * Get the vector along a particular dimension
     *
     * @param index     the index of the vector to getScalar
     * @param dimension the dimension to getScalar the vector from
     * @return the vector along a particular dimension
     */
    @Override
    public INDArray javaTensorAlongDimension(int index, int... dimension) {
        return doTad(index, dimension);
    }

    private void setShapeInformation(DataBuffer shapeInfo) {
        this.shapeInformation = shapeInfo;
    }


    private INDArray doTad(int index, int... dimension) {
        if (dimension == null || dimension.length == 0)
            throw new IllegalArgumentException("Invalid input: dimensions not specified (null or length 0)");

        if (dimension.length >= rank())
            return this;
        for (int i = 0; i < dimension.length; i++)
            if (dimension[i] < 0)
                dimension[i] += rank();

        if (dimension.length > 1)
            Arrays.sort(dimension);

        int tads = tensorssAlongDimension(dimension);
        if (index >= tads)
            throw new IllegalArgumentException("Illegal index " + index + " out of tads " + tads);


        if (dimension.length == 1) {
            if (dimension[0] == 0 && isColumnVector()) {
                return this.transpose();
            } else if (dimension[0] == 1 && isRowVector()) {
                return this;
            }
        }


        int[] tensorShape = ArrayUtil.keep(shape(), dimension);
        int[] reverseDimensions = ArrayUtil.reverseCopy(dimension);
        int[] remove = ArrayUtil.removeIndex(ArrayUtil.range(0, rank()), dimension);
        int[] newPermuteDims = Ints.concat(remove, reverseDimensions);
        int[] finalPermuteDims = tadFinalPermuteDimensions[dimension.length];

        INDArray permuted = permute(newPermuteDims);
        int sliceIdx = NDArrayMath.sliceOffsetForTensor(index, permuted, tensorShape);

        INDArray ret2 = permuted.slice(sliceIdx);
        if (dimension.length == tensorShape.length && ArrayUtil.prod(tensorShape) == ret2.length()) {
            if (dimension.length == 1 && ret2.isRowVector())
                return ret2;
            if (finalPermuteDims.length != ret2.rank()) {
                finalPermuteDims = new int[ret2.rank()];
                int count = 0;
                for (int i = finalPermuteDims.length - 1; i >= 0; i--)
                    finalPermuteDims[count++] = i;
            }
            return ret2.permutei(finalPermuteDims);
        }


        int length = ArrayUtil.prod(tensorShape);
        int tensorLength = ArrayUtil.prod(tensorShape);
        int offset = index * tensorLength / NDArrayMath.lengthPerSlice(ret2);

        if (sliceIdx == 0 && length == NDArrayMath.lengthPerSlice(ret2)) {
            ret2 = ret2.slice(offset);
            if (dimension.length == 1 && ret2.isRowVector())
                return ret2;
            return ret2.permutei(finalPermuteDims);
        }

        else if (length == NDArrayMath.lengthPerSlice(ret2)) {
            offset -= ret2.slices() * (offset / ret2.slices());
            ret2 = ret2.slice(offset);
            if (dimension.length == 1 && ret2.isRowVector())
                return ret2;
            return ret2.permutei(finalPermuteDims);
        }

        while (ret2.length() > length) {
            sliceIdx = NDArrayMath.sliceOffsetForTensor(index, ret2, tensorShape);
            sliceIdx -= ret2.slices() * (sliceIdx / ret2.slices());
            ret2 = ret2.slice(sliceIdx);
        }

        if (dimension.length == 1 && ret2.isRowVector())
            return ret2;
        return ret2.permutei(finalPermuteDims);
    }



    /**
     * Returns the number of possible vectors for a given dimension
     *
     * @param dimension the dimension to calculate the number of vectors for
     * @return the number of possible vectors along a dimension
     */
    @Override
    public int vectorsAlongDimension(int dimension) {
        if (dimension == 0 && isVector() || isRowVector())
            return 1;
        if (size(dimension) == 1 && !isVector()) {
            for (int i = dimension; i < rank(); i++) {
                if (size(i) != 1)
                    return vectorsAlongDimension(i);
            }

            return length();

        } else if (size(0) == 1 && !isVector()) {
            int realDimension = rank() - getLeadingOnes();
            if (length / size(realDimension) >= Integer.MAX_VALUE)
                throw new IllegalArgumentException("Vectors along dimension can not be >= Integer.MAX_VALUE");
            return (int) (length / size(realDimension));
        }

        if (dimension >= Shape.rank(shapeInformation)) {
            if (length / size(Shape.rank(shapeInformation) - 1) >= Integer.MAX_VALUE)
                throw new IllegalArgumentException("Vectors along dimension can not be >= Integer.MAX_VALUE");
            return (int) (length / size(Shape.rank(shapeInformation) - 1));
        }
        if (length / size(dimension) >= Integer.MAX_VALUE)
            throw new IllegalArgumentException("Vectors along dimension can not be >= Integer.MAX_VALUE");
        return (int) (length / size(dimension));
    }

    /**
     * Get the vector along a particular dimension
     *
     * @param index     the index of the vector to get
     * @param dimension the dimension to get the vector from
     * @return the vector along a particular dimension
     */
    @Override
    public INDArray vectorAlongDimension(int index, int dimension) {
        if (dimension < 0)
            dimension = Shape.rank(shapeInformation) + dimension;

        //return the whole thing
        if (dimension == Shape.rank(shapeInformation) - 1 && size(dimension) == 1 && rank() > 2
                        || rank() > 2 && dimension == 0 && size(dimension) == 1) {
            return this;
        }

        INDArray ret = tensorAlongDimension(index, dimension);
        if (isMatrix() && ret.isVector() && dimension == 1 && !ret.isRowVector())
            return ret.reshape(ArrayUtil.reverseCopy(ret.shape()));
        else if (isMatrix() && ret.isVector() && dimension == 0 && !ret.isColumnVector())
            return ret.reshape(ArrayUtil.reverseCopy(ret.shape()));
        return ret;
    }

    @Override
    public void setOrder(char order) {
        //Shape.setOrder(shapeInfo(),order);
        this.shapeInformation = Nd4j.getShapeInfoProvider().createShapeInformation(shape(), stride(), 0,
                        elementWiseStride(), order);
    }

    @Override
    public void setShape(int... shape) {
        /*
        if (1 > 0)
            throw new RuntimeException("setShape() called");
        
        DataBuffer shapeView = Shape.shapeOf(shapeInformation);
        for(int i = 0; i < shape.length; i++) {
            shapeView.put(i,shape[i]);
        }
        */
        this.shapeInformation = Nd4j.getShapeInfoProvider().createShapeInformation(shape, stride(), 0,
                        elementWiseStride(), ordering());
    }

    @Override
    public void setStride(int[] stride) {
        /*        if (1 > 0)
            throw new RuntimeException("setStride() called");
        
        DataBuffer strideView = Shape.stride(shapeInformation);
        for(int i = 0; i < stride.length; i++)
            strideView.put(i,stride[i]);
        */
        this.shapeInformation = Nd4j.getShapeInfoProvider().createShapeInformation(shape(), stride, 0,
                        elementWiseStride(), ordering());
    }



    /**
     * Cumulative sum along a dimension
     *
     * @param dimension the dimension to perform cumulative sum along
     * @return the cumulative sum along the specified dimension
     */
    @Override
    public INDArray cumsumi(int dimension) {

        if (isVector()) {
            double s = 0.0;
            for (int i = 0; i < length; i++) {
                s += getDouble(i);
                putScalar(i, s);
            }
        } else if (dimension == Integer.MAX_VALUE) {
            INDArray flattened = ravel();
            double prevVal = flattened.getDouble(0);
            for (int i = 1; i < flattened.length(); i++) {
                double d = prevVal + flattened.getDouble(i);
                flattened.putScalar(i, d);
                prevVal = d;
            }

            return flattened;
        } else {
            for (int i = 0; i < vectorsAlongDimension(dimension); i++) {
                INDArray vec = vectorAlongDimension(i, dimension);
                vec.cumsumi(0);

            }
        }


        return this;
    }

    @Override
    public Number normmaxNumber() {
        return normmax(Integer.MAX_VALUE).getDouble(0);
    }

    @Override
    public IComplexNumber normmaxComplex() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Number norm2Number() {
        return norm2(Integer.MAX_VALUE).getDouble(0);
    }

    @Override
    public IComplexNumber norm2Complex() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Number norm1Number() {
        return norm1(Integer.MAX_VALUE).getDouble(0);
    }

    @Override
    public IComplexNumber norm1Complex() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Number stdNumber() {
        return std(Integer.MAX_VALUE).getDouble(0);
    }

    @Override
    public IComplexNumber stdComplex() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Number prodNumber() {
        return prod(Integer.MAX_VALUE).getDouble(0);
    }

    @Override
    public IComplexNumber prodComplex() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Number meanNumber() {
        return mean(Integer.MAX_VALUE).getDouble(0);
    }

    @Override
    public IComplexNumber meanComplex() {
        throw new UnsupportedOperationException();

    }

    @Override
    public Number varNumber() {
        return var(Integer.MAX_VALUE).getDouble(0);
    }

    @Override
    public IComplexNumber varComplex() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Number maxNumber() {
        return max(Integer.MAX_VALUE).getDouble(0);
    }

    @Override
    public IComplexNumber maxComplex() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Number minNumber() {
        return min(Integer.MAX_VALUE).getDouble(0);
    }

    @Override
    public IComplexNumber minComplex() {
        throw new UnsupportedOperationException();

    }

    @Override
    public Number sumNumber() {
        return sum(Integer.MAX_VALUE).getDouble(0);
    }

    @Override
    public IComplexNumber sumComplex() {
        throw new UnsupportedOperationException();
    }

    /**
     * Cumulative sum along a dimension (in place)
     *
     * @param dimension the dimension to perform cumulative sum along
     * @return the cumulative sum along the specified dimension
     */
    @Override
    public INDArray cumsum(int dimension) {
        return dup().cumsumi(dimension);
    }

    /**
     * Assign all of the elements in the given
     * ndarray to this ndarray
     *
     * @param arr the elements to assign
     * @return this
     */
    @Override
    public INDArray assign(final INDArray arr) {
        Nd4j.getExecutioner().exec(new org.nd4j.linalg.api.ops.impl.transforms.Set(this, arr, this, length()));
        return this;

    }

    @Override
    public INDArray putScalar(int i, double value) {
        if (i < 0)
            i += rank();
        if (isScalar()) {
            if (Nd4j.getExecutioner().getProfilingMode() != OpExecutioner.ProfilingMode.DISABLED)
                OpProfiler.getInstance().processScalarCall();

            data.put(i, value);
            return this;
        }

        if (isRowVector()) {
            return putScalar(0, i, value);
        } else if (isColumnVector()) {
            return putScalar(i, 0, value);
        }
        int[] indexes = ordering() == 'c' ? Shape.ind2subC(this, i) : Shape.ind2sub(this, i);
        return putScalar(indexes, value);

    }

    @Override
    public INDArray putScalar(int i, float value) {
        return putScalar(i, (double) value);

    }

    @Override
    public INDArray putScalar(int i, int value) {
        return putScalar(i, (double) value);
    }

    @Override
    public INDArray putScalar(int[] indexes, double value) {
        for (int i = 0; i < indexes.length; i++) {
            if (indexes[i] < 0)
                indexes[i] += rank();
        }

        if (indexes.length == 1) {
            return putScalar(indexes[0], value);
        } else if (indexes.length == 2) {
            return putScalar(indexes[0], indexes[1], value);
        } else if (indexes.length == 3) {
            return putScalar(indexes[0], indexes[1], indexes[2], value);
        } else if (indexes.length == 4) {
            return putScalar(indexes[0], indexes[1], indexes[2], indexes[3], value);
        } else {
            if (Nd4j.getExecutioner().getProfilingMode() != OpExecutioner.ProfilingMode.DISABLED)
                OpProfiler.getInstance().processScalarCall();

            long offset = Shape.getOffset(shapeInformation, indexes);
            data.put(offset, value);
        }
        return this;
    }

    @Override
    public INDArray putScalar(int row, int col, double value) {
        if (Nd4j.getExecutioner().getProfilingMode() != OpExecutioner.ProfilingMode.DISABLED)
            OpProfiler.getInstance().processScalarCall();

        if (rank != 2)
            throw new IllegalStateException("Cannot use putScalar(int,int,double) on a rank " + rank + " INDArray");
        long offset = Shape.getOffsetUnsafe(shapeInformation, row, col);
        data.put(offset, value);
        return this;
    }

    @Override
    public INDArray putScalar(int dim0, int dim1, int dim2, double value) {
        if (Nd4j.getExecutioner().getProfilingMode() != OpExecutioner.ProfilingMode.DISABLED)
            OpProfiler.getInstance().processScalarCall();

        if (rank != 3)
            throw new IllegalStateException("Cannot use putScalar(int,int,int,double) on a rank " + rank + " INDArray");
        long offset = Shape.getOffsetUnsafe(shapeInformation, dim0, dim1, dim2);
        data.put(offset, value);
        return this;
    }

    @Override
    public INDArray putScalar(int dim0, int dim1, int dim2, int dim3, double value) {
        if (Nd4j.getExecutioner().getProfilingMode() != OpExecutioner.ProfilingMode.DISABLED)
            OpProfiler.getInstance().processScalarCall();

        if (rank != 4)
            throw new IllegalStateException(
                            "Cannot use putScalar(int,int,int,int,double) on a rank " + rank + " INDArray");
        long offset = Shape.getOffsetUnsafe(shapeInformation, dim0, dim1, dim2, dim3);
        data.put(offset, value);
        return this;
    }


    @Override
    public INDArray putScalar(int[] indexes, float value) {
        return putScalar(indexes, (double) value);
    }

    @Override
    public INDArray putScalar(int[] indexes, int value) {
        return putScalar(indexes, (double) value);
    }

    /**
     * Returns an ndarray with 1 if the element is epsilon equals
     *
     * @param other the number to compare
     * @return a copied ndarray with the given
     * binary conditions
     */
    @Override
    public INDArray eps(Number other) {
        return dup().epsi(other);
    }

    /**
     * Returns an ndarray with 1 if the element is epsilon equals
     *
     * @param other the number to compare
     * @return a ndarray with the given
     * binary conditions
     */
    @Override
    public INDArray epsi(Number other) {
        INDArray otherArr = Nd4j.valueArrayOf(shape(), other.doubleValue());
        return epsi(otherArr);
    }

    /**
     * epsilon equals than comparison:
     * If the given number is less than the
     * comparison number the item is 0 otherwise 1
     *
     * @param other the number to compare
     * @return
     */
    @Override
    public INDArray eps(INDArray other) {
        return dup().epsi(other);
    }

    /**
     * In place epsilon equals than comparison:
     * If the given number is less than the
     * comparison number the item is 0 otherwise 1
     *
     * @param other the number to compare
     * @return
     */
    @Override
    public INDArray epsi(INDArray other) {
        Nd4j.getExecutioner().exec(new Eps(this, other, this, length()));
        return this;
    }

    @Override
    public INDArray lt(Number other) {
        return dup().lti(other);
    }

    @Override
    public INDArray lte(Number other) {
        return dup().ltei(other);
    }

    @Override
    public INDArray lti(Number other) {

        Nd4j.getExecutioner().exec(new ScalarLessThan(this, other));
        return this;
    }

    @Override
    public INDArray ltei(Number other) {

        Nd4j.getExecutioner().exec(new ScalarLessThanOrEqual(this, other));
        return this;
    }

    @Override
    public INDArray eq(Number other) {
        return dup().eqi(other);
    }

    @Override
    public INDArray eqi(Number other) {

        Nd4j.getExecutioner().exec(new ScalarEquals(this, other));
        return this;
    }

    @Override
    public INDArray gt(Number other) {
        return dup().gti(other);
    }

    @Override
    public INDArray gte(Number other) {
        return dup().gtei(other);
    }

    @Override
    public INDArray gtei(Number other) {
        Nd4j.getExecutioner().exec(new ScalarGreaterThanOrEqual(this, other));
        return this;
    }

    @Override
    public INDArray gti(Number other) {
        Nd4j.getExecutioner().exec(new ScalarGreaterThan(this, other));
        return this;
    }

    @Override
    public INDArray lt(INDArray other) {
        return dup().lti(other);
    }

    @Override
    public INDArray lti(INDArray other) {
        Nd4j.getExecutioner().exec(new LessThan(this, other, this, length()));
        return this;
    }

    @Override
    public INDArray neq(Number other) {
        return dup().neqi(other);
    }

    @Override
    public INDArray neqi(Number other) {
        Nd4j.getExecutioner().exec(new ScalarNotEquals(this, other));
        return this;
    }

    @Override
    public INDArray neq(INDArray other) {
        return dup().neqi(other);
    }

    @Override
    public INDArray neqi(INDArray other) {
        Nd4j.getExecutioner().exec(new NotEqualTo(this, other, this, length()));
        return this;
    }

    @Override
    public INDArray eq(INDArray other) {
        return dup().eqi(other);
    }

    @Override
    public INDArray eqi(INDArray other) {
        Nd4j.getExecutioner().exec(new EqualTo(this, other, this, length()));
        return this;
    }

    @Override
    public INDArray gt(INDArray other) {
        return dup().gti(other);
    }

    @Override
    public INDArray gti(INDArray other) {
        Nd4j.getExecutioner().exec(new GreaterThan(this, other, this, length()));
        return this;
    }

    /**
     * Negate each element.
     */
    @Override
    public INDArray neg() {
        return dup().negi();
    }

    /**
     * Negate each element (in-place).
     */
    @Override
    public INDArray negi() {
        Nd4j.getExecutioner().exec(new Negative(this));
        return this;
    }

    @Override
    public INDArray rdiv(Number n, INDArray result) {
        return dup().rdivi(n, result);
    }

    @Override
    public INDArray rdivi(Number n, INDArray result) {

        if (Double.isNaN(n.doubleValue()))
            n = Nd4j.EPS_THRESHOLD;
        Nd4j.getExecutioner().exec(new ScalarReverseDivision(this, null, result, result.length(), n));
        if (Nd4j.ENFORCE_NUMERICAL_STABILITY)
            Nd4j.clearNans(result);
        return result;
    }

    @Override
    public INDArray rsub(Number n, INDArray result) {
        return dup().rsubi(n, result);
    }

    @Override
    public INDArray rsubi(Number n, INDArray result) {

        if (Double.isNaN(n.doubleValue()))
            n = Nd4j.EPS_THRESHOLD;

        Nd4j.getExecutioner().exec(new ScalarReverseSubtraction(this, null, result, result.lengthLong(), n));

        if (Nd4j.ENFORCE_NUMERICAL_STABILITY)
            Nd4j.clearNans(result);
        return result;
    }

    @Override
    public INDArray div(Number n, INDArray result) {
        return dup().divi(n, result);
    }

    @Override
    public INDArray divi(Number n, INDArray result) {

        if (Double.isNaN(n.doubleValue()))
            n = Nd4j.EPS_THRESHOLD;
        Nd4j.getExecutioner().exec(new ScalarDivision(this, null, result, result.lengthLong(), n));


        if (Nd4j.ENFORCE_NUMERICAL_STABILITY)
            Nd4j.clearNans(result);

        return result;
    }

    @Override
    public INDArray mul(Number n, INDArray result) {
        return dup().muli(n, result);
    }

    @Override
    public INDArray muli(Number n, INDArray result) {
        if (Double.isNaN(n.doubleValue()))
            n = Nd4j.EPS_THRESHOLD;
        Nd4j.getExecutioner().exec(new ScalarMultiplication(this, null, result, result.lengthLong(), n));

        if (Nd4j.ENFORCE_NUMERICAL_STABILITY)
            Nd4j.clearNans(result);

        return result;
    }

    @Override
    public INDArray sub(Number n, INDArray result) {
        return dup().subi(n, result);
    }

    @Override
    public INDArray subi(Number n, INDArray result) {

        if (Double.isNaN(n.doubleValue()))
            n = Nd4j.EPS_THRESHOLD;

        Nd4j.getExecutioner().exec(new ScalarSubtraction(this, null, result, result.lengthLong(), n));
        if (Nd4j.ENFORCE_NUMERICAL_STABILITY)
            Nd4j.clearNans(result);

        return result;
    }

    @Override
    public INDArray add(Number n, INDArray result) {
        return dup().addi(n, result);
    }

    @Override
    public INDArray addi(Number n, INDArray result) {
        if (Double.isNaN(n.doubleValue()))
            n = Nd4j.EPS_THRESHOLD;
        Nd4j.getExecutioner().exec(new ScalarAdd(this, null, result, result.lengthLong(), n));
        return this;
    }



    /**
     * Returns the element at the specified row/column
     * This will throw an exception if the
     *
     * @param row    the row of the element to return
     * @param column the row of the element to return
     * @return a scalar indarray of the element at this index
     */
    @Override
    public INDArray getScalar(int row, int column) {
        return getScalar(new int[] {row, column});
    }

    @Override
    public INDArray dup() {
        if (this.isCompressed() && this.ordering() == Nd4j.order().charValue()) {
            INDArray ret = Nd4j.createArrayFromShapeBuffer(data().dup(), this.shapeInfoDataBuffer());
            ret.markAsCompressed(true);
            return ret;
        }
        Nd4j.getCompressor().autoDecompress(this);
        INDArray ret = Shape.toOffsetZeroCopy(this);
        return ret;
    }

    @Override
    public INDArray dup(char order) {
        if (this.isCompressed() && this.ordering() == order) {
            INDArray ret = Nd4j.createArrayFromShapeBuffer(data().dup(), this.shapeInfoDataBuffer());
            ret.markAsCompressed(true);
            return ret;
        }
        Nd4j.getCompressor().autoDecompress(this);
        return Shape.toOffsetZeroCopy(this, order);
    }

    /**
     * Returns the elements at the the specified indices
     *
     * @param indices the indices to getScalar
     * @return the array with the specified elements
     */
    @Override
    public int getInt(int... indices) {
        return (int) getDouble(indices);

    }

    /**
     * Returns the elements at the the specified indices
     *
     * @param indices the indices to get
     * @return the array with the specified elements
     */
    @Override
    public double getDouble(int... indices) {
        if (Nd4j.getExecutioner().getProfilingMode() != OpExecutioner.ProfilingMode.DISABLED)
            OpProfiler.getInstance().processScalarCall();

        Nd4j.getCompressor().autoDecompress(this);

        for (int i = 0; i < indices.length; i++) {
            if (indices[i] < 0)
                indices[i] += rank();
        }
        if (indices.length == 1) {
            if (isRowVector())
                return Shape.getDouble(this, 0, indices[0]);
            else if (isColumnVector())
                return Shape.getDouble(this, indices[0], 0);
            else if (isScalar() && indices[0] == 0)
                return data().getDouble(0);
            else
                throw new IllegalStateException("Indexes length must be > 1 for non vectors and scalars");
        }
        return Shape.getDouble(this, indices);
    }

    /**
     * Returns the elements at the the specified indices
     *
     * @param indices the indices to get
     * @return the array with the specified elements
     */
    @Override
    public float getFloat(int... indices) {
        return (float) getDouble(indices);
    }

    /**
     * Test whether a matrix is scalar.
     */
    @Override
    public boolean isScalar() {
        if (isScalar != null)
            return isScalar;
        if (Shape.rank(shapeInformation) > 2) {
            isScalar = false;
        } else if (Shape.rank(shapeInformation) == 1) {
            isScalar = shapeOf().getInt(0) == 1;
        } else if (Shape.rank(shapeInformation) == 2) {
            isScalar = shapeOf().getInt(0) == 1 && shapeOf().getInt(1) == 1;
        }

        else
            isScalar = false;

        return isScalar;
    }

    /**
     * Inserts the element at the specified index
     *
     * @param indices the indices to insert into
     * @param element a scalar ndarray
     * @return a scalar ndarray of the element at this index
     */
    @Override
    public INDArray put(int[] indices, INDArray element) {

        if (!element.isScalar())
            throw new IllegalArgumentException("Unable to insert anything but a scalar");
        if (isRowVector() && indices[0] == 0 && indices.length == 2) {
            int ix = Shape.offset(shapeInformation);
            for (int i = 1; i < indices.length; i++)
                ix += indices[i] * stride(i);
            if (ix >= data.length())
                throw new IllegalArgumentException("Illegal indices " + Arrays.toString(indices));
            data.put(ix, element.getDouble(0));
        } else {
            int ix = Shape.offset(shapeInformation);
            for (int i = 0; i < indices.length; i++)
                if (size(i) != 1)
                    ix += indices[i] * stride(i);
            if (ix >= data.length())
                throw new IllegalArgumentException("Illegal indices " + Arrays.toString(indices));
            data.put(ix, element.getDouble(0));
        }


        return this;

    }

    /**
     * Inserts the element at the specified index
     *
     * @param i       the row insert into
     * @param j       the column to insert into
     * @param element a scalar ndarray
     * @return a scalar ndarray of the element at this index
     */
    @Override
    public INDArray put(int i, int j, INDArray element) {
        return put(new int[] {i, j}, element);
    }

    /**
     * Inserts the element at the specified index
     *
     * @param i       the row insert into
     * @param j       the column to insert into
     * @param element a scalar ndarray
     * @return a scalar ndarray of the element at this index
     */
    @Override
    public INDArray put(int i, int j, Number element) {
        return putScalar(new int[] {i, j}, element.doubleValue());
    }

    /**
     * Assigns the given matrix (put) to the specified slice
     *
     * @param slice the slice to assign
     * @param put   the slice to put
     * @return this for chainability
     */
    @Override
    public INDArray putSlice(int slice, INDArray put) {
        if (isScalar()) {
            assert put.isScalar() : "Invalid dimension. Can only insert a scalar in to another scalar";
            put(0, put.getScalar(0));
            return this;
        } else if (isVector()) {
            assert put.isScalar() || put.isVector() && put
                            .length() == length() : "Invalid dimension on insertion. Can only insert scalars input vectors";
            if (put.isScalar())
                putScalar(slice, put.getDouble(0));
            else
                for (int i = 0; i < length(); i++)
                    putScalar(i, put.getDouble(i));

            return this;
        }

        assertSlice(put, slice);


        INDArray view = slice(slice);

        if (put.length() == 1)
            putScalar(slice, put.getDouble(0));
        else if (put.isVector())
            for (int i = 0; i < put.length(); i++)
                view.putScalar(i, put.getDouble(i));
        else {
            assert Shape.shapeEquals(view.shape(), put.shape());
            INDArray linear = view;
            INDArray putLinearView = put;
            for (int i = 0; i < linear.length(); i++) {
                linear.putScalar(i, putLinearView.getDouble(i));
            }


        }

        return this;

    }

    protected void assertSlice(INDArray put, int slice) {

        assert slice <= slices() : "Invalid slice specified " + slice;
        int[] sliceShape = put.shape();
        if (Shape.isRowVectorShape(sliceShape)) {
            return;
        } else {
            int[] requiredShape = ArrayUtil.removeIndex(shape(), 0);

            //no need to compare for scalar; primarily due to shapes either being [1] or length 0
            if (put.isScalar())
                return;

            if (isVector() && put.isVector() && put.length() < length())
                return;
            //edge case for column vectors
            if (Shape.isColumnVectorShape(sliceShape))
                return;
            if (!Shape.shapeEquals(sliceShape, requiredShape) && !Shape.isRowVectorShape(requiredShape)
                            && !Shape.isRowVectorShape(sliceShape))
                throw new IllegalStateException(String.format("Invalid shape size of %s . Should have been %s ",
                                Arrays.toString(sliceShape), Arrays.toString(requiredShape)));

        }

    }

    /**
     * Returns true if this ndarray is 2d
     * or 3d with a singleton element
     *
     * @return true if the element is a matrix, false otherwise
     */
    public boolean isMatrix() {
        if (isMatrix != null)
            return isMatrix;
        isMatrix = (rank == 2 && (size(0) != 1 && size(1) != 1));
        return isMatrix;
    }


    @Override
    public int index(int row, int column) {
        if (!isMatrix()) {
            if (isColumnVector()) {
                int idx = linearIndex(row);
                return idx;
            } else if (isRowVector()) {
                int idx = linearIndex(column);
                return idx;
            } else
                throw new IllegalStateException("Unable to get row/column from a non matrix");
        }


        return Shape.offset(shapeInformation) + (row * stride(0) + column * stride(1));
    }

    protected INDArray newShape(int[] newShape, char ordering) {

        return create(data(), newShape, stride(), Shape.offset(shapeInformation));
    }

    protected INDArray create(DataBuffer data, int[] newShape, int[] newStrides, int offset, char ordering) {
        if (this instanceof IComplexNDArray)
            return Nd4j.createComplex(data, newShape, newStrides, offset, ordering);
        else
            return Nd4j.create(data, newShape, newStrides, offset, ordering);
    }

    protected INDArray create(DataBuffer data, int[] newShape, int[] newStrides, int offset) {
        if (this instanceof IComplexNDArray)
            return Nd4j.createComplex(data, newShape, newStrides, offset);
        else
            return Nd4j.create(data, newShape, newStrides, offset);
    }

    protected INDArray create(int[] shape) {
        if (this instanceof IComplexNDArray)
            return Nd4j.createComplex(shape, getStrides(shape, Nd4j.order()), 0);
        else
            return Nd4j.create(shape, getStrides(shape, Nd4j.order()), 0);
    }

    protected INDArray create(int[] shape, int[] strides, int offset) {
        if (this instanceof IComplexNDArray)
            return Nd4j.createComplex(shape, strides, offset);
        else
            return Nd4j.create(shape, strides, offset);
    }

    protected int[] getStrides(int[] shape, char ordering) {
        return Nd4j.getStrides(shape, ordering);
    }


    /**
     * Returns the square of the Euclidean distance.
     */
    @Override
    public double squaredDistance(INDArray other) {
        double d2 = distance2(other);
        return d2 * d2;
    }

    /**
     * Returns the (euclidean) distance.
     */
    @Override
    public double distance2(INDArray other) {
        return Nd4j.getExecutioner().execAndReturn(new EuclideanDistance(this, other)).getFinalResult().doubleValue();
    }

    /**
     * Returns the (1-norm) distance.
     */
    @Override
    public double distance1(INDArray other) {
        return Nd4j.getExecutioner().execAndReturn(new ManhattanDistance(this, other)).getFinalResult().doubleValue();
    }


    @Override
    public INDArray put(INDArrayIndex[] indices, INDArray element) {
        if (indices[0] instanceof SpecifiedIndex && element.isVector()) {
            indices[0].reset();
            int cnt = 0;
            while (indices[0].hasNext()) {
                int idx = indices[0].next();
                putScalar(idx, element.getDouble(cnt));
                cnt++;
            }
            return this;
        } else {
            return get(indices).assign(element);
        }
    }

    @Override
    public INDArray put(INDArrayIndex[] indices, Number element) {
        INDArray get = get(indices);
        for (int i = 0; i < get.length(); i++)
            get.putScalar(i, element.doubleValue());
        return this;
    }


    /**
     * Mainly here for people coming from numpy.
     * This is equivalent to a call to permute
     *
     * @param dimension the dimension to swap
     * @param with      the one to swap it with
     * @return the swapped axes view
     */
    @Override
    public INDArray swapAxes(int dimension, int with) {
        int[] shape = ArrayUtil.range(0, shape().length);
        shape[dimension] = with;
        shape[with] = dimension;
        return permute(shape);
    }


    @Override
    public boolean isView() {
        /*
            We don't really use Shape offset value anywhere
            And it's possible to be not a view, and have non-empty originalBuffer
         */
        return Shape.offset(shapeInformation) > 0 || length() < data().length() || data().originalDataBuffer() != null;
    }

    @Override
    public DataBuffer data() {
        return data;
    }

    @Override
    public void setData(DataBuffer data) {
        this.data = data;
    }

    /**
     * Number of slices: aka shape[0]
     *
     * @return the number of slices
     * for this nd array
     */
    @Override
    public int slices() {
        if (isRowVector())
            return length();

        return size(0);
    }

    @Override
    public INDArray subArray(ShapeOffsetResolution resolution) {
        int[] offsets = resolution.getOffsets();
        int[] shape = resolution.getShapes();
        int[] stride = resolution.getStrides();

        if (offset() + resolution.getOffset() >= Integer.MAX_VALUE)
            throw new IllegalArgumentException("Offset of array can not be >= Integer.MAX_VALUE");
        int offset = (int) (offset() + resolution.getOffset());

        int n = shape.length;
        if (shape.length < 1)
            return create(Nd4j.createBuffer(shape));
        if (offsets.length != n)
            throw new IllegalArgumentException("Invalid offset " + Arrays.toString(offsets));
        if (stride.length != n)
            throw new IllegalArgumentException("Invalid stride " + Arrays.toString(stride));

        if (shape.length == rank() && Shape.contentEquals(shape, shapeOf())) {
            if (ArrayUtil.isZero(offsets)) {
                return this;
            } else {
                throw new IllegalArgumentException("Invalid subArray offsets");
            }
        }

        char newOrder = Shape.getOrder(shape, stride, 1);

        return create(data, Arrays.copyOf(shape, shape.length), stride, offset, newOrder);
    }

    @Override
    public INDArray subArray(int[] offsets, int[] shape, int[] stride) {
        int n = shape.length;
        if (shape.length < 1)
            return create(Nd4j.createBuffer(shape));
        if (offsets.length != n)
            throw new IllegalArgumentException("Invalid offset " + Arrays.toString(offsets));
        if (stride.length != n)
            throw new IllegalArgumentException("Invalid stride " + Arrays.toString(stride));

        if (Shape.contentEquals(shape, shapeOf())) {
            if (ArrayUtil.isZero(offsets)) {
                return this;
            } else {
                throw new IllegalArgumentException("Invalid subArray offsets");
            }
        }

        int[] dotProductOffsets = offsets;
        int[] dotProductStride = stride;

        int offset = Shape.offset(shapeInfo()) + NDArrayIndex.offset(dotProductStride, dotProductOffsets);
        if (offset >= data().length())
            offset = ArrayUtil.sum(offsets);

        return create(data, Arrays.copyOf(shape, shape.length), stride, offset, ordering());
    }

    protected INDArray create(DataBuffer buffer) {
        return Nd4j.create(buffer);
    }

    @Override
    public INDArray cond(Condition condition) {
        return dup().condi(condition);
    }

    @Override
    public INDArray condi(Condition condition) {
        INDArray linear = this;
        for (int i = 0; i < length(); i++) {
            boolean met = condition.apply(linear.getDouble(i));
            linear.putScalar(i, met ? 1 : 0);
        }
        return this;
    }


    protected void init(int[] shape, int[] stride) {
        if (shape.length == 1) {
            rows = 1;
            columns = shape[0];
        } else if (this.shape().length == 2) {
            rows = shape[0];
            columns = shape[1];
        }

        //default row vector
        if (shape.length == 1) {
            init(new int[] {1, shape[0]}, new int[] {1, stride[0]});
        }

        //null character
        if (ordering() == '\u0000') {
            Shape.setOrder(shapeInfo(), Nd4j.order());
            throw new IllegalStateException("setOrder() shouldn't ever happen here");
        }

        this.length = ArrayUtil.prodLong(shape);
        rank = shape.length;

        // TODO: this, probably, may be reconsidered and removed
        if (this.elementWiseStride() == -1 && !attemptedToFindElementWiseStride) {
            //log.info("Calling to computeEWS");

            INDArray reshapeAttempt = Shape.newShapeNoCopy(this, new int[] {1, this.length()}, Nd4j.order() == 'f');
            if (reshapeAttempt != null) {
                //log.info("Got new EWS: " + reshapeAttempt.elementWiseStride());
                this.shapeInformation = Nd4j.getShapeInfoProvider().createShapeInformation(this.shape(), this.stride(),
                                this.offset(), reshapeAttempt.elementWiseStride(), ordering());
            }
            attemptedToFindElementWiseStride = true;
        }
    }


    @Override
    public INDArray getScalar(int i) {
        return Nd4j.scalar(getDouble(i));
    }

    /**
     * Do a row wise op (a,s,m,d)
     * a : add
     * s : subtract
     * m : multiply
     * d : divide
     * h : reverse subtraction
     * t : reverse division
     *
     * @param columnVector the column  vector
     * @param operation    the operation
     * @return
     */
    protected INDArray doColumnWise(INDArray columnVector, char operation) {
        //Input validation: require (a) columnVector to actually be a column vector, and (b) this.size(0) to match columnVector.size(0)
        if (!columnVector.isColumnVector() || this.size(0) != columnVector.size(0)) {
            throw new IllegalStateException("Mismatched shapes (shape = " + Arrays.toString(shape())
                            + ", row vector shape =" + Arrays.toString(columnVector.shape()) + ")");
        }

        if (columnVector.data().sameUnderlyingData(data()))
            return doColumnWise(columnVector.dup(), operation);
        if (isVector()) {
            switch (operation) {
                case 'a':
                    addi(columnVector);
                    break;
                case 's':
                    subi(columnVector);
                    break;
                case 'm':
                    muli(columnVector);
                    break;
                case 'd':
                    divi(columnVector);
                    break;
                case 'h':
                    rsubi(columnVector);
                    break;
                case 't':
                    rdivi(columnVector);
                    break;
            }

            return this;
        }
        if (rows() == 1 && columnVector.isScalar()) {
            applyScalarOp(columnVector, operation);
        } else {
            // special optimization case, broadcast turns into ScalarOp Along Dimension
            if (rank() == 2 && elementWiseStride() == 1 && ordering() == 'c' && columnVector.elementWiseStride() == 1) {
                switch (operation) {
                    case 'a': {
                        ScalarAdd op = new ScalarAdd(this, columnVector, this, this.length(), 0.0);
                        op.setDimension(1);
                        Nd4j.getExecutioner().exec(op);
                        break;
                    }
                    case 's': {
                        ScalarSubtraction op = new ScalarSubtraction(this, columnVector, this, this.length(), 0.0);
                        op.setDimension(1);
                        Nd4j.getExecutioner().exec(op);
                        break;
                    }
                    case 'm': {
                        ScalarMultiplication op =
                                        new ScalarMultiplication(this, columnVector, this, this.length(), 0.0);
                        op.setDimension(1);
                        Nd4j.getExecutioner().exec(op);
                        break;
                    }
                    case 'd': {
                        ScalarDivision op = new ScalarDivision(this, columnVector, this, this.length(), 0.0);
                        op.setDimension(1);
                        Nd4j.getExecutioner().exec(op);
                        break;
                    }
                    case 'h': {
                        ScalarReverseSubtraction op =
                                        new ScalarReverseSubtraction(this, columnVector, this, this.length(), 0.0);
                        op.setDimension(1);
                        Nd4j.getExecutioner().exec(op);
                        break;
                    }
                    case 't': {
                        ScalarReverseDivision op =
                                        new ScalarReverseDivision(this, columnVector, this, this.length(), 0.0);
                        op.setDimension(1);
                        Nd4j.getExecutioner().exec(op);
                        break;
                    }

                }
            } else {
                applyBroadcastOp(columnVector, operation);
            }

        }

        return this;

    }

    @Override
    public boolean isCleanedUp() {
        return cleanedUp;
    }

    @Override
    public void cleanup() {
        if (Nd4j.shouldInstrument)
            Nd4j.getInstrumentation().log(this, Instrumentation.DESTROYED);
        cleanedUp = true;
    }



    /**
     * Do a row wise op (a,s,m,d)
     * a : add
     * s : subtract
     * m : multiply
     * d : divide
     * h : reverse subtraction
     * t : reverse division
     *
     * @param rowVector the row vector
     * @param operation the operation
     * @return
     */
    protected INDArray doRowWise(final INDArray rowVector, final char operation) {
        //Input validation: require (a) rowVector to actually be a row vector, and (b) this.size(1) to match rowVector.size(1)
        if (!rowVector.isRowVector() || this.size(1) != rowVector.size(1)) {
            throw new IllegalStateException("Mismatched shapes (shape = " + Arrays.toString(shape())
                            + ", row vector shape =" + Arrays.toString(rowVector.shape()) + ")");
        }

        if (rowVector.data().sameUnderlyingData(data()))
            return doRowWise(rowVector.dup(), operation);

        if (isVector()) {
            switch (operation) {
                case 'a':
                    addi(rowVector);
                    break;
                case 's':
                    subi(rowVector);
                    break;
                case 'm':
                    muli(rowVector);
                    break;
                case 'd':
                    divi(rowVector);
                    break;
                case 'h':
                    rsubi(rowVector);
                    break;
                case 't':
                    rdivi(rowVector);
                    break;
            }

            return this;
        }

        if (rank() == 2 && columns() == 1 && rowVector.isScalar()) {
            if (this instanceof IComplexNDArray) {
                applyScalarOp(rowVector, operation);
            }
        } else {
            // special optimization case, broadcast turns into ScalarOp Along Dimension
            if (rank() == 2 && elementWiseStride() == 1 && ordering() == 'f' && rowVector.elementWiseStride() == 1) {
                switch (operation) {
                    case 'a': {
                        ScalarAdd op = new ScalarAdd(this, rowVector, this, this.length(), 0.0);
                        op.setDimension(0);
                        Nd4j.getExecutioner().exec(op);
                        break;
                    }
                    case 's': {
                        ScalarSubtraction op = new ScalarSubtraction(this, rowVector, this, this.length(), 0.0);
                        op.setDimension(0);
                        Nd4j.getExecutioner().exec(op);
                        break;
                    }
                    case 'm': {
                        ScalarMultiplication op = new ScalarMultiplication(this, rowVector, this, this.length(), 0.0);
                        op.setDimension(0);
                        Nd4j.getExecutioner().exec(op);
                        break;
                    }
                    case 'd': {
                        ScalarDivision op = new ScalarDivision(this, rowVector, this, this.length(), 0.0);
                        op.setDimension(0);
                        Nd4j.getExecutioner().exec(op);
                        break;
                    }
                    case 'h': {
                        ScalarReverseSubtraction op =
                                        new ScalarReverseSubtraction(this, rowVector, this, this.length(), 0.0);
                        op.setDimension(0);
                        Nd4j.getExecutioner().exec(op);
                        break;
                    }
                    case 't': {
                        ScalarReverseDivision op = new ScalarReverseDivision(this, rowVector, this, this.length(), 0.0);
                        op.setDimension(0);
                        Nd4j.getExecutioner().exec(op);
                        break;
                    }

                }
            } else {
                applyBroadcastOp(rowVector, operation);
            }
        }

        return this;
    }


    private void applyBroadcastOp(INDArray vector, final char operation) {
        int alongDimension = Shape.isRowVectorShape(vector.shape()) ? 1 : 0;

        // FIXME: probably this is wrong, because strict equality is always false in current DataBuffer mechanics
        if (this.data() == vector.data())
            vector = vector.dup();
        switch (operation) {
            case 'a':
                Nd4j.getExecutioner().exec(new BroadcastAddOp(this, vector, this, alongDimension), alongDimension);
                return;
            case 's':
                Nd4j.getExecutioner().exec(new BroadcastSubOp(this, vector, this, alongDimension), alongDimension);
                return;
            case 'm':
                Nd4j.getExecutioner().exec(new BroadcastMulOp(this, vector, this, alongDimension), alongDimension);
                return;
            case 'd':
                Nd4j.getExecutioner().exec(new BroadcastDivOp(this, vector, this, alongDimension), alongDimension);
                return;
            case 'h':
                Nd4j.getExecutioner().exec(new BroadcastRSubOp(this, vector, this, alongDimension), alongDimension);
                return;
            case 't':
                Nd4j.getExecutioner().exec(new BroadcastRDivOp(this, vector, this, alongDimension), alongDimension);
                return;
            case 'p':
                Nd4j.getExecutioner().exec(new BroadcastCopyOp(this, vector, this, alongDimension), alongDimension);
                return;
            default:
                throw new UnsupportedOperationException("Unknown operation: " + operation);
        }
    }

    private void applyScalarOp(INDArray vector, char operation) {
        if (this instanceof IComplexNDArray) {
            IComplexNDArray row = (IComplexNDArray) vector;
            switch (operation) {
                case 'a':
                    addi(row.getComplex(0));
                    break;
                case 's':
                    subi(row.getComplex(0));
                    break;
                case 'm':
                    muli(row.getComplex(0));
                    break;
                case 'd':
                    divi(row.getComplex(0));
                    break;
                case 'h':
                    rsubi(row.getComplex(0));
                    break;
                case 't':
                    rdivi(row.getComplex(0));
                    break;
            }
        } else {
            switch (operation) {
                case 'a':
                    addi(vector.getDouble(0));
                    break;
                case 's':
                    subi(vector.getDouble(0));
                    break;
                case 'm':
                    muli(vector.getDouble(0));
                    break;
                case 'd':
                    divi(vector.getDouble(0));
                    break;
                case 'h':
                    rsubi(vector.getDouble(0));
                    break;
                case 't':
                    rdivi(vector.getDouble(0));
                    break;
            }

        }

    }

    protected DataBuffer shapeOf() {
        if (shape == null)
            shape = Shape.shapeOf(shapeInfoDataBuffer());
        return shape;
    }

    protected DataBuffer strideOf() {
        if (stride == null)
            stride = Shape.stride(shapeInfoDataBuffer());
        return stride;
    }

    @Override
    public int stride(int dimension) {
        int rank = Shape.rank(shapeInformation);
        if (dimension < 0)
            return strideOf().getInt(dimension + rank);
        return strideOf().getInt(dimension);
    }

    @Override
    public INDArray rdiviColumnVector(INDArray columnVector) {
        return doColumnWise(columnVector, 't');
    }

    @Override
    public INDArray rdivColumnVector(INDArray columnVector) {
        return dup().rdiviColumnVector(columnVector);
    }

    @Override
    public INDArray rdiviRowVector(INDArray rowVector) {
        return doRowWise(rowVector, 't');
    }

    @Override
    public INDArray rdivRowVector(INDArray rowVector) {
        return dup().rdiviRowVector(rowVector);
    }

    @Override
    public INDArray rsubiColumnVector(INDArray columnVector) {
        return doColumnWise(columnVector, 'h');
    }

    @Override
    public INDArray rsubColumnVector(INDArray columnVector) {
        return dup().rsubiColumnVector(columnVector);
    }

    @Override
    public INDArray rsubiRowVector(INDArray rowVector) {
        return doRowWise(rowVector, 'h');
    }

    @Override
    public INDArray rsubRowVector(INDArray rowVector) {
        return dup().rsubiRowVector(rowVector);
    }

    /**
     * Inserts the element at the specified index
     *
     * @param i       the index insert into
     * @param element a scalar ndarray
     * @return a scalar ndarray of the element at this index
     */
    @Override
    public INDArray put(int i, INDArray element) {
        if (!element.isScalar())
            throw new IllegalArgumentException("Element must be a scalar");
        return putScalar(i, element.getDouble(0));
    }

    /**
     * In place addition of a column vector
     *
     * @param columnVector the column vector to add
     * @return the result of the addition
     */
    @Override
    public INDArray diviColumnVector(INDArray columnVector) {
        return doColumnWise(columnVector, 'd');
    }

    /**
     * In place addition of a column vector
     *
     * @param columnVector the column vector to add
     * @return the result of the addition
     */
    @Override
    public INDArray divColumnVector(INDArray columnVector) {
        return dup().diviColumnVector(columnVector);
    }

    /**
     * In place addition of a column vector
     *
     * @param rowVector the row vector to add
     * @return the result of the addition
     */
    @Override
    public INDArray diviRowVector(INDArray rowVector) {
        return doRowWise(rowVector, 'd');
    }

    /**
     * In place addition of a column vector
     *
     * @param rowVector the row vector to add
     * @return the result of the addition
     */
    @Override
    public INDArray divRowVector(INDArray rowVector) {
        return dup().diviRowVector(rowVector);
    }

    /**
     * In place addition of a column vector
     *
     * @param columnVector the column vector to add
     * @return the result of the addition
     */
    @Override
    public INDArray muliColumnVector(INDArray columnVector) {
        return doColumnWise(columnVector, 'm');
    }

    /**
     * In place addition of a column vector
     *
     * @param columnVector the column vector to add
     * @return the result of the addition
     */
    @Override
    public INDArray mulColumnVector(INDArray columnVector) {
        return dup().muliColumnVector(columnVector);
    }

    /**
     * In place addition of a column vector
     *
     * @param rowVector the row vector to add
     * @return the result of the addition
     */
    @Override
    public INDArray muliRowVector(INDArray rowVector) {
        return doRowWise(rowVector, 'm');
    }

    /**
     * In place addition of a column vector
     *
     * @param rowVector the row vector to add
     * @return the result of the addition
     */
    @Override
    public INDArray mulRowVector(INDArray rowVector) {
        return dup().muliRowVector(rowVector);
    }

    /**
     * In place addition of a column vector
     *
     * @param columnVector the column vector to add
     * @return the result of the addition
     */
    @Override
    public INDArray subiColumnVector(INDArray columnVector) {
        return doColumnWise(columnVector, 's');
    }

    /**
     * In place addition of a column vector
     *
     * @param columnVector the column vector to add
     * @return the result of the addition
     */
    @Override
    public INDArray subColumnVector(INDArray columnVector) {
        return dup().subiColumnVector(columnVector);
    }

    /**
     * In place addition of a column vector
     *
     * @param rowVector the row vector to add
     * @return the result of the addition
     */
    @Override
    public INDArray subiRowVector(INDArray rowVector) {
        return doRowWise(rowVector, 's');
    }

    /**
     * In place addition of a column vector
     *
     * @param rowVector the row vector to add
     * @return the result of the addition
     */
    @Override
    public INDArray subRowVector(INDArray rowVector) {
        return dup().subiRowVector(rowVector);
    }

    /**
     * In place addition of a column vector
     *
     * @param columnVector the column vector to add
     * @return the result of the addition
     */
    @Override
    public INDArray addiColumnVector(INDArray columnVector) {
        return doColumnWise(columnVector, 'a');
    }

    /**
     * In place addition of a column vector
     *
     * @param columnVector the column vector to add
     * @return the result of the addition
     */
    @Override
    public INDArray addColumnVector(INDArray columnVector) {
        return dup().addiColumnVector(columnVector);
    }

    /**
     * In place addition of a column vector
     *
     * @param rowVector the row vector to add
     * @return the result of the addition
     */
    @Override
    public INDArray addiRowVector(INDArray rowVector) {
        return doRowWise(rowVector, 'a');
    }

    /**
     * In place addition of a column vector
     *
     * @param rowVector the row vector to add
     * @return the result of the addition
     */
    @Override
    public INDArray addRowVector(INDArray rowVector) {
        return dup().addiRowVector(rowVector);
    }

    /**
     * Perform a copy matrix multiplication
     *
     * @param other the other matrix to perform matrix multiply with
     * @return the result of the matrix multiplication
     */
    @Override
    public INDArray mmul(INDArray other) {
        int[] shape = {rows(), other.columns()};
        INDArray result = createUninitialized(shape, 'f');
        if (result.isScalar())
            return Nd4j.scalar(Nd4j.getBlasWrapper().dot(this, other));
        return mmuli(other, result);
    }

    protected INDArray create(int[] shape, char ordering) {
        if (this instanceof IComplexNDArray)
            return Nd4j.createComplex(shape, ordering);
        else
            return Nd4j.create(shape, ordering);
    }

    /**
     * Perform an copy matrix multiplication
     *
     * @param other  the other matrix to perform matrix multiply with
     * @param result the result ndarray
     * @return the result of the matrix multiplication
     */
    @Override
    public INDArray mmul(INDArray other, INDArray result) {
        return dup().mmuli(other, result);
    }

    /**
     * in place (element wise) division of two matrices
     *
     * @param other the second ndarray to divide
     * @return the result of the divide
     */
    @Override
    public INDArray div(INDArray other) {
        return dup().divi(other);
    }

    /**
     * copy (element wise) division of two matrices
     *
     * @param other  the second ndarray to divide
     * @param result the result ndarray
     * @return the result of the divide
     */
    @Override
    public INDArray div(INDArray other, INDArray result) {
        return dup().divi(other, result);
    }

    /**
     * copy (element wise) multiplication of two matrices
     *
     * @param other the second ndarray to multiply
     * @return the result of the addition
     */
    @Override
    public INDArray mul(INDArray other) {
        return dup().muli(other);
    }

    /**
     * copy (element wise) multiplication of two matrices
     *
     * @param other  the second ndarray to multiply
     * @param result the result ndarray
     * @return the result of the multiplication
     */
    @Override
    public INDArray mul(INDArray other, INDArray result) {
        return dup().muli(other, result);
    }

    /**
     * copy subtraction of two matrices
     *
     * @param other the second ndarray to subtract
     * @return the result of the addition
     */
    @Override
    public INDArray sub(INDArray other) {
        return dup().subi(other);
    }

    /**
     * copy subtraction of two matrices
     *
     * @param other  the second ndarray to subtract
     * @param result the result ndarray
     * @return the result of the subtraction
     */
    @Override
    public INDArray sub(INDArray other, INDArray result) {
        return dup().subi(other, result);
    }

    /**
     * copy addition of two matrices
     *
     * @param other the second ndarray to add
     * @return the result of the addition
     */
    @Override
    public INDArray add(INDArray other) {
        return dup().addi(other);
    }

    /**
     * copy addition of two matrices
     *
     * @param other  the second ndarray to add
     * @param result the result ndarray
     * @return the result of the addition
     */
    @Override
    public INDArray add(INDArray other, INDArray result) {
        return dup().addi(other, result);
    }

    /**
     * Perform an copy matrix multiplication
     *
     * @param other the other matrix to perform matrix multiply with
     * @return the result of the matrix multiplication
     */
    @Override
    public INDArray mmuli(INDArray other) {
        return dup().mmuli(other, this);
    }

    /**
     * Perform an copy matrix multiplication
     *
     * @param other  the other matrix to perform matrix multiply with
     * @param result the result ndarray
     * @return the result of the matrix multiplication
     */
    @Override
    public INDArray mmuli(INDArray other, INDArray result) {
        LinAlgExceptions.assertMultiplies(this, other);


        if (other.isScalar()) {
            return muli(other.getDouble(0), result);
        }
        if (isScalar()) {
            return other.muli(getDouble(0), result);
        }

        /* check sizes and resize if necessary */


        if (result == this || result == other) {
            /* actually, blas cannot do multiplications in-place. Therefore, we will fake by
             * allocating a temporary object on the side and copy the result later.
             */
            INDArray temp = create(result.shape(), Nd4j.getStrides(result.shape(), 'f'));
            temp.setOrder('f');

            if (other.columns() == 1) {
                Nd4j.getBlasWrapper().level2().gemv(BlasBufferUtil.getCharForTranspose(result),
                                BlasBufferUtil.getCharForTranspose(this), 1.0, this, other, 0.0, temp);
            }

            else {
                Nd4j.getBlasWrapper().level3().gemm(BlasBufferUtil.getCharForTranspose(result),
                                BlasBufferUtil.getCharForTranspose(this), BlasBufferUtil.getCharForTranspose(temp), 1.0,
                                this, other, 0.0, temp);
            }

            result.assign(temp);


        } else {

            //We require that the result array is 'f' (fortran) order
            // However, user might have called mmuli with a c order array for the result
            // In which case, we need to allocate a temporary f order array, and later do an assign to the real result array

            boolean requiresTemp = result.ordering() == 'c';
            INDArray gemmResultArr;
            if (requiresTemp) {
                //Can use createUninitialized due to beta==0.0 parameter in gemm
                gemmResultArr = Nd4j.createUninitialized(result.shape(), 'f');
            } else {
                gemmResultArr = result;
            }

            if (other.columns() == 1) {
                Nd4j.getBlasWrapper().level2().gemv(ordering(), BlasBufferUtil.getCharForTranspose(other), 1.0, this,
                                other, 0.0, gemmResultArr);
            } else {
                Nd4j.getBlasWrapper().level3().gemm(ordering(), BlasBufferUtil.getCharForTranspose(other),
                                BlasBufferUtil.getCharForTranspose(gemmResultArr), 1.0, this, other, 0.0,
                                gemmResultArr);
            }

            if (requiresTemp) {
                result.assign(gemmResultArr);
            }
        }

        if (Nd4j.ENFORCE_NUMERICAL_STABILITY)
            Nd4j.clearNans(result);
        return result;
    }

    private INDArray create(int[] shape, int[] stride) {
        if (this instanceof IComplexNDArray)
            return Nd4j.createComplex(shape, stride);
        else
            return Nd4j.create(shape, stride);
    }

    /**
     * in place (element wise) division of two matrices
     *
     * @param other the second ndarray to divide
     * @return the result of the divide
     */
    @Override
    public INDArray divi(INDArray other) {
        return divi(other, this);
    }

    /**
     * in place (element wise) division of two matrices
     *
     * @param other  the second ndarray to divide
     * @param result the result ndarray
     * @return the result of the divide
     */
    @Override
    public INDArray divi(INDArray other, INDArray result) {
        if (other.isScalar()) {
            return divi(other.getDouble(0), result);
        }

        if (isScalar()) {
            return other.divi(getDouble(0), result);
        }

        Nd4j.getExecutioner().exec(new DivOp(this, other, result, length()));

        if (Nd4j.ENFORCE_NUMERICAL_STABILITY)
            Nd4j.clearNans(result);
        return result;
    }

    /**
     * in place (element wise) multiplication of two matrices
     *
     * @param other the second ndarray to multiply
     * @return the result of the addition
     */
    @Override
    public INDArray muli(INDArray other) {
        return muli(other, this);
    }

    /**
     * in place (element wise) multiplication of two matrices
     *
     * @param other  the second ndarray to multiply
     * @param result the result ndarray
     * @return the result of the multiplication
     */
    @Override
    public INDArray muli(INDArray other, INDArray result) {
        if (other.isScalar()) {
            return muli(other.getDouble(0), result);
        }
        if (isScalar()) {
            return other.muli(getDouble(0), result);
        }

        LinAlgExceptions.assertSameLength(other, result);

        Nd4j.getExecutioner().exec(new MulOp(this, other, result, length()));

        if (Nd4j.ENFORCE_NUMERICAL_STABILITY)
            Nd4j.clearNans(result);

        return result;
    }

    /**
     * in place subtraction of two matrices
     *
     * @param other the second ndarray to subtract
     * @return the result of the addition
     */
    @Override
    public INDArray subi(INDArray other) {
        return subi(other, this);
    }

    /**
     * in place subtraction of two matrices
     *
     * @param other  the second ndarray to subtract
     * @param result the result ndarray
     * @return the result of the subtraction
     */
    @Override
    public INDArray subi(INDArray other, INDArray result) {
        if (other.isScalar()) {
            return subi(other.getDouble(0), result);
        }
        if (isScalar()) {
            return other.subi(getDouble(0), result);
        }

        LinAlgExceptions.assertSameLength(other, result);


        Nd4j.getExecutioner().exec(new SubOp(this, other, result, length()));

        if (Nd4j.ENFORCE_NUMERICAL_STABILITY)
            Nd4j.clearNans(result);

        return result;
    }

    /**
     * in place addition of two matrices
     *
     * @param other the second ndarray to add
     * @return the result of the addition
     */
    @Override
    public INDArray addi(INDArray other) {
        return addi(other, this);
    }

    /**
     * in place addition of two matrices
     *
     * @param other  the second ndarray to add
     * @param result the result ndarray
     * @return the result of the addition
     */
    @Override
    public INDArray addi(INDArray other, INDArray result) {
        if (other.isScalar()) {
            return result.addi(other.getDouble(0), result);
        }

        if (isScalar()) {
            return other.addi(getDouble(0), result);
        }


        LinAlgExceptions.assertSameShape(other, result);

        Nd4j.getExecutioner().exec(new AddOp(this, other, result));


        if (Nd4j.ENFORCE_NUMERICAL_STABILITY)
            Nd4j.clearNans(result);

        return result;
    }

    /**
     * Returns the normmax along the specified dimension
     *
     * @param dimension the dimension to getScalar the norm1 along
     * @return the norm1 along the specified dimension
     */
    @Override
    public INDArray normmax(int... dimension) {
        return Nd4j.getExecutioner().exec(new NormMax(this), dimension);
    }

    /**
     * Reverse division
     *
     * @param other the matrix to divide from
     * @return
     */
    @Override
    public INDArray rdiv(INDArray other) {
        return dup().rdivi(other);
    }

    /**
     * Reverse divsion (in place)
     *
     * @param other
     * @return
     */
    @Override
    public INDArray rdivi(INDArray other) {
        return rdivi(other, this);
    }

    /**
     * Reverse division
     *
     * @param other  the matrix to subtract from
     * @param result the result ndarray
     * @return
     */
    @Override
    public INDArray rdiv(INDArray other, INDArray result) {
        return dup().rdivi(other, result);
    }

    /**
     * Reverse division (in-place)
     *
     * @param other  the other ndarray to subtract
     * @param result the result ndarray
     * @return the ndarray with the operation applied
     */
    @Override
    public INDArray rdivi(INDArray other, INDArray result) {
        return other.divi(this, result);
    }

    /**
     * Reverse subtraction
     *
     * @param other  the matrix to subtract from
     * @param result the result ndarray
     * @return
     */
    @Override
    public INDArray rsub(INDArray other, INDArray result) {
        return dup().rsubi(other, result);
    }

    /**
     * @param other
     * @return
     */
    @Override
    public INDArray rsub(INDArray other) {
        return dup().rsubi(other);
    }

    /**
     * @param other
     * @return
     */
    @Override
    public INDArray rsubi(INDArray other) {
        return rsubi(other, this);
    }

    /**
     * Reverse subtraction (in-place)
     *
     * @param other  the other ndarray to subtract
     * @param result the result ndarray
     * @return the ndarray with the operation applied
     */
    @Override
    public INDArray rsubi(INDArray other, INDArray result) {
        return other.subi(this, result);
    }

    /**
     * Set the value of the ndarray to the specified value
     *
     * @param value the value to assign
     * @return the ndarray with the values
     */
    @Override
    public INDArray assign(Number value) {
        Nd4j.getExecutioner().exec(new ScalarSet(this, value));
        return this;
    }


    /**
     * Assign all elements from given ndarray that are matching given condition,
     * ndarray to this ndarray
     *
     * @param arr       the elements to assign
     * @param condition
     * @return this
     */
    @Override
    public INDArray assignIf(INDArray arr, Condition condition) {
        BooleanIndexing.assignIf(this, arr, condition);
        return this;
    }

    /**
     * Replaces all elements in this ndarray that are matching give condition, with corresponding elements from given array
     *
     * @param arr
     * @param condition
     * @return
     */
    @Override
    public INDArray replaceWhere(INDArray arr, Condition condition) {
        BooleanIndexing.replaceWhere(this, arr, condition);
        return this;
    }

    @Override
    public int linearIndex(int i) {
        setLinearStride();
        int idx = i;
        for (int j = 0; j < Shape.rank(shapeInformation) - 1; j++) {
            if (size(i) == 1)
                continue;
            idx += i * stride(j);
        }
        return Shape.offset(shapeInformation) + (idx);
    }

    private void setLinearStride() {
        if (linearStride >= 0)
            return;

        linearStride = ArrayUtil.prod(reshape(1, length()).stride());
    }


    /**
     * Returns the specified slice of this matrix.
     * In matlab, this would be equivalent to (given a 2 x 2 x 2):
     * A(:,:,x) where x is the slice you want to return.
     * <p/>
     * The slice is always relative to the final dimension of the matrix.
     *
     * @param slice the slice to return
     * @return the specified slice of this matrix
     */
    @Override
    public INDArray slice(int slice) {
        int slices = slices();
        if (slice >= slices)
            throw new IllegalArgumentException("Illegal slice " + slice);

        if (Shape.rank(shapeInformation) == 0 || isRowVector()) {
            if (slice == 0)
                return createScalarForIndex(slice, true);
            else if (isRowVector())
                return createScalarForIndex(slice, true);

            else {
                throw new IllegalArgumentException("Can't slice a 0-d NDArray");
            }
        }


        if (slice < 0)
            slice += rank();
        INDArrayIndex[] indexes = new INDArrayIndex[rank()];
        indexes[0] = NDArrayIndex.point(slice);
        for (int i = 1; i < rank(); i++) {
            indexes[i] = NDArrayIndex.all();
        }
        return get(indexes);
    }



    protected INDArray createScalarForIndex(int i, boolean applyOffset) {
        return create(data(), new int[] {1, 1}, new int[] {1, 1}, applyOffset ? Shape.offset(shapeInformation) + i : i);
    }

    protected INDArray createScalar(double d) {
        return Nd4j.scalar(d);
    }



    @Override
    public int getTrailingOnes() {
        if (this.numTrailingOnes >= 0)
            return this.numTrailingOnes;

        int numLeadingOnes = 0;
        for (int i = rank() - 1; i > 0; i--) {
            if (size(i) == 1)
                numLeadingOnes++;
        }

        this.numTrailingOnes = numLeadingOnes;
        return numLeadingOnes;
    }



    @Override
    public int getLeadingOnes() {
        if (this.numLeadingOnes >= 0)
            return this.numLeadingOnes;

        int numLeadingOnes = 0;
        for (int i = 0; i < rank(); i++) {
            if (size(i) == 1)
                numLeadingOnes++;
        }

        this.numLeadingOnes = numLeadingOnes;
        return numLeadingOnes;
    }



    /**
     * Returns the slice of this from the specified dimension
     *
     * @param slice     the dimension to return from
     * @param dimension the dimension of the slice to return
     * @return the slice of this matrix from the specified dimension
     * and dimension
     */
    @Override
    public INDArray slice(int slice, int dimension) {
        int slices = size(dimension);
        if (slice >= slices)
            throw new IllegalArgumentException("Illegal slice " + slice);

        if (Shape.rank(shapeInformation) == 0) {
            if (slice == 0)
                return createScalarForIndex(slice, true);
            else
                throw new IllegalArgumentException("Can't slice a 0-d NDArray");

        }


        if (slice < 0)
            slice += rank();
        INDArrayIndex[] indexes = new INDArrayIndex[rank()];
        indexes[dimension] = NDArrayIndex.point(slice);
        for (int i = 0; i < rank(); i++) {
            if (i != dimension)
                indexes[i] = NDArrayIndex.all();
        }
        return get(indexes);

    }

    /**
     * Fetch a particular number on a multi dimensional scale.
     *
     * @param indexes the indexes to get a number from
     * @return the number at the specified indices
     */
    @Override
    public INDArray getScalar(int... indexes) {
        return Nd4j.scalar(getDouble(indexes));
    }

    @Override
    public INDArray rdiv(Number n) {
        return dup().rdivi(n);
    }

    @Override
    public INDArray rdivi(Number n) {
        return rdivi(n, this);
    }

    @Override
    public INDArray rsub(Number n) {
        return dup().rsubi(n);
    }

    @Override
    public INDArray rsubi(Number n) {
        return rsubi(n, this);
    }

    @Override
    public INDArray div(Number n) {
        return dup().divi(n);
    }

    @Override
    public INDArray divi(Number n) {
        return divi(n, this);
    }

    @Override
    public INDArray mul(Number n) {
        return dup().muli(n);
    }

    @Override
    public INDArray muli(Number n) {
        return muli(n, this);
    }

    @Override
    public INDArray sub(Number n) {
        return dup().subi(n);
    }

    @Override
    public INDArray subi(Number n) {
        return subi(n, this);
    }

    @Override
    public INDArray add(Number n) {
        return dup().addi(n);
    }

    @Override
    public INDArray addi(Number n) {
        return addi(n, this);
    }



    /**
     * Replicate and tile array to fill out to the given shape
     * See:
     * https://github.com/numpy/numpy/blob/master/numpy/matlib.py#L310-L358
     * @param shape the new shape of this ndarray
     * @return the shape to fill out to
     */
    @Override
    public INDArray repmat(int[] shape) {
        int rows = rows() * shape[0];
        int cols = columns() * shape[1];
        INDArray ret = reshape(1, length()).repeat(0, shape[0]).reshape(rows, columns()).repeat(0, shape[1]);
        return ret.reshape(rows, cols);
    }

    @Override
    public INDArray repeat(int dimension, int... repeats) {
        if (dimension < 0)
            dimension += rank();

        if (repeats.length < rank()) {
            if (dimension > 0)
                repeats = Ints.concat(ArrayUtil.nTimes(rank() - repeats.length, 1), repeats);
            //append rather than prepend for dimension == 0
            else
                repeats = Ints.concat(repeats, ArrayUtil.nTimes(rank() - repeats.length, 1));

        }

        int[] newShape = new int[rank()];

        for (int i = 0; i < newShape.length; i++)
            newShape[i] = size(i) * repeats[i];

        INDArray ret = create(newShape);

        //number of times to repeat each value
        int repeatDelta = ArrayUtil.prod(newShape) / length();
        for (int i = 0; i < tensorssAlongDimension(dimension); i++) {
            INDArray thisTensor = tensorAlongDimension(i, dimension);
            INDArray retTensor = ret.tensorAlongDimension(i, dimension);
            int retIdx = 0;
            for (int k = 0; k < thisTensor.length(); k++) {
                for (int j = 0; j < repeatDelta; j++) {
                    retTensor.putScalar(retIdx++, thisTensor.getDouble(k));
                }
            }
        }

        return ret;
    }


    /**
     * Insert a row in to this array
     * Will throw an exception if this
     * ndarray is not a matrix
     *
     * @param row   the row insert into
     * @param toPut the row to insert
     * @return this
     */
    @Override
    public INDArray putRow(int row, INDArray toPut) {
        if (isRowVector() && toPut.isVector()) {
            return assign(toPut);
        }
        return put(new INDArrayIndex[] {NDArrayIndex.point(row), NDArrayIndex.all()}, toPut);
    }

    /**
     * Insert a column in to this array
     * Will throw an exception if this
     * ndarray is not a matrix
     *
     * @param column the column to insert
     * @param toPut  the array to put
     * @return this
     */
    @Override
    public INDArray putColumn(int column, INDArray toPut) {
        if (isColumnVector() && toPut.isVector()) {
            return assign(toPut);
        }
        return put(new INDArrayIndex[] {NDArrayIndex.all(), NDArrayIndex.point(column)}, toPut);

    }


    @Override
    public double getDouble(int i) {
        if (i >= length()) {
            throw new IllegalArgumentException("Unable to get linear index >= " + length());
        }

        if (Nd4j.getExecutioner().getProfilingMode() != OpExecutioner.ProfilingMode.DISABLED)
            OpProfiler.getInstance().processScalarCall();

        Nd4j.getCompressor().autoDecompress(this);


        if (i == 0)
            return data().getDouble(i);

        int[] dimensions = ordering() == 'c' ? Shape.ind2subC(this, i) : Shape.ind2sub(this, i);
        Shape.assertShapeLessThan(dimensions, shape());
        return getDouble(dimensions);

    }

    @Override
    public double getDouble(int i, int j) {
        return getDouble(new int[] {i, j});
    }

    @Override
    public float getFloat(int i) {
        return (float) getDouble(i);
    }

    @Override
    public float getFloat(int i, int j) {
        return (float) getDouble(i, j);
    }

    /**
     * Return transposed copy of this matrix.
     */
    @Override
    public INDArray transpose() {
        return transposei();
    }


    /**
     *
     * Return transposed version of this matrix.
     *
     * PLEASE NOTE: This method is NOT in place, it will return transposed copy instead.
     */
    @Override
    public INDArray transposei() {
        return permute(ArrayUtil.reverseCopy(ArrayUtil.range(0, rank())));
    }

    protected INDArray create(DataBuffer data, int[] shape, int[] strides) {
        if (this instanceof IComplexNDArray)
            return Nd4j.createComplex(data, shape, strides, 0, ordering());
        else
            return Nd4j.create(data, shape, strides, 0, ordering());
    }

    @Override
    public INDArray reshape(char order, int... newShape) {
        int numberNegativesOnes = 0;
        int[] shape = ArrayUtil.copy(newShape);
        for (int i = 0; i < shape.length; i++) {
            if (shape[i] < 0) {
                if (numberNegativesOnes >= 1)
                    throw new IllegalArgumentException("Only one dimension can be negative ones");

                numberNegativesOnes++;

                int shapeLength = 1;
                for (int j = 0; j < shape.length; j++)
                    if (shape[j] >= 1)
                        shapeLength *= shape[j];
                int realShape = Math.abs(length() / shapeLength);
                int[] thisNewShape = new int[shape.length];
                for (int j = 0; j < shape.length; j++) {
                    if (i != j) {
                        thisNewShape[j] = shape[j];
                    } else
                        thisNewShape[j] = realShape;
                }

                shape = thisNewShape;
                break;

            }

        }


        INDArray reshapeAttempt = Shape.newShapeNoCopy(this, shape, order == 'f');
        if (reshapeAttempt != null) {
            // kinda strange get/set usage
            //  reshapeAttempt.setOrder(Shape.getOrder(reshapeAttempt));
            return reshapeAttempt;
        }


        INDArray ret = Nd4j.createUninitialized(shape, order);
        if (order != ordering()) {
            ret.setData(dup(order).data());
        } else
            ret.assign(this);
        return ret;
    }

    @Override
    public double getDoubleUnsafe(int offset) {
        return data().getDouble(offset);
    }

    @Override
    public INDArray putScalarUnsafe(int offset, double value) {
        if (Nd4j.getExecutioner().getProfilingMode() != OpExecutioner.ProfilingMode.DISABLED)
            OpProfiler.getInstance().processScalarCall();

        data().put(offset, value);
        return this;
    }

    @Override
    public int innerMostStride() {
        if (ordering() == 'c')
            return stride(-1);
        return stride(0);
    }

    @Override
    public INDArray reshape(char order, int rows, int columns) {
        return reshape(order, new int[] {rows, columns});
    }

    /**
     * Reshape the ndarray in to the specified dimensions,
     * possible errors being thrown for invalid shapes
     *
     * Note here that one dimension can be -1.
     * The dimension that is -1 will be inferred from the shape and
     * the length of the ndarray
     *
     * @param shape the shape of the ndarray.
     * @return the new reshaped nd array
     */
    @Override
    public INDArray reshape(int... shape) {
        return reshape(Nd4j.order(), shape);
    }

    @Override
    public void checkDimensions(INDArray other) {
        assert Shape.contentEquals(other.shape(),
                        Shape.shapeOf(shapeInformation)) : " Other array should have been shape: "
                                        + Shape.toString(Shape.shapeOf(shapeInformation)) + " but was "
                                        + Arrays.toString(other.shape());
        assert Shape.contentEquals(other.stride(),
                        Shape.stride(shapeInformation)) : " Other array should have been stride: "
                                        + Shape.toString(Shape.stride(shapeInformation)) + " but was "
                                        + Arrays.toString(other.stride());
        assert Shape.offset(shapeInformation) == other.offset() : "Offset of this array is "
                        + Shape.offset(shapeInformation) + " but other was " + other.offset();

    }


    /**
     * Returns the product along a given dimension
     *
     * @param dimension the dimension to getScalar the product along
     * @return the product along the specified dimension
     */
    @Override
    public INDArray prod(int... dimension) {
        return Nd4j.getExecutioner().exec(new Prod(this), dimension);
    }

    /**
     * Returns the overall mean of this ndarray
     *
     * @param dimension the dimension to getScalar the mean along
     * @return the mean along the specified dimension of this ndarray
     */
    @Override
    public INDArray mean(int... dimension) {
        return Nd4j.getExecutioner().exec(new Mean(this), dimension);
    }

    /**
     * Returns the overall variance of this ndarray
     *
     * @param dimension the dimension to getScalar the mean along
     * @return the mean along the specified dimension of this ndarray
     */
    @Override
    public INDArray var(int... dimension) {
        return Nd4j.getExecutioner().exec(new Variance(this), dimension);
    }

    /**
     * Returns the overall variance of this ndarray
     *
     * @param biasCorrected boolean on whether to apply corrected bias
     * @param dimension the dimension to getScalar the mean along
     * @return the mean along the specified dimension of this ndarray
     */
    @Override
    public INDArray var(boolean biasCorrected, int... dimension) {
        return Nd4j.getExecutioner().exec(new Variance(this, biasCorrected), dimension);
    }

    /**
     * Returns the overall max of this ndarray
     *
     * @param dimension the dimension to getScalar the mean along
     * @return the mean along the specified dimension of this ndarray
     */
    @Override
    public INDArray max(int... dimension) {
        return Nd4j.getExecutioner().exec(new Max(this), dimension);
    }

    /**
     * Returns the overall min of this ndarray
     *
     * @param dimension the dimension to getScalar the mean along
     * @return the mean along the specified dimension of this ndarray
     */
    @Override
    public INDArray min(int... dimension) {
        return Nd4j.getExecutioner().exec(new Min(this), dimension);
    }

    /**
     * Returns the sum along the last dimension of this ndarray
     *
     * @param dimension the dimension to getScalar the sum along
     * @return the sum along the specified dimension of this ndarray
     */
    @Override
    public INDArray sum(int... dimension) {
        return Nd4j.getExecutioner().exec(new Sum(this), dimension);
    }


    /**
     * Returns the norm1 along the specified dimension
     *
     * @param dimension the dimension to getScalar the norm1 along
     * @return the norm1 along the specified dimension
     */
    @Override
    public INDArray norm1(int... dimension) {
        return Nd4j.getExecutioner().exec(new Norm1(this), dimension);
    }


    /**
     * Standard deviation of an ndarray along a dimension
     *
     * @param dimension the dimension to getScalar the std along
     * @return the standard deviation along a particular dimension
     */
    @Override
    public INDArray std(int... dimension) {
        return Nd4j.getExecutioner().exec(new StandardDeviation(this), dimension);
    }

    @Override
    public INDArray std(boolean biasCorrected, int... dimension) {
        return Nd4j.getExecutioner().exec(new StandardDeviation(this, biasCorrected), dimension);
    }

    @Override
    public Number stdNumber(boolean biasCorrected) {
        return Nd4j.getExecutioner().exec(new StandardDeviation(this, biasCorrected), new int[] {Integer.MAX_VALUE})
                        .getDouble(0);
    }

    /**
     * Returns the norm2 along the specified dimension
     *
     * @param dimension the dimension to getScalar the norm2 along
     * @return the norm2 along the specified dimension
     */
    @Override
    public INDArray norm2(int... dimension) {
        return Nd4j.getExecutioner().exec(new Norm2(this), dimension);
    }



    /**
     * Number of columns (shape[1]), throws an exception when
     * called when not 2d
     *
     * @return the number of columns in the array (only 2d)
     */
    @Override
    public int columns() {
        if (isMatrix())
            return size(1);
        else if (isVector()) {
            if (isColumnVector())
                return 1;
            else if (isRowVector() && rank > 1)
                return size(1);
            else
                return size(0);
        }

        throw new IllegalStateException("Unable to get number of of columns for a non 2d matrix");
    }

    /**
     * Returns the number of rows
     * in the array (only 2d) throws an exception when
     * called when not 2d
     *
     * @return the number of rows in the matrix
     */
    @Override
    public int rows() {
        if (isMatrix())
            return size(0);
        else if (isVector()) {
            if (isRowVector())
                return 1;
            else
                return size(0);
        }

        throw new IllegalStateException("Unable to get number of of rows for a non 2d matrix");
    }


    /**
     * Flattens the array for linear indexing
     *
     * @return the flattened version of this array
     */
    @Override
    public INDArray ravel(char ordering) {
        if (length >= Integer.MAX_VALUE)
            throw new IllegalArgumentException("Length can not be >= Integer.MAX_VALUE");
        INDArray ret = create(new int[] {1, (int) length}, ordering);
        NDArrayIndex index = new NDArrayIndex(this.shape());

        for (int i = 0; i < length(); i++) {
            double val = getDouble(index.next());
            ret.putScalar(new int[] {0, i}, val);
        }

        return ret;

    }

    /**
     * Flattens the array for linear indexing
     *
     * @return the flattened version of this array
     */
    @Override
    public INDArray ravel() {
        return reshape(1, length());
    }

    /**
     * Flattens the array for linear indexing
     *
     * @return the flattened version of this array
     */
    @Override
    public void sliceVectors(List<INDArray> list) {
        if (isVector())
            list.add(this);
        else {
            for (int i = 0; i < slices(); i++) {
                slice(i).sliceVectors(list);
            }
        }
    }

    /**
     * Reshape the matrix. Number of elements must not change.
     *
     * @param newRows
     * @param newColumns
     */
    @Override
    public INDArray reshape(int newRows, int newColumns) {
        return reshape(new int[] {newRows, newColumns});
    }

    /**
     * Get the specified column
     *
     * @param c
     */
    @Override
    public INDArray getColumn(int c) {
        if (isColumnVector() && c == 0)
            return this;
        else if (isColumnVector() && c > 0)
            throw new IllegalArgumentException("Illegal index for row");
        return get(NDArrayIndex.all(), NDArrayIndex.point(c));
    }


    /**
     * Get whole rows from the passed indices.
     *
     * @param rindices
     */
    @Override
    public INDArray getRows(int[] rindices) {
        if (!isMatrix() && !isVector())
            throw new IllegalArgumentException("Unable to get columns from a non matrix or vector");
        if (isVector())
            return Nd4j.pullRows(this, 1, rindices);
        else {
            INDArray ret = Nd4j.create(rindices.length, columns());
            for (int i = 0; i < rindices.length; i++)
                ret.putRow(i, getRow(rindices[i]));
            return ret;
        }
    }

    /**
     * Returns a subset of this array based on the specified
     * indexes
     *
     * @param indexes the indexes in to the array
     * @return a view of the array with the specified indices
     */
    @Override
    public INDArray get(INDArrayIndex... indexes) {
        //check for row/column vector and point index being 0
        if (indexes.length == 1 && indexes[0] instanceof NDArrayIndexAll || (indexes.length == 2 && (isRowVector()
                        && indexes[0] instanceof PointIndex && indexes[0].offset() == 0
                        && indexes[1] instanceof NDArrayIndexAll
                        || isColumnVector() && indexes[1] instanceof PointIndex && indexes[0].offset() == 0
                                        && indexes[0] instanceof NDArrayIndexAll)))
            return this;


        indexes = NDArrayIndex.resolve(shapeInfoDataBuffer(), indexes);
        ShapeOffsetResolution resolution = new ShapeOffsetResolution(this);
        resolution.exec(indexes);

        if (indexes.length < 1)
            throw new IllegalStateException("Invalid index found of zero length");

        int[] shape = resolution.getShapes();
        int numSpecifiedIndex = 0;
        for (int i = 0; i < indexes.length; i++)
            if (indexes[i] instanceof SpecifiedIndex)
                numSpecifiedIndex++;


        if (shape != null && numSpecifiedIndex > 0) {
            Generator<List<List<Integer>>> gen = SpecifiedIndex.iterate(indexes);
            INDArray ret = Nd4j.create(shape, 'c');
            int count = 0;
            while (true) {
                try {
                    List<List<Integer>> next = gen.next();
                    List<Integer> coordsCombo = new ArrayList<>();
                    for (int i = 0; i < next.size(); i++) {
                        if (next.get(i).size() > 1)
                            throw new IllegalStateException("Illegal entry returned");
                        coordsCombo.add(next.get(i).get(0));
                    }
                    ret.putScalar(count++, getDouble(Ints.toArray(coordsCombo)));


                } catch (NoSuchElementException e) {
                    break;
                }

                if (count >= ret.length())
                    break;
            }

            return ret;

        }



        INDArray ret = subArray(resolution);
        return ret;
    }


    /**
     * Get whole columns
     * from the passed indices.
     *
     * @param cindices
     */
    @Override
    public INDArray getColumns(int... cindices) {
        if (!isMatrix() && !isVector())
            throw new IllegalArgumentException("Unable to get columns from a non matrix or vector");
        if (isVector()) {
            return Nd4j.pullRows(this, 0, cindices, this.ordering());
        } else {
            INDArray ret = Nd4j.create(rows(), cindices.length);
            for (int i = 0; i < cindices.length; i++)
                ret.putColumn(i, getColumn(cindices[i]));
            return ret;
        }

    }

    protected INDArray create(int rows, int length) {
        return create(new int[] {rows, length});
    }

    /**
     * Get a copy of a row.
     *
     * @param r the row to get
     */
    @Override
    public INDArray getRow(int r) {
        if (isRowVector() && r == 0)
            return this;
        else if (isRowVector() && r > 0)
            throw new IllegalArgumentException("Illegal index for row");
        return get(NDArrayIndex.point(r), NDArrayIndex.all());
    }


    /**
     * This method allows you to compare INDArray against other INDArray, with variable eps
     *
     * @param o
     * @param eps
     * @return
     */
    public boolean equalsWithEps(Object o, double eps) {
        if (o == null)
            return false;

        if (!(o instanceof INDArray))
            return false;

        INDArray n = (INDArray) o;

        if (this.lengthLong() != n.lengthLong())
            return false;


        //epsilon equals
        if (isScalar() && n.isScalar()) {
            if (data.dataType() == DataBuffer.Type.FLOAT) {
                double val = getDouble(0);
                double val2 = n.getDouble(0);

                if (Double.isNaN(val) != Double.isNaN(val2))
                    return false;

                return Math.abs(val - val2) < eps;
            } else {
                double val = getDouble(0);
                double val2 = n.getDouble(0);

                if (Double.isNaN(val) != Double.isNaN(val2))
                    return false;

                return Math.abs(val - val2) < eps;
            }

        } else if (isVector() && n.isVector()) {

            EqualsWithEps op = new EqualsWithEps(this, n, eps);
            Nd4j.getExecutioner().exec(op);
            double diff = op.getFinalResult().doubleValue();

            return diff < 0.5;
        }

        if (!Arrays.equals(this.shape(), n.shape()))
            return false;


        if (!Shape.shapeEquals(shape(), n.shape())) {
            return false;
        }


        if (slices() != n.slices())
            return false;

        if (n.ordering() == ordering()) {
            EqualsWithEps op = new EqualsWithEps(this, n, eps);
            Nd4j.getExecutioner().exec(op);
            double diff = op.getFinalResult().doubleValue();

            return diff < 0.5;
        } else {
            EqualsWithEps op = new EqualsWithEps(this, n, eps);
            Nd4j.getExecutioner().exec(op);
            double diff = op.getFinalResult().doubleValue();

            return diff < 0.5;
        }
    }

    /**
     * Compare two matrices. Returns true if and only if other is also a
     * DoubleMatrix which has the same size and the maximal absolute
     * difference in matrix elements is smaller than 1e-5.
     *
     * @param o
     */
    @Override
    public boolean equals(Object o) {
        return equalsWithEps(o, Nd4j.EPS_THRESHOLD);
    }

    @Override
    public DataBuffer shapeInfoDataBuffer() {
        return shapeInformation;
    }

    @Override
    public IntBuffer shapeInfo() {
        return shapeInformation.asNioInt();
    }

    /**
     * Returns the shape(dimensions) of this array
     *
     * @return the shape of this matrix
     */
    public int[] shape() {
        return Shape.shape(shapeInformation);
    }

    /**
     * Returns the shape information debugging
     * information
     *
     * @return the shape information debugging information
     */
    @Override
    public String shapeInfoToString() {
        return Shape.shapeToString(this);
    }

    /**
     * Returns the stride(indices along the linear index for which each slice is accessed) of this array
     *
     * @return the stride of this array
     */
    @Override
    public int[] stride() {
        int[] ret = new int[Shape.rank(shapeInformation)];
        DataBuffer buffer = Shape.stride(shapeInformation);
        for (int i = 0; i < ret.length; i++)
            ret[i] = buffer.getInt(i);
        return ret;
    }


    @Override
    public int offset() {
        if (data().offset() >= Integer.MAX_VALUE)
            throw new IllegalArgumentException("Offset of buffer can not be >= Integer.MAX_VALUE");
        //  return Shape.offset(shapeInfo());
        return (int) data().offset();
    }

    @Override
    public char ordering() {
        return Shape.order(shapeInformation);
    }

    /**
     * Returns the size of this array
     * along a particular dimension
     *
     * @param dimension the dimension to return from
     * @return the shape of the specified dimension
     */
    @Override
    public int size(int dimension) {
        if (isScalar()) {
            if (dimension == 0 || dimension == 1 || dimension < 0)
                return (int) length;
            else
                throw new IllegalArgumentException("Illegal dimension for scalar " + dimension);
        }

        if (dimension < 0) {
            return shapeOf().getInt(dimension + Shape.rank(shapeInformation));
        }

        if (dimension >= rank())
            throw new IllegalArgumentException("Invalid size: cannot get size of dimension " + dimension + " for rank "
                            + rank() + " NDArray (array shape: " + Arrays.toString(this.shape()) + ")");


        return shapeOf().getInt(dimension);
    }

    @Override
    public int rank() {
        return Shape.rank(shapeInformation);
    }

    /**
     * Returns the total number of elements in the ndarray
     *
     * @return the number of elements in the ndarray
     */
    @Override
    public int length() {
        if (length >= Integer.MAX_VALUE)
            throw new IllegalArgumentException("Length is >= Integer.MAX_VALUE: lengthLong() must be called instead");
        return (int) length;
    }

    /**
     * Returns the total number of elements in the ndarray
     *
     * @return the number of elements in the ndarray
     */
    @Override
    public long lengthLong() {
        return length;
    }

    /**
     * Broadcasts this ndarray to be the specified shape
     *
     * @param shape the new shape of this ndarray
     * @return the broadcasted ndarray
     */
    @Override
    public INDArray broadcast(int... shape) {
        if (Shape.shapeEquals(shape, shape()))
            return this;

        boolean compatible = true;
        int count = shape.length - 1;
        int thisCount = Shape.rank(shapeInformation) - 1;
        for (int i = shape.length - 1; i > 0; i--) {
            if (count < 0 || thisCount < 0)
                break;
            if (shape[count] != shape()[thisCount] && shape[count] != 1 && shape()[thisCount] != 1) {
                compatible = false;
                break;
            }

            count--;
            thisCount--;
        }

        if (!compatible)
            throw new IllegalArgumentException("Incompatible broadcast from " + Arrays.toString(shape()) + " to "
                            + Arrays.toString(shape));



        int[] retShape = new int[shape.length];
        List<Integer> broadCastDimensions = new ArrayList<>();
        List<Integer> nonBroadCastDimensions = new ArrayList<>();
        for (int i = 0; i < retShape.length; i++) {
            if (shape().length == 1) {
                if (i == 0) {
                    if (i < shape().length)
                        retShape[i] = Math.max(1, shape[i]);
                    else
                        retShape[i] = shape[i];
                } else {
                    if (i < shape().length)
                        retShape[i] = Math.max(shape[i], size(i));
                    else
                        retShape[i] = shape[i];
                }
            } else {
                if (i < rank() && size(i) == 1)
                    broadCastDimensions.add(i);
                else
                    nonBroadCastDimensions.add(i);
                if (i < shape().length)
                    retShape[i] = Math.max(shape[i], size(i));
                else
                    retShape[i] = shape[i];
            }

        }

        INDArray ret = create(retShape, ordering());

        if (isRowVector()) {
            //number of times to repeat each value
            for (int i = 0; i < ret.slices(); i++) {
                ret.putSlice(i, this);
            }
        } else {
            //number of times to repeat each value
            int repeatDelta = ArrayUtil.prod(retShape) / length();
            for (int i = 0; i < slices(); i++) {
                INDArray thisTensor = slice(i);
                INDArray retTensor = ret.slice(i);
                int retIdx = 0;
                int tensorLen = thisTensor.rank();
                outer: for (int k = 0; k < tensorLen; k++) {
                    for (int j = 0; j < repeatDelta; j++) {
                        if (retIdx >= retTensor.length())
                            break outer;
                        retTensor.putScalar(retIdx++, thisTensor.getDouble(k));
                    }
                }
            }
        }
        return ret;

    }


    /**
     * Dimshuffle: an extension of permute that adds the ability
     * to broadcast various dimensions.
     * <p/>
     * See theano for more examples.
     * This will only accept integers and xs.
     * <p/>
     * An x indicates a dimension should be broadcasted rather than permuted.
     *
     * @param rearrange the dimensions to swap to
     * @return the newly permuted array
     */
    @Override
    public INDArray dimShuffle(Object[] rearrange, int[] newOrder, boolean[] broadCastable) {
        if (broadCastable.length != Shape.rank(shapeInformation))
            throw new IllegalArgumentException(
                            "The broadcastable dimensions must be the same length as the current shape");

        boolean broadcast = false;
        Set<Object> set = new HashSet<>();
        for (int i = 0; i < rearrange.length; i++) {
            set.add(rearrange[i]);
            if (rearrange[i] instanceof Integer) {
                Integer j = (Integer) rearrange[i];
                if (j >= broadCastable.length)
                    throw new IllegalArgumentException(
                                    "Illegal dimension, dimension must be < broadcastable.length (aka the real dimensions");
            } else if (rearrange[i] instanceof Character) {
                Character c = (Character) rearrange[i];
                if (c != 'x')
                    throw new IllegalArgumentException("Illegal input: Must be x");
                broadcast = true;

            } else
                throw new IllegalArgumentException("Only characters and integers allowed");
        }

        //just do permute
        if (!broadcast) {
            int[] ret = new int[rearrange.length];
            for (int i = 0; i < ret.length; i++)
                ret[i] = (Integer) rearrange[i];
            return permute(ret);
        } else {
            List<Integer> drop = new ArrayList<>();
            for (int i = 0; i < broadCastable.length; i++) {
                if (!set.contains(i)) {
                    if (broadCastable[i])
                        drop.add(i);
                    else
                        throw new IllegalArgumentException(
                                        "We can't drop the given dimension because its not broadcastable");
                }

            }


            //list of dimensions to keep
            int[] shuffle = new int[broadCastable.length];
            int count = 0;
            for (int i = 0; i < rearrange.length; i++) {
                if (rearrange[i] instanceof Integer) {
                    shuffle[count++] = (Integer) rearrange[i];
                }
            }


            List<Integer> augment = new ArrayList<>();
            for (int i = 0; i < rearrange.length; i++) {
                if (rearrange[i] instanceof Character)
                    augment.add(i);
            }

            Integer[] augmentDims = augment.toArray(new Integer[1]);

            count = 0;

            int dropIdx = 0;
            int[] newShape = new int[shuffle.length + drop.size()];
            for (int i = 0; i < newShape.length; i++) {
                if (i < shuffle.length) {
                    newShape[count++] = shuffle[i];
                } else
                    newShape[count++] = drop.get(dropIdx++);
            }


            INDArray ret = permute(newShape);
            List<Integer> newDims = new ArrayList<>();
            int[] shape = Arrays.copyOfRange(ret.shape(), 0, shuffle.length);
            for (int i = 0; i < shape.length; i++) {
                newDims.add(shape[i]);
            }

            for (int i = 0; i < augmentDims.length; i++) {
                newDims.add(augmentDims[i], 1);
            }

            int[] toReshape = ArrayUtil.toArray(newDims);


            ret = ret.reshape(toReshape);
            return ret;

        }


    }

    /**
     * See: http://www.mathworks.com/help/matlab/ref/permute.html
     *
     * @param rearrange the dimensions to swap to
     * @return the newly permuted array
     */
    @Override
    public INDArray permute(int... rearrange) {
        if (rearrange.length != rank())
            return dup();
        boolean alreadyInOrder = true;
        IntBuffer shapeInfo = shapeInfo();
        int rank = Shape.rank(shapeInfo);
        for (int i = 0; i < rank; i++) {
            if (rearrange[i] != i) {
                alreadyInOrder = false;
                break;
            }
        }

        if (alreadyInOrder)
            return this;

        checkArrangeArray(rearrange);
        int[] newShape = doPermuteSwap(shapeOf(), rearrange);
        int[] newStride = doPermuteSwap(strideOf(), rearrange);

        char newOrder = Shape.getOrder(newShape, newStride, elementStride());

        INDArray value = create(data(), newShape, newStride, offset(), newOrder);
        return value;
    }

    /**
     * An <b>in-place</b> version of permute. The array  shape information (shape, strides)
     * is modified by this operation (but not the data itself)
     * See: http://www.mathworks.com/help/matlab/ref/permute.html
     *
     * @param rearrange the dimensions to swap to
     * @return the current array
     */
    @Override
    public INDArray permutei(int... rearrange) {
        boolean alreadyInOrder = true;
        IntBuffer shapeInfo = shapeInfo();
        int rank = Shape.rank(shapeInfo);
        for (int i = 0; i < rank; i++) {
            if (rearrange[i] != i) {
                alreadyInOrder = false;
                break;
            }
        }

        if (alreadyInOrder)
            return this;

        checkArrangeArray(rearrange);
        int[] newShape = doPermuteSwap(Shape.shapeOf(shapeInfo), rearrange);
        int[] newStride = doPermuteSwap(Shape.stride(shapeInfo), rearrange);
        char newOrder = Shape.getOrder(newShape, newStride, elementStride());

        //Set the shape information of this array: shape, stride, order.
        //Shape info buffer: [rank, [shape], [stride], offset, elementwiseStride, order]
        /*for( int i=0; i<rank; i++ ){
            shapeInfo.put(1+i,newShape[i]);
            shapeInfo.put(1+i+rank,newStride[i]);
        }
        shapeInfo.put(3+2*rank,newOrder);
        */
        int ews = shapeInfo.get(2 * rank + 2);
        /*
        if (ews < 1 && !attemptedToFindElementWiseStride)
            throw new RuntimeException("EWS is -1");
            */

        shapeInformation = Nd4j.getShapeInfoProvider().createShapeInformation(newShape, newStride, offset(), ews,
                        newOrder);
        if (ews < 0) {

            INDArray reshapeAttempt = Shape.newShapeNoCopy(this, new int[] {1, this.length()}, newOrder == 'f');
            if (reshapeAttempt != null) {
                this.shapeInformation = Nd4j.getShapeInfoProvider().createShapeInformation(newShape, newStride,
                                this.offset(), reshapeAttempt.elementWiseStride(), newOrder);
            }
            this.attemptedToFindElementWiseStride = true;
        }

        if (shapeInfo.get(2 * rank + 2) > 0) {
            //for the backend to work - no ews for permutei
            //^^ not true anymore? Not sure here. Marking this for raver
            this.shapeInformation = Nd4j.getShapeInfoProvider().createShapeInformation(newShape, newStride,
                            this.offset(), -1, newOrder);
        }

        this.shape = null;
        this.stride = null;

        this.rows = size(0);
        this.columns = size(1);
        this.numLeadingOnes = -1;
        this.numTrailingOnes = -1;

        return this;
    }


    protected void copyRealTo(INDArray arr) {
        INDArray flattened = this;
        INDArray arrLinear = arr;
        for (int i = 0; i < flattened.length(); i++) {
            arrLinear.putScalar(i, flattened.getDouble(i));
        }

    }

    protected int[] doPermuteSwap(IntBuffer shape, int[] rearrange) {
        int[] ret = new int[rearrange.length];
        for (int i = 0; i < rearrange.length; i++) {
            ret[i] = shape.get(rearrange[i]);
        }
        return ret;
    }

    protected int[] doPermuteSwap(DataBuffer shape, int[] rearrange) {
        int[] ret = new int[rearrange.length];
        for (int i = 0; i < rearrange.length; i++) {
            ret[i] = shape.getInt(rearrange[i]);
        }

        return ret;
    }


    protected void checkArrangeArray(int[] arr) {
        assert arr.length == Shape.rank(shapeInformation) : "Invalid rearrangement: number of arrangement != shape";
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] >= arr.length)
                throw new IllegalArgumentException("The specified dimensions can't be swapped. Given element " + i
                                + " was >= number of dimensions");
            if (arr[i] < 0)
                throw new IllegalArgumentException("Invalid dimension: " + i + " : negative value");


        }

        for (int i = 0; i < arr.length; i++) {
            for (int j = 0; j < arr.length; j++) {
                if (i != j && arr[i] == arr[j])
                    throw new IllegalArgumentException("Permute array must have unique elements");
            }
        }

    }

    /**
     * Checks whether the matrix is a vector.
     */
    @Override
    public boolean isVector() {
        return isRowVector() || isColumnVector();
    }

    @Override
    public boolean isSquare() {

        return isMatrix() && rows() == columns();
    }

    /**
     * Checks whether the matrix is a row vector.
     */
    @Override
    public boolean isRowVector() {
        return rank == 2 && rows == 1;
    }

    /**
     * Checks whether the matrix is a column vector.
     */
    @Override
    public boolean isColumnVector() {
        return rank == 2 && columns == 1;
    }

    /**
     * Generate string representation of the matrix.
     */
    @Override
    public String toString() {
        if (!isCompressed() && !preventUnpack)
            return new NDArrayStrings().format(this);
        else if (isCompressed() && compressDebug)
            return "COMPRESSED ARRAY. SYSTEM PROPERTY compressdebug is true. This is to prevent auto decompression from being triggered.";
        else if (preventUnpack)
            return "Array string unpacking is disabled.";
        return new NDArrayStrings().format(this);

    }

    /**
     * Returns a scalar (individual element)
     * of a scalar ndarray
     *
     * @return the individual item in this ndarray
     */
    @Override
    public Object element() {

        if (!isScalar())
            throw new IllegalStateException("Unable to retrieve element from non scalar matrix");
        if (data.dataType() == DataBuffer.Type.FLOAT)
            return data.getFloat(0);
        return data.getDouble(0);
    }


    @Override
    public IComplexNDArray rdiv(IComplexNumber n) {
        return dup().rdivi(n);
    }

    @Override
    public IComplexNDArray rdivi(IComplexNumber n) {
        return rdivi(n, Nd4j.createComplex(shape()));

    }

    @Override
    public IComplexNDArray rsub(IComplexNumber n) {
        return dup().rsubi(n);
    }

    @Override
    public IComplexNDArray rsubi(IComplexNumber n) {
        return rsubi(n, Nd4j.createComplex(shape()));

    }

    @Override
    public IComplexNDArray div(IComplexNumber n) {
        return dup().divi(n);
    }

    @Override
    public IComplexNDArray divi(IComplexNumber n) {

        return divi(n, Nd4j.createComplex(shape()));

    }

    @Override
    public IComplexNDArray mul(IComplexNumber n) {
        return dup().muli(n);
    }

    @Override
    public IComplexNDArray muli(IComplexNumber n) {
        return muli(n, Nd4j.createComplex(shape()));

    }

    @Override
    public IComplexNDArray sub(IComplexNumber n) {
        return dup().subi(n);
    }

    @Override
    public IComplexNDArray subi(IComplexNumber n) {
        return subi(n, Nd4j.createComplex(shape()));
    }

    @Override
    public IComplexNDArray add(IComplexNumber n) {
        return dup().addi(n);
    }

    @Override
    public IComplexNDArray addi(IComplexNumber n) {
        return addi(n, Nd4j.createComplex(shape()));

    }

    @Override
    public IComplexNDArray rdiv(IComplexNumber n, IComplexNDArray result) {
        return dup().rdivi(n, result);
    }

    @Override
    public IComplexNDArray rdivi(IComplexNumber n, IComplexNDArray result) {
        return Nd4j.createComplex(this).rdivi(n, result);

    }

    @Override
    public IComplexNDArray rsub(IComplexNumber n, IComplexNDArray result) {
        return dup().rsubi(n, result);
    }

    @Override
    public IComplexNDArray rsubi(IComplexNumber n, IComplexNDArray result) {
        return Nd4j.createComplex(this).rsubi(n, result);
    }

    @Override
    public IComplexNDArray div(IComplexNumber n, IComplexNDArray result) {
        return dup().divi(n, result);
    }

    @Override
    public IComplexNDArray divi(IComplexNumber n, IComplexNDArray result) {
        return Nd4j.createComplex(this).divi(n, result);

    }

    @Override
    public IComplexNDArray mul(IComplexNumber n, IComplexNDArray result) {
        return dup().muli(n, result);
    }

    @Override
    public IComplexNDArray muli(IComplexNumber n, IComplexNDArray result) {
        return Nd4j.createComplex(this).muli(n, result);

    }

    @Override
    public IComplexNDArray sub(IComplexNumber n, IComplexNDArray result) {
        return dup().subi(n, result);
    }

    @Override
    public IComplexNDArray subi(IComplexNumber n, IComplexNDArray result) {
        return Nd4j.createComplex(this).subi(n, result);

    }

    @Override
    public IComplexNDArray add(IComplexNumber n, IComplexNDArray result) {
        return dup().addi(n, result);
    }

    @Override
    public IComplexNDArray addi(IComplexNumber n, IComplexNDArray result) {
        return Nd4j.createComplex(this).addi(n, result);

    }

    protected INDArray create(BaseNDArray baseNDArray) {
        return baseNDArray;
    }


    @Override
    public INDArray remainder(INDArray denominator) {
        return remainder(denominator, Nd4j.createUninitialized(this.shape()));
    }

    @Override
    public INDArray remainder(INDArray denominator, INDArray result) {
        RemainderOp op = new RemainderOp(this, denominator, result);
        Nd4j.getExecutioner().exec(op);
        return result;
    }

    @Override
    public INDArray remainder(Number denominator) {
        return remainder(denominator, Nd4j.createUninitialized(this.shape()));
    }

    @Override
    public INDArray remainder(Number denominator, INDArray result) {
        ScalarRemainder op = new ScalarRemainder(this, null, result, this.length(), denominator);
        Nd4j.getExecutioner().exec(op);
        return result;
    }

    @Override
    public INDArray remainderi(INDArray denominator) {
        RemainderOp op = new RemainderOp(this, denominator, this);
        Nd4j.getExecutioner().exec(op);
        return this;
    }

    @Override
    public INDArray remainderi(Number denominator) {
        ScalarRemainder op = new ScalarRemainder(this, null, this, this.length(), denominator);
        Nd4j.getExecutioner().exec(op);
        return this;
    }

    @Override
    public INDArray fmod(INDArray denominator) {
        return fmod(denominator, Nd4j.createUninitialized(this.shape()));
    }

    @Override
    public INDArray fmod(INDArray denominator, INDArray result) {
        FModOp op = new FModOp(this, denominator, result);
        Nd4j.getExecutioner().exec(op);
        return result;
    }

    @Override
    public INDArray fmod(Number denominator) {
        return fmod(denominator, Nd4j.createUninitialized(this.shape()));
    }

    @Override
    public INDArray fmod(Number denominator, INDArray result) {
        ScalarFMod op = new ScalarFMod(this, null, result, this.length(), denominator);
        Nd4j.getExecutioner().exec(op);
        return result;
    }

    @Override
    public INDArray fmodi(INDArray denominator) {
        FModOp op = new FModOp(this, denominator, this);
        Nd4j.getExecutioner().exec(op);
        return this;
    }

    @Override
    public INDArray fmodi(Number denominator) {
        ScalarFMod op = new ScalarFMod(this, null, this, this.length(), denominator);
        Nd4j.getExecutioner().exec(op);
        return this;
    }

    @Override
    public Iterator<Object> iterator() {
        return new FirstAxisIterator(this);
    }

    /**
     * Returns the start of where the ndarray is for the original data buffer
     *
     * @return
     */
    @Override
    public int originalOffset() {
        if (data().originalOffset() >= Integer.MAX_VALUE)
            throw new IllegalArgumentException("Original offset of buffer can not be >= Integer.MAX_VALUE");
        return (int) data().originalOffset();
    }

    private void readObject(ObjectInputStream s) {
        try {
            s.defaultReadObject();
            read(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        write(out);
    }

    //Custom serialization for Java serialization
    protected void write(ObjectOutputStream out) throws IOException {
        if (this.isView()) {
            //As per Nd4j.write, duplicate before writing to the output stream
            //BaseDataBuffer.write(...) doesn't know about strides etc, so dup (or equiv. strategy) is necessary here
            //Furthermore, because we only want to save the *actual* data for a view (not the full data), the shape info
            // (mainly strides, offset, element-wise stride) may be different in the duped array vs. the view array
            INDArray copy = this.dup();
            copy.shapeInfoDataBuffer().write(out);
            copy.data().write(out);
        } else {
            shapeInformation.write(out);
            data().write(out);
        }
    }

    //Custom deserialization for Java serialization
    protected void read(ObjectInputStream s) {
        shapeInformation = Nd4j.createBuffer(new int[Shape.shapeInfoLength(rank)], 0);
        shapeInformation.read(s);
        data = Nd4j.createBuffer(length, false);
        data().read(s);
    }



}
