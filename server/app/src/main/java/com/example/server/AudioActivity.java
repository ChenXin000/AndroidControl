package com.example.server;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingDeque;

public class AudioActivity extends AppCompatActivity {

    private final static String TAG = "AudioActivity";
    private final static int[] ADTSUtils = {96000,88200,64000,48000,44100,32000,24000,22050,16000,12000,11025,8000,7350};
    private final static int[] bitRateList = {32000,96000,128000,160000,192000,256000,320000};
    private int sampleRateType = 4;
    private int bitRateType = 2;
    private MediaCodec mediaDecoder;
    private AudioTrack audioTrack;
    private boolean playState = false;
    private boolean firstFps;

    private Thread readDecodeDataThread;
    private Thread playThread;

    private boolean readDecodeDataState;
    MediaCodec.BufferInfo bufferInfo;
    private BlockingDeque<byte[]> dataQueue;
    private ByteBuffer byteBuffer;

    private final static int START_OK = 1;
    private final static int START_ERROR = 0;

    private byte BITRATE;
    private byte SAMPLERATE;
    private static byte START_AUDIO = 7;
    private static byte STOP_AUDIO = 8;

    private boolean mIsFirstFrame = true;

    private AudioDecode audioDecode;
//    private AudioDecode audioDecode;

//    public static void sendMsg(int msg) {
//        if(handler != null) {
//            Message message = new Message();
//            message.what = msg;
//            handler.sendMessage(message);
//        }
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);
        BITRATE = 2;
        SAMPLERATE = 4;

        TextView title = findViewById(R.id.title);
        title.setText("音 频");
//        byteBuffer = ByteBuffer.allocate(1024 * 20);
        Log.e(TAG, "创建 " );

//        readData();
        audioDecode = new AudioDecode();

//        handler = new Handler(Looper.getMainLooper()) {
//            @Override
//            public void handleMessage(@NonNull Message message) {
//                switch (message.what) {
//                    case START_OK:
//                        playButton.setText("暂 停");
//                        break;
//                    case START_ERROR:
//                        playButton.setText("开 始");
//                        break;
//
//                }
//            }
//        };


        //    public static Handler handler;
        Button playButton = findViewById(R.id.playButton);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(audioDecode != null) {
                    audioDecode.playAudio(SAMPLERATE, BITRATE);
                }
            }
        });

        Button stopButton = findViewById(R.id.settingButton);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(audioDecode != null) {
                    audioDecode.stopPlay();
                }
            }
        });
//         RadioButton radioButton = findViewById(R.id.radioButton12);
//         radioButton.setClickable(true);
//        RadioButton radioButton2 = findViewById(R.id.radioButton3);
//        radioButton2.setClickable(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.e(TAG, "onStop: 开启" );
    }

    @Override
    protected void onStop() {
        super.onStop();
//        stopPlay();
//        stopDecoder();
        Log.e(TAG, "onStop: 关闭" );
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
//        byteBuffer = null;
        if(audioDecode != null) {
            audioDecode.stopPlay();
            audioDecode = null;
        }
        Log.e(TAG, "onDestroy: 销毁" );
    }


    @SuppressLint("NonConstantResourceId")
    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();
        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radioButton:
                if (checked)
                    BITRATE = 0;
                    break;
            case R.id.radioButton2:
                if (checked)
                    BITRATE = 1;
                break;
            case R.id.radioButton3:
                if (checked)
                    BITRATE= 2;
                break;
            case R.id.radioButton4:
                if (checked)
                    BITRATE = 3;
                break;
            case R.id.radioButton5:
                if (checked)
                    BITRATE = 4;
                break;
            case R.id.radioButton6:
                if (checked)
                    BITRATE = 5;
                break;
            case R.id.radioButton7:
                if (checked)
                    BITRATE = 6;
                break;
        }
        bitRateType = BITRATE;
        TextView textView = findViewById(R.id.bitText);
        textView.setText(((RadioButton) view).getText());
//        Log.e(TAG, "onRadioButtonClicked: " + BITRATE[0] );
    }

    @SuppressLint("NonConstantResourceId")
    public void onRadioButtonClicked2(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();
        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radioButton8:
                if (checked)
                    SAMPLERATE = 0;
                    break;
            case R.id.radioButton9:
                if (checked)
                    SAMPLERATE = 1;
                break;
            case R.id.radioButton10:
                if (checked)
                    SAMPLERATE = 2;
                break;
            case R.id.radioButton11:
                if (checked)
                    SAMPLERATE = 3;
                break;
            case R.id.radioButton12:
                if (checked)
                    SAMPLERATE = 4;
                break;
            case R.id.radioButton13:
                if (checked)
                    SAMPLERATE = 5;
                break;
            case R.id.radioButton14:
                if (checked)
                    SAMPLERATE = 6;
                break;
            case R.id.radioButton15:
                if (checked)
                    SAMPLERATE = 7;
                break;
            case R.id.radioButton16:
                if (checked)
                    SAMPLERATE = 8;
                break;
            case R.id.radioButton17:
                if (checked)
                    SAMPLERATE = 9;
                break;
            case R.id.radioButton18:
                if (checked)
                    SAMPLERATE = 10;
                break;
            case R.id.radioButton19:
                if (checked)
                    SAMPLERATE = 11;
                break;
            case R.id.radioButton20:
                if (checked)
                    SAMPLERATE = 12;
                break;
        }
        sampleRateType = SAMPLERATE;
        TextView textView = findViewById(R.id.sampleText);
        textView.setText(((RadioButton) view).getText());
//        Log.e(TAG, "onRadioButtonClicked2: " + SAMPLERATE[0] );
    }



}