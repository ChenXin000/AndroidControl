package com.example.server;


public class DataBufferManager {
    private byte[] dataBuffer;
    public int length;
    private int rPoint;
    private int rLength;
    private int wPoint;
    private int wLength;

    public DataBufferManager(byte[] buffer) {
        dataBuffer = buffer;
        clear();
    }
    /**
     * @param len 设置以写入长度
     */
    public void setWAvailable(int len) {
        wPoint += len;
        if(wPoint > rPoint)
            rLength = wPoint;
        if(wPoint == length) {
            wPoint = 0;
            wLength = rPoint;
        }
    }
    /**
     * @param len 设置以读取长度
     */
    public void setRAvailable(int len) {
        rPoint += len;
        if(rPoint > wLength) {
            wLength = rPoint;
        }
        if(rPoint == length) {
            rPoint = 0;
            rLength = wPoint;
        }
    }
    /**
     * @return 返回dataBuffer
     */
    public byte[] getDataBuffer() {
        return dataBuffer;
    }
    /**
     * @return 返回当前可写入长度
     */
    public int getWAvailable() {
        return wLength - wPoint;
    }
    /**
     * @return 返回当前可读取长度
     */
    public int getRAvailable() {
        return rLength - rPoint;
    }
    /**
     * @return 返回当前可读取点
     */
    public int getRPoint() {
        return rPoint;
    }
    /**
     * @return 返回当前可写入点
     */
    public int getWPoint() {
        return wPoint;
    }

    /**
     * 重置数据
     */
    public void clear() {
        rPoint = 0;
        rLength = 0;
        wPoint = 0;
        length = 0;
        wLength = 0;
        if(dataBuffer != null){
            length = dataBuffer.length;
            wLength = length;
        }
    }
}
