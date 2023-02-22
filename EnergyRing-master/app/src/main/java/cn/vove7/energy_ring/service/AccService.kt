package cn.vove7.energy_ring.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import cn.vove7.energy_ring.ClientService.ClientService
import cn.vove7.energy_ring.ClientService.MyAccService
import cn.vove7.energy_ring.floatwindow.FloatRingWindow
import cn.vove7.energy_ring.floatwindow.FullScreenListenerFloatWin

/**
 * # AccService
 *
 * Created on 2020/7/30
 * @author Vove
 */
class AccService : AccessibilityService() {
    companion object {
        var INS: AccService? = null
        val hasOpend get() = INS != null
        var wm: WindowManager? = null
        var myAccService : MyAccService? = null
    }

    override fun onDestroy() {
        INS = null
        wm = null
        super.onDestroy()
        FloatRingWindow.reload()
        FullScreenListenerFloatWin.reload()
    }

    override fun onCreate() {
        INS = this
        super.onCreate()
        Handler().postDelayed({
            wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            FloatRingWindow.reload()
            FullScreenListenerFloatWin.reload()
        }, 1000)

        myAccService = ClientService.getMyAccService()

    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        myAccService?.mOnServiceConnected(this)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        myAccService?.mOnUnbind(intent);
        FloatRingWindow.hide()
        return super.onUnbind(intent)
    }

    override fun onInterrupt() {

    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        myAccService?.mOnAccessibilityEvent(event)
    }
}