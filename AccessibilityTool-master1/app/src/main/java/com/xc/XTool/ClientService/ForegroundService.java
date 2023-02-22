package com.xc.XTool.ClientService;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.xc.XTool.R;

public class ForegroundService extends Service {
    private static final String TAG = "ForegroundService";

    public static boolean ForegroundServiceState = false;
    private ClientService clientService;

    public ForegroundService() {
    }

    @Override
    public void onCreate() {
        if(clientService == null) {
            clientService = new ClientService(this);
        }
        ForegroundServiceState = true;

        Log.e(TAG, "前台服务创建");
    }


    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG, "onBind: 绑定");
        return null;
        // TODO: Return the communication channel to the service.
//        throw new UnsupportedOperationException("Not yet implemented");
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String input = intent.getStringExtra("inputExtra");
        NotificationCompat.Builder builder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            createNotificationChannel();
//            Intent notificationIntent = new Intent(this, MainActivity.class);
//            PendingIntent pendingIntent = PendingIntent.getActivity(this,
//                    0, notificationIntent, 0);
            builder = new NotificationCompat.Builder(this, "ForegroundService");
        } else {
            builder = new NotificationCompat.Builder(this);
        }
        Notification notification = builder
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
//                    .setContentText(input)
                .setSmallIcon(R.drawable.tool_icon)
//                    .setContentIntent(pendingIntent)
                .setContentTitle(input)
                .build();

        startForeground(1, notification);

        Log.e(TAG, "前台服务正在运行");

        return START_REDELIVER_INTENT;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        if(clientService != null) {
            clientService.stopService();
            clientService = null;
        }
        Log.e(TAG, "onDestroy: 前台服务退出");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    "ForegroundService",
                    "点击关闭通知",
                    NotificationManager.IMPORTANCE_MIN
            );
            serviceChannel.setShowBadge(false);

            serviceChannel.enableVibration(false);
            serviceChannel.enableLights(false);
            serviceChannel.setSound(null, null);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}