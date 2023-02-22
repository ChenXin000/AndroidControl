package cn.vove7.energy_ring.ClientService;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

public class Main {

    public static void main(Context context) {
        if (!ForegroundService.ForegroundServiceState) {
            Intent serviceIntent = new Intent(context, ForegroundService.class);
            serviceIntent.putExtra("inputExtra", "前台服务(可左滑关闭通知)");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        } else {
            ClientService.startService();
        }

//        Log.e("Main", "main: 启动前台服务" );
    }
}
