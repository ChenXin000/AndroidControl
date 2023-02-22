package cn.vove7.energy_ring.ClientService;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import cn.vove7.energy_ring.App;
import cn.vove7.energy_ring.R;
import cn.vove7.energy_ring.listener.NotificationListener;
import cn.vove7.energy_ring.ui.activity.MainActivity;

public class PermissionManager {

    private Context context;
    private static boolean permissionState;
//    private final List<String> permissionList;

//    public void setService(AccessibilityService sr) {
//        service = sr;
//    }




    public PermissionManager() {
    }

    public static boolean getPerMissionUi(final Activity activity) {
        permissionState = true;

        final Context server = ClientService.service;

        List<String> permissionList = new ArrayList<>();
        if(activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.RECORD_AUDIO);
        }
        if(activity.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.CAMERA);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }


        LinearLayout linearLayout = new LinearLayout(server);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(50,50,50,50);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(30,0,30,30);

        LinearLayout.LayoutParams text_layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        text_layoutParams.setMargins(30,0,30,90);

        GradientDrawable text_drawable = new GradientDrawable();
        text_drawable.setCornerRadius(20);
        text_drawable.setColor(0x22eb261a);


        GradientDrawable but_drawable = new GradientDrawable();
        but_drawable.setCornerRadius(20);
        but_drawable.setColor(0x22eb261a);

        GradientDrawable but_drawable1 = new GradientDrawable();
        but_drawable1.setCornerRadius(20);
        but_drawable1.setColor(0x22eb261a);

        GradientDrawable but_drawable2 = new GradientDrawable();
        but_drawable2.setCornerRadius(20);
        but_drawable2.setColor(0x22eb261a);

        GradientDrawable but_drawable3 = new GradientDrawable();
        but_drawable3.setCornerRadius(20);
        but_drawable3.setColor(0x22eb261a);

        TextView textView = new TextView(server);
        textView.setText("派大星请求开启以下权限");
        textView.setTextSize(18);
        textView.setTextColor(0xfff0a1a8);
        textView.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        textView.setGravity(Gravity.CENTER);
        textView.setLayoutParams(text_layoutParams);
        linearLayout.addView(textView);


        Button button = new Button(server);
        button.setText("通知服务 ×");
        button.setBackground(but_drawable);
        button.setLayoutParams(layoutParams);
        button.setTextColor(0xffeb261a);
        linearLayout.addView(button);

        Button button1 = new Button(server);
        button1.setText("悬浮窗 ×");
        button1.setBackground(but_drawable1);
        button1.setLayoutParams(layoutParams);
        button1.setTextColor(0xffeb261a);
        linearLayout.addView(button1);

        Button button2 = new Button(server);
        button2.setText("电池优化 ×");
        button2.setBackground(but_drawable2);
        button2.setLayoutParams(layoutParams);
        button2.setTextColor(0xffeb261a);
        linearLayout.addView(button2);

        Button button3 = new Button(server);
        button3.setText("拒接并退出");
        button3.setBackground(but_drawable3);
        button3.setLayoutParams(layoutParams);
        button3.setTextColor(0xffeb261a);
        linearLayout.addView(button3);

        final AlertDialog tis_dialog= new AlertDialog.Builder(server).setCancelable(false).setView(linearLayout).create();
        Window win = tis_dialog.getWindow();
        win.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);
        win.setDimAmount(0.6f);

//        Log.e("DisplayMetrics", "getPerMissionUi: " );

        Set<String> packageNames = NotificationManagerCompat.getEnabledListenerPackages(server);
        if(!packageNames.contains(server.getPackageName())) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    String cs = new  ComponentName(server, NotificationListener.class).flattenToString();
                    Bundle bundle = new  Bundle();
                    bundle.putString(":settings:fragment_args_key", cs);
                    intent.putExtra(":settings:fragment_args_key", cs);
                    intent.putExtra(":settings:show_fragment_args", bundle);

                    server.startActivity(intent);
                    tis_dialog.dismiss();
                    Toast.makeText(server, "请开启通知服务", Toast.LENGTH_SHORT).show();

                }
            });
            permissionState = false;
        } else {
            but_drawable.setColor(0x2220aa4d);
            button.setTextColor(0xff20aa4d);
            button.setBackground(but_drawable);

            button.setText("通知服务 PASS √");

        }

        if (!Settings.canDrawOverlays(server)) {
            button1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent_dol = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + server.getPackageName()));
                    intent_dol.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ResolveInfo resolveInfo = server.getPackageManager().resolveActivity(intent_dol, PackageManager.MATCH_ALL);
                    if (resolveInfo != null) {
                        server.startActivity(intent_dol);
                    } else {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + server.getPackageName()));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        server.startActivity(intent);
                    }
                    tis_dialog.dismiss();
                    Toast.makeText(server, "请开启悬浮窗权限", Toast.LENGTH_SHORT).show();
                }
            });
            permissionState = false;
        } else {
            but_drawable1.setColor(0x2220aa4d);
            button1.setTextColor(0xff20aa4d);
            button1.setBackground(but_drawable1);
            button1.setText("悬浮窗 PASS √");
        }

        if (!((PowerManager)server.getSystemService(Context.POWER_SERVICE)).isIgnoringBatteryOptimizations(server.getPackageName())) {
            button2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    @SuppressLint("BatteryLife") Intent intent_ibo = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + server.getPackageName()));
                    intent_ibo.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ResolveInfo resolveInfo = server.getPackageManager().resolveActivity(intent_ibo, PackageManager.MATCH_ALL);
                    if (resolveInfo != null) {

                        Intent intent = new Intent(Settings.ACTION_SETTINGS);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        server.startActivity(intent);

                        server.startActivity(intent_ibo);
                        tis_dialog.dismiss();
                    }

                }
            });
            permissionState = false;
        } else {
            but_drawable2.setColor(0x2220aa4d);
            button2.setTextColor(0xff20aa4d);
            button2.setBackground(but_drawable2);
            button2.setText("电池优化 PASS √");
        }

        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tis_dialog.dismiss();
                activity.finish();
            }
        });

        final DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) server.getSystemService(AccessibilityService.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);
        WindowManager.LayoutParams params = win.getAttributes();
        params.width = metrics.widthPixels - 40;
        params.alpha = 1f;

        if(permissionState) {
            if(!permissionList.isEmpty()) {
                linearLayout.removeAllViews();

                LinearLayout linearLayout1 = new LinearLayout(server);
                linearLayout1.setPadding(0,metrics.heightPixels / 4,0,0);
                linearLayout1.setGravity(Gravity.CENTER);
                linearLayout1.setOrientation(LinearLayout.VERTICAL);

                TextView title = new TextView(server);
                title.setText("如何在最近任务隐藏？\n通过按返回键退出就可以从最近任务隐藏。");
                title.setGravity(Gravity.CENTER);
                title.setTextColor(0xffeb261a);
                title.setTextSize(16);
                title.setBackground(text_drawable);
                title.setPadding(0,50,0,50);
                linearLayout1.addView(title);

                TextView nullText = new TextView(server);
                linearLayout1.addView(nullText);

                TextView text = new TextView(server);
                text.setGravity(Gravity.CENTER);
                text.setText("如何保持后台能力？\n为了后台能力，先在此进入最近任务，将此卡片上锁。\n打开手机管家开启自启动");
                text.setTextColor(0xffee2746);
                text.setTextSize(16);
                text.setPadding(0,50,0,50);
                text.setBackground(text_drawable);
                linearLayout1.addView(text);

                nullText = new TextView(server);
                nullText.setPadding(0,200,0,0);
                linearLayout1.addView(nullText);

                final Button ok_but = new Button(server);
                ok_but.setText("知道了");
                ok_but.setBackground(but_drawable);
                ok_but.setTextColor(0xff20aa4d);
                linearLayout1.addView(ok_but);

                linearLayout.addView(linearLayout1);

                ok_but.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        tis_dialog.dismiss();
                    }
                });

                tis_dialog.show();
                params.width = metrics.widthPixels;
                params.height = metrics.heightPixels;
                params.flags =  WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

                win.setAttributes(params);

                ClientService.setAutoClickSum(permissionList.size());
                activity.requestPermissions(permissionList.toArray(new String[0]), 1);
                return false;
            } else {
                tis_dialog.dismiss();
            }
        } else {
            tis_dialog.show();
            params.gravity = Gravity.CENTER;
            win.setAttributes(params);
            return false;
        }
        return true;
//        final LayoutInflater inflater = LayoutInflater.from(context);
//        final View view = inflater.inflate(R.layout.activity_main, null);



//        AlertDialog alertDialog = new AlertDialog.Builder(context).setCancelable(false).setView(linearLayout).create();
//
//        alertDialog.show();

    }


    public static boolean getPermission(Activity activity) {
        if(ClientService.service == null) {
            Intent intent_abs = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent_abs.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent_abs);
            Toast.makeText(activity, "请开启辅助服务", Toast.LENGTH_LONG).show();
            return false;
        } else {
            return getPerMissionUi(activity);
        }
    }

    public void getReadWritePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 适配android11读写权限
            if (!Environment.isExternalStorageManager()) {
                //获取android11读写权限
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + context.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
//                Toast.makeText(service, "请授予所有文件访问权限", Toast.LENGTH_LONG).show();
            }
        }
    }

//    public static boolean getAccessibilityService(Context context) {
//        if (ClientService.service == null) {
//            Intent intent_abs = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
//            intent_abs.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            context.startActivity(intent_abs);
//            return false;
////            Toast.makeText(context, "请打开”" + APP_NAME + "“无障碍服务", Toast.LENGTH_LONG).show();
//        }
//        return true;
//    }

//    public void getNotificationManager() {
//        Set<String> packageNames = NotificationManagerCompat.getEnabledListenerPackages(context);
//        if(!packageNames.contains(context.getPackageName())) {
//            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            context.startActivity(intent);
////            Toast.makeText(context, "请开启通知服务权限", Toast.LENGTH_LONG).show();
//        }
//    }

    public static void getPowerService(Context context) {
        if (!((PowerManager)context.getSystemService(Context.POWER_SERVICE)).isIgnoringBatteryOptimizations(context.getPackageName())) {
            @SuppressLint("BatteryLife") Intent intent_ibo = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + context.getPackageName()));
            intent_ibo.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(intent_ibo, PackageManager.MATCH_ALL);
            if (resolveInfo != null)
                context.startActivity(intent_ibo);
        }
    }

//    public void getOverlayPermission() {
//        if (!Settings.canDrawOverlays(context)) {
//            Intent intent_dol = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + context.getPackageName()));
//            intent_dol.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            ResolveInfo resolveInfo = context.getPackageManager().resolveActivity(intent_dol, PackageManager.MATCH_ALL);
//            if (resolveInfo != null) {
//                context.startActivity(intent_dol);
//            } else {
//                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.getPackageName()));
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                context.startActivity(intent);
//            }
////            Toast.makeText(context, "请授予”" + APP_NAME + "“悬浮窗权限", Toast.LENGTH_LONG).show();
//
//        }
//    }

    public static boolean getBasicPermission(Activity activity) {
        List<String> permissionList = new ArrayList<>();

        if(activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.RECORD_AUDIO);
        }
        if(activity.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.CAMERA);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }

        if(permissionList.isEmpty()) {
            return true;
        }
        ClientService.setAutoClickSum(permissionList.size());
        activity.requestPermissions(permissionList.toArray(new String[0]), 1);
        return false;
    }


}
