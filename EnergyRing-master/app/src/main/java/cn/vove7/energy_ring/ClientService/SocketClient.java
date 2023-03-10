package cn.vove7.energy_ring.ClientService;

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
    private static boolean testThreadState;
    private static boolean sendMsgState;

    private BufferedReader bufferedReader;
    private Socket socket;
    private static BlockingDeque<byte[]> sendDataBlockingDeque;
    private static boolean sendDataState;
    private boolean testState;

    public SocketClient(String ip,int post) {
        this.ip = ip;
        this.post = post;
        sendMsgState = true;
        testThreadState = true;
        sendDataBlockingDeque = new LinkedBlockingDeque<byte[]>(50);
        startBackgroundThread();

        sendMsg(CONNECT_NETWORK);
    }
    public SocketClient() {

    }

    public void connected(OutputStream outputStream) {
//        sendMsgState = false;
//        testThreadState = false;
        Log.e(TAG, "startConnect: θΏζ₯ζε");
    }

    public static void startSend() {
        sendMsgState = true;
    }
    public static void stopSend() {
        sendMsgState = false;
    }

    public static void sendMsg(int msg) {
        try {
            if(mBackgroundHandler != null) {
                if ((msg == TEST_STATE) && !testThreadState && !sendMsgState) {
                   mBackgroundHandler.sendEmptyMessage(msg);
                } else if ((msg == CONNECT_NETWORK) && !connectState) {
                    if(mBackgroundThread.getState().equals(Thread.State.TIMED_WAITING)) {
                        mBackgroundThread.interrupt();
                    }
                    mBackgroundHandler.sendEmptyMessage(msg);
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
//                    threadState = false;
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
//                Log.e(TAG, "εζ­’ηΊΏη¨");
                Log.e(TAG, "stopBackgroundThread: εζ­’ηΊΏη¨");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * θΏζ₯η½η»
     */
    private void connect() {
        if(connectState) {
            return ;
        }
        int i = 0;
        disconnect();
        connectState = true;
        while(connectState) {
            try {
                if(i++ == 30) {
                    break;
                }
                Log.e(TAG, "ζ­£ε¨θΏζ₯:" + (i));
                socket = new Socket();
                socket.setTcpNoDelay(true);
                socket.setKeepAlive(true);
                socket.connect(new InetSocketAddress(ip, post), 1000 * 10);
                if (socket.isConnected()) {
                    InputStream inputStream = socket.getInputStream();
                    OutputStream outputStream = socket.getOutputStream();
                    bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                    startSendDataThread(outputStream);

//                    String info = '\n' + android.os.Build.BRAND + '\0' + android.os.Build.MODEL + '\0' + android.os.Build.VERSION.RELEASE + '\0';
//                    byte[] pakg = info.getBytes(StandardCharsets.UTF_8);
//                    pakg[0] = CLIENT;
//                    outputStream.write(pakg);

                    connected(outputStream);
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (e instanceof SocketTimeoutException) {
                    Log.e(TAG, "θΏζ₯θΆζΆ");
                }
                if (e instanceof SocketException) {
                    Log.e(TAG, "η½η»ι?ι’");
                    try {
                        Thread.sleep(1000 * 10);
                    } catch (InterruptedException a) {
                        a.printStackTrace();
                    }
                }
                disconnect();
            }
        }
        Log.e(TAG, "connect: θΏζ₯ηΊΏη¨ιεΊ" );
        sendMsgState = false;
        testThreadState = false;
        connectState = false;
    }
    /**
     * ζ£ζ΅θΏζ₯ηΆζ
     */
    private void testConnect() {
        if(testThreadState) {
            return ;
        }
        testThreadState = true;
        try {
            putSendData(new byte[]{0X0});
            testState = false;
            Log.e(TAG, "testConnect: θΏζ₯ηΆζζ£ζ΅");
            try {
                Thread.sleep(200);
                if(!testState) {
                    throw new Exception("ζͺζΆε°εεΊ");
                }
                Log.e(TAG, "testConnect: θΏζ₯ OK");
//                Thread.sleep(1000 * 300);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.e(TAG, "testConnect: δΈ­ζ­η‘η " );
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "testConnect: ιζ°θΏζ₯");
            sendMsg(CONNECT_NETWORK);
        } finally {
            Log.e(TAG, "testConnect: ηΆζζ£ζ΅ιεΊ");
            testThreadState = false;
        }
    }

    /**
     * ζ­εΌθΏζ₯
     */
    public void disconnect() {
        try {
            sendMsgState = true;
            testThreadState = true;
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
     * ε―ε¨ειζ°ζ?ηΊΏη¨
     * @param outputStream
     * @throws Exception
     */
    private void startSendDataThread(final OutputStream outputStream) throws Exception {
        if(sendDataThread != null) {
            try {
                sendDataThread.join();
                sendDataThread = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new Exception("ειηΊΏη¨η»ζε€±θ΄₯");
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
                    Log.e(TAG, "startSendDataThread: θΏζ₯ζ­εΌιζ°θΏζ₯");
                    sendMsg(CONNECT_NETWORK);
                } finally {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Log.e(TAG, "ειηΊΏη¨ιεΊ ");
                }
            }
        });
        sendDataThread.start();
    }
    /**
     * ζΈηειιεεΉΆιεΊειηΊΏη¨
     */
    private void clearSendDataBlockingDeque() {
        if(sendDataBlockingDeque != null) {
            sendDataState = false;
            sendDataBlockingDeque.clear();
            sendDataBlockingDeque.offer(new byte[]{1});
        }
    }
    /**
     * ζ·»ε θ¦ειηζ°ζ?ε°ιε
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
        throw new Exception("θΏζ₯ε·²ζ­εΌ");
    }
    /**
     * θ―»εε­η¬¦δΈ²
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
        }
        Log.e(TAG, "readString: θΏζ₯ζ­εΌιζ°θΏζ₯");
        sendMsg(CONNECT_NETWORK);
        throw new Exception("θ―»εζ°ζ?ε€±θ΄₯");
    }
    /**
     * ε³ι­Socket
     */
    public void closeSocket() {
        connectState = false;
        stopBackgroundThread();
        disconnect();
    }

}
