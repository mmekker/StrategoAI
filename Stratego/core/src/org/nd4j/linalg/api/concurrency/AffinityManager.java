package org.nd4j.linalg.api.concurrency;

import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.ndarray.INDArray;

/**
 * @author raver119@gmail.com
 */
public interface AffinityManager {

    enum Location {
        HOST, DEVICE, EVERYWHERE,
    }

    /**
     * This method returns deviceId for current thread
     * @return
     */
    Integer getDeviceForCurrentThread();

    /**
     * This method returns deviceId for specified thread
     * @param thread
     * @return
     */
    Integer getDeviceForThread(Thread thread);

    /**
     * This method returns deviceId for specified threadId
     *
     * @param threadId
     * @return
     */
    Integer getDeviceForThread(long threadId);

    /**
     * This method returns id of current device for a given INDArray
     *
     * @param array
     * @return
     */
    Integer getDeviceForArray(INDArray array);

    /**
     * This method attaches specified thread to specified device
     *
     * @param thread
     * @param deviceId
     */
    void attachThreadToDevice(Thread thread, Integer deviceId);


    /**
     * This method attaches specified thread (by Id) to specified device
     *
     * @param threadId java ID of the thread
     * @param deviceId
     */
    void attachThreadToDevice(long threadId, Integer deviceId);

    /**
     * This method returns number of available devices
     * @return
     */
    int getNumberOfDevices();

    /**
     * Utility method, to associate INDArray with specific device (backend-specific)
     *
     * @param array
     */
    void touch(INDArray array);

    /**
     * Utility method, to associate INDArray with specific device (backend-specific)
     * 
     * @param buffer
     */
    void touch(DataBuffer buffer);

    /**
     * This method replicates given INDArray, and places it to target device.
     *
     * @param deviceId  target deviceId
     * @param array INDArray to replicate
     * @return
     */
    INDArray replicateToDevice(Integer deviceId, INDArray array);

    /**
     * This method replicates given DataBuffer, and places it to target device.
     *
     * @param deviceId  target deviceId
     * @param buffer
     * @return
     */
    DataBuffer replicateToDevice(Integer deviceId, DataBuffer buffer);

    /**
     * This method tags specific INDArray as "recent" on specified location
     *
     * @param location
     */
    void tagLocation(INDArray array, Location location);

    /**
     * This method tags specific DataBuffer as "recent" on specified location
     *
     * @param location
     */
    void tagLocation(DataBuffer buffer, Location location);
}
