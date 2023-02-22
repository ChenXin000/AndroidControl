package com.example.server;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.BlockingDeque;

public class CameraActivity extends AppCompatActivity {

    private final static  String TAG = "CameraActivity";
    public final static int[] widthList = {320,640,960,1280,1600};
    public final static int[] heightList = {240,480,720,960,1200};
    public final static int[] bitRateList = {400000,800000,1500000,2000000,2500000,4000000,6000000,8000000,16000000};
    private final static String[] cameraList = {"0","1","2","3","4","5","6","7"};

    private static final int OPEN_CAMERA = 1;
    private static final int TAKE_PICTURE = 2;
    private static final int CLOSE_CAMERA = 3;

    private static String VIDEO_PATH;

    private byte widthAndHeight;
    private byte bitRateType;
    private byte cameraId;

//    private boolean cameraState;
    private boolean mIsFirstFrame;
    private boolean readDecodeDataState;
    private boolean playState;
    private boolean mirrorState;
    private FileOutputStream fileOutputStream;
    private MediaCodec mediaDecoder;
    private MediaCodec.BufferInfo bufferInfo;
    private Thread readDecodeDataThread;
    private Thread playThread;
    private File file;
    private ByteBuffer byteBuffer;
    private Surface surface;
    private TextureView textureView;
    private int textureViewWidth;
    private int textureViewHeight;
    private int rotation;

    private View camera_setting;

//    private Handler handler;

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


    private void configureTransform() {
        textureView.setRotationY(0f);
        if(cameraId == 0) {
            rotation = 90;
        } else if(cameraId == 1) {
            rotation = 270;
            textureView.setRotationY(180.0f);
            mirrorState = true;
        }
        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getSize(displaySize);
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, textureViewWidth, textureViewHeight);
        RectF bufferRect = new RectF(0, 0, widthList[widthAndHeight], heightList[widthAndHeight]);
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
        matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
        float scale = Math.min(
                (float) textureViewWidth / heightList[widthAndHeight],
                (float) textureViewHeight / widthList[widthAndHeight]);
//        float scale = Math.min(
//                (float)  textureViewWidth / widthList[widthAndHeight] ,
//                (float)  textureViewHeight / heightList[widthAndHeight]);
        Log.e(TAG, "configureTransform: " + scale );
        matrix.postScale(scale,scale , centerX, centerY);
        Log.e(TAG, "configureTransform: "+ displaySize.x + " " + displaySize.y );
//        matrix.preScale(scale,scale);
        matrix.postRotate(rotation, centerX, centerY);
        textureView.setTransform(matrix);
//        textureView.setScaleX();
//        textureView.setRotation(90);
    }

    private AcvDecoder acvDecoder;
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        acvDecoder =new AcvDecoder();

        widthAndHeight = 0;
        bitRateType = 0;
        cameraId = 0;
//        cameraState = false;
        mirrorState = false;
        rotation = 0;

        VIDEO_PATH = MainActivity.MAIN_PATH + MainActivity.MODEL + "/Video/";

        TextView title = findViewById(R.id.title);
        title.setText("相 机");

//        handler = new Handler(new Handler.Callback() {
//            @Override
//            public boolean handleMessage(@NonNull Message message) {
////                configureTransform();
//                return false;
//            }
//        });

        byteBuffer = ByteBuffer.allocateDirect(1024 * 512);
        textureView = findViewById(R.id.textureView);
//        textureView.setBackgroundColor(0XFF000000);
//        textureView.setRotation(270.0f);
//        textureView.setRotationY(180.0f);

        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
                textureViewWidth = i;
                textureViewHeight = i1;
                configureTransform();

//                surfaceTexture.setDefaultBufferSize(720,960);
//                handler.sendEmptyMessage(1);
//                configureTransform(i,i1);
//                configureTransform();
                Log.e(TAG, "onSurfaceTextureAvailable: "+ i+ " " + i1 );
                surface = new Surface(surfaceTexture);

            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
                textureViewWidth = i;
                textureViewHeight = i1;
                Log.e(TAG, "onSurfaceTextureSizeChanged: " + i + " " + i1 );
//                handler.sendEmptyMessage(1);
//                configureTransform(i,i1);
//                configureTransform();
            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {
//                configureTransform();

//                Log.e(TAG, "onSurfaceTextureUpdated: " );
            }
        });



        Button startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(playState) {
                    closeCamera();
                    startButton.setBackgroundColor(0xFF00A8E1);
                    startButton.setText("开 始");

                } else {
                    configureTransform();
                    openCamera();
                    startButton.setBackgroundColor(0xffee0000);
                    startButton.setText("停 止");
                }
            }
        });


        final LayoutInflater inflater = LayoutInflater.from(CameraActivity.this);
        camera_setting = inflater.inflate(R.layout.camera_setting, null);
        final AlertDialog dialog_main = new AlertDialog.Builder(CameraActivity.this).setCancelable(false).setView(camera_setting).create();
        Button okButton = camera_setting.findViewById(R.id.okButton);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog_main.dismiss();
            }
        });


        Button stopButton = findViewById(R.id.settingButton);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog_main.show();
            }
        });

        Button rotationButton = findViewById(R.id.rotation_button);
        rotationButton.setBackgroundColor(0X00000000);
        rotationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(playState) {
                    rotation += 90;
                    if (rotation > 270) {
                        rotation = 0;
                    }
                    textureView.setRotation(rotation);
                }
            }
        });
        Button mirrorButton = findViewById(R.id.mirror_button);
        mirrorButton.setBackgroundColor(0X00000000);
        mirrorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(playState) {
                    if (mirrorState) {
                        textureView.setRotationY(0.0f);
                        mirrorState = false;
                    } else {
                        textureView.setRotationY(180.0f);
                        mirrorState = true;
                    }
                }
            }
        });

        Button tackPictureButton = findViewById(R.id.takePicture_button);
        tackPictureButton.setBackgroundColor(0X00000000);
        tackPictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(playState) {
                    takePicture();
                }
            }
        });

//        configureTransform();

//        TextView widthHeight = findViewById(R.id.widthHeight);
//        widthHeight.setText("分辨率: " + widthAndHeight);
//        TextView bitReadText = findViewById(R.id.bitRateText);
//        bitReadText.setText("bit率: " + bitRateType);
//        TextView cameraIdText = findViewById(R.id.cameraIdText);
//        cameraIdText.setText("相机ID: " + cameraId);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        byteBuffer = null;
        closeCamera();
        acvDecoder = null;
        Log.e(TAG, "onDestroy: 销毁" );
    }

    @SuppressLint({"NonConstantResourceId", "SetTextI18n"})
    public void onRadioButtonClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();
        switch (view.getId()) {
            case R.id.c_radioButton1:
                if (checked)
                    widthAndHeight = 0;
                break;
            case R.id.c_radioButton2:
                if (checked)
                    widthAndHeight = 1;
                break;
            case R.id.c_radioButton3:
                if (checked)
                    widthAndHeight = 2;
                break;
            case R.id.c_radioButton19:
                if (checked)
                    widthAndHeight = 3;
                break;
            case R.id.c_radioButton4:
                if (checked)
                    widthAndHeight = 4;
                break;
        }
        TextView widthHeight = camera_setting.findViewById(R.id.widthHeight);
        widthHeight.setText("分辨率: " + ((RadioButton)view).getText());
    }

    @SuppressLint({"NonConstantResourceId", "SetTextI18n"})
    public void onRadioButtonClicked2(View view) {
        boolean checked = ((RadioButton) view).isChecked();
        switch (view.getId()) {
            case R.id.c_radioButton5:
                if (checked)
                    bitRateType = 0;
                break;
            case R.id.c_radioButton6:
                if (checked)
                    bitRateType = 1;
                break;
            case R.id.c_radioButton7:
                if (checked)
                    bitRateType = 2;
                break;
            case R.id.c_radioButton8:
                if (checked)
                    bitRateType = 3;
                break;
            case R.id.c_radioButton9:
                if (checked)
                    bitRateType = 4;
                break;
            case R.id.c_radioButton10:
                if (checked)
                    bitRateType = 5;
                break;
            case R.id.c_radioButton17:
                if (checked)
                    bitRateType = 6;
                break;
            case R.id.c_radioButton18:
                if (checked)
                    bitRateType = 7;
                break;
            case R.id.c_radioButton20:
                if (checked)
                    bitRateType = 8;
                break;
        }
        TextView bitReadText = camera_setting.findViewById(R.id.bitRateText);
        bitReadText.setText("Bit率: " + ((RadioButton)view).getText());
    }

    @SuppressLint({"NonConstantResourceId", "SetTextI18n"})
    public void onRadioButtonClicked3(View view) {
        boolean checked = ((RadioButton) view).isChecked();
        switch (view.getId()) {
            case R.id.c_radioButton11:
                if (checked)
                    cameraId = 0;
                break;
            case R.id.c_radioButton12:
                if (checked)
                    cameraId = 1;
                break;
            case R.id.c_radioButton13:
                if (checked)
                    cameraId = 2;
                break;
            case R.id.c_radioButton14:
                if (checked)
                    cameraId = 3;
                break;
            case R.id.c_radioButton15:
                if (checked)
                    cameraId = 4;
                break;
            case R.id.c_radioButton16:
                if (checked)
                    cameraId = 5;
                break;
            case R.id.c_radioButton21:
                if (checked)
                    cameraId = 6;
                break;
            case R.id.c_radioButton22:
                if (checked)
                    cameraId = 7;
                break;
        }
        TextView cameraIdText = camera_setting.findViewById(R.id.cameraIdText);
        cameraIdText.setText("相机ID: " + ((RadioButton)view).getText());
    }

    private void openCamera() {
        if(playState) {
            return ;
        }
//        if(cameraId == 0) {
//            rotation = 90;
//        } else if(cameraId == 1) {
//            rotation = 270;
//            textureView.setRotationY(180.0f);
//            mirrorState = true;
//        }
//        playState = false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] pakg = {SocketServer.CLIENT_ID,OPEN_CAMERA,widthAndHeight,cameraId,bitRateType,SocketServer.EOF};
                if(SocketServer.sendDataFlush(pakg)) {
                    playVideo();
                }

            }
        }).start();
    }

    private void closeCamera() {
        if(playState) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] pakg = {SocketServer.CLIENT_ID,CLOSE_CAMERA,SocketServer.EOF};
                    SocketServer.sendDataFlush(pakg);
                    playState = false;
                    acvDecoder.closeDecode();
                }
            }).start();
        }
    }

    private boolean takePictureState;
    private void takePicture() {
        if(playState && !takePictureState) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] pakg = {SocketServer.CLIENT_ID,TAKE_PICTURE,SocketServer.EOF};
                    try {
                        takePictureState = true;
                        SocketServer.sendData(pakg);
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    takePictureState = false;

                }
            }).start();
        }
    }

    private boolean createFile() {
        String time = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss", Locale.CHINESE).format(new Date(System.currentTimeMillis()));
        file = new File(VIDEO_PATH);
        if(!file.exists()) {
            if(!file.mkdir()) {
                return false;
            }
        }
        file = new File(VIDEO_PATH, time + ".h264");
        try {
            fileOutputStream = new FileOutputStream(file);
            Log.e(TAG, "playAudio: 文件打开"  );
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return true;
    }

//
//    private void decodeData(BlockingDeque<byte[]> dataQueue) {
//        byteBuffer.clear();
//        byte[] head = new byte[4],data,buff;
//        int length,len;
//        while(playState) {
//            if((data= dataQueue.poll()) == null) {
//                continue;
//            }
//            len = (data[0] << 24) | ((data[1] << 16) & 0XFFFFFF) | ((data[2] << 8) & 0XFFFF) | (data[3] & 0XFF);
//            byteBuffer.put(data, 4, len);
//            byteBuffer.flip();
//            while (byteBuffer.limit() - byteBuffer.position() >= 4) {
//                byteBuffer.get(head);
//                if ((head[0] & 0XFF) == 0XFF) {
//                    length = (((head[1] << 16) & 0XFFFFFF) | ((head[2] << 8) & 0XFFFF) | (head[3] & 0XFF)) - 4;
//                    if (byteBuffer.limit() - byteBuffer.position() >= length) {
//                        buff = byteBuffer.array();
//                        putDecodeData(buff, byteBuffer.position(), length);
//                        try {
//                            fileOutputStream.write(buff,byteBuffer.position(),length);
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                            closeCamera();
//                            break;
//                        }
//                        byteBuffer.position(byteBuffer.position() + length);
//                    } else {
//                        byteBuffer.position(byteBuffer.position() - 4);
//                        Log.e(TAG, "run: 长度不够" );
//                        break;
//                    }
//                } else {
//                    byteBuffer.position(byteBuffer.position() - 3);
//                    Log.e(TAG, "run: 没有找到" );
//                }
//            }
//            Log.e(TAG, "run: 重置" );
//            byteBuffer.compact();
//        }
//        try {
////                    fileOutputStream.flush();
//            fileOutputStream.close();
//            fileOutputStream = null;
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        if(file.length() <= 0) {
//            if(file.delete()) {
//                Log.e(TAG, "没用视频清理完成" );
//            }
//        }
//    }

    private void playVideo() {
        try {
            if(playThread != null) {
                return ;
            }
            if(!createFile()) {
                return;
            }
            playState = true;
            acvDecoder.initDecoder(surface,widthList[widthAndHeight],heightList[widthAndHeight],bitRateList[bitRateType],fileOutputStream);
//        handler.sendEmptyMessage(1);
//        configureTransform();
            playThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    BlockingDeque<byte[]> dataQueue = SocketServer.getReadBlockingDeque();
                    try {
                        acvDecoder.decodeData(dataQueue);
                    } catch (Exception e) {
                        e.printStackTrace();
                        closeCamera();
                    }
                    playThread = null;
//                playState = false;
                    if(file.length() <= 0) {
                        if(file.delete()) {
                            Log.e(TAG, "没用视频清理完成" );
                        }
                    }
                    playState = false;
                }
            });
            playThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    public void initDecoder() {
//        try {
//            if(mediaDecoder != null) {
//                return ;
//            }
//            mIsFirstFrame = true;
//            if (selectCodec(MediaFormat.MIMETYPE_VIDEO_AVC) == null) {
//                return;
//            }
//            MediaFormat decodeFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, widthList[widthAndHeight], heightList[widthAndHeight]);
////            decodeFormat.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC);
////            decodeFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, ADTSUtils[sampleRateType]);
//            decodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRateList[bitRateType]);
//            decodeFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 60); // 最大帧率
//            decodeFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);
////            decodeFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
////
////            decodeFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
////            decodeFormat.setInteger(MediaFormat.KEY_IS_ADTS, 1);
//            decodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 256 * 1024);
//
//            mediaDecoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
//
//            mediaDecoder.configure(decodeFormat, surface, null, 0);
//            mediaDecoder.start();
//            bufferInfo = new MediaCodec.BufferInfo();
//            readDecodeData();
//        } catch (IOException e) {
//            Log.e(TAG, "解码器初始化错误 ");
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * 填充数据开始解码
//     * @param data
//     */
//    public void putDecodeData(byte[] data,int offset,int len) {
//        if(mediaDecoder == null) {
//            return ;
//        }
//        try {
//            int inputBufferIndex = mediaDecoder.dequeueInputBuffer(0);
//            if (inputBufferIndex >= 0) {
//                ByteBuffer inputBuffer = mediaDecoder.getInputBuffer(inputBufferIndex);
//                inputBuffer.clear();
//                inputBuffer.put(data,offset,len);
////                Log.e(TAG, "putDecodeData: " + Arrays.toString(data) );
//                if (mIsFirstFrame) {
//                    /**
//                     * Some formats, notably AAC audio and MPEG4, H.264 and H.265 video formats
//                     * require the actual data to be prefixed by a number of buffers containing
//                     * setup data, or codec specific data. When processing such compressed formats,
//                     * this data must be submitted to the codec after start() and before any frame data.
//                     * Such data must be marked using the flag BUFFER_FLAG_CODEC_CONFIG in a call to queueInputBuffer.
//                     */
//                    mediaDecoder.queueInputBuffer(inputBufferIndex, 0, len, System.currentTimeMillis() / 1000, MediaCodec.BUFFER_FLAG_CODEC_CONFIG);
//                    mIsFirstFrame = false;
//                } else {
//                    mediaDecoder.queueInputBuffer(inputBufferIndex, 0, len, System.currentTimeMillis() / 1000,0);
//                }
//            }
//
//        } catch (Throwable t) {
//            Log.e(TAG, "offerDecoder: 编码错误" );
//            closeCamera();
//            t.printStackTrace();
//        }
//    }

//    /**
//     * 读取解码器数据
//     */
//    private void readDecodeData() {
//        readDecodeDataState = true;
//        readDecodeDataThread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while(readDecodeDataState) {
//                    if(mediaDecoder == null) {
//                        break;
//                    }
//                    try {
//                        int outputBufferIndex = mediaDecoder.dequeueOutputBuffer(bufferInfo, 10000);
//                        if(outputBufferIndex >= 0) {
////                            ByteBuffer outputBuffer = mediaDecoder.getOutputBuffer(outputBufferIndex);
////                            audioTrack.write(outputBuffer,bufferInfo.size, AudioTrack.WRITE_BLOCKING);
////                            outputBuffer.clear();
////                            Log.e(TAG, "run: 解码" );
//                            mediaDecoder.releaseOutputBuffer(outputBufferIndex, true);
//                        }
//
//                    } catch (Throwable t) {
//                        closeCamera();
//                        readDecodeDataState = false;
//                        t.printStackTrace();
//                        break;
//                    }
//                }
//            }
//        });
//        readDecodeDataThread.start();
//    }
//
//    private void closeDecode() {
//        readDecodeDataThread = null;
//        if (mediaDecoder != null) {
//            mediaDecoder.stop();
//            mediaDecoder.release();
//            mediaDecoder = null;
//        }
//    }
}