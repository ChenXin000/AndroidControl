package cn.vove7.energy_ring.ClientService;

import android.content.Context;
import android.content.Intent;

public class MyScreenListener {

    public void mOnReceive(Context context, Intent intent) {
        try {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals(Intent.ACTION_SCREEN_ON)) {
                    screenOn();
                }

                if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                    screenOff();
                }
                // 解锁
                if(action.equals(Intent.ACTION_USER_PRESENT)) {
                    screenUnlock();
                }
            }

        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void screenOn() {}
    public void screenOff() {}
    public void screenUnlock() {}
}
