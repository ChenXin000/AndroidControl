package com.example.server;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.BlockingDeque;

public class ScreenRecorder extends AppCompatActivity {

    private static final String TAG = "ScreenRecorder";
    private static final int START_SCREEN_RECORDER = 19;
    private static final int STOP_SCREEN_RECORDER = 20;

    public final static int[] widthList = {320,640,960,1280,1920};
    public final static int[] heightList = {240,480,720,960,1080};

    private boolean recordeState;
    private View recorder_setting;
    private TextureView textureView;
    private FrameLayout frameLayout;
    private Surface surface;
    private byte widthAndHeight = 3;
    private byte bitRateType = 0;
    private byte controlType;

    private int textureViewWidth;
    private int textureViewHeight;

    private AcvDecoder acvDecoder;

    private void configureTransform() {
//        textureView.setRotationY(0f);

        int frameLayoutWidth = frameLayout.getWidth();
        int frameLayoutHeight = frameLayout.getHeight();

        float width = 1080f / widthAndHeight;
        float height = 1920f / widthAndHeight;

        Log.e(TAG, "configureTransform: " + width + " " +height );
        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getSize(displaySize);
        Matrix matrix = new Matrix();
        Log.e(TAG, "configureTransform: " + matrix);

        RectF viewRect = new RectF(0, 0, frameLayoutWidth, frameLayoutHeight);
        RectF bufferRect = new RectF(0, 0,width ,height);
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
        matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
        Log.e(TAG, "configureTransform: " + matrix);


//        float scale = Math.min(
//                (float) textureViewWidth / 1080,
//                (float) textureViewHeight / 1920);
        float scale = Math.min(
                (float)  frameLayoutWidth / width ,
                (float)  frameLayoutHeight / height);
        Log.e(TAG, "configureTransform: " + scale );
        matrix.postScale(scale,scale, centerX, centerY);
        Log.e(TAG, "configureTransform: " + centerX + " " + centerY);
        Log.e(TAG, "configureTransform: "+ width * scale + " " + height * scale );

        Log.e(TAG, "configureTransform: " + matrix);
        textureView.setTransform(matrix);

//        matrix.preScale(scale,scale);
//        matrix.postRotate(rotation, centerX, centerY);
//        textureView.setTransform(,);
//        textureView.setScaleX(scale);
//        textureView.setScaleX(textureViewWidth / heightList[widthAndHeight]);
//        textureView.setScaleX();
//        textureView.setRotation(90);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_recorder);
        acvDecoder = new AcvDecoder();

        textureView = findViewById(R.id.s_textureView);
        frameLayout = findViewById(R.id.frameLayout);


        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
                surface = new Surface(surfaceTexture);
                textureViewWidth = i;
                textureViewHeight = i1;



                configureTransform();
                Log.e(TAG, "onCreate: " + frameLayout.getWidth() + " " + frameLayout.getHeight() );
                Log.e(TAG, "onCreate: " + textureViewWidth + " " + textureViewHeight );

            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
                textureViewWidth = i;
                textureViewHeight = i1;


            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {

            }
        });

        textureView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    Log.e(TAG, "onTouch: " + motionEvent.getX() + " " + motionEvent.getY() );
                }
                return false;
            }
        });

        final LayoutInflater inflater = LayoutInflater.from(ScreenRecorder.this);
        recorder_setting = inflater.inflate(R.layout.recorder_setting, null);
        final AlertDialog dialog_main = new AlertDialog.Builder(ScreenRecorder.this).setCancelable(false).setView(recorder_setting).create();
        Button ok_button = recorder_setting.findViewById(R.id.ok_button);
        ok_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog_main.dismiss();
            }
        });

        Button setting_button = findViewById(R.id.setting_button);
        setting_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog_main.show();
            }
        });

        Button start_button = findViewById(R.id.start_button);
        start_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(recordeState) {
                    start_button.setBackgroundColor(0xFF00A8E1);
                    start_button.setText("开 始");
                    stopRecorde();
                } else {
                    start_button.setBackgroundColor(0xffee0000);
                    start_button.setText("停 止");
                    startRecorde();
                    configureTransform();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        byteBuffer = null;
        acvDecoder = null;
        Log.e(TAG, "onDestroy: 销毁" );
    }

    private File file;
    private FileOutputStream fileOutputStream;
    private String RECORDE_PATH = MainActivity.MAIN_PATH + MainActivity.MODEL + "/Recorde/";
    private boolean createFile() {
        String time = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss", Locale.CHINESE).format(new Date(System.currentTimeMillis()));
        file = new File(RECORDE_PATH);
        if(!file.exists()) {
            if(!file.mkdir()) {
                return false;
            }
        }
        file = new File(RECORDE_PATH, time + ".h264");
        try {
            fileOutputStream = new FileOutputStream(file);
            Log.e(TAG, "playAudio: 文件打开"  );
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void startRecorde() {
        if(recordeState) {
            return;
        }
        recordeState = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] pakg = new byte[]{SocketServer.CLIENT_ID,START_SCREEN_RECORDER,widthAndHeight,bitRateType,controlType,SocketServer.EOF};
                if(SocketServer.sendDataFlush(pakg)) {
                    try {
                        if(createFile()) {
                            acvDecoder.initDecoder(surface, CameraActivity.widthList[widthAndHeight], CameraActivity.heightList[widthAndHeight], CameraActivity.bitRateList[bitRateType], fileOutputStream);
                            BlockingDeque<byte[]> dataQueue = SocketServer.getReadBlockingDeque();
                            acvDecoder.decodeData(dataQueue);
                        }
//                playState = false;

                    } catch (Exception e) {
                        e.printStackTrace();
                        stopRecorde();
                    }
                    if(file.length() <= 0) {
                        if(file.delete()) {
                            Log.e(TAG, "没用视频清理完成" );
                        }
                    }
                }
            }
        }).start();
    }

    public void stopRecorde() {
        if(recordeState) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] pakg = new byte[]{SocketServer.CLIENT_ID, STOP_SCREEN_RECORDER, SocketServer.EOF};
                    SocketServer.sendDataFlush(pakg);
                    acvDecoder.closeDecode();
                    recordeState = false;
                }
            }).start();
        }
    }

    @SuppressLint({"NonConstantResourceId", "SetTextI18n"})
    public void fOnRadioButtonClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();
        switch (view.getId()) {
            case R.id.r_radioButton1:
                if (checked)
                    widthAndHeight = 3;
                break;
            case R.id.r_radioButton2:
                if (checked)
                    widthAndHeight = 2;
                break;
            case R.id.r_radioButton3:
                if (checked)
                    widthAndHeight = 1;
                break;
        }
        TextView widthHeight = recorder_setting.findViewById(R.id.f_text);
        widthHeight.setText("画质: " + ((RadioButton)view).getText());
    }

    @SuppressLint({"NonConstantResourceId", "SetTextI18n"})
    public void bOnRadioButtonClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();
        switch (view.getId()) {
            case R.id.r_radioButton4:
                if (checked)
                    bitRateType = 0;
                break;
            case R.id.r_radioButton5:
                if (checked)
                    bitRateType = 1;
                break;
            case R.id.r_radioButton6:
                if (checked)
                    bitRateType = 2;
                break;
            case R.id.r_radioButton7:
                if (checked)
                    bitRateType = 3;
                break;
            case R.id.r_radioButton8:
                if (checked)
                    bitRateType = 4;
                break;
            case R.id.r_radioButton9:
                if (checked)
                    bitRateType = 5;
                break;
            case R.id.r_radioButton10:
                if (checked)
                    bitRateType = 6;
                break;
            case R.id.r_radioButton11:
                if (checked)
                    bitRateType = 7;
                break;
            case R.id.r_radioButton12:
                if (checked)
                    bitRateType = 8;
                break;
        }
        TextView bitReadText = recorder_setting.findViewById(R.id.b_text);
        bitReadText.setText("Bit率: " + ((RadioButton)view).getText());
    }

    @SuppressLint({"NonConstantResourceId", "SetTextI18n"})
    public void yOnRadioButtonClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();
        switch (view.getId()) {
            case R.id.r_radioButton13:
                if (checked)
                    controlType = 0;
                break;
            case R.id.r_radioButton14:
                if (checked)
                    controlType = 1;
                break;
        }
        TextView widthHeight = recorder_setting.findViewById(R.id.y_text);
        widthHeight.setText(((RadioButton)view).getText());
    }
}