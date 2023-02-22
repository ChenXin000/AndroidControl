package com.xc.XTool.ClientService;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;

public class Main {

    public static void main(Activity activity) {
        if (!ForegroundService.ForegroundServiceState) {
            Intent serviceIntent = new Intent(activity, ForegroundService.class);
            serviceIntent.putExtra("inputExtra", "前台服务(可长按关闭通知)");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity.startForegroundService(serviceIntent);
            } else {
                activity.startService(serviceIntent);
            }
        }
    }
}
