package org.nd4j.linalg.compression;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.nd4j.linalg.api.buffer.BaseDataBuffer;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.complex.IComplexDouble;
import org.nd4j.linalg.api.complex.IComplexFloat;
import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author raver119@gmail.com
 */
public class CompressedDataBuffer extends BaseDataBuffer {
    @Getter
    @Setter
    protected CompressionDescriptor compressionDescriptor;
    private static Logger logger = LoggerFactory.getLogger(CompressedDataBuffer.class);

    public CompressedDataBuffer(Pointer pointer, @NonNull CompressionDescriptor descriptor) {
        this.compressionDescriptor = descriptor;
        this.pointer = pointer;
        this.length = descriptor.getNumberOfElements();
        this.elementSize = (int) descriptor.getOriginalElementSize();

        initTypeAndSize();
    }

    /**
     * Initialize the type of this buffer
     */
    @Override
    protected void initTypeAndSize() {
        type = Type.COMPRESSED;
        allocationMode = AllocationMode.JAVACPP;
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        //        logger.info("Writing out CompressedDataBuffer");
        // here we should mimic to usual DataBuffer array
        out.writeUTF(allocationMode.name());
        out.writeInt((int) compressionDescriptor.getCompressedLength());
        out.writeUTF(Type.COMPRESSED.name());
        // at this moment we don't care about mimics anymore
        //ByteRawIndexer indexer = new ByteRawIndexer((BytePointer) pointer);
        out.writeUTF(compressionDescriptor.getCompressionAlgorithm());
        out.writeLong(compressionDescriptor.getCompressedLength());
        out.writeLong(compressionDescriptor.getOriginalLength());
        out.writeLong(compressionDescriptor.getNumberOfElements());
        //        out.write(((BytePointer) pointer).getStringBytes());
        for (int x = 0; x < pointer.capacity() * pointer.sizeof(); x++) {
            byte b = pointer.asByteBuffer().get(x);
            out.writeByte(b);
        }



    }

    /**
     * Drop-in replacement wrapper for BaseDataBuffer.read() method, aware of CompressedDataBuffer
     * @param s
     * @return
     */
    public static DataBuffer readUnknown(DataInputStream s, long length) {
        DataBuffer buffer = Nd4j.createBuffer(length);
        buffer.read(s);
        // if buffer is uncompressed, it'll be valid buffer, so we'll just return it
        if (buffer.dataType() != Type.COMPRESSED)
            return buffer;
        else {
            try {

                // if buffer is compressed one, we''ll restore and decompress it here
                String compressionAlgorithm = s.readUTF();
                long compressedLength = s.readLong();
                long originalLength = s.readLong();
                long numberOfElements = s.readLong();

                byte[] temp = new byte[(int) compressedLength];
                for (int i = 0; i < compressedLength; i++) {
                    temp[i] = s.readByte();
                }

                try (Pointer pointer = new BytePointer(temp)) {
                    CompressionDescriptor descriptor = new CompressionDescriptor();
                    descriptor.setCompressedLength(compressedLength);
                    descriptor.setCompressionAlgorithm(compressionAlgorithm);
                    descriptor.setOriginalLength(originalLength);
                    descriptor.setNumberOfElements(numberOfElements);

                    CompressedDataBuffer compressedBuffer = new CompressedDataBuffer(pointer, descriptor);
                    return Nd4j.getCompressor().decompress(compressedBuffer);
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public DataBuffer dup() {
        Pointer nPtr = new BytePointer(compressionDescriptor.getCompressedLength());
        Pointer.memcpy(nPtr, pointer, compressionDescriptor.getCompressedLength());
        CompressionDescriptor nDesc = compressionDescriptor.clone();

        CompressedDataBuffer nBuf = new CompressedDataBuffer(nPtr, nDesc);
        return nBuf;
    }

    @Override
    public long length() {
        return compressionDescriptor.getNumberOfElements();
    }

    /**
     * Create with length
     *
     * @param length a databuffer of the same type as
     *               this with the given length
     * @return a data buffer with the same length and datatype as this one
     */
    @Override
    protected DataBuffer create(long length) {
        throw new UnsupportedOperationException("This operation isn't supported for CompressedDataBuffer");
    }

    /**
     * Create the data buffer
     * with respect to the given byte buffer
     *
     * @param data the buffer to create
     * @return the data buffer based on the given buffer
     */
    @Override
    public DataBuffer create(double[] data) {
        throw new UnsupportedOperationException("This operation isn't supported for CompressedDataBuffer");
    }

    /**
     * Create the data buffer
     * with respect to the given byte buffer
     *
     * @param data the buffer to create
     * @return the data buffer based on the given buffer
     */
    @Override
    public DataBuffer create(float[] data) {
        throw new UnsupportedOperationException("This operation isn't supported for CompressedDataBuffer");
    }

    /**
     * Create the data buffer
     * with respect to the given byte buffer
     *
     * @param data the buffer to create
     * @return the data buffer based on the given buffer
     */
    @Override
    public DataBuffer create(int[] data) {
        throw new UnsupportedOperationException("This operation isn't supported for CompressedDataBuffer");
    }

    @Override
    public IComplexFloat getComplexFloat(long i) {
        throw new UnsupportedOperationException("This operation isn't supported for CompressedDataBuffer");
    }

    @Override
    public IComplexDouble getComplexDouble(long i) {
        throw new UnsupportedOperationException("This operation isn't supported for CompressedDataBuffer");
    }
}
