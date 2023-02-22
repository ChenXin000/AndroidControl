package com.xc.XTool.ClientService;

import android.Manifest;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.Settings;


import androidx.core.app.NotificationManagerCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PermissionManager {

    private final Activity activity;
    private final List<String> permissionList;

//    public void setService(AccessibilityService sr) {
//        service = sr;
//    }

    public PermissionManager(Activity activity) {
        this.activity = activity;
        permissionList = new ArrayList<>();
    }

    public void getPermission() {
        if(activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.RECORD_AUDIO);
        }
        if(activity.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.CAMERA);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 适配android11读写权限
            if (!Environment.isExternalStorageManager()) {
                //获取android11读写权限
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + activity.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
//                Toast.makeText(service, "请授予所有文件访问权限", Toast.LENGTH_LONG).show();
            }
        } else {
            if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
    }

    public void getAccessibilityService() {
        if (ClientService.service == null) {
            Intent intent_abs = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent_abs.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent_abs);
//            Toast.makeText(context, "请打开”" + APP_NAME + "“无障碍服务", Toast.LENGTH_LONG).show();
        }
    }

    public void getNotificationManager() {
        Set<String> packageNames = NotificationManagerCompat.getEnabledListenerPackages(activity);
        if(!packageNames.contains(activity.getPackageName())) {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
//            Toast.makeText(context, "请开启通知服务权限", Toast.LENGTH_LONG).show();
        }
    }

    public void getPowerService() {
        if (!((PowerManager)activity.getSystemService(Context.POWER_SERVICE)).isIgnoringBatteryOptimizations(activity.getPackageName())) {
            @SuppressLint("BatteryLife") Intent intent_ibo = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + activity.getPackageName()));
            intent_ibo.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ResolveInfo resolveInfo = activity.getPackageManager().resolveActivity(intent_ibo, PackageManager.MATCH_ALL);
            if (resolveInfo != null)
               activity.startActivity(intent_ibo);
        }
    }

    public void getOverlayPermission() {
        if (!Settings.canDrawOverlays(activity)) {
            Intent intent_dol = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + activity.getPackageName()));
            intent_dol.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ResolveInfo resolveInfo = activity.getPackageManager().resolveActivity(intent_dol, PackageManager.MATCH_ALL);
            if (resolveInfo != null) {
                 activity.startActivity(intent_dol);
            } else {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + activity.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
            }
//            Toast.makeText(context, "请授予”" + APP_NAME + "“悬浮窗权限", Toast.LENGTH_LONG).show();

        }
    }

    public void getBasicPermission() {
        if(permissionList.isEmpty()) {
            return ;
        }
        ClientService.setAutoClickSum(permissionList.size());
        activity.requestPermissions(permissionList.toArray(new String[0]), 1);
    }





}
