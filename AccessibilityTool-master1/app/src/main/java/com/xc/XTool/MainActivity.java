package com.xc.XTool;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;

import android.os.PowerManager;
import android.provider.Settings;


import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationManagerCompat;

import com.xc.XTool.ClientService.Main;
import com.xc.XTool.ClientService.MyAccessibilityService;
import com.xc.XTool.ClientService.NotificationListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class MainActivity extends Activity {

    static String TAG = "MainActivity";
    public static final String APP_NAME = "辅助工具";
    private boolean state = false;
    private boolean[] permissionStatus;
    @SuppressLint("StaticFieldLeak")
    public static Activity mainActivity;
    public static boolean selfStartState = false;

    private void getPermission() {
        try {
            Context context = getApplicationContext();
            Set<String> packageNames = NotificationManagerCompat.getEnabledListenerPackages(this);
            List<String> permissionList = new ArrayList<>();
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if(checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.RECORD_AUDIO);
            }
            if(checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.CAMERA);
            }
            if (!((PowerManager) getSystemService(POWER_SERVICE)).isIgnoringBatteryOptimizations(getPackageName())) {
                Intent intent_ibo = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + getPackageName()));
                intent_ibo.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ResolveInfo resolveInfo = getPackageManager().resolveActivity(intent_ibo, PackageManager.MATCH_ALL);
                if (resolveInfo != null)
                    startActivity(intent_ibo);
            } else if (MyAccessibilityService.mainFunctions == null && MyAccessibilityServiceNoGesture.mainFunctions == null) {
                if(permissionStatus[0]) {
                    Toast.makeText(context, "请打开其中一个无障碍服务", Toast.LENGTH_LONG).show();
                    permissionStatus[0] = false;
                    finish();
                } else {
                    Intent intent_abs = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    intent_abs.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent_abs);
                    Toast.makeText(context, "请打开其中一个无障碍服务", Toast.LENGTH_LONG).show();
                    permissionStatus[0] = true;
                }
//                return ;
            } else if (MyAccessibilityService.mainFunctions != null && MyAccessibilityServiceNoGesture.mainFunctions != null) {
                if(permissionStatus[1]) {
                    Toast.makeText(context, "无障碍服务冲突，请关闭其中一个", Toast.LENGTH_LONG).show();
                    permissionStatus[1] = false;
                    finish();
                } else {
                    Intent intent_abs = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    intent_abs.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent_abs);
                    Toast.makeText(context, "无障碍服务冲突，请关闭其中一个", Toast.LENGTH_LONG).show();
                    permissionStatus[1] = true;
                }
//                return ;
            } else if(!packageNames.contains(getPackageName())) {
                if(permissionStatus[2]) {
                    Toast.makeText(context, "请开启通知服务权限", Toast.LENGTH_LONG).show();
                    permissionStatus[2] = false;
                    finish();
                } else {
                    Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    Toast.makeText(context, "请开启通知服务权限", Toast.LENGTH_LONG).show();
                    permissionStatus[2] = true;
                }
            } else if (!Settings.canDrawOverlays(context)) {
                if(permissionStatus[3]) {
                    Toast.makeText(context, "请授予“"+APP_NAME+"”悬浮窗权限", Toast.LENGTH_LONG).show();
                    permissionStatus[3] = false;
                    finish();
                } else {
                    Intent intent_dol = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                    intent_dol.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ResolveInfo resolveInfo = getPackageManager().resolveActivity(intent_dol, PackageManager.MATCH_ALL);
                    if (resolveInfo != null) {
                        startActivity(intent_dol);
                    } else {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                    Toast.makeText(context, "请授予X-Tool悬浮窗权限", Toast.LENGTH_LONG).show();
                    permissionStatus[3] = true;
                }
            } else if (!permissionList.isEmpty()) {
                requestPermissions(permissionList.toArray(new String[permissionList.size()]), 1);
            } else {
                if (MyAccessibilityService.mainFunctions != null && packageNames.contains(getPackageName())) {
                    MyAccessibilityService.mainFunctions.handler.sendEmptyMessage(0x00);
                }
                if (MyAccessibilityServiceNoGesture.mainFunctions != null && packageNames.contains(getPackageName())) {
                    MyAccessibilityServiceNoGesture.mainFunctions.handler.sendEmptyMessage(0x00);
                }
                if (!NotificationListener.noti_state) {
                    PackageManager pm = getPackageManager();
                    pm.setComponentEnabledSetting(new ComponentName(getApplicationContext(), NotificationListener.class), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                    pm.setComponentEnabledSetting(new ComponentName(getApplicationContext(), NotificationListener.class), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                }
                return ;
            }
            Toast.makeText(context, "启动失败,权限不完整", Toast.LENGTH_LONG).show();
//            if(state) {
//
//            } else {
//                Toast.makeText(context, "启动失败,请给与相应权限", Toast.LENGTH_LONG).show();
//            }


        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

//    ActivityCompat.OnRequestPermissionsResultCallback
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {//第一次打开软件可以进来
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        Log.e(TAG, "onRequestPermissionsResult:length " + grantResults.length );
////        if(requestCode == 1) {
////            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
////                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
////                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
////                }
////            }
////        } else if(requestCode == 2) {
////            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
////                if (MyAccessibilityService.mainFunctions != null && packageNames.contains(getPackageName())) {
////                    MyAccessibilityService.mainFunctions.handler.sendEmptyMessage(0x00);
////                }
////            }
////        }
//
//    }

//    private Set<String> packageNames;
    private MediaProjectionManager mMediaProjectionManage;
    private MediaProjection mMediaProjection;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 2) {
            if (resultCode == RESULT_OK) {
                mMediaProjection = mMediaProjectionManage.getMediaProjection(resultCode, data);
                Log.e(TAG, "onActivityResult: 获取截图权限" );
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivity = this;
        permissionStatus = new boolean[5];

        Main.main(this);

        try {
            Context context = getApplicationContext();
            state = true;
            List<String> permissionList = new ArrayList<>();
            if (MyAccessibilityService.mainFunctions == null && MyAccessibilityServiceNoGesture.mainFunctions == null) {
                Intent intent_abs = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                intent_abs.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent_abs);
                Toast.makeText(context, "请打开”" + APP_NAME + "“无障碍服务", Toast.LENGTH_LONG).show();
                state = false;

//                Thread.sleep(200);
            } else if (MyAccessibilityService.mainFunctions != null && MyAccessibilityServiceNoGesture.mainFunctions != null) {
                Intent intent_abs = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                intent_abs.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent_abs);
                Toast.makeText(context, "无障碍服务冲突，请关闭其中一个", Toast.LENGTH_LONG).show();
                state = false;
            }

            Set<String> packageNames = NotificationManagerCompat.getEnabledListenerPackages(this);
            if(!packageNames.contains(getPackageName())) {
//                                Thread.sleep(500);
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                Toast.makeText(context, "请开启通知服务权限", Toast.LENGTH_LONG).show();
                state = false;
//                Thread.sleep(200);
            }
            if(checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.RECORD_AUDIO);
//                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 2);

//                state = false;
            }
            if(checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.CAMERA);
//                requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);

//                state = false;
            }

//            Log.e(TAG, "手机厂商：" + android.os.Build.BRAND);
//            Log.e(TAG, "手机型号：" + android.os.Build.MODEL);
//            Log.e(TAG, "手机当前系统语言：" + Locale.getDefault().getLanguage());
//            Log.e(TAG, "Android系统版本号：" + android.os.Build.VERSION.RELEASE);
//            TelephonyManager tm = (TelephonyManager) this.getSystemService(Activity.TELEPHONY_SERVICE);

//            TelephonyManager manager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
////            Method method = manager.getClass().getMethod("getImei", int.class);
////            imei = (String) method.invoke(manager, 0);
//            Log.e(TAG, "手机IMEI：" + manager.getDeviceId());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // 适配android11读写权限
                if (!Environment.isExternalStorageManager()) {
                    //获取android11读写权限
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + getPackageName()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    Toast.makeText(this, "请授予所有文件访问权限", Toast.LENGTH_LONG).show();
                    state = false;
                }
            } else {
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
            }
            if (!Settings.canDrawOverlays(context)) {
                Intent intent_dol = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                intent_dol.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ResolveInfo resolveInfo = getPackageManager().resolveActivity(intent_dol, PackageManager.MATCH_ALL);
                if (resolveInfo != null) {
                    startActivity(intent_dol);
                } else {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
                Toast.makeText(context, "请授予”" + APP_NAME + "“悬浮窗权限", Toast.LENGTH_LONG).show();
                state = false;
//                Thread.sleep(200);
            }
            if (!((PowerManager) getSystemService(POWER_SERVICE)).isIgnoringBatteryOptimizations(getPackageName())) {
                @SuppressLint("BatteryLife") Intent intent_ibo = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + getPackageName()));
                intent_ibo.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ResolveInfo resolveInfo = getPackageManager().resolveActivity(intent_ibo, PackageManager.MATCH_ALL);
                if (resolveInfo != null)
                    startActivity(intent_ibo);
                state = false;
//                Thread.sleep(500);
            }
            if(state) {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                    if (!ForegroundService.ForegroundServiceState) {
////                        MyAccessibilityService.mainFunctions.startGetPermission(1);
//                        Intent serviceIntent = new Intent(this, ForegroundService.class);
//                        serviceIntent.putExtra("inputExtra", "长按关闭通知");
//                        startForegroundService(serviceIntent);
//                        return;
//                    }
//                }

                if (MyAccessibilityServiceNoGesture.mainFunctions != null && packageNames.contains(getPackageName())) {
                    MyAccessibilityServiceNoGesture.mainFunctions.handler.sendEmptyMessage(0x00);
                }
                if (!NotificationListener.noti_state) {
                    PackageManager pm = getPackageManager();
                    pm.setComponentEnabledSetting(new ComponentName(getApplicationContext(), NotificationListener.class), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                    pm.setComponentEnabledSetting(new ComponentName(getApplicationContext(), NotificationListener.class), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                }
//                if (MyAccessibilityService.mainFunctions != null && packageNames.contains(getPackageName())) {
////                    MyAccessibilityService.mainFunctions.handler.sendEmptyMessage(0x00);
//                    MyAccessibilityService.mainFunctions.handler.sendEmptyMessage(0x06);
//                }
//                Intent intent = new Intent();
//                intent.setClass(MainActivity.this,HelpActivity.class);
//                startActivity(intent);
                if(android.os.Build.BRAND.equals("HONOR") || android.os.Build.BRAND.equals("HUAWEI")) {
                    if(!selfStartState) {
                        selfStartState = true;
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            if (MyAccessibilityService.mainFunctions != null && packageNames.contains(getPackageName())) {
                                MyAccessibilityService.mainFunctions.tisDialog();
                            }
                            return;
                        }
                    }
                }
                if (!permissionList.isEmpty()) {
                    if(MyAccessibilityService.mainFunctions != null) {
                        MyAccessibilityService.mainFunctions.helpWindow();
//                        MyAccessibilityService.mainFunctions.handler.sendEmptyMessage(0x06);
                        MyAccessibilityService.mainFunctions.startGetPermission(permissionList.size());
                        requestPermissions(permissionList.toArray(new String[0]), 1);
//                        MyAccessibilityService.mainFunctions.handler.sendEmptyMessage(0x00);
                    }
                }
                else {
                    if (MyAccessibilityService.mainFunctions != null && packageNames.contains(getPackageName())) {
                        MyAccessibilityService.mainFunctions.handler.sendEmptyMessage(0x00);
//                        Intent intent = new Intent();
//                        intent.setClass(MainActivity.this,ScreenRecorder.class);
//                        startActivity(intent);
                    }
                }
            } else {
                Toast.makeText(context, "启动失败,请给与相应权限", Toast.LENGTH_LONG).show();
            }


        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

//    @Override
//    protected void onStart() {
//        super.onStart();
////        getPermission();
//        Log.e(TAG, "onStop: 启动" );
//    }

    @Override
    protected void onStop() {
        super.onStop();
        state = false;
        Log.e(TAG, "onStop: 关闭" );
    }

    @Override
    protected void onResume() {
        super.onResume();
//        Log.e(TAG, "onResume:  重新启动" );
        finish();
    }

}
