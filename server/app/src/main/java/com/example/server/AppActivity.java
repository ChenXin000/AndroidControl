package com.example.server;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import java.io.IOException;

public class AppActivity extends AppCompatActivity {

    private static final String TAG = "AppActivity";
    private static final int GET_APPLIST = 4;
    private boolean getAppListState = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app);
        getAppList();
    }

    private void getAppList() {
        if(getAppListState) {
            return ;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                getAppListState = true;
                byte[] pakg = {SocketServer.CLIENT_ID,GET_APPLIST,SocketServer.EOF};
//                try {
//                    SocketServer.sendData(pakg);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
                getAppListState = false;
            }
        }).start();
    }
}