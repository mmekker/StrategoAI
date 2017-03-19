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

package org.nd4j.linalg.api.shape;


import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.complex.IComplexNDArray;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.shape.loop.coordinatefunction.CoordinateFunction;
import org.nd4j.linalg.api.shape.loop.one.RawArrayIterationInformation1;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.INDArrayIndex;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.indexing.ShapeOffsetResolution;
import org.nd4j.linalg.util.ArrayUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Encapsulates all shape related logic (vector of 0 dimension is a scalar is equivalent to
 * a vector of length 1...)
 *
 * @author Adam Gibson
 */
public class Shape {


    private Shape() {}

    /**
     * Create a copy of the matrix
     * where the new offset is zero
     *
     * @param arr the array to copy to offset 0
     * @return the same array if offset is zero
     * otherwise a copy of the array with
     * elements set to zero
     */
    public static INDArray toOffsetZero(INDArray arr) {
        if (arr.offset() < 1 && arr.data().length() == arr.length()
                        || arr instanceof IComplexNDArray && arr.length() * 2 == arr.data().length())
            if (arr.ordering() == 'f' && arr.stride(-1) != arr.elementStride()
                            || arr.ordering() == 'c' && arr.stride(0) != arr.elementStride())
                return arr;

        if (arr.isRowVector()) {
            if (arr instanceof IComplexNDArray) {
                IComplexNDArray ret = Nd4j.createComplex(arr.shape());
                for (int i = 0; i < ret.length(); i++)
                    ret.putScalar(i, ((IComplexNDArray) arr).getComplex(i));
                return ret;
            } else {
                INDArray ret = Nd4j.create(arr.shape());
                for (int i = 0; i < ret.length(); i++)
                    ret.putScalar(i, arr.getDouble(i));
                return ret;
            }
        }


        if (arr instanceof IComplexNDArray) {
            IComplexNDArray ret = Nd4j.createComplex(arr.shape());
            for (int i = 0; i < ret.slices(); i++)
                ret.putSlice(i, arr.slice(i));
            return ret;
        } else {
            INDArray ret = Nd4j.create(arr.shape(), arr.ordering());
            ret.assign(arr);
            return ret;
        }
    }



    /**
     * Create a copy of the ndarray where the new offset is zero
     *
     * @param arr the array to copy to offset 0
     * @return a copy of the array with elements set to zero offset
     */
    public static INDArray toOffsetZeroCopy(INDArray arr) {
        return toOffsetZeroCopyHelper(arr, Nd4j.order(), false);
    }

    /**Create a copy of the ndarray where the new offset is zero, and has specified order
     * @param arr the array to copy to offset 0
     * @param order the order of the returned array
     * @return a copy of the array with elements set to zero offset, and with specified order
     */
    public static INDArray toOffsetZeroCopy(INDArray arr, char order) {
        return toOffsetZeroCopyHelper(arr, order, false);
    }

    /** Create a copy of the ndarray where the new offset is zero.
     * Unlike toOffsetZeroCopy(INDArray) (which always returns arrays of order Nd4j.order()),
     * and toOffsetZeroCopy(INDArray,char) (which always returns arrays of a specified order)
     * this method returns NDArrays of any order (sometimes c, sometimes f).<br>
     * This method may be faster than the other two toOffsetZeroCopyAnyOrder methods as a result,
     * however no performance benefit (or cost) relative to them will be observed in many cases.
     * If a copy is necessary, the output will have order Nd4j.order()
     * @param arr NDArray to duplicate
     * @return Copy with offset 0, but order might be c, or might be f
     */
    public static INDArray toOffsetZeroCopyAnyOrder(INDArray arr) {
        return toOffsetZeroCopyHelper(arr, Nd4j.order(), true);
    }

    private static INDArray toOffsetZeroCopyHelper(final INDArray arr, char order, boolean anyOrder) {
        if (arr instanceof IComplexNDArray) {
            if (arr.isRowVector()) {
                IComplexNDArray ret = Nd4j.createComplex(arr.shape(), order);
                for (int i = 0; i < ret.length(); i++)
                    ret.putScalar(i, ((IComplexNDArray) arr).getComplex(i));
                return ret;
            }
            IComplexNDArray ret = Nd4j.createComplex(arr.shape(), order);
            for (int i = 0; i < ret.slices(); i++)
                ret.putSlice(i, arr.slice(i));
            return ret;
        } else {
            //Use CopyOp:
            char outOrder = (anyOrder ? arr.ordering() : order);
            if (outOrder == 'a')
                outOrder = Nd4j.order();
            INDArray z = Nd4j.createUninitialized(arr.shape(), outOrder);
            z.assign(arr);
            return z;
        }
    }


    /**
     * Get a double based on the array and given indices
     *
     * @param arr     the array to retrieve the double from
     * @param indices the indices to iterate over
     * @return the double at the specified index
     */
    public static double getDouble(INDArray arr, int... indices) {
        long offset = getOffset(arr.shapeInfo(), indices);
        return arr.data().getDouble(offset);
    }

    /**
     * Iterate over 2
     * coordinate spaces given 2 arrays
     * @param arr the first array
     * @param coordinateFunction the coordinate function to use
     *
     */
    public static void iterate(INDArray arr, CoordinateFunction coordinateFunction) {
        Shape.iterate(0, arr.rank(), arr.shape(), new int[arr.rank()], coordinateFunction);
    }

    /**
     * Iterate over 2
     * coordinate spaces given 2 arrays
     * @param arr the first array
     * @param arr2 the second array
     * @param coordinateFunction the coordinate function to use
     *
     */
    public static void iterate(INDArray arr, INDArray arr2, CoordinateFunction coordinateFunction) {
        Shape.iterate(0, arr.rank(), arr.shape(), new int[arr.rank()], 0, arr2.rank(), arr2.shape(),
                        new int[arr2.rank()], coordinateFunction);
    }

    /**
     * Iterate over a pair of coordinates
     * @param dimension
     * @param n
     * @param size
     * @param res
     * @param dimension2
     * @param n2
     * @param size2
     * @param res2
     * @param func
     */
    public static void iterate(int dimension, int n, int[] size, int[] res, int dimension2, int n2, int[] size2,
                    int[] res2, CoordinateFunction func) {
        if (dimension >= n || dimension2 >= n2) {
            // stop clause
            func.process(res, res2);
            return;
        }

        if (size2.length != size.length) {
            if (dimension >= size.length)
                return;
            for (int i = 0; i < size[dimension]; i++) {
                if (dimension2 >= size2.length)
                    break;
                for (int j = 0; j < size2[dimension2]; j++) {
                    res[dimension] = i;
                    res2[dimension2] = j;
                    iterate(dimension + 1, n, size, res, dimension2 + 1, n2, size2, res2, func);
                }

            }
        } else {
            if (dimension >= size.length)
                return;

            for (int i = 0; i < size[dimension]; i++) {
                for (int j = 0; j < size2[dimension2]; j++) {
                    if (dimension2 >= size2.length)
                        break;
                    res[dimension] = i;
                    res2[dimension2] = j;
                    iterate(dimension + 1, n, size, res, dimension2 + 1, n2, size2, res2, func);
                }

            }
        }
    }


    /**
     * Iterate over a pair of coordinates
     * @param dimension
     * @param n
     * @param size
     */
    public static void iterate(int dimension, int n, int[] size, int[] res, CoordinateFunction func) {
        if (dimension >= n) { //stop clause
            func.process(res);
            return;
        }
        for (int i = 0; i < size[dimension]; i++) {
            res[dimension] = i;
            iterate(dimension + 1, n, size, res, func);
        }
    }



    /**
     * Get an offset for retrieval
     * from a data buffer
     * based on the given
     * shape stride and given indices
     * @param baseOffset the offset to start from
     * @param shape the shape of the array
     * @param stride the stride of the array
     * @param indices the indices to iterate over
     * @return the double at the specified index
     */
    public static long getOffset(long baseOffset, int[] shape, int[] stride, int... indices) {
        //int ret =  mappers[shape.length].getOffset(baseOffset, shape, stride, indices);
        if (shape.length != stride.length || indices.length != shape.length)
            throw new IllegalArgumentException("Indexes, shape, and stride must be the same length");
        long offset = baseOffset;
        for (int i = 0; i < shape.length; i++) {
            if (indices[i] >= shape[i])
                throw new IllegalArgumentException(
                                String.format("Index [%d] must not be >= shape[%d]=%d.", i, i, shape[i]));
            if (shape[i] != 1) {
                offset += indices[i] * stride[i];
            }
        }

        return offset;
    }

    /**
     * Get the offset of the specified indices from the shape info buffer
     *
     * @param shapeInformation    Shape information to get the offset for
     * @param indices             Indices array to get the offset for (must be same length as array rank)
     * @return                    Buffer offset fo the specified indices
     */
    public static long getOffset(IntBuffer shapeInformation, int... indices) {
        int rank = rank(shapeInformation);
        if (indices.length != rank)
            throw new IllegalArgumentException("Indexes must be same length as array rank");
        long offset = 0;
        for (int i = 0; i < rank; i++) {
            int size_dimi = size(shapeInformation, i);
            if (indices[i] >= size_dimi)
                throw new IllegalArgumentException(
                                String.format("Index [%d] must not be >= shape[%d]=%d.", i, i, size_dimi));
            if (size_dimi != 1) {
                offset += indices[i] * stride(shapeInformation, i);
            }
        }
        return offset;
    }

    /**
     * Get the offset of the specified indices from the shape info buffer
     *
     * @param shapeInformation    Shape information to get the offset for
     * @param indices             Indices array to get the offset for (must be same length as array rank)
     * @return                    Buffer offset fo the specified indices
     */
    public static long getOffset(DataBuffer shapeInformation, int... indices) {
        int rank = rank(shapeInformation);
        if (indices.length != rank)
            throw new IllegalArgumentException("Indexes must be same length as array rank");
        long offset = 0;
        for (int i = 0; i < rank; i++) {
            int size_dimi = size(shapeInformation, i);
            if (indices[i] > size_dimi)
                throw new IllegalArgumentException(
                                String.format("Index [%d] must not be >= shape[%d]=%d.", i, i, size_dimi));
            if (size_dimi != 1) {
                offset += indices[i] * stride(shapeInformation, i);
            }
        }
        return offset;
    }

    /** Get the offset of the specified [row,col] for the 2d array
     *
     * @param shapeInformation    Shape information
     * @param row                 Row index to get the offset for
     * @param col                 Column index to get the offset for
     * @return                    Buffer offset
     */
    public static long getOffset(DataBuffer shapeInformation, int row, int col) {
        int rank = rank(shapeInformation);
        if (rank != 2)
            throw new IllegalArgumentException(
                            "Cannot use this getOffset method on arrays of rank != 2 (rank is: " + rank + ")");
        return getOffsetUnsafe(shapeInformation, row, col);
    }

    /**
     * Identical to {@link Shape#getOffset(DataBuffer, int, int)} but without input validation on array rank
     */
    public static long getOffsetUnsafe(DataBuffer shapeInformation, int row, int col) {
        long offset = 0;
        int size_0 = sizeUnsafe(shapeInformation, 0);
        int size_1 = sizeUnsafe(shapeInformation, 1);
        if (row >= size_0 || col >= size_1)
            throw new IllegalArgumentException("Invalid indices: cannot get [" + row + "," + col + "] from a "
                            + Arrays.toString(shape(shapeInformation)) + " NDArray");

        if (size_0 != 1)
            offset += row * strideUnsafe(shapeInformation, 0, 2);
        if (size_1 != 1)
            offset += col * strideUnsafe(shapeInformation, 1, 2);

        return offset;
    }

    /** Get the offset of the specified [row,col] for the 2d array
     *
     * @param shapeInformation    Shape information
     * @param row                 Row index to get the offset for
     * @param col                 Column index to get the offset for
     * @return                    Buffer offset
     */
    public static long getOffset(IntBuffer shapeInformation, int row, int col) {
        int rank = rank(shapeInformation);
        if (rank != 2)
            throw new IllegalArgumentException(
                            "Cannot use this getOffset method on arrays of rank != 2 (rank is: " + rank + ")");
        long offset = 0;
        int size_0 = size(shapeInformation, 0);
        int size_1 = size(shapeInformation, 1);
        if (row >= size_0 || col >= size_1)
            throw new IllegalArgumentException("Invalid indices: cannot get [" + row + "," + col + "] from a "
                            + Arrays.toString(shape(shapeInformation)) + " NDArray");

        if (size_0 != 1)
            offset += row * stride(shapeInformation, 0);
        if (size_1 != 1)
            offset += col * stride(shapeInformation, 1);

        return offset;
    }

    /** Get the offset of the specified [dim0,dim1,dim2] for the 3d array
     *
     * @param shapeInformation    Shape information
     * @param dim0                Row index to get the offset for
     * @param dim1                Column index to get the offset for
     * @param dim2                dimension 2 index to get the offset for
     * @return                    Buffer offset
     */
    public static long getOffset(IntBuffer shapeInformation, int dim0, int dim1, int dim2) {
        int rank = rank(shapeInformation);
        if (rank != 3)
            throw new IllegalArgumentException(
                            "Cannot use this getOffset method on arrays of rank != 3 (rank is: " + rank + ")");
        long offset = 0;
        int size_0 = size(shapeInformation, 0);
        int size_1 = size(shapeInformation, 1);
        int size_2 = size(shapeInformation, 2);
        if (dim0 >= size_0 || dim1 >= size_1 || dim2 >= size_2)
            throw new IllegalArgumentException("Invalid indices: cannot get [" + dim0 + "," + dim1 + "," + dim2
                            + "] from a " + Arrays.toString(shape(shapeInformation)) + " NDArray");

        if (size_0 != 1)
            offset += dim0 * stride(shapeInformation, 0);
        if (size_1 != 1)
            offset += dim1 * stride(shapeInformation, 1);
        if (size_2 != 1)
            offset += dim2 * stride(shapeInformation, 2);

        return offset;
    }

    /** Get the offset of the specified [dim0,dim1,dim2] for the 3d array
     *
     * @param shapeInformation    Shape information
     * @param dim0                Row index to get the offset for
     * @param dim1                Column index to get the offset for
     * @param dim2                dimension 2 index to get the offset for
     * @return                    Buffer offset
     */
    public static long getOffset(DataBuffer shapeInformation, int dim0, int dim1, int dim2) {
        int rank = rank(shapeInformation);
        if (rank != 3)
            throw new IllegalArgumentException(
                            "Cannot use this getOffset method on arrays of rank != 3 (rank is: " + rank + ")");
        return getOffsetUnsafe(shapeInformation, dim0, dim1, dim2);
    }

    /**
     * Identical to {@link Shape#getOffset(DataBuffer, int, int, int)} but without input validation on array rank
     */
    public static long getOffsetUnsafe(DataBuffer shapeInformation, int dim0, int dim1, int dim2) {
        long offset = 0;
        int size_0 = sizeUnsafe(shapeInformation, 0);
        int size_1 = sizeUnsafe(shapeInformation, 1);
        int size_2 = sizeUnsafe(shapeInformation, 2);
        if (dim0 >= size_0 || dim1 >= size_1 || dim2 >= size_2)
            throw new IllegalArgumentException("Invalid indices: cannot get [" + dim0 + "," + dim1 + "," + dim2
                            + "] from a " + Arrays.toString(shape(shapeInformation)) + " NDArray");

        if (size_0 != 1)
            offset += dim0 * strideUnsafe(shapeInformation, 0, 3);
        if (size_1 != 1)
            offset += dim1 * strideUnsafe(shapeInformation, 1, 3);
        if (size_2 != 1)
            offset += dim2 * strideUnsafe(shapeInformation, 2, 3);

        return offset;
    }

    /** Get the offset of the specified [dim0,dim1,dim2,dim3] for the 4d array
     *
     * @param shapeInformation    Shape information
     * @param dim0                Row index to get the offset for
     * @param dim1                Column index to get the offset for
     * @param dim2                dimension 2 index to get the offset for
     * @param dim3                dimension 3 index to get the offset for
     * @return                    Buffer offset
     */
    public static long getOffset(IntBuffer shapeInformation, int dim0, int dim1, int dim2, int dim3) {
        int rank = rank(shapeInformation);
        if (rank != 4)
            throw new IllegalArgumentException(
                            "Cannot use this getOffset method on arrays of rank != 4 (rank is: " + rank + ")");
        long offset = 0;
        int size_0 = size(shapeInformation, 0);
        int size_1 = size(shapeInformation, 1);
        int size_2 = size(shapeInformation, 2);
        int size_3 = size(shapeInformation, 3);
        if (dim0 >= size_0 || dim1 >= size_1 || dim2 >= size_2 || dim3 >= size_3)
            throw new IllegalArgumentException("Invalid indices: cannot get [" + dim0 + "," + dim1 + "," + dim2 + ","
                            + dim3 + "] from a " + Arrays.toString(shape(shapeInformation)) + " NDArray");

        if (size_0 != 1)
            offset += dim0 * stride(shapeInformation, 0);
        if (size_1 != 1)
            offset += dim1 * stride(shapeInformation, 1);
        if (size_2 != 1)
            offset += dim2 * stride(shapeInformation, 2);
        if (size_3 != 1)
            offset += dim3 * stride(shapeInformation, 3);

        return offset;
    }

    /** Get the offset of the specified [dim0,dim1,dim2,dim3] for the 4d array
     *
     * @param shapeInformation    Shape information
     * @param dim0                Row index to get the offset for
     * @param dim1                Column index to get the offset for
     * @param dim2                dimension 2 index to get the offset for
     * @param dim3                dimension 3 index to get the offset for
     * @return                    Buffer offset
     */
    public static long getOffset(DataBuffer shapeInformation, int dim0, int dim1, int dim2, int dim3) {
        int rank = rank(shapeInformation);
        if (rank != 4)
            throw new IllegalArgumentException(
                            "Cannot use this getOffset method on arrays of rank != 4 (rank is: " + rank + ")");
        return getOffsetUnsafe(shapeInformation, dim0, dim1, dim2, dim3);
    }

    public static long getOffsetUnsafe(DataBuffer shapeInformation, int dim0, int dim1, int dim2, int dim3) {
        long offset = 0;
        int size_0 = sizeUnsafe(shapeInformation, 0);
        int size_1 = sizeUnsafe(shapeInformation, 1);
        int size_2 = sizeUnsafe(shapeInformation, 2);
        int size_3 = sizeUnsafe(shapeInformation, 3);
        if (dim0 >= size_0 || dim1 >= size_1 || dim2 >= size_2 || dim3 >= size_3)
            throw new IllegalArgumentException("Invalid indices: cannot get [" + dim0 + "," + dim1 + "," + dim2 + ","
                            + dim3 + "] from a " + Arrays.toString(shape(shapeInformation)) + " NDArray");

        if (size_0 != 1)
            offset += dim0 * strideUnsafe(shapeInformation, 0, 4);
        if (size_1 != 1)
            offset += dim1 * strideUnsafe(shapeInformation, 1, 4);
        if (size_2 != 1)
            offset += dim2 * strideUnsafe(shapeInformation, 2, 4);
        if (size_3 != 1)
            offset += dim3 * strideUnsafe(shapeInformation, 3, 4);

        return offset;
    }


    /**
     * Output an int array for a particular dimension
     * @param axes the axes
     * @param shape the current shape
     * @return
     */
    public static int[] sizeForAxes(int[] axes, int[] shape) {
        int[] ret = new int[shape.length];
        for (int i = 0; i < axes.length; i++) {
            ret[i] = shape[axes[i]];
        }
        return ret;
    }

    /**
     * Returns whether the given shape is a vector
     *
     * @param shapeInfo the shapeinfo to test
     * @return whether the given shape is a vector
     */
    public static boolean isVector(IntBuffer shapeInfo) {
        int rank = Shape.rank(shapeInfo);
        if (rank > 2 || rank < 1)
            return false;
        else {
            int len = Shape.length(shapeInfo);
            IntBuffer shape = Shape.shapeOf(shapeInfo);
            return shape.get(0) == len || shape.get(1) == len;
        }
    }

    /**
     * Returns whether the given shape is a vector
     *
     * @param shapeInfo the shapeinfo to test
     * @return whether the given shape is a vector
     */
    public static boolean isVector(DataBuffer shapeInfo) {
        int rank = Shape.rank(shapeInfo);
        if (rank > 2 || rank < 1)
            return false;
        else {
            int len = Shape.length(shapeInfo);
            DataBuffer shape = Shape.shapeOf(shapeInfo);
            return shape.getInt(0) == len || shape.getInt(1) == len;
        }
    }

    /**
     * Returns whether the given shape is a vector
     *
     * @param shape the shape to test
     * @return whether the given shape is a vector
     */
    public static boolean isVector(int[] shape) {
        if (shape.length > 2 || shape.length < 1)
            return false;
        else {
            long len = ArrayUtil.prodLong(shape);
            return shape[0] == len || shape[1] == len;
        }
    }


    /**
     * Returns whether the passed in shape is a matrix
     *
     * @param shapeInfo whether the passed in shape is a matrix
     * @return true if the shape is a matrix false otherwise
     */
    public static boolean isMatrix(IntBuffer shapeInfo) {
        int rank = Shape.rank(shapeInfo);
        if (rank != 2)
            return false;
        return !isVector(shapeInfo);
    }


    /**
     * Returns whether the passed in shape is a matrix
     *
     * @param shapeInfo whether the passed in shape is a matrix
     * @return true if the shape is a matrix false otherwise
     */
    public static boolean isMatrix(DataBuffer shapeInfo) {
        int rank = Shape.rank(shapeInfo);
        if (rank != 2)
            return false;
        return !isVector(shapeInfo);
    }

    /**
     * Returns whether the passed in shape is a matrix
     *
     * @param shape whether the passed in shape is a matrix
     * @return true if the shape is a matrix false otherwise
     */
    public static boolean isMatrix(int[] shape) {
        if (shape.length != 2)
            return false;
        return !isVector(shape);
    }


    /**
     * Gets rid of any singleton dimensions of the given array
     *
     * @param shape the shape to squeeze
     * @return the array with all of the singleton dimensions removed
     */
    public static int[] squeeze(int[] shape) {
        if (isColumnVectorShape(shape))
            return shape;

        List<Integer> ret = new ArrayList<>();

        //strip all but last dimension
        for (int i = 0; i < shape.length; i++)
            if (shape[i] != 1)
                ret.add(shape[i]);
        return ArrayUtil.toArray(ret);
    }



    /**
     * Returns whether 2 shapes are equals by checking for dimension semantics
     * as well as array equality
     *
     * @param shape1 the first shape for comparison
     * @param shape2 the second shape for comparison
     * @return whether the shapes are equivalent
     */
    public static boolean shapeEquals(int[] shape1, int[] shape2) {
        if (isColumnVectorShape(shape1) && isColumnVectorShape(shape2)) {
            return Arrays.equals(shape1, shape2);
        }

        if (isRowVectorShape(shape1) && isRowVectorShape(shape2)) {
            int[] shape1Comp = squeeze(shape1);
            int[] shape2Comp = squeeze(shape2);
            return Arrays.equals(shape1Comp, shape2Comp);
        }

        shape1 = squeeze(shape1);
        shape2 = squeeze(shape2);

        return scalarEquals(shape1, shape2) || Arrays.equals(shape1, shape2);
    }


    /**
     * Returns true if the given shapes are both scalars (0 dimension or shape[0] == 1)
     *
     * @param shape1 the first shape for comparison
     * @param shape2 the second shape for comparison
     * @return whether the 2 shapes are equal based on scalar rules
     */
    public static boolean scalarEquals(int[] shape1, int[] shape2) {
        if (shape1.length == 0 && shape2.length == 1 && shape2[0] == 1) {
            return true;
        } else if (shape2.length == 0 && shape1.length == 1 && shape1[0] == 1) {
            return true;
        }

        return false;
    }

    /**
     * Returns true if the given shape is of length 1
     * or provided the shape length is 2:
     * element 0 is 1
     * @param shapeInfo the shape info to check
     * @return true if the above conditions hold,false otherwise
     */
    public static boolean isRowVectorShape(DataBuffer shapeInfo) {
        int rank = Shape.rank(shapeInfo);
        DataBuffer shape = Shape.shapeOf(shapeInfo);
        return (rank == 2 && shape.getInt(0) == 1) || rank == 1;

    }

    /**
     * Returns true if the given shape is of length 1
     * or provided the shape length is 2:
     * element 0 is 1
     * @param shapeInfo the shape info to check
     * @return true if the above conditions hold,false otherwise
     */
    public static boolean isRowVectorShape(IntBuffer shapeInfo) {
        int rank = Shape.rank(shapeInfo);
        IntBuffer shape = Shape.shapeOf(shapeInfo);
        return (rank == 2 && shape.get(0) == 1) || rank == 1;

    }

    /**
     * Returns true if the given shape is of length 1
     * or provided the shape length is 2:
     * element 0 is 1
     * @param shape the shape to check
     * @return true if the above conditions hold,false otherwise
     */
    public static boolean isRowVectorShape(int[] shape) {
        return (shape.length == 2 && shape[0] == 1) || shape.length == 1;

    }

    /**
     * Returns true if the given shape is length 2 and
     * the size at element 1 is 1
     * @param shape the shape to check
     * @return true if the above listed conditions
     * hold false otherwise
     */
    public static boolean isColumnVectorShape(int[] shape) {
        return (shape.length == 2 && shape[1] == 1);

    }

    /**
     * Prepares two arrays for
     * raw iteration linearly through the data.
     * It uses the same data for allocation
     * @param dst the first array
     */
    public static RawArrayIterationInformation1 prepareRawArrayIter(INDArray dst) {
        return RawArrayIterationInformation1.builder().aOffset(dst.offset()).a(dst.data()).aStrides(dst.stride())
                        .nDim(dst.rank()).shape(dst.shape()).build().computeOut();
    }



    /**
     * Creates sorted strides
     *  whlie retaining the permutation
     * @param strides the strides
     * @return the ordered
     * strides with the permutation/order retained
     */
    public static StridePermutation[] createSortedStrides(int[] strides) {
        StridePermutation[] perm = StridePermutation.create(strides);
        Arrays.sort(perm);
        return perm;
    }

    /**
     * If a shape array is ony 1 in length
     * it returns a row vector
     * @param shape the shape of the array
     * @return the shape as is if its already >= 2 in length
     * otherwise a row vector shape
     */
    public static int[] ensureAtMinRowVector(int... shape) {
        if (shape.length >= 2)
            return shape;
        return new int[] {1, shape[0]};
    }


    public static long getTADLength(int[] shape, int... dimensions) {
        int tadLength = 1;
        for (int i = 0; i < dimensions.length; i++) {
            tadLength *= shape[dimensions[i]];
        }

        return tadLength;
    }


    /**
     *
     * @param shape
     * @param stride
     * @param isFOrder
     * @return
     */
    public static int elementWiseStride(int[] shape, int[] stride, boolean isFOrder) {
        int oldnd;
        int[] olddims = ArrayUtil.copy(shape);
        int[] oldstrides = ArrayUtil.copy(stride);
        long np, op, last_stride;
        int oi, oj, ok, ni, nj, nk;
        long[] newStrides = new long[stride.length];
        oldnd = 0;
        //set the shape to be 1 x length
        int newShapeRank = 2;
        long[] newShape = new long[shape.length];
        newShape[0] = 1;
        newShape[1] = ArrayUtil.prodLong(shape);

        /*
         * Remove axes with dimension 1 from the old array. They have no effect
         * but would need special cases since their strides do not matter.
         */
        for (oi = 0; oi < shape.length; oi++) {
            if (shape[oi] != 1) {
                olddims[oldnd] = shape[oi];
                oldstrides[oldnd] = stride[oi];
                oldnd++;
            }
        }

        np = 1;
        for (ni = 0; ni < newShapeRank; ni++) {
            np *= newShape[ni];
        }
        op = 1;
        for (oi = 0; oi < oldnd; oi++) {
            op *= olddims[oi];
        }
        if (np != op) {
            /* different total sizes; no hope */
            return -1;
        }

        if (np == 0) {
            /* the current code does not handle 0-sized arrays, so give up */
            return -1;
        }

        /* oi to oj and ni to nj give the axis ranges currently worked with */
        oi = 0;
        oj = 1;
        ni = 0;
        nj = 1;
        while (ni < newShapeRank && oi < oldnd) {
            np = newShape[ni];
            op = olddims[oi];

            while (np != op) {
                if (np < op) {
                    /* Misses trailing 1s, these are handled later */
                    np *= newShape[nj++];
                } else {
                    op *= olddims[oj++];
                }
            }

            /* Check whether the original axes can be combined */
            for (ok = oi; ok < oj - 1; ok++) {
                if (isFOrder) {
                    if (oldstrides[ok + 1] != olddims[ok] * oldstrides[ok]) {
                        /* not contiguous enough */
                        return -1;
                    }
                } else {
                    /* C order */
                    if (oldstrides[ok] != olddims[ok + 1] * oldstrides[ok + 1]) {
                        /* not contiguous enough */
                        return -1;
                    }
                }
            }

            /* Calculate new strides for all axes currently worked with */
            if (isFOrder) {
                newStrides[ni] = oldstrides[oi];
                for (nk = ni + 1; nk < nj; nk++) {
                    newStrides[nk] = newStrides[nk - 1] * newShape[nk - 1];
                }
            } else {
                /* C order */
                newStrides[nj - 1] = oldstrides[oj - 1];
                for (nk = nj - 1; nk > ni; nk--) {
                    newStrides[nk - 1] = newStrides[nk] * newShape[nk];
                }
            }
            ni = nj++;
            oi = oj++;
        }

        /*
         * Set strides corresponding to trailing 1s of the new shape.
         */
        if (ni >= 1) {
            last_stride = newStrides[ni - 1];
        } else {
            last_stride = stride[shape.length - 1];
        }
        if (isFOrder && ni >= 1) {
            last_stride *= newShape[ni - 1];
        }
        for (nk = ni; nk < newShapeRank; nk++) {
            newStrides[nk] = last_stride;
        }
        if (newStrides[newShapeRank - 1] >= Integer.MAX_VALUE)
            throw new IllegalArgumentException("Element size can not be >= Integer.MAX_VALUE");
        //returns the last element of the new stride array
        return (int) newStrides[newShapeRank - 1];
    }

    /**
     * A port of numpy's reshaping algorithm that leverages
     * no copy where possible and returns
     * null if the reshape
     * couldn't happen without copying
     * @param arr  the array to reshape
     * @param newShape the new shape
     * @param isFOrder whether the array will be fortran ordered or not
     * @return null if a reshape isn't possible, or a new ndarray
     */
    public static INDArray newShapeNoCopy(INDArray arr, int[] newShape, boolean isFOrder) {
        int oldnd;
        int[] olddims = ArrayUtil.copy(arr.shape());
        int[] oldstrides = ArrayUtil.copy(arr.stride());
        int np, op, last_stride;
        int oi, oj, ok, ni, nj, nk;
        int[] newStrides = new int[newShape.length];
        oldnd = 0;
        /*
         * Remove axes with dimension 1 from the old array. They have no effect
         * but would need special cases since their strides do not matter.
         */
        for (oi = 0; oi < arr.rank(); oi++) {
            if (arr.size(oi) != 1) {
                olddims[oldnd] = arr.size(oi);
                oldstrides[oldnd] = arr.stride(oi);
                oldnd++;
            }
        }

        np = 1;
        for (ni = 0; ni < newShape.length; ni++) {
            np *= newShape[ni];
        }
        op = 1;
        for (oi = 0; oi < oldnd; oi++) {
            op *= olddims[oi];
        }
        if (np != op) {
            /* different total sizes; no hope */
            return null;
        }

        if (np == 0) {
            /* the current code does not handle 0-sized arrays, so give up */
            return null;
        }

        /* oi to oj and ni to nj give the axis ranges currently worked with */
        oi = 0;
        oj = 1;
        ni = 0;
        nj = 1;
        while (ni < newShape.length && oi < oldnd) {
            np = newShape[ni];
            op = olddims[oi];

            while (np != op) {
                if (np < op) {
                    /* Misses trailing 1s, these are handled later */
                    np *= newShape[nj++];
                } else {
                    op *= olddims[oj++];
                }
            }

            /* Check whether the original axes can be combined */
            for (ok = oi; ok < oj - 1; ok++) {
                if (isFOrder) {
                    if (oldstrides[ok + 1] != olddims[ok] * oldstrides[ok]) {
                        /* not contiguous enough */
                        return null;
                    }
                } else {
                    /* C order */
                    if (oldstrides[ok] != olddims[ok + 1] * oldstrides[ok + 1]) {
                        /* not contiguous enough */
                        return null;
                    }
                }
            }

            /* Calculate new strides for all axes currently worked with */
            if (isFOrder) {
                newStrides[ni] = oldstrides[oi];
                for (nk = ni + 1; nk < nj; nk++) {
                    newStrides[nk] = newStrides[nk - 1] * newShape[nk - 1];
                }
            } else {
                /* C order */
                newStrides[nj - 1] = oldstrides[oj - 1];
                for (nk = nj - 1; nk > ni; nk--) {
                    newStrides[nk - 1] = newStrides[nk] * newShape[nk];
                }
            }
            ni = nj++;
            oi = oj++;
        }

        /*
         * Set strides corresponding to trailing 1s of the new shape.
         */
        if (ni >= 1) {
            last_stride = newStrides[ni - 1];
        } else {
            last_stride = arr.elementStride();
        }
        if (isFOrder && ni >= 1) {
            last_stride *= newShape[ni - 1];
        }
        for (nk = ni; nk < newShape.length; nk++) {
            newStrides[nk] = last_stride;
        }

        if (arr instanceof IComplexNDArray)
            return Nd4j.createComplex(arr.data(), newShape, newStrides, arr.offset());


        INDArray ret = Nd4j.create(arr.data(), newShape, newStrides, arr.offset(), isFOrder ? 'f' : 'c');


        return ret;
    }

    /**
     * Infer order from
     * @param shape the shape to infer by
     * @param stride the stride to infer by
     * @param elementStride the element stride to start at
     * @return the storage order given shape and element stride
     */
    public static boolean cOrFortranOrder(int[] shape, int[] stride, int elementStride) {
        int sd;
        int dim;
        int i;
        boolean cContiguous = true;
        boolean isFortran = true;

        sd = 1;
        for (i = shape.length - 1; i >= 0; --i) {
            dim = shape[i];

            if (stride[i] != sd) {
                cContiguous = false;
                break;
            }
            /* contiguous, if it got this far */
            if (dim == 0) {
                break;
            }
            sd *= dim;

        }


        /* check if fortran contiguous */
        sd = elementStride;
        for (i = 0; i < shape.length; ++i) {
            dim = shape[i];
            if (stride[i] != sd) {
                isFortran = false;
            }
            if (dim == 0) {
                break;
            }
            sd *= dim;

        }

        return cContiguous || isFortran;

    }

    /**
     * Infer order from
     * @param shape the shape to infer by
     * @param stride the stride to infer by
     * @param elementStride the element stride to start at
     * @return the storage order given shape and element stride
     */
    public static char getOrder(int[] shape, int[] stride, int elementStride) {
        int sd;
        int dim;
        int i;
        boolean cContiguous = true;
        boolean isFortran = true;

        sd = 1;
        for (i = shape.length - 1; i >= 0; --i) {
            dim = shape[i];

            if (stride[i] != sd) {
                cContiguous = false;
                break;
            }
            /* contiguous, if it got this far */
            if (dim == 0) {
                break;
            }
            sd *= dim;

        }


        /* check if fortran contiguous */
        sd = elementStride;
        for (i = 0; i < shape.length; ++i) {
            dim = shape[i];
            if (stride[i] != sd) {
                isFortran = false;
            }
            if (dim == 0) {
                break;
            }
            sd *= dim;

        }

        if (isFortran && cContiguous)
            return 'a';
        else if (isFortran && !cContiguous)
            return 'f';
        else if (!isFortran && !cContiguous)
            return 'c';
        else
            return 'c';

    }

    /**
     * Infer the order for the ndarray based on the
     * array's strides
     * @param arr the array to get the
     *            ordering for
     * @return the ordering for the given array
     */
    public static char getOrder(INDArray arr) {
        return getOrder(arr.shape(), arr.stride(), arr.elementStride());
    }

    /**
     * Convert the given index (such as 1,1)
     * to a linear index
     * @param shape the shape of the indexes to convert
     * @param indices the index to convert
     * @return the linear index given the shape
     * and indices
     */
    public static long sub2Ind(int[] shape, int[] indices) {
        long index = 0;
        int shift = 1;
        for (int i = 0; i < shape.length; i++) {
            index += shift * indices[i];
            shift *= shape[i];
        }
        return index;
    }

    /**
     * Convert a linear index to
     * the equivalent nd index
     * @param shape the shape of the dimensions
     * @param index the index to map
     * @param numIndices the number of total indices (typically prod of shape(
     * @return the mapped indexes along each dimension
     */
    public static int[] ind2sub(int[] shape, long index, long numIndices) {
        long denom = numIndices;
        int[] ret = new int[shape.length];
        for (int i = ret.length - 1; i >= 0; i--) {
            denom /= shape[i];
            if (index / denom >= Integer.MAX_VALUE)
                throw new IllegalArgumentException("Dimension can not be >= Integer.MAX_VALUE");
            ret[i] = (int) (index / denom);
            index %= denom;

        }
        return ret;
    }

    /**
     * Convert a linear index to
     * the equivalent nd index.
     * Infers the number of indices from the specified shape.
     *
     * @param shape the shape of the dimensions
     * @param index the index to map
     * @return the mapped indexes along each dimension
     */
    public static int[] ind2sub(int[] shape, long index) {
        return ind2sub(shape, index, ArrayUtil.prodLong(shape));
    }

    /**
     * Convert a linear index to
     * the equivalent nd index based on the shape of the specified ndarray.
     * Infers the number of indices from the specified shape.
     *
     * @param arr the array to compute the indexes
     *            based on
     * @param index the index to map
     * @return the mapped indexes along each dimension
     */
    public static int[] ind2sub(INDArray arr, long index) {
        return ind2sub(arr.shape(), index, ArrayUtil.prodLong(arr.shape()));
    }



    /**
     * Convert a linear index to
     * the equivalent nd index
     * @param shape the shape of the dimensions
     * @param index the index to map
     * @param numIndices the number of total indices (typically prod of shape(
     * @return the mapped indexes along each dimension
     */
    public static int[] ind2subC(int[] shape, long index, long numIndices) {
        long denom = numIndices;
        int[] ret = new int[shape.length];
        for (int i = 0; i < shape.length; i++) {
            denom /= shape[i];
            if (index / denom >= Integer.MAX_VALUE)
                throw new IllegalArgumentException("Dimension can not be >= Integer.MAX_VALUE");
            ret[i] = (int) (index / denom);
            index %= denom;

        }
        return ret;
    }



    /**
     * Convert a linear index to
     * the equivalent nd index.
     * Infers the number of indices from the specified shape.
     *
     * @param shape the shape of the dimensions
     * @param index the index to map
     * @return the mapped indexes along each dimension
     */
    public static int[] ind2subC(int[] shape, long index) {
        return ind2subC(shape, index, ArrayUtil.prodLong(shape));
    }

    /**
     * Convert a linear index to
     * the equivalent nd index based on the shape of the specified ndarray.
     * Infers the number of indices from the specified shape.
     *
     * @param arr the array to compute the indexes
     *            based on
     * @param index the index to map
     * @return the mapped indexes along each dimension
     */
    public static int[] ind2subC(INDArray arr, long index) {
        return ind2subC(arr.shape(), index, ArrayUtil.prodLong(arr.shape()));
    }

    /**
     * Compute the offset for the given array
     * given the indices
     * @param arr the array to compute the offset for
     * @param indexes the indexes along each dimension to create the offset for
     * @return the offset for the given array and indexes
     */
    public static long offsetFor(INDArray arr, int[] indexes) {
        ShapeOffsetResolution resolution = new ShapeOffsetResolution(arr);
        resolution.exec(Shape.toIndexes(indexes));
        return resolution.getOffset();
    }



    /**
     * Assert the both shapes are the same length
     * and shape[i] < lessThan[i]
     * @param shape the shape to check
     * @param lessThan the shape to assert against
     */
    public static void assertShapeLessThan(int[] shape, int[] lessThan) {
        if (shape.length != lessThan.length) {
            throw new IllegalArgumentException("Shape length must be == less than length");
        }
        for (int i = 0; i < shape.length; i++) {
            if (shape[i] >= lessThan[i])
                throw new IllegalStateException("Shape[" + i + "] should be less than lessThan[" + i + "]");
        }
    }



    /**
     * Convert the given int indexes
     * to nd array indexes
     * @param indices the indices to convert
     * @return the converted indexes
     */
    public static INDArrayIndex[] toIndexes(int[] indices) {
        INDArrayIndex[] ret = new INDArrayIndex[indices.length];
        for (int i = 0; i < ret.length; i++)
            ret[i] = new NDArrayIndex(indices[i]);
        return ret;
    }


    public static int[] newStrides(int[] strides, int newLength, INDArrayIndex[] indexes) {
        if (strides.length > newLength) {
            int[] newStrides = new int[strides.length - 1];
            for (int i = 0; i < newStrides.length; i++) {
                newStrides[i] = strides[i + 1];
            }
            strides = newStrides;
        }

        return strides;
    }



    /** Check if strides are in order suitable for non-strided mmul etc.
     * Returns true if c order and strides are descending [100,10,1] etc
     * Returns true if f order and strides are ascending [1,10,100] etc
     * False otherwise.
     * @return true if c+descending, f+ascending, false otherwise
     */
    public static boolean strideDescendingCAscendingF(INDArray array) {
        int[] strides = array.stride();
        if (array.isVector() && strides[0] == 1 && strides[1] == 1)
            return true;
        char order = array.ordering();

        if (order == 'c') { //Expect descending. [100,10,1] etc
            for (int i = 1; i < strides.length; i++)
                if (strides[i - 1] <= strides[i])
                    return false;
            return true;
        } else if (order == 'f') {//Expect ascending. [1,10,100] etc
            for (int i = 1; i < strides.length; i++)
                if (strides[i - 1] >= strides[i])
                    return false;
            return true;
        } else if (order == 'a') {
            return true;
        } else {
            throw new RuntimeException("Invalid order: not c or f (is: " + order + ")");
        }
    }

    /**
     * Gets the rank given the shape info buffer
     * @param buffer the buffer to get the rank for
     * @return the rank for the shape buffer
     */
    public static int length(IntBuffer buffer) {
        int ret = 1;
        IntBuffer shape = Shape.shapeOf(buffer);
        int rank = Shape.rank(buffer);
        for (int i = 0; i < rank; i++)
            ret *= shape.get(i);
        return ret;
    }


    /**
     * Gets the rank given the shape info buffer
     * @param buffer the buffer to get the rank for
     * @return the rank for the shape buffer
     */
    public static int length(DataBuffer buffer) {
        int ret = 1;
        DataBuffer shape = Shape.shapeOf(buffer);
        int rank = Shape.rank(buffer);
        for (int i = 0; i < rank; i++)
            ret *= shape.getInt(i);
        return ret;
    }

    /**
     * Gets the rank given the shape info buffer
     * @param buffer the buffer to get the rank for
     * @return the rank for the shape buffer
     */
    public static int rank(DataBuffer buffer) {
        return buffer.getInt(0);
    }

    /**
     * Gets the rank given the shape info buffer
     * @param buffer the buffer to get the rank for
     * @return the rank for the shape buffer
     */
    public static int rank(IntBuffer buffer) {
        IntBuffer ret = (IntBuffer) buffer.position(0);
        return ret.get(0);
    }

    /**
     * Get the size of the specified dimension. Equivalent to shape()[dimension]
     * @param buffer       The buffer to get the
     * @param dimension    The dimension to get.
     * @return             The size of the specified dimension
     */
    public static int size(IntBuffer buffer, int dimension) {
        int rank = rank(buffer);
        if (dimension >= rank)
            throw new IllegalArgumentException("Invalid dimension " + dimension + " for rank " + rank + " array");
        return buffer.get(1 + dimension);
    }

    /**
     * Get the size of the specified dimension. Equivalent to shape()[dimension]
     * @param buffer       The buffer to get the shape from
     * @param dimension    The dimension to get.
     * @return             The size of the specified dimension
     */
    public static int size(DataBuffer buffer, int dimension) {
        int rank = rank(buffer);
        if (dimension >= rank)
            throw new IllegalArgumentException("Invalid dimension " + dimension + " for rank " + rank + " array");
        return buffer.getInt(1 + dimension);
    }

    /**
     * Get the size of the specified dimension. Identical to Shape.size(...), but does not perform any input validation
     * @param buffer       The buffer to get the shape from
     * @param dimension    The dimension to get.
     * @return             The size of the specified dimension
     */
    public static int sizeUnsafe(DataBuffer buffer, int dimension) {
        return buffer.getInt(1 + dimension);
    }

    /**
     * Get array shape from the buffer, as an int[]
     * @param buffer    Buffer to get the shape from
     * @return          Shape array
     */
    public static int[] shape(IntBuffer buffer) {
        int[] ret = new int[rank(buffer)];
        for (int i = 0; i < ret.length; i++)
            ret[i] = buffer.get(1 + i);
        return ret;
    }

    /**
     * Get array shape from the buffer, as an int[]
     * @param buffer    Buffer to get the shape from
     * @return          Shape array
     */
    public static int[] shape(DataBuffer buffer) {
        int[] ret = new int[rank(buffer)];
        for (int i = 0; i < ret.length; i++)
            ret[i] = buffer.getInt(1 + i);
        return ret;
    }

    /**
     * Get the stride of the specified dimension
     * @param buffer       The buffer to get the stride from
     * @param dimension    The dimension to get.
     * @return             The stride of the specified dimension
     */
    public static int stride(IntBuffer buffer, int dimension) {
        int rank = rank(buffer);
        if (dimension >= rank)
            throw new IllegalArgumentException("Invalid dimension " + dimension + " for rank " + rank + " array");
        return buffer.get(1 + rank + dimension);
    }

    /**
     * Get the stride of the specified dimension
     * @param buffer       The buffer to get the stride from
     * @param dimension    The dimension to get.
     * @return             The stride of the specified dimension
     */
    public static int stride(DataBuffer buffer, int dimension) {
        int rank = rank(buffer);
        if (dimension >= rank)
            throw new IllegalArgumentException("Invalid dimension " + dimension + " for rank " + rank + " array");
        return buffer.getInt(1 + rank + dimension);
    }

    /**
     * Get the stride of the specified dimension, without any input validation
     * @param buffer       The buffer to get the stride from
     * @param dimension    The dimension to get.
     * @param rank         Rank of the array
     * @return             The stride of the specified dimension
     */
    public static int strideUnsafe(DataBuffer buffer, int dimension, int rank) {
        return buffer.getInt(1 + rank + dimension);
    }

    /**
     * Return the shape info length
     * given the rank
     * @param rank the rank to get the length for
     * @return rank * 2 + 4
     */
    public static int shapeInfoLength(int rank) {
        return rank * 2 + 4;
    }

    /**
     * Get the stride for the given
     * shape information buffer
     * @param buffer
     * @return
     */
    public static IntBuffer stride(IntBuffer buffer) {
        int rank = rank(buffer);
        IntBuffer ret = (IntBuffer) buffer.position(1 + rank);
        return ret.slice();
    }

    /**
     * Get the shape from
     * the given int buffer
     * @param buffer the buffer to get the shape information for
     * @return
     */
    public static DataBuffer stride(DataBuffer buffer) {
        int rank = rank(buffer);
        return Nd4j.createBuffer(buffer, 1 + rank, rank);
    }



    /**
     * Get the shape from
     * the given int buffer
     * @param buffer the buffer to get the shape information for
     * @return
     */
    public static DataBuffer shapeOf(DataBuffer buffer) {
        int rank = buffer.getInt(0);
        return Nd4j.createBuffer(buffer, 1, rank);
    }

    /**
     * Get the shape from
     * the given int buffer
     * @param buffer the buffer to get the shape information for
     * @return
     */
    public static IntBuffer shapeOf(IntBuffer buffer) {
        IntBuffer ret = (IntBuffer) buffer.position(1);
        return ret.slice();
    }


    /**
     * Prints the shape
     * for this shape information
     * @param arr the shape information to print
     * @return the shape information to string
     */
    public static String shapeToString(INDArray arr) {
        return shapeToString(arr.shapeInfo());
    }

    /**
     * Prints the shape
     * for this shape information
     * @param buffer the shape information to print
     * @return the shape information to string
     */
    public static String shapeToString(IntBuffer buffer) {
        IntBuffer shapeBuff = shapeOf(buffer);
        int rank = Shape.rank(buffer);
        IntBuffer strideBuff = stride(buffer);
        StringBuilder sb = new StringBuilder();
        sb.append("Rank: " + rank + ",");
        sb.append("Offset: " + Shape.offset(buffer) + "\n");
        sb.append(" Order: " + Shape.order(buffer));
        sb.append(" Shape: [");
        for (int i = 0; i < rank; i++) {
            sb.append(shapeBuff.get(i));
            if (i < rank - 1)
                sb.append(",");
        }
        sb.append("], ");

        sb.append(" stride: [");
        for (int i = 0; i < rank; i++) {
            sb.append(strideBuff.get(i));
            if (i < rank - 1)
                sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Get the offset for the buffer
     * @param buffer the shape info buffer to get the offset for
     * @return
     */
    public static int offset(DataBuffer buffer) {
        int length = shapeInfoLength(rank(buffer));
        int ret = buffer.getInt(length - 3);
        return ret;
    }

    /**
     * Get the offset for the buffer
     * @param buffer the shape info buffer to get the offset for
     * @return
     */
    public static int offset(IntBuffer buffer) {
        int length = shapeInfoLength(rank(buffer));
        int ret = buffer.get(length - 3);
        return ret;
    }



    /**
     * Get the element wise stride for the
     * shape info buffer
     * @param buffer the buffer to get the element
     *               wise stride from
     * @return the element wise stride for the buffer
     */
    public static int elementWiseStride(DataBuffer buffer) {
        int length2 = shapeInfoLength(buffer.getInt(0));
        return buffer.getInt(length2 - 2);
    }

    /**
     * Get the element wise stride for the
     * shape info buffer
     * @param buffer the buffer to get the element
     *               wise stride from
     * @return the element wise stride for the buffer
     */
    public static int elementWiseStride(IntBuffer buffer) {
        int length2 = shapeInfoLength(buffer.get(0));
        return buffer.get(length2 - 2);
    }


    /**
     * Get the element wise stride for the
     * shape info buffer
     * @param buffer the buffer to get the element
     *               wise stride from
     * @return the element wise stride for the buffer
     */
    public static void setElementWiseStride(IntBuffer buffer, int elementWiseStride) {
        int length2 = shapeInfoLength(buffer.get(0));
        //        if (1 > 0) throw new RuntimeException("setElementWiseStride called: [" + elementWiseStride + "], buffer: " + bufferToString(buffer));
        buffer.put(length2 - 2, elementWiseStride);
    }

    /**
     * Get the element wise stride for the
     * shape info buffer
     * @param buffer the buffer to get the element
     *               wise stride from
     * @return the element wise stride for the buffer
     */
    public static void setElementWiseStride(DataBuffer buffer, int elementWiseStride) {
        int length2 = shapeInfoLength(Shape.rank(buffer));
        //if (1 > 0) throw new RuntimeException("setElementWiseStride called: [" + elementWiseStride + "], buffer: " + buffer);
        buffer.put(length2 - 2, elementWiseStride);
    }

    /**
     * Prints the {@link IntBuffer}
     * @param buffer the buffer to print
     * @return the to string for the buffer
     *
     */
    public static String bufferToString(IntBuffer buffer) {
        StringBuilder builder = new StringBuilder();
        int rank = buffer.get(0);
        builder.append("[ ").append(rank).append(", ");
        for (int p = 1; p < rank * 2 + 4; p++) {
            builder.append(buffer.get(p));
            if (p < rank * 2 + 4 - 1)
                builder.append(", ");
        }
        builder.append("]");
        return builder.toString();
    }


    /**
     * Returns the order given the shape information
     * @param buffer the buffer
     * @return
     */
    public static char order(IntBuffer buffer) {
        int length = Shape.shapeInfoLength(Shape.rank(buffer));
        return (char) buffer.get(length - 1);
    }

    /**
     * Returns the order given the shape information
     * @param buffer the buffer
     * @return
     */
    public static char order(DataBuffer buffer) {
        int length = Shape.shapeInfoLength(Shape.rank(buffer));
        return (char) buffer.getInt(length - 1);
    }


    /**
     * Returns the order given the shape information
     * @param buffer the buffer
     * @return
     */
    @Deprecated
    public static void setOrder(IntBuffer buffer, char order) {
        int length = Shape.shapeInfoLength(Shape.rank(buffer));
        buffer.put(length - 1, (int) order);
        throw new RuntimeException("setOrder called");
    }

    /**
     * Creates the shape information buffer
     * given the shape,stride
     * @param shape the shape for the buffer
     * @param stride the stride for the buffer
     * @param offset the offset for the buffer
     * @param elementWiseStride the element wise stride for the buffer
     * @param order the order for the buffer
     * @return the shape information buffer given the parameters
     */
    public static DataBuffer createShapeInformation(int[] shape, int[] stride, int offset, int elementWiseStride,
                    char order) {
        if (shape.length != stride.length)
            throw new IllegalStateException("Shape and stride must be the same length");

        int rank = shape.length;
        int shapeBuffer[] = new int[rank * 2 + 4];
        shapeBuffer[0] = rank;
        int count = 1;
        for (int e = 0; e < shape.length; e++)
            shapeBuffer[count++] = shape[e];

        for (int e = 0; e < stride.length; e++)
            shapeBuffer[count++] = stride[e];

        shapeBuffer[count++] = offset;
        shapeBuffer[count++] = elementWiseStride;
        shapeBuffer[count] = (int) order;

        DataBuffer ret = Nd4j.createBuffer(shapeBuffer);
        ret.setConstant(true);
        /*
        DataBuffer ret = Nd4j.createBuffer(new int[shapeInfoLength(shape.length)]);
        ret.setConstant(true);
        int count = 1;
        ret.put(0,shape.length);
        if(shape.length != stride.length)
            throw new IllegalStateException("Shape and stride must be the same length");
        for (int i = 0; i < shape.length; i++) {
            ret.put(count++,shape[i]);
        }
        for (int i = 0; i < shape.length; i++) {
            ret.put(count++,stride[i]);
        }
        
        ret.put(count++,offset);
        ret.put(count++,elementWiseStride);
        ret.put(count++,order);
        
        */
        return ret;
    }


    /**
     * Convert an array to a byte buffer
     * @param arr the array
     * @return a direct byte buffer with the array contents
     */
    public static IntBuffer toBuffer(int... arr) {
        ByteBuffer directBuffer = ByteBuffer.allocateDirect(arr.length * 4).order(ByteOrder.nativeOrder());
        IntBuffer buffer = directBuffer.asIntBuffer();
        for (int i = 0; i < arr.length; i++)
            buffer.put(i, arr[i]);

        return buffer;
    }

    /**
     * To String for an int buffer
     * @param buffer
     * @return
     */
    public static String toString(IntBuffer buffer) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < buffer.capacity(); i++) {
            sb.append(buffer.get(i));
            if (i < buffer.capacity() - 1)
                sb.append(",");
        }

        return sb.toString();
    }


    /**
     * To String for an int buffer
     * @param buffer
     * @return
     */
    public static String toString(DataBuffer buffer) {
        return buffer.toString();
    }

    /**
     * Returns true if the given array
     * is meant for the whole dimension
     * @param arr the array to test
     * @return true if arr.length == 1 && arr[0] is Integer.MAX_VALUE
     */
    public static boolean wholeArrayDimension(int... arr) {
        return arr.length == 1 && arr[0] == Integer.MAX_VALUE;
    }

    /**
     *
     * Compare the contents of a buffer and
     * an array for equals
     * @param arr the array
     * @param other the buffer
     * @return true if the content equals false otherwise
     */
    public static boolean contentEquals(int[] arr, DataBuffer other) {
        for (int i = 0; i < arr.length; i++) {
            if (other.getInt(i) != arr[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     *
     * Compare the contents of a buffer and
     * an array for equals
     * @param arr the array
     * @param other the buffer
     * @return true if the content equals false otherwise
     */
    public static boolean contentEquals(int[] arr, IntBuffer other) {
        for (int i = 0; i < arr.length; i++) {
            other.position(i);
            if (arr[i] != other.get()) {
                return false;
            }
        }
        return true;
    }

    /** Are the elements in the buffer contiguous for this NDArray? */
    public static boolean isContiguousInBuffer(INDArray in) {
        int length = in.length();
        long dLength = in.data().length();
        if (length == dLength)
            return true; //full buffer, always contiguous

        char order = in.ordering();

        int[] shape = in.shape();
        int[] stridesIfContiguous;
        if (order == 'f') {
            stridesIfContiguous = ArrayUtil.calcStridesFortran(shape);
        } else if (order == 'c') {
            stridesIfContiguous = ArrayUtil.calcStrides(shape);
        } else if (order == 'a') {
            stridesIfContiguous = new int[] {1, 1};
        } else {
            throw new RuntimeException("Invalid order: not c or f (is: " + order + ")");
        }

        return Arrays.equals(in.stride(), stridesIfContiguous);
    }

    /**
     *
     * Idea: make an matrix compatible for mmul without needing to be copied first<br>
     * A matrix is compatible for mmul if its values are contiguous in memory. Offset is OK.
     * Returns the input array if input can be used in mmul without additional copy overhead
     * Otherwise returns a copy of the input ndarray that can be used in mmul without additional copy overhead<br>
     * This is useful for example if a matrix is going to be used in multiple mmul operations, so that we only
     * have the overhead of copying at most once (rather than in every mmul operation)
     * @param input Input ndarray
     * @return ndarray that can be used in mmul without copy overhead
     */
    public static INDArray toMmulCompatible(INDArray input) {
        if (input.rank() != 2)
            throw new IllegalArgumentException("Input must be rank 2 (matrix)");
        //Same conditions as GemmParams.copyIfNecessary()
        boolean doCopy = false;
        if (input.ordering() == 'c' && (input.stride(0) != input.size(1) || input.stride(1) != 1))
            doCopy = true;
        else if (input.ordering() == 'f' && (input.stride(0) != 1 || input.stride(1) != input.size(0)))
            doCopy = true;

        if (doCopy)
            return Shape.toOffsetZeroCopyAnyOrder(input);
        else
            return input;
    }
}
