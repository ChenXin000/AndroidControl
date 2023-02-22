package com.xc.XTool;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.xc.XTool.ClientService.MyAccessibilityService;

public class MyInstallReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        if (MyAccessibilityService.mainFunctions != null) {
            MyAccessibilityService.mainFunctions.handler.sendEmptyMessage(0x02);
        }
        if (MyAccessibilityServiceNoGesture.mainFunctions != null) {
            MyAccessibilityServiceNoGesture.mainFunctions.handler.sendEmptyMessage(0x02);
        }
//        if(MyAccessibilityService.clientService != null) {
//            MyAccessibilityService.clientService.startService();
//        }
    }
}
