package org.nd4j.linalg.api.blas.params;

import lombok.Data;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.Arrays;

/**
 * Used for setting the gemm parameters
 * Separates blas logic from
 * the run time itself.
 *
 * @author Adam Gibson
 */
public @Data class GemmParams {
    private int lda, ldb, ldc, m, n, k;
    private INDArray a, b, c;
    private char transA = 'N';
    private char transB = 'N';
    private char ordering = 'f';


    /**
     *
     * @param a
     * @param b
     * @param c
     */
    public GemmParams(INDArray a, INDArray b, INDArray c) {
        if (a.columns() != b.rows()) {
            throw new IllegalArgumentException("A columns must equal B rows. MMul attempt: "
                            + Arrays.toString(a.shape()) + "x" + Arrays.toString(b.shape()));
        }
        if (b.columns() != c.columns()) {
            throw new IllegalArgumentException("B columns must match C columns. MMul attempt: "
                            + Arrays.toString(a.shape()) + "x" + Arrays.toString(b.shape())
                            + "; result array provided: " + Arrays.toString(c.shape()));
        }
        if (a.rows() != c.rows()) {
            throw new IllegalArgumentException("A rows must equal C rows. MMul attempt: " + Arrays.toString(a.shape())
                            + "x" + Arrays.toString(b.shape()) + "; result array provided: "
                            + Arrays.toString(c.shape()));
        }



        if (Nd4j.allowsSpecifyOrdering()) {
            if (a.ordering() == b.ordering()) {
                //both will be same ordering for cblas
                this.ordering = a.ordering();
                //automatically assume fortran ordering
                //multiple backends force us to be
                //in fortran ordering only
                this.a = copyIfNeccessary(a);
                this.b = copyIfNeccessary(b);
                this.c = c;
                if (ordering == 'c') {
                    this.m = c.columns();
                    this.n = c.rows();
                    this.k = a.columns();
                } else {
                    this.m = c.rows();
                    this.n = c.columns();
                    this.k = b.columns();
                }

                this.lda = a.rows();
                this.ldb = b.rows();
                this.ldc = c.rows();

                this.transA = 'N';
                this.transB = 'N';
            } else {
                //automatically assume fortran ordering
                //multiple backends force us to be
                //in fortran ordering only
                this.a = copyIfNeccessary(a);
                this.b = b.dup(a.ordering());
                this.c = c;

                this.m = c.rows();
                this.n = c.columns();
                this.k = a.columns();

                this.ordering = a.ordering();

                this.lda = a.rows();
                this.ldb = b.rows();
                this.ldc = c.rows();

                this.transA = 'N';
                this.transB = 'N';
            }


        } else {
            //automatically assume fortran ordering
            //multiple backends force us to be
            //in fortran ordering only
            this.a = copyIfNeccessary(a);
            this.b = copyIfNeccessary(b);
            this.c = c;

            this.m = c.rows();
            this.n = c.columns();
            this.k = a.columns();

            //always fortran ordering
            this.lda = (this.a.ordering() == 'f' ? this.a.rows() : this.a.columns()); //Leading dimension of a, as declared. But swap if 'c' order
            this.ldb = (this.b.ordering() == 'f' ? this.b.rows() : this.b.columns()); //Leading dimension of b, as declared. But swap if 'c' order
            this.ldc = c.rows();

            this.transA = (this.a.ordering() == 'c' ? 'T' : 'N');
            this.transB = (this.b.ordering() == 'c' ? 'T' : 'N');

        }

        ///validate();
    }

    public GemmParams(INDArray a, INDArray b, INDArray c, boolean transposeA, boolean transposeB) {
        this(transposeA ? a.transpose() : a, transposeB ? b.transpose() : b, c);
    }



    private INDArray copyIfNeccessary(INDArray arr) {
        //See also: Shape.toMmulCompatible - want same conditions here and there
        if (arr.isMatrix()) {
            //Check if matrix values are contiguous in memory. If not: dup
            //Contiguous for c if: stride[0] == shape[1] and stride[1] = 1
            //Contiguous for f if: stride[0] == 1 and stride[1] == shape[0]
            if (!Nd4j.allowsSpecifyOrdering() && arr.ordering() == 'c'
                            && (arr.stride(0) != arr.size(1) || arr.stride(1) != 1))
                return arr.dup();
            else if (arr.ordering() == 'f' && (arr.stride(0) != 1 || arr.stride(1) != arr.size(0)))
                return arr.dup();
            else if (arr.elementWiseStride() < 0)
                return arr.dup();
        }
        return arr;
    }



    private void validate() {
        if (ordering == 'c') {
            if (transA == 'T' || transA == 't') {
                if (m != a.rows())
                    throw new IllegalArgumentException("M under transpose and c ordering must be A.columns()");
                if (k != a.columns())
                    throw new IllegalArgumentException("K under transpose and c ordering must be A.rows()");
            }
            //N
            else {
                if (m != a.columns())
                    throw new IllegalArgumentException("M under no transpose and c ordering must be A.rows()");
                if (k != a.rows())
                    throw new IllegalArgumentException("K under no transpose and c ordering must be A.columns()");
            }
        } else {
            if (transB == 't' || transB == 'T') {
                if (n != b.columns())
                    throw new IllegalArgumentException("N under transpose and c ordering ust be B.rows()");
                if (k != b.rows())
                    throw new IllegalArgumentException("K under tranpose and c ordering must be B.columns()");
            }
            //N
            else {
                if (n != b.rows())
                    throw new IllegalArgumentException("N under no transpose and c ordering must be B.columns()");
                if (k != b.columns())
                    throw new IllegalArgumentException("K under no transpose and c ordering must be B.rows()");
            }
        }


    }



}
