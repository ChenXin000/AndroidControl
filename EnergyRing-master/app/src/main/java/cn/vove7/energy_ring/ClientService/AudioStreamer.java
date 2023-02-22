package cn.vove7.energy_ring.ClientService;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AudioStreamer {

    private static final String TAG = "AudioStreamer";
    private boolean AudioState = false;
    private Thread AudioThread;
    private Thread radEncodeDataThread;
    private int minBufferSize;
    private int sampleRateType;
    private AudioRecord audioRec;
    private byte[] buffer;

    private MediaCodec mediaEncode;
    private MediaCodec.BufferInfo bufferInfo;
    private Context context;

    private boolean readEncodeDataState = true;

    private static final int[] ADTSUtils = {96000,88200,64000,48000,44100,32000,24000,22050,16000,12000,11025,8000,7350};
    private static final int[] bitRateList = {32000,96000,128000,160000,192000,256000,320000};
    public AudioStreamer(Context context) {
        this.context = context;
    }

    /**
     * 初始化音频
     */
    private void initAudio(int sampleRate) {
        int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT);
        buffer = new byte[minBufferSize];//-------缓存数据-------
        Log.e(TAG, "AudioStreamer: " + buffer.length );
        audioRec = new AudioRecord(
                MediaRecorder.AudioSource.MIC,//-------设定录音来源为主麦克风。
                sampleRate,//-------采样频率-------
                channelConfig,//-------录音用输入单声道-------
                AudioFormat.ENCODING_PCM_16BIT,//-------设置音频数据块是8位还是16位。这里设置为16位。
                minBufferSize);//-------设置采集音频文件最小的buffer大小-------
        if (AcousticEchoCanceler.isAvailable()) {
            AcousticEchoCanceler aec = AcousticEchoCanceler.create(audioRec.getAudioSessionId());
            if (aec != null) {
                aec.setEnabled(true);
            }
        }
        //-------自动增益控制 AutomaticGainControl 自动恢复正常捕获的信号输出-------
        if (AutomaticGainControl.isAvailable()) {
            AutomaticGainControl agc = AutomaticGainControl.create(audioRec.getAudioSessionId());
            if (agc != null) {
                agc.setEnabled(true);
            }
        }

//        -------噪声抑制器 NoiseSuppressor 可以消除被捕获信号的背景噪音-------
        if (NoiseSuppressor.isAvailable()) {
            NoiseSuppressor nc = NoiseSuppressor.create(audioRec.getAudioSessionId());
            if (nc != null) {
                nc.setEnabled(true);
            }
        }
    }

    /**
     * 查找编码类型是否存在
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
     * 初始化编码器
     */
    private void initEncoder(int sampleRate,int bitRate) {
        try {
            // 判断编码器是否存在
            if(selectCodec(MediaFormat.MIMETYPE_AUDIO_AAC) == null) {
                Log.e(TAG, "initEncoder: 没有编码器" );
                return ;
            }
            MediaFormat encodeFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, 1);
            encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate); // 设置比特率
            encodeFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC); // AAC LC 格式
            encodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 100 * 1024); // buffer 大小
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                encodeFormat.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT); // 16位音频源
            }

            mediaEncode = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            mediaEncode.configure(encodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaEncode.start();
            bufferInfo = new MediaCodec.BufferInfo();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 填充数据开始编码
     */
    public void putEncodeData(byte[] data,int len) {
        if(mediaEncode == null) {
            return ;
        }
        try {
            int inputBufferIndex = mediaEncode.dequeueInputBuffer(-1);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = mediaEncode.getInputBuffer(inputBufferIndex);
                inputBuffer.clear();
                inputBuffer.put(data,0,len);
//                Log.e(TAG, "offerEncoder: " + len);
                mediaEncode.queueInputBuffer(inputBufferIndex, 0, len,
                        System.nanoTime(), 0);
            }
//            int outputBufferIndex = mediaEncode.dequeueOutputBuffer(bufferInfo, 10000);
//            while (outputBufferIndex >= 0) {
//                ByteBuffer outputBuffer = mediaEncode.getOutputBuffer(outputBufferIndex);
//                int length = bufferInfo.size;
//                byte[] buf;
//                if(length > 2) {
//                    buf = new byte[length + 7];
//                    addADTStoPacket(buf, length + 7);
//                    outputBuffer.get(buf, 7, length);
//                }
//                else
//                {
//                    buf = new byte[length];
//                    outputBuffer.get(buf, 0, length);
//                }
//                sendTask.pushData(buf);
//                outputBuffer.clear();
//
//                Log.e(TAG, "offerEncoder: " + buf.length);
//                Log.e(TAG, "offerEncoder: " + Arrays.toString(buf));
//
//                mediaEncode.releaseOutputBuffer(outputBufferIndex, false);
//                outputBufferIndex = mediaEncode.dequeueOutputBuffer(bufferInfo, 10000);
//            }
        } catch (Throwable t) {
            AudioStop();
            t.printStackTrace();
        }
    }

    /**
     * 读取编码数据
     */
    public void readEncodeData() {
        readEncodeDataState = true;
        radEncodeDataThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (readEncodeDataState) {
                    try {
                        int outputBufferIndex = mediaEncode.dequeueOutputBuffer(bufferInfo, 10000);
                        if (outputBufferIndex >= 0) {
                            ByteBuffer outputBuffer = mediaEncode.getOutputBuffer(outputBufferIndex);
                            int length = bufferInfo.size;
                            byte[] buf;
                            // 编码器特殊2字节必须保存或者解码失败
                            if(length > 2) {
                                buf = new byte[length + 7];
                                addADTStoPacket(buf, length + 7);
                                outputBuffer.get(buf, 7, length);
                            }
                            else
                            {
                                buf = new byte[length];
                                outputBuffer.get(buf, 0, length);
                            }
                            outputBuffer.clear();
                            mediaEncode.releaseOutputBuffer(outputBufferIndex, false);
                            // 由Socket发送给服务端
                            SocketClient.putSendData(buf);
                        }
                    } catch (Throwable t) {
                        readEncodeDataState = false;
                        AudioStop();
                        t.printStackTrace();
                        break;
                    }
                }
            }
        });
        radEncodeDataThread.start();
    }
    /**
     * 开始录音
     * @param sampleRateType
     * @param bitRateType
     */
    public void AudioStart(int sampleRateType,int bitRateType) {
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "AudioStart: 没有音频权限" );
            return;
        }
        if(AudioState || audioRec != null) {
            return ;
        }
        int sampleRate = ADTSUtils[sampleRateType >= ADTSUtils.length ? 0 : sampleRateType];
        int bitRate = bitRateList[bitRateType >= bitRateList.length ? 0 : bitRateType];

//        if(!SocketClient.startSendDataThread()) {
//            return ;
//        }
//        SocketClient.sendState = true;
        SocketClient.startSend();

        this.sampleRateType = sampleRateType;
        Log.e(TAG, "sampleRate: " + sampleRate );
        Log.e(TAG, "bitRate: " + bitRate );
//        this.bitRateType = bitRateType;
        initAudio(sampleRate);
        initEncoder(sampleRate,bitRate);
//        sendTask = new SendTask(this);
//        sendTask.start();
        audioRec.startRecording();
        AudioState = true;

        readEncodeData();

        AudioThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(AudioState) {
                    int len;
                    if((len = audioRec.read(buffer,0,minBufferSize)) > 0) {
                        putEncodeData(buffer,len);
                    }
//                    Log.e(TAG, "run: " + len);
                }
            }
        });
        AudioThread.start();
    }

    /**
     * 添加ADTS头
     *
     * @param packet
     * @param packetLen
     */
    private void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = MediaCodecInfo.CodecProfileLevel.AACObjectLC; // AAC LC
        int freqIdx = sampleRateType; // 44.1KHz
        int chanCfg = 1; // CPE

        // fill in ADTS data
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }

//    /**
//     * 发送线程
//     */
//    private static class SendTask extends Thread {
////        private File file;
//        private ArrayList<byte[]> dataList;
//        private boolean sendState = true;
//        private AudioStreamer audioStreamer;
////        private FileOutputStream output;
//        public SendTask(AudioStreamer audioStreamer) {
//            this.audioStreamer = audioStreamer;
//            dataList = new ArrayList<byte[]>();
////            file = new File("storage/emulated/0/", "pic.aac");
////            try {
////                output = new FileOutputStream(file);
////            } catch (FileNotFoundException e) {
////                e.printStackTrace();
////            }
//        }
//        public void pushData(byte[] data) {
//            dataList.add(data);
////            Log.e(TAG, "pushData: " + dataList.size() );
//        }
//        @Override
//        public void run() {
//            SocketClient.sendDataState = true;
//            while(sendState) {
//                if(dataList.size() < 1)
//                    continue;
//                try {
////                    if(dataList.get(0) != null) {
//                    if(dataList.get(0) != null) {
//                        SocketClient.sendData(dataList.get(0),0,dataList.get(0).length);
//                        dataList.remove(0);
//                    }
////                        output.write(dataList.get(0));
////                        Log.e(TAG, "send: " + dataList.get(0).length);
////                    dataList.remove(0);
////                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    sendState = false;
//                    audioStreamer.AudioStop();
//                    SocketClient.disconnect();
//                    SocketClient.sendMsg(SocketClient.CONNECT_NETWORK);
////                    ClientService.reconnect();
//                    dataList.clear();
//                    dataList = null;
//                    break;
//                }
//            }
//            SocketClient.sendDataState = false;
////            if (null != output) {
////                try {
////                    output.close();
////                } catch (IOException e) {
////                    e.printStackTrace();
////                }
////            }
//        }
//
//    }

    /**
     * 关闭录音
     */
    public void AudioStop() {
        try {
            AudioState = false;
            readEncodeDataState = false;
            if(AudioThread != null) {
                AudioThread = null;
            }
            if(radEncodeDataThread != null) {
                radEncodeDataThread = null;
            }
            if (audioRec != null) {
                audioRec.stop();
                audioRec.release();
                audioRec = null;
            }
            if (mediaEncode != null) {
                mediaEncode.stop();
                mediaEncode.release();
                mediaEncode = null;
            }
//            SocketClient.sendState = false;
            SocketClient.stopSend();
//            SocketClient.clearSendDataBlockingDeque();
//            if (sendTask != null) {
//                sendTask.sendState = false;
//                sendTask = null;
//            }
            Log.e(TAG, "AudioStop: " + "关闭音频");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
