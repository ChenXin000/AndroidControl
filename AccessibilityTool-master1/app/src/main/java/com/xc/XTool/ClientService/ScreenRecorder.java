package com.xc.XTool.ClientService;

import android.app.Activity;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;

public class ScreenRecorder extends Activity {
    private static final String TAG = "ScreenRecorder";
    private MediaProjectionManager mMediaProjectionManage;
    private static MediaProjection mMediaProjection;
    private static VirtualDisplay virtualDisplay;
    private static AvcEncoder avcEncoder;
    private static int width;
    private static int height;
    private static int bitRate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMediaProjectionManage = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent captureIntent = mMediaProjectionManage.createScreenCaptureIntent();
        startActivityForResult(captureIntent, 5);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    public static void startScreenRecorder(int w, int h ,int bit) {
        width = w;
        height = h;
        bitRate = bit;
    }

    public static void stopScreenRecorder() {
        if(avcEncoder != null) {
            avcEncoder.closeEncoder();
            avcEncoder = null;
        } else {
            close();
        }
    }

    private static void close() {
        try {
            if(mMediaProjection != null) {
                mMediaProjection.stop();
                mMediaProjection = null;
            }
            if(virtualDisplay != null) {
                virtualDisplay.release();
                virtualDisplay = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 5) {
            if (resultCode == RESULT_OK) {
                mMediaProjection = mMediaProjectionManage.getMediaProjection(resultCode, data);
                avcEncoder = new AvcEncoder(width,height,60,bitRate) {
                    @Override
                    public void closeEncoder() {
                        close();
                        super.closeEncoder();
                    }
                };
                Surface surface = avcEncoder.getSurface();
                virtualDisplay = mMediaProjection.createVirtualDisplay(TAG,width,height,1, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,surface,null,null);
                Log.e(TAG, "onActivityResult: 获取截图权限" );
            }
            finish();
        }
    }

}
