package com.xc.XTool.ClientService;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import com.xc.XTool.MainFunctions;
import com.xc.XTool.MyAccessibilityServiceNoGesture;

public class MyAccessibilityService extends AccessibilityService {

    private int create_num, connect_num;
    public static MainFunctions mainFunctions;

    private static Callback callback = new Callback();

    static class Callback {
        public void onCreate(Context context) {}
        public void onServiceConnected(Context context) {}
        public void onAccessibilityEvent(AccessibilityEvent event) {}
        public void onKeyEvent(KeyEvent event) {}
        public void onConfigurationChanged(Configuration newConfig) {}
        public void onUnbind(Intent intent) {}
    }

    public static void setCallback(Callback cb) {
        callback = cb;
    }

    public void mOnCreate(Context context) {

    }

    public void mOnServiceConnected(Context context) {

    }

    public void mOnAccessibilityEvent(AccessibilityEvent event) {

    }

    public void mOnUnbind(Intent intent) {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            create_num = 0;
            connect_num = 0;
            create_num++;

            callback.onCreate(this);

        } catch (Throwable e) {
            e.printStackTrace();
        }
        Log.e( "onCreate: ","服务创建" );
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        if (++connect_num != create_num) {
            throw new RuntimeException("无障碍服务出现异常");
        }
        callback.onServiceConnected(this);

        mainFunctions = new MainFunctions(this);
        mainFunctions.onServiceConnected();
        if (MyAccessibilityServiceNoGesture.mainFunctions != null) {
            MyAccessibilityServiceNoGesture.mainFunctions.handler.sendEmptyMessage(0x04);
        }

        mOnServiceConnected(this);
    }
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        callback.onAccessibilityEvent(event);


        mainFunctions.onAccessibilityEvent(event);
    }
    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        callback.onKeyEvent(event);

        return mainFunctions.onKeyEvent(event);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        callback.onConfigurationChanged(newConfig);

        mainFunctions.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        callback.onUnbind(intent);
        Log.e( "onUnbind: ","关闭" );

        mainFunctions.onUnbind(intent);
        mainFunctions = null;

        return super.onUnbind(intent);
    }

    @Override
    public void onInterrupt() {
    }
}
