package com.example.server;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.BlockingDeque;

public class AudioDecode {
    private final static String TAG = "AudiDecode";
    private final static int[] ADTSUtils = {96000,88200,64000,48000,44100,32000,24000,22050,16000,12000,11025,8000,7350};
    private final static int[] bitRateList = {32000,96000,128000,160000,192000,256000,320000};
//    private static final byte BITRATE =  2;
//    private static final byte SAMPLERATE = 4;
    private static final byte START_AUDIO = 7;
    private static final byte STOP_AUDIO = 8;
    private static String AUDIO_PATH;
//    private  byte sampleRateType = 4;
//    private  byte bitRateType = 2;
    private MediaCodec mediaDecoder;
    private AudioTrack audioTrack;
    private Thread stopPlayThread;
    private Thread playThread;
    private Thread readDecodeDataThread;
    private MediaCodec.BufferInfo bufferInfo;
    private BlockingDeque<byte[]> dataQueue;
    private ByteBuffer byteBuffer;

    private File file;
    private FileOutputStream fileOutputStream;

    private boolean mIsFirstFrame;
    private boolean readDecodeDataState;
    private boolean playState = false;
    private boolean firstFps;


    public AudioDecode() {
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

    public void initDecoder(byte sampleRateType,byte bitRateType) {
        try {
            if(mediaDecoder != null) {
//                Log.e(TAG, "initDecoder: 解码器未释放" );
                return ;
            }
            mIsFirstFrame = true;
            Log.e(TAG, "sampleRateType: " + sampleRateType );
            Log.e(TAG, "bitRateType: " + bitRateType );
            if (selectCodec(MediaFormat.MIMETYPE_AUDIO_AAC) == null) {
                return;
            }
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,ADTSUtils[sampleRateType],AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT,4096,AudioTrack.MODE_STREAM);
            audioTrack.play();

//            MediaFormat decodeFormat = new MediaFormat();
            MediaFormat decodeFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, ADTSUtils[sampleRateType], 1);
//            decodeFormat.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC);
//            decodeFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, ADTSUtils[sampleRateType]);
            decodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRateList[bitRateType]);
//            decodeFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);

            decodeFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            decodeFormat.setInteger(MediaFormat.KEY_IS_ADTS, 1);
            decodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16 * 1024);

            mediaDecoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);

            mediaDecoder.configure(decodeFormat, null, null, 0);
            mediaDecoder.start();
            bufferInfo = new MediaCodec.BufferInfo();
            readDecodeData();
        } catch (IOException e) {
            Log.e(TAG, "解码器初始化错误 ");
            e.printStackTrace();
        }
    }

    /**
     * 填充数据开始解码
     * @param data
     */
    public void putDecodeData(byte[] data,int offset,int len) {
        if(mediaDecoder == null) {
            return ;
        }
        try {
            int inputBufferIndex = mediaDecoder.dequeueInputBuffer(-1);
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
                    mediaDecoder.queueInputBuffer(inputBufferIndex, 0, len, System.currentTimeMillis(), MediaCodec.BUFFER_FLAG_CODEC_CONFIG);
                    mIsFirstFrame = false;
                } else {
                    mediaDecoder.queueInputBuffer(inputBufferIndex, 0, len, System.currentTimeMillis(),0);
                }
            }

        } catch (Throwable t) {
            Log.e(TAG, "offerDecoder: 编码错误" );
//            stopDecoder();
            stopPlay();
            t.printStackTrace();
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
                while(readDecodeDataState) {
                    if(mediaDecoder == null) {
                        break;
                    }
                    try {
                        int outputBufferIndex = mediaDecoder.dequeueOutputBuffer(bufferInfo, 0);
                        if(outputBufferIndex >= 0) {
                            ByteBuffer outputBuffer = mediaDecoder.getOutputBuffer(outputBufferIndex);
                            audioTrack.write(outputBuffer,bufferInfo.size, AudioTrack.WRITE_BLOCKING);
                            outputBuffer.clear();
//                            Log.e(TAG, "run: 解码" );
                            mediaDecoder.releaseOutputBuffer(outputBufferIndex, false);
                        }

                    } catch (Throwable t) {
//                        stopDecoder();
                        stopPlay();
                        readDecodeDataState = false;
                        t.printStackTrace();
                        break;
                    }
                }
            }
        });
        readDecodeDataThread.start();
    }

    private boolean createFile() {
        AUDIO_PATH = MainActivity.MAIN_PATH + MainActivity.MODEL + "/Audio/";
        String time = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss", Locale.CHINESE).format(new Date(System.currentTimeMillis()));
        file = new File( AUDIO_PATH);
        if(!file.exists()) {
            if(!file.mkdir()) {
                return false;
            }
        }
        file = new File(AUDIO_PATH, time + ".aac");
        try {
            fileOutputStream = new FileOutputStream(file);
            Log.e(TAG, "playAudio: 文件打开"  );
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    private void decodeData(BlockingDeque<byte[]> dataQueue) {
        byteBuffer.clear();
        byte[] head = new byte[7],data;
        int len,offset;
        int syncword,aac_frame_length;
        while(playState) {
            if((data= dataQueue.poll()) == null) {
                continue;
            }
            offset = 4;
            len = (data[0] << 24) | ((data[1] << 16) & 0XFFFFFF) | ((data[2] << 8) & 0XFFFF) | (data[3] & 0XFF);
            if(!firstFps) {
                byteBuffer.put(data, offset, len);
            } else if(len == 2) {
                putDecodeData(data,offset,len);
                firstFps = false;
                continue;
            } else if (len > 2) {
                putDecodeData(data,offset,2);
                firstFps = false;
                len -= 2;
                byteBuffer.put(data,offset,len);
                offset = 6;
            }
            byteBuffer.flip();
            try {
                fileOutputStream.write(data,offset,len);
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
            while (byteBuffer.limit() - byteBuffer.position() >= 7) {
                byteBuffer.get(head);
                syncword = ((head[0] << 4) & 0XFFF) | ((head[1] >> 4) & 0xF);
                aac_frame_length = ((head[3] << 11) & 0X1FFF) | ((head[4] << 3) & 0X7FF) | ((head[5] >> 5) & 0X7);
                byteBuffer.position(byteBuffer.position() - 7);
                if (syncword == 0XFFF) {
                    if (byteBuffer.limit() - byteBuffer.position() >= aac_frame_length) {
                        putDecodeData(byteBuffer.array(), byteBuffer.position(), aac_frame_length);
                        byteBuffer.position(byteBuffer.position() + aac_frame_length);
                    } else {
                        Log.e(TAG, "run: 长度不够" );
                        break;
                    }
                } else {
                    Log.e(TAG, "run: 没有找到" );
                    byteBuffer.position(byteBuffer.position() + 1);
                }
            }
            Log.e(TAG, "run: 重置" );
            byteBuffer.compact();
        }
        if (null != fileOutputStream) {
            try {
                fileOutputStream.close();
                fileOutputStream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(file.length() <= 0) {
            if(file.delete()) {
                Log.e(TAG, "没用音频清理完成" );
            }
        }
        file = null;
        byteBuffer = null;
    }


    public void playAudio(byte sampleRateType,byte bitRateType) {
        if(playThread != null || file != null) {
            return ;
        }
        if(!createFile()) {
            return;
        }
        Log.e(TAG, "playAudio: " + sampleRateType + " " +bitRateType );
        byteBuffer = ByteBuffer.allocate(1024 * 16);
        initDecoder(sampleRateType,bitRateType);
        playThread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] msg = {SocketServer.CLIENT_ID,START_AUDIO,bitRateType,sampleRateType,SocketServer.EOF};
                BlockingDeque<byte[]> dataQueue = SocketServer.getReadBlockingDeque();
                if(SocketServer.sendDataFlush(msg)) {
                    playState = true;
                    firstFps = true;
                    decodeData(dataQueue);
                } else {
                    stopDecoder();
                }
                SocketServer.clearReadBlockingDeque();
//                try {
//                    SocketServer.sendData(msg);
//
//                } catch (IOException e) {
//                    MyService.socketServer.connect();
//                    e.printStackTrace();
//                }
//


//                byte[] head = new byte[7];
//                int syncword;
//                int aac_frame_length;
//                byte[] data;
//                int len;
//                while (playState) {
//                    if((data = dataQueue.poll()) == null) {
//                        continue;
//                    }
//                    len = data.length;
////                    Log.e(TAG, "run: " + Arrays.toString(data) );
//                    if(!firstFps) {
//                        byteBuffer.put(data, 0, len);
//                    } else if(len == 2) {
//                        putDecodeData(data,0,len);
//                        firstFps = false;
//                        continue;
//                    } else if (len > 2) {
//                        putDecodeData(data,0,2);
//                        firstFps = false;
//                        len -= 2;
//                        byteBuffer.put(data,2,len);
//                    }
//                    try {
//                        fileOutputStream.write(data,data.length - len,len);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    byteBuffer.limit(byteBuffer.position());
//                    byteBuffer.position(0);
////                    byteBuffer.flip();
//                    while(byteBuffer.limit() - byteBuffer.position() >= 7) {
//                        byteBuffer.get(head);
//                        syncword = ((head[0] << 4) & 0XFFF) | ((head[1] >> 4) & 0xF);
//                        aac_frame_length = ((head[3] << 11) & 0X1FFF) | ((head[4] << 3) & 0X7FF) | ((head[5] >> 5) & 0X7);
//                        byteBuffer.position(byteBuffer.position() - 7);
//                        if (syncword == 0XFFF) {
//                            if ((byteBuffer.position() + aac_frame_length) <= byteBuffer.limit()) {
//                                putDecodeData(byteBuffer.array(),byteBuffer.position(),aac_frame_length);
//                                byteBuffer.position(byteBuffer.position() + aac_frame_length);
//                            } else {
//                                break;
//                            }
//                        } else {
//                            byteBuffer.position(byteBuffer.position() + 1);
//                        }
//                    }
//                    if(byteBuffer.limit() - byteBuffer.position() > 0) {
//                        byte[] endData = new byte[byteBuffer.limit() - byteBuffer.position()];
//                        byteBuffer.get(endData);
//                        byteBuffer.clear();
//                        byteBuffer.put(endData);
//                    } else {
//                        byteBuffer.clear();
//                    }
//
//                }
//                SocketServer.clearReadBlockingDeque();
//                if (null != fileOutputStream) {
//                    try {
//                        fileOutputStream.close();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//                if(file.length() <= 0) {
//                    if(file.delete()) {
//                        Log.e(TAG, "没用音频清理完成" );
//                    }
//                }
//                file = null;
//                byteBuffer = null;
            }
        });
        playThread.start();
    }



    public void stopPlay() {
        if(stopPlayThread != null) {
            return ;
        }
        stopPlayThread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] msg = {SocketServer.CLIENT_ID,STOP_AUDIO,SocketServer.EOF};
                SocketServer.sendDataFlush(msg);
                stopDecoder();
//                try {
//                    SocketServer.sendData(msg);
//                } catch (IOException e) {
//                    e.printStackTrace();
////                    MyService.socketServer.connect();
//                }
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
//                stopDecoder();
                stopPlayThread = null;
            }
        });
        stopPlayThread.start();
    }

    private void stopDecoder() {
        try {
            playState = false;
            bufferInfo = null;
            readDecodeDataState = false;
            if(readDecodeDataThread != null) {
                readDecodeDataThread = null;
            }
            if(playThread != null) {
                playThread = null;
            }
            if (audioTrack != null) {
                audioTrack.stop();
                audioTrack.release();
                audioTrack = null;
            }
            if (mediaDecoder != null) {
                mediaDecoder.stop();
                mediaDecoder.release();
                mediaDecoder = null;
//                Log.e(TAG, "stopDecoder: 释放解码器");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
