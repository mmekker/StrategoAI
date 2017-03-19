package org.nd4j.linalg.api.ops.grid;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bytedeco.javacpp.Pointer;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.BaseOp;
import org.nd4j.linalg.api.ops.Op;

/**
 * POJO describing OP
 * @author raver119@gmail.com
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GridPointers {
    private Op.Type type;
    private int opNum;
    private DataBuffer.Type dtype;

    // indarrays
    private INDArray opX;
    private INDArray opY;
    private INDArray opZ;

    // data buffers
    private Pointer x;
    private Pointer y;
    private Pointer z;


    // strides
    private int xStride = -1;
    private int yStride = -1;
    private int zStride = -1;

    private int xLength = 0;
    private int yLength = 0;
    private int zLength = 0;

    private char xOrder;
    private char yOrder;
    private char zOrder;

    // shapeInfo pointers
    private Pointer xShapeInfo;
    private Pointer yShapeInfo;
    private Pointer zShapeInfo;

    // dimension-related data
    private Pointer dimensions;
    private int dimensionsLength = 0;

    // TAD shapes
    private Pointer tadShape;
    private Pointer tadOffsets;

    // Op extraArgs
    private Pointer extraArgs;

    public GridPointers(Op op, int... dimensions) {
        this.type = BaseOp.getOpType(op);
        this.dtype = op.x().data().dataType();
        this.opNum = op.opNum();

        this.opX = op.x();
        this.opZ = op.z();

        this.xLength = op.x().length();
        this.zLength = op.z().length();

        this.xOrder = op.x().ordering();
        this.zOrder = op.z().ordering();

        this.xStride = op.x().elementWiseStride();
        this.zStride = op.z().elementWiseStride();
        if (op.y() != null) {
            this.yStride = op.y().elementWiseStride();
            this.yLength = op.y().length();
            this.yOrder = op.y().ordering();
            this.opY = op.y();
        }

        if (dimensions != null)
            this.dimensionsLength = dimensions.length;
    }
}
