package com.example.server;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.net.Socket;

public class MyService extends Service {

    private final static String TAG = "MyService";
    public static SocketServer socketServer;
    public MyService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
//        socketServer = new SocketServer("192.168.1.81",6000);
        socketServer = new SocketServer("175.178.232.68",6000);
//        socketServer = new SocketServer("192.168.1.4",6000);
//        socketServer = new SocketServer("192.168.31.239",6000);
        Log.e(TAG, "onCreate: " );
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand: " );
        socketServer.connect();
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        socketServer.closeSocketServer();
    }
}