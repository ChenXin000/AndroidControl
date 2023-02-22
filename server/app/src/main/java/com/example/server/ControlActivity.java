package com.example.server;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;

public class ControlActivity extends AppCompatActivity {

    private final static String TAG = "ControlActivity";

    private Thread disThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        TextView title = findViewById(R.id.title);
        title.setText("选择服务");

        Button audioButton = findViewById(R.id.audio_button);
        audioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setClass(ControlActivity.this,AudioActivity.class);
                startActivity(intent);
            }
        });

        Button fileButton = findViewById(R.id.file_button);
        fileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setClass(ControlActivity.this,FileActivity.class);
                startActivity(intent);
            }
        });

        Button AppButton = findViewById(R.id.app_button);
        AppButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setClass(ControlActivity.this,ScreenRecorder.class);
                startActivity(intent);
            }
        });

        Button faceButton = findViewById(R.id.face_button);
        faceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setClass(ControlActivity.this,FaceActivity.class);
                startActivity(intent);
            }
        });

        Button cameraButton = findViewById(R.id.camera_button);
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setClass(ControlActivity.this,CameraActivity.class);
                startActivity(intent);
            }
        });

        Button disButton = findViewById(R.id.dis_button);
        disButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clientDisconnect();
            }
        });
    }

    private Thread clientDisThread;
    private void clientDisconnect() {
        if(clientDisThread != null) {
            return ;
        }
        byte[] msg = {SocketServer.DISCONNECT,SocketServer.CLIENT_ID};
        clientDisThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SocketServer.sendData(msg);
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    MyService.socketServer.disconnect();
                    e.printStackTrace();
                }
                clientDisThread = null;
            }
        });
        clientDisThread.start();
    }

    private void disClient() {
        if(disThread != null)
            return ;
        byte[] msg = {SocketServer.DIS_CLIENT,SocketServer.CLIENT_ID};
        disThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SocketServer.sendData(msg);
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    MyService.socketServer.disconnect();
                    e.printStackTrace();
                }
                disThread = null;
            }
        });
        disThread.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.e(TAG, "onStop: 关闭" );
    }

    protected void onDestroy() {
        super.onDestroy();
        disClient();
        Log.e(TAG, "onStop: 销毁" );
    }
}