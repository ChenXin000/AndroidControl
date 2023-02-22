package com.xc.XTool.ClientService;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class SocketClient {

    private static final String TAG = "SocketClient";
    private String ip;
    private int post;

    public static final int CONNECT_NETWORK = 1;
    public static final int TEST_STATE = 2;
    public static final byte CLIENT = (byte) 0XEE;

    private Thread sendDataThread;
    private static HandlerThread mBackgroundThread;
    private static Handler mBackgroundHandler;
    private static boolean connectState;
    private static boolean threadState;
    private static boolean sendState;

    private BufferedReader bufferedReader;
    private static Socket socket;
    private static BlockingDeque<byte[]> sendDataBlockingDeque;
    private static boolean sendDataState;
    private boolean testState;

    public SocketClient(String ip,int post) {
        this.ip = ip;
        this.post = post;
        sendState = true;
        threadState = true;
        sendDataBlockingDeque = new LinkedBlockingDeque<byte[]>(50);
        startBackgroundThread();

        sendMsg(CONNECT_NETWORK);
    }
    public SocketClient() {

    }

    public void connected() {
        sendState = false;
        threadState = false;
        Log.e(TAG, "startConnect: 连接成功");
    }

    public static void startSend() {
        sendState = true;
    }
    public static void stopSend() {
        sendState = false;
    }

    public static void sendMsg(int msg) {
        try {
            if(mBackgroundHandler != null) {
                if ((msg == TEST_STATE) && !threadState && !sendState) {
                    if (mBackgroundHandler.sendEmptyMessage(msg)) {
                        threadState = true;
                    }
                } else if ((msg == CONNECT_NETWORK) && !connectState) {
                    if (mBackgroundHandler.sendEmptyMessage(msg)) {
                        connectState = true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("SocketFunctions");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper()) {
            @Override
            public void handleMessage(@NonNull Message message) {
                if(message.what == CONNECT_NETWORK) {
                    connect();
                } else if(message.what == TEST_STATE) {
                    testConnect();
                    threadState = false;
//                    Log.e(TAG, "handleMessage: testConnect" );
                }
            }
        };
    }

    private static void stopBackgroundThread() {
        if(mBackgroundThread != null) {
            mBackgroundThread.interrupt();
            mBackgroundThread.quitSafely();
            try {
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
//                Log.e(TAG, "停止线程");
                Log.e(TAG, "stopBackgroundThread: 停止线程");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * 连接网络
     */
    private void connect() {
        int i = 0;
        disconnect();
        connectState = true;
        while(connectState) {
            try {
                if(i++ == 30) {
                    break;
                }
                Log.e(TAG, "正在连接:" + (i));
                socket = new Socket();
                socket.setTcpNoDelay(true);
                socket.setKeepAlive(true);
                socket.connect(new InetSocketAddress(ip, post), 1000 * 10);
                if (socket.isConnected()) {
                    InputStream inputStream = socket.getInputStream();
                    OutputStream outputStream = socket.getOutputStream();
                    bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                    startSendDataThread(outputStream);

                    String info = '\n' + android.os.Build.BRAND + '\0' + android.os.Build.MODEL + '\0' + android.os.Build.VERSION.RELEASE + '\0';
                    byte[] pakg = info.getBytes(StandardCharsets.UTF_8);
                    pakg[0] = CLIENT;
                    outputStream.write(pakg);

                    connected();
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (e instanceof SocketTimeoutException) {
                    Log.e(TAG, "连接超时");
                }
                if (e instanceof SocketException) {
                    Log.e(TAG, "网络问题");
                    try {
                        Thread.sleep(1000 * 10);
                    } catch (InterruptedException a) {
                        a.printStackTrace();
                    }
                }
                disconnect();
            }
        }
        Log.e(TAG, "connect: 连接线程退出" );
        sendState = false;
        threadState = false;
        connectState = false;
    }
    /**
     * 检测连接状态
     */
    private void testConnect() {
        try {
            putSendData(new byte[]{0X0});
            testState = false;
            Log.e(TAG, "testConnect: 连接状态检测");
            try {
                Thread.sleep(1000 * 10);
                if(!testState) {
                    throw new Exception("未收到回应");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.e(TAG, "testConnect: 中断睡眠" );
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "testConnect: 重新连接");
            sendMsg(CONNECT_NETWORK);
        }
        Log.e(TAG, "testConnect: 状态检测退出");
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        try {
            sendState = true;
            threadState = true;
            if(socket != null) {
                socket.close();
                socket = null;
            }
            clearSendDataBlockingDeque();
            if(bufferedReader != null) {
                bufferedReader.close();
                bufferedReader = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * 启动发送数据线程
     * @param outputStream
     * @throws Exception
     */
    private void startSendDataThread(OutputStream outputStream) throws Exception {
        if(sendDataThread != null) {
            try {
                sendDataThread.join();
                sendDataThread = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new Exception("发送线程结束失败");
            }
        }
        sendDataThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] buff;
                    sendDataState = true;
                    sendDataBlockingDeque.clear();
                    while(sendDataState) {
                        if ((buff = sendDataBlockingDeque.take()) != null) {
                            if (sendDataState) {
                                outputStream.write(buff);
                            }
                        }
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                    Log.e(TAG, "startSendDataThread: 连接断开重新连接");
                    sendMsg(CONNECT_NETWORK);
                } finally {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Log.e(TAG, "发送线程退出 ");
                }
            }
        });
        sendDataThread.start();
    }
    /**
     * 清理发送队列并退出发送线程
     */
    private void clearSendDataBlockingDeque() {
        if(sendDataBlockingDeque != null) {
            sendDataState = false;
            sendDataBlockingDeque.clear();
            sendDataBlockingDeque.offer(new byte[]{1});
        }
    }
    /**
     * 添加要发送的数据到队列
     * @param data
     * @throws Exception
     */
    public static void putSendData(byte[] data) throws Exception{
        try {
            if (sendDataState) {
                if(sendDataBlockingDeque.offer(data, 2, TimeUnit.SECONDS)) {
                    return;
                } else {
                    sendMsg(CONNECT_NETWORK);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new Exception("连接已断开");
    }
    /**
     * 读取字符串
     * @return String
     * @throws Exception
     */
    public String readString() throws Exception {
        try {
            if (bufferedReader != null) {
                String str = bufferedReader.readLine();
                if(str != null && !str.isEmpty()) {
                    testState = true;
                    return str;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "readString: 连接断开重新连接");
            sendMsg(CONNECT_NETWORK);
        }
        throw new Exception("读取数据失败");
    }
    /**
     * 关闭Socket
     */
    public void closeSocket() {
        connectState = false;
        stopBackgroundThread();
        disconnect();
    }

}
