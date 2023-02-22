package com.xc.XTool.ClientService;

import android.graphics.Bitmap;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import com.xc.XTool.SocketLive;

import java.io.IOException;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;

public class AvcEncoder {

    private static final String TAG = "AvcEncoder";

    private static final String KEY_MAX_FPS_TO_ENCODER = "max-fps-to-encoder";
//    private UdpSendTask udpSendTask;
//    private SendTask sendTask;
    private Thread readEncodeDataThread;
    private boolean readEncodeDataState;
    private MediaCodec.BufferInfo bufferInfo;

    private MediaCodec mediaCodec;
    private DatagramSocket socket;
    private SocketLive socketLive;
    private byte[] sps_pps_buf;
    public static final int NAL_I = 5;
    public static final int NAL_SPS = 7;
    private Surface surface;

//    private CameraFunctions camera;
    private int maxFps;

    //pts时间基数
    long presentationTimeUs = 0;

    /**
     * 查找编码类型是否存在
     * @param mimeType
     * @return
     */
    private MediaCodecInfo selectCodec(String mimeType) {
        MediaCodecList list = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
        MediaCodecInfo[] codecs = list.getCodecInfos();
        for(MediaCodecInfo codecInfo : codecs) {
            if (codecInfo.isEncoder()) {
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

    /**
     * 构造方法
     * @param maxFps 帧数
     * @param bitRate   码流  2500000
     */
    public AvcEncoder(int width,int height,int maxFps, int bitRate) {
//        this.camera = camera;
        this.maxFps = maxFps;
//        //这里的大小要通过计算，而不是网上简单的 *3/2
//        yuv420 = new byte[getYuvBuffer(width, height)];
//        rotateYuv420 = new byte[getYuvBuffer(width, height)];

        //确定当前MediaCodec支持的图像格式
//        int colorFormat = selectColorFormat(selectCodec(mime), MediaFormat.MIMETYPE_VIDEO_AVC);
        try {
//            this.socket = new DatagramSocket();
//            sendTask = new SendTask(camera);
//            sendTask.start();

//            SocketClient.sendState = true;
            SocketClient.startSend();

//            if(!SocketClient.startSendDataThread()) {
//                Log.e(TAG, "AvcEncoder: sendDataThread error" );
//                return;
//            }

//            socketLive = new SocketLive(null);
//            socketLive.start();

            if(selectCodec(MediaFormat.MIMETYPE_VIDEO_AVC) == null) {
                Log.e(TAG, "AvcEncoder: selectCodec error" );
                return ;
            }
            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            //正常的编码出来是横屏的。因为手机本身采集的数据默认就是横屏的
            // MediaFormat mediaFormat = MediaFormat.createVideoFormat(mime, width, height);
            //如果你需要旋转90度或者270度，那么需要把宽和高对调。否则会花屏。因为比如你320 X 240，图像旋转90°之后宽高变成了240 X 320。
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
            //设置参数

//            mediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 60);
//            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2); //关键帧间隔时间 单位s
            mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 256 * 1024);
            mediaFormat.setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 100_000); // µs
            if(maxFps > 0) {
                mediaFormat.setFloat(KEY_MAX_FPS_TO_ENCODER, maxFps);
            }
            mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            surface = mediaCodec.createInputSurface();
            mediaCodec.start();
            bufferInfo = new MediaCodec.BufferInfo();

            readEncodeData();

        } catch (Exception e) {
            closeEncoder();
//            camera.closeCamera();
            e.printStackTrace();
        }

    }

    public Surface getSurface() {
        return surface;
    }

    public void closeEncoder() {
        try {
            readEncodeDataState = false;
            if(readEncodeDataThread != null) {
                readEncodeDataThread = null;
            }
            if(surface != null) {
                surface = null;
            }
//            if(sendTask != null) {
//                sendTask.sendState = false;
//                sendTask = null;
//            }
            Y = null;
            U = null;
            V = null;
            nv12 = null;
//            SocketClient.clearSendDataBlockingDeque();
//            SocketClient.sendState = false;
            if(mediaCodec != null) {
                mediaCodec.stop();
                mediaCodec.release();
                mediaCodec = null;
            }
            SocketClient.stopSend();

        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.e(TAG, "closeEncoder: 关闭解码器" );
    }
    /**
     * 开始编码
     *
     **/

    public void putEncodeData(Image image) {
        if(mediaCodec == null) {
            return ;
        }
        try {
//            byte[] NV12 = nv21toNV12(input);
            byte[] NV12 = yuv420_888ToNv12(image);
            int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = mediaCodec.getInputBuffer(inputBufferIndex);
                inputBuffer.clear();
                inputBuffer.put(NV12,0,NV12.length);
//                Log.e(TAG, "inputBuffer: "+ inputBuffer.remaining());
//                long n = inputBuffer.limit() - inputBuffer.position();
//                Log.e(TAG, "inputBuffer: "+ n);

//                inputBuffer.put(NV12);
//                Log.e(TAG, "NV21: "+ NV12.length);
//                Log.e(TAG, "input: "+ input.length );
                //计算pts，这个值是一定要设置的
                long pts = computePresentationTime(presentationTimeUs);
                mediaCodec.queueInputBuffer(inputBufferIndex, 0, NV12.length, pts, 0);
                presentationTimeUs ++;
            }

//            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
//            int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
//
//            while (outputBufferIndex >= 0) {
//                ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex);
//                dealFrame(outputBuffer,bufferInfo);
//                outputBuffer.clear();
//                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
//                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
//            }

        } catch (Throwable t) {
//            camera.closeCamera();
            t.printStackTrace();
        }
    }

    /**
     * 读取编码器数据
     */
    public void readEncodeData() {
        readEncodeDataState = true;
        readEncodeDataThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while(readEncodeDataState) {
                        int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 20000);
//                        Log.e(TAG, "run: " + outputBufferIndex );
                        if(outputBufferIndex >= 0) {
                            ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(outputBufferIndex);
                            dealFrame(outputBuffer,bufferInfo.size);
                            outputBuffer.clear();
                            mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                        }
                    }
                } catch (NullPointerException n) {
                    n.printStackTrace();
                    Log.e(TAG, "run: 空异常");
                } catch (IllegalStateException i) {
                    i.printStackTrace();
                    Log.e(TAG, "run: 非法异常");
                } catch (Exception t) {
                    t.printStackTrace();
                    closeEncoder();
                } finally {
                    readEncodeDataState = false;
                }
//                mediaCodec = null;
            }
        });
        readEncodeDataThread.start();
    }

    /**
     * 添加关键帧
     * @param buf 数据
     * @param length 长度
     */
    private void dealFrame(ByteBuffer buf, int length) throws Exception{
        int offset = 4;
        if (buf.get(2) == 0x01) {
            offset = 3;
        }
        byte[] bytes;
        int type = buf.get(offset) & 0x1F;
        if (type == NAL_SPS) {
            sps_pps_buf = new byte[length];
            buf.get(sps_pps_buf);
            bytes = new byte[sps_pps_buf.length + 4];
            System.arraycopy(sps_pps_buf,0,bytes,4,sps_pps_buf.length);

        } else if (type == NAL_I) {
            bytes = new byte[length + sps_pps_buf.length + 4];
            System.arraycopy(sps_pps_buf,0,bytes,4,sps_pps_buf.length);
            buf.get(bytes,sps_pps_buf.length + 4,length);

        } else {
            bytes = new byte[length + 4];
            buf.get(bytes,4,length);

        }
        int len = bytes.length;
        bytes[0] = (byte) 0XFF;
        bytes[1] = (byte) ((len >> 16) & 0XFF);
        bytes[2] = (byte) ((len >> 8) & 0XFF);
        bytes[3] = (byte) (len & 0XFF);

        SocketClient.putSendData(bytes);

//        if(!SocketClient.putSendData(bytes)) {
//            camera.closeCamera();
//        }
    }

    /**
     * 计算视频pts
     */
    private long computePresentationTime(long frameIndex) {
        return 132 + frameIndex * 1000000 / maxFps;
    }


//    private void sendData(byte[] data) {
//        try {
//            SocketClient.outputStream.write(data);
//        } catch (IOException e) {
//            e.printStackTrace();
//            closeEncoder();
//            camera.closeCamera();
//        }
//    }

    private static byte[] Y;
    private static byte[] U;
    private static byte[] V;
    private static byte[] nv12;

    public static byte[] yuv420_888ToNv12(Image image) {
//        Log.e(TAG, "yuv420_888ToNv12:getWidth " + image.getWidth() );
//        Log.e(TAG, "yuv420_888ToNv12:getHeight " + image.getHeight() );
        Image.Plane[] planes = image.getPlanes();
        int y_length = planes[0].getBuffer().remaining();
        if(nv12 == null) {
//            Y = new byte[planes[0].getBuffer().remaining()];
            U = new byte[planes[1].getBuffer().remaining()];
            V = new byte[planes[2].getBuffer().remaining()];
            nv12 = new byte[getYuvBuffer(image.getWidth(), image.getHeight())];
        }
//        if(image.getPlanes()[0].getBuffer().remaining() == Y.length) {
            planes[0].getBuffer().get(nv12,0,y_length);
//        U = planes[1].getBuffer().array();
//        V = planes[2].getBuffer().array();
            planes[1].getBuffer().get(U);
            planes[2].getBuffer().get(V);
//        }
//        Log.e(TAG, "yuv420_888ToNv12:nv12 " + nv12.length );
//        System.arraycopy(Y, 0, nv12, 0, Y.length);
        int j = 0,k = 0;
        for(int i = y_length;i < nv12.length;i+=2) {
            nv12[i] = U[j];
            j += planes[1].getPixelStride();
            nv12[i+1] = V[k];
            k += planes[2].getPixelStride();
        }
        return nv12;
    }

    public static byte[] yuvToNv21(byte[] y, byte[] u, byte[] v, int width, int height) {
        byte[] nv21 = new byte[getYuvBuffer(width,height)];
        System.arraycopy(y, 0, nv21, 0, y.length);
        // 注意，若length值为 y.length * 3 / 2 会有数组越界的风险，需使用真实数据长度计算
        int length = y.length + u.length / 2 + v.length / 2;
        int uIndex = 0, vIndex = 0;
        for (int i = width * height; i < length; i += 2) {
            nv21[i] = v[vIndex];
            nv21[i + 1] = u[uIndex];
            vIndex += 2;
            uIndex += 2;
        }
        return nv21;
    }

    public  static byte[] nv21toNV12(byte[] nv21) {
        int size = nv21.length;
        byte[] nv12 = new byte[size];
        int len = size * 2 / 3;
        System.arraycopy(nv21, 0, nv12, 0, len);

        int i = len;
        while (i < size - 1) {
            nv12[i] = nv21[i + 1];
            nv12[i + 1] = nv21[i];
            i += 2;
        }
        return nv12;
    }

    //计算YUV的buffer的函数，需要根据文档计算，而不是简单“*3/2”
    public static int getYuvBuffer(int width, int height) {
        // stride = ALIGN(width, 16)
        int stride = (int) Math.ceil(width / 16.0) * 16;
        // y_size = stride * height
        int y_size = stride * height;
        // c_stride = ALIGN(stride/2, 16)
        int c_stride = (int) Math.ceil(width / 32.0) * 16;
        // c_size = c_stride * height/2
        int c_size = c_stride * height / 2;
        // size = y_size + c_size * 2
        return y_size + c_size * 2;
    }


    //-----------下面是常用的格式转换方法-----------------------------

    //yv12 转 yuv420p  yvu -> yuv,yuv420p就是I420格式，使用极其广泛
    private void swapYV12toI420(byte[] yv12bytes, byte[] i420bytes, int width, int height) {
        System.arraycopy(yv12bytes, 0, i420bytes, 0, width * height);
        System.arraycopy(yv12bytes, width * height + width * height / 4, i420bytes, width * height, width * height / 4);
        System.arraycopy(yv12bytes, width * height, i420bytes, width * height + width * height / 4, width * height / 4);
    }

    //选择了YUV420SP作为编码的目标颜色空间，其实YUV420SP就是NV12，咱们CAMERA设置的是NV21，所以需要转一下
    private void NV21ToNV12(byte[] nv21, byte[] nv12, int width, int height) {
        if (nv21 == null || nv12 == null) return;
        int framesize = width * height;
        int i = 0, j = 0;
        System.arraycopy(nv21, 0, nv12, 0, framesize);
        for (i = 0; i < framesize; i++) {
            nv12[i] = nv21[i];
        }
        for (j = 0; j < framesize / 2; j += 2) {
            nv12[framesize + j - 1] = nv21[j + framesize];
        }
        for (j = 0; j < framesize / 2; j += 2) {
            nv12[framesize + j] = nv21[j + framesize - 1];
        }
    }

    public Bitmap rawByteArray2RGBABitmap2(byte[] data, int width, int height) {
        int frameSize = width * height;
        int[] rgba = new int[frameSize];

        for (int i = 0; i < height; i++)
            for (int j = 0; j < width; j++) {
                int y = (0xff & ((int) data[i * width + j]));
                int u = (0xff & ((int) data[frameSize + (i >> 1) * width + (j & ~1) + 0]));
                int v = (0xff & ((int) data[frameSize + (i >> 1) * width + (j & ~1) + 1]));
                y = y < 16 ? 16 : y;

                int r = Math.round(1.164f * (y - 16) + 1.596f * (v - 128));
                int g = Math.round(1.164f * (y - 16) - 0.813f * (v - 128) - 0.391f * (u - 128));
                int b = Math.round(1.164f * (y - 16) + 2.018f * (u - 128));

                r = r < 0 ? 0 : (r > 255 ? 255 : r);
                g = g < 0 ? 0 : (g > 255 ? 255 : g);
                b = b < 0 ? 0 : (b > 255 ? 255 : b);

                rgba[i * width + j] = 0xff000000 + (b << 16) + (g << 8) + r;
            }

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bmp.setPixels(rgba, 0, width, 0, 0, width, height);
        return bmp;
    }
    //镜像
    private void Mirror(byte[] yuv_temp, int w, int h) {
        int i, j;

        int a, b;
        byte temp;
        //mirror y
        for (i = 0; i < h; i++) {
            a = i * w;
            b = (i + 1) * w - 1;
            while (a < b) {
                temp = yuv_temp[a];
                yuv_temp[a] = yuv_temp[b];
                yuv_temp[b] = temp;
                a++;
                b--;
            }
        }
        //mirror u
        int uindex = w * h;
        for (i = 0; i < h / 2; i++) {
            a = i * w / 2;
            b = (i + 1) * w / 2 - 1;
            while (a < b) {
                temp = yuv_temp[a + uindex];
                yuv_temp[a + uindex] = yuv_temp[b + uindex];
                yuv_temp[b + uindex] = temp;
                a++;
                b--;
            }
        }
        //mirror v
        uindex = w * h / 4 * 5;
        for (i = 0; i < h / 2; i++) {
            a = i * w / 2;
            b = (i + 1) * w / 2 - 1;
            while (a < b) {
                temp = yuv_temp[a + uindex];
                yuv_temp[a + uindex] = yuv_temp[b + uindex];
                yuv_temp[b + uindex] = temp;
                a++;
                b--;
            }
        }
    }

}
