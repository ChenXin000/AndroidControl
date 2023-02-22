package com.example.server;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingDeque;

public class AcvDecoder {

    private static final String TAG = "AcvDecoder";
    private boolean mIsFirstFrame;
    private boolean decodeState;
    private boolean readDecodeDataState;
    private ByteBuffer byteBuffer;
    private MediaCodec mediaDecoder;
    private MediaCodec.BufferInfo bufferInfo;
    private FileOutputStream fileOutputStream;
    private Thread readDecodeDataThread;

    public AcvDecoder() {
        byteBuffer = ByteBuffer.allocateDirect(1024 * 512);
    }

    /**
     * 查找编码类型是否存在
     * @param mimeType
     * @return
     */
    private MediaCodecInfo selectCodec(String mimeType) {
        MediaCodecList list = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
        MediaCodecInfo[] codecs = list.getCodecInfos();
        for(MediaCodecInfo codecInfo : codecs) {
            if (!codecInfo.isEncoder()) {
                String[] types = codecInfo.getSupportedTypes();
                for(String type : types) {
                    if(type.equalsIgnoreCase(mimeType)) {
                        return codecInfo;
                    }
                }
            }
        }
        return null;
    }

    public void initDecoder(Surface surface ,int width,int height,int bitRate,FileOutputStream fileOutputStream) throws Exception {
        try {
            mIsFirstFrame = true;
            this.fileOutputStream = fileOutputStream;
            if (selectCodec(MediaFormat.MIMETYPE_VIDEO_AVC) != null) {
                MediaFormat decodeFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
//            decodeFormat.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC);
//            decodeFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, ADTSUtils[sampleRateType]);
                decodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
                decodeFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 60); // 最大帧率
                decodeFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);
//            decodeFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
//
//            decodeFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
//            decodeFormat.setInteger(MediaFormat.KEY_IS_ADTS, 1);
                decodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 256 * 1024);

                mediaDecoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);

                mediaDecoder.configure(decodeFormat, surface, null, 0);
                mediaDecoder.start();
                bufferInfo = new MediaCodec.BufferInfo();
                readDecodeData();
                return;
            }
        } catch (IOException e) {
//            Log.e(TAG, "解码器初始化错误 ");
            e.printStackTrace();
        }
        throw new Exception("解码器初始化错误");
    }

    public void decodeData(BlockingDeque<byte[]> dataQueue) throws Exception{
        byteBuffer.clear();
        byte[] head = new byte[4],data,buff;
        int length,len;
        decodeState = true;
        try {
            while(decodeState) {
                if((data = dataQueue.take()) == null) {
                    continue;
                }
                len = (data[0] << 24) | ((data[1] << 16) & 0XFFFFFF) | ((data[2] << 8) & 0XFFFF) | (data[3] & 0XFF);
                if(len == 0) {
                    break;
                }
                byteBuffer.put(data, 4, len);
                byteBuffer.flip();
                while (byteBuffer.limit() - byteBuffer.position() >= 4) {
                    byteBuffer.get(head);
                    if ((head[0] & 0XFF) == 0XFF) {
                        length = (((head[1] << 16) & 0XFFFFFF) | ((head[2] << 8) & 0XFFFF) | (head[3] & 0XFF)) - 4;
                        if (byteBuffer.limit() - byteBuffer.position() >= length) {
                            buff = byteBuffer.array();
                            putDecodeData(buff, byteBuffer.position(), length);

                            fileOutputStream.write(buff,byteBuffer.position(),length);

                            byteBuffer.position(byteBuffer.position() + length);
                        } else {
                            byteBuffer.position(byteBuffer.position() - 4);
//                            Log.e(TAG, "run: 长度不够" );
                            break;
                        }
                    } else {
                        byteBuffer.position(byteBuffer.position() - 3);
//                        Log.e(TAG, "run: 没有找到" );
                    }
                }
//                Log.e(TAG, "run: 重置" );
                byteBuffer.compact();
            }
        } catch (Exception e) {
            e.printStackTrace();
            closeDecode();
            throw new Exception("解码错误");
        }
        try {
            fileOutputStream.flush();
            fileOutputStream.close();
            fileOutputStream = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "decodeData: 解码线程退出" );
    }

    /**
     * 填充数据开始解码
     * @param data
     */
    private void putDecodeData(byte[] data,int offset,int len) throws Exception {
        if(mediaDecoder == null) {
            return ;
        }
        try {
            int inputBufferIndex = mediaDecoder.dequeueInputBuffer(0);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = mediaDecoder.getInputBuffer(inputBufferIndex);
                inputBuffer.clear();
                inputBuffer.put(data,offset,len);
//                Log.e(TAG, "putDecodeData: " + Arrays.toString(data) );
                if (mIsFirstFrame) {
                    /**
                     * Some formats, notably AAC audio and MPEG4, H.264 and H.265 video formats
                     * require the actual data to be prefixed by a number of buffers containing
                     * setup data, or codec specific data. When processing such compressed formats,
                     * this data must be submitted to the codec after start() and before any frame data.
                     * Such data must be marked using the flag BUFFER_FLAG_CODEC_CONFIG in a call to queueInputBuffer.
                     */
                    mediaDecoder.queueInputBuffer(inputBufferIndex, 0, len, System.currentTimeMillis() / 1000, MediaCodec.BUFFER_FLAG_CODEC_CONFIG);
                    mIsFirstFrame = false;
                } else {
                    mediaDecoder.queueInputBuffer(inputBufferIndex, 0, len, System.currentTimeMillis() / 1000,0);
                }
            }

        } catch (Throwable t) {
//            Log.e(TAG, "offerDecoder: 编码错误" );
            t.printStackTrace();
            throw new Exception("编码错误");
        }
    }

    /**
     * 读取解码器数据
     */
    private void readDecodeData() {
        readDecodeDataState = true;
        readDecodeDataThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while(readDecodeDataState) {
                        int outputBufferIndex = mediaDecoder.dequeueOutputBuffer(bufferInfo, 10000);
                        if(outputBufferIndex >= 0) {
//                            ByteBuffer outputBuffer = mediaDecoder.getOutputBuffer(outputBufferIndex);
//                            audioTrack.write(outputBuffer,bufferInfo.size, AudioTrack.WRITE_BLOCKING);
//                            outputBuffer.clear();
//                            Log.e(TAG, "run: 解码" );
                            mediaDecoder.releaseOutputBuffer(outputBufferIndex, true);
                        }
                    }
                } catch (Throwable t) {
                    readDecodeDataState = false;
                    closeDecode();
                    t.printStackTrace();
                }
            }
        });
        readDecodeDataThread.start();
    }

    public void closeDecode() {
        try {
            if(readDecodeDataThread != null) {
                readDecodeDataThread = null;
            } else {
                return;
            }
            decodeState = false;
            if (mediaDecoder != null) {
                mediaDecoder.stop();
                mediaDecoder.release();
                mediaDecoder = null;
                SocketServer.clearReadBlockingDeque();
                SocketServer.blockingDequeOffer();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
