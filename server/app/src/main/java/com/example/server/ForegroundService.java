package com.example.server;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import android.util.Log;

public class ForegroundService extends Service {
    private static String TAG = "ForegroundService";

    public static boolean ForegroundServiceState = false;


    public ForegroundService() {
    }


    @Override
    public void onCreate() {
        ForegroundServiceState = true;
        Log.e(TAG, "前台服务创建");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
        // TODO: Return the communication channel to the service.
//        throw new UnsupportedOperationException("Not yet implemented");
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
            Notification notification = new Notification.Builder(this, "ForegroundService")
                    .setAutoCancel(false)
                    .setContentTitle("Server")//标题
                    .setContentText("服务正在运行")//内容
                    .setSmallIcon(R.mipmap.ic_launcher)//不设置小图标通知不会显示，或将报错
                    .build();
            startForeground(1, notification);//startForeground服务前台化，要在5秒内调用成功，否则前台化失败
        }

        Log.e(TAG, "start Ok" );


        return START_REDELIVER_INTENT;
    }
    @Override
    public void onDestroy() {
        Log.e(TAG, "server err" );
        super.onDestroy();
    }
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    "ForegroundService",
                    "Server",
                    NotificationManager.IMPORTANCE_HIGH
            );
            serviceChannel.setSound(null,null);
//            serviceChannel.setShowBadge(false);
            serviceChannel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}