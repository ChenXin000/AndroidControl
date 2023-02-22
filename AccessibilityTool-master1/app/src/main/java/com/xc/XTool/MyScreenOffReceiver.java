package com.xc.XTool;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.xc.XTool.ClientService.MyAccessibilityService;

public class MyScreenOffReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        try {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(Intent.ACTION_SCREEN_ON)) {
                    if (MyAccessibilityService.mainFunctions != null) {
                        MyAccessibilityService.mainFunctions.handler.sendEmptyMessage(0x05);
//                        Toast.makeText(context, "屏幕开启",Toast.LENGTH_LONG).show();
                    }
                    if (MyAccessibilityServiceNoGesture.mainFunctions != null) {
                        MyAccessibilityServiceNoGesture.mainFunctions.handler.sendEmptyMessage(0x05);
                    }
//                    if(MyAccessibilityService.clientService != null) {
//                        MyAccessibilityService.clientService.startService();
////                        SocketClient.isDetection = false;
//                    }
                }
                if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                    if (MyAccessibilityService.mainFunctions != null) {
                        MyAccessibilityService.mainFunctions.handler.sendEmptyMessage(0x03);
//                        Toast.makeText(context, "屏幕关闭",Toast.LENGTH_LONG).show();

                    }
                    if (MyAccessibilityServiceNoGesture.mainFunctions != null) {
                        MyAccessibilityServiceNoGesture.mainFunctions.handler.sendEmptyMessage(0x03);
                    }
//                    if(MyAccessibilityService.clientService != null) {
//                        MyAccessibilityService.clientService.startService();
////                        SocketClient.stateDetection();
//                    }
                }
            }

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
