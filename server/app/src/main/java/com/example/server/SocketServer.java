package com.example.server;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class SocketServer {

    private final static String TAG = "SocketServer";

    private String ip;
    private int post;
    private final byte[] CONTROL = {(byte) 0XFF};
    public static byte CLIENT_ID;
    public static byte DIS_CLIENT = 0x12;
    public static byte DISCONNECT = 0X13;
    public static byte EOF = 0X0A;
    private Thread connectThread;
    private static Thread readDataThread;
    private static boolean readDataState = false;
    private static BlockingDeque<byte[]> readDataBlockingDeque;

    private HandlerThread mBackgroundThread;
    private static Handler handler;
//    private static ArrayList<byte[]> readDataList;

    public static Socket socket;
    public static InputStream inputStream;
    public static OutputStream outputStream;
//    private static DataInputStream dataInputStream;
    public static boolean connectState = false;
    public static boolean sendMsgState = false;

//    private static DataBufferManager dataBufferManager;

    private void setDataBuffer(byte[] data,int offset,int len) {

    }



    public SocketServer(String ip , int post) {
        this.ip = ip;
        this.post = post;
//        byte[] dataBuffer = new byte[1024 * 512];
        readDataBlockingDeque = new LinkedBlockingDeque<byte[]>(50);

        mBackgroundThread = new HandlerThread("SocketFunctions");
        mBackgroundThread.start();

        handler = new Handler(mBackgroundThread.getLooper()) {
            @Override
            public void handleMessage(@NonNull Message message) {
                byte[] pakg = (byte[]) message.obj;
                if(outputStream != null) {
                    try {
                        outputStream.write(pakg);
                        outputStream.flush();
//                        try {
//                            Thread.sleep(200);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
                        sendMsgState = false;
                        return ;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                sendMsgState = false;
                disconnect();
                connect();
            }
        };

    }

    public static boolean sendCtrlMsg(byte[] msg) {
        if(handler != null && !sendMsgState) {
            Message message = new Message();
            message.obj = msg;
            handler.sendMessage(message);
            sendMsgState = true;
            while(sendMsgState) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            };
            return true;
        }
        return false;
    }

    /**
     * 连接
     */
    public void connect() {
        if(socket != null || connectState) {
//            MainActivity.sendMsg(MainActivity.GET_CLIENT);
            return ;
        }
        connectState = true;
        connectThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket();
                    socket.setTcpNoDelay(true);
                    socket.setKeepAlive(true);
//                    Log.e(TAG, "run: " + socket.getReceiveBufferSize() );
//                    socket.setReceiveBufferSize(1024 * 256);
//                    Log.e(TAG, "run: " + socket.getReceiveBufferSize() );
                    MainActivity.sendMsg(MainActivity.CONNECT_WAIT);
                    socket.connect(new InetSocketAddress(ip,post),5000);
                    if(socket.isConnected())
                    {
                        inputStream = socket.getInputStream();
                        outputStream = socket.getOutputStream();
                        sendMsgState = false;
//                        dataInputStream = new DataInputStream(inputStream);
                        sendDataFlush(CONTROL);
                        startReadDataThread();
                        MainActivity.sendMsg(MainActivity.GET_CLIENT);
//                        MainActivity.tipsText.setText("服务器连接成功");
//                        MainActivity.tipsText.setText("正在获取客户端...");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    MainActivity.sendMsg(MainActivity.CONNECT_ERROR);
//                    MainActivity.tipsText.setText("服务器连接失败");
                    disconnect();
                }
                connectState = false;
            }
        });
        connectThread.start();
    }

    /**
     *
     */
     public static void setInputStream() {
         try {
             inputStream = socket.getInputStream();
         } catch (IOException e) {
             e.printStackTrace();
         }
     }
    /**
     *
     */
    public static void setOutputStream() {
        try {
            outputStream = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     *
     */
    public static void closeInputStream() {
        if(inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            inputStream = null;
        }
    }
    /**
     *
     */
    public static void closeOutputStream() {
        if(outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            outputStream = null;
        }

    }
    /**
     * 断开连接
     */
    public void disconnect() {
        try {
            connectState = false;
            readDataState = false;
            if(socket != null) {
                socket.close();
                socket = null;
            }
            if(inputStream != null) {
                inputStream.close();
                inputStream = null;
            }
            if(outputStream != null) {
                outputStream.close();
                outputStream = null;
            }
//            if(dataInputStream != null) {
//                dataInputStream.close();
//                dataInputStream = null;
//            }
            if(connectThread != null) {
                connectThread = null;
            }
//            if(dataBufferManager != null) {
//                dataBufferManager.clear();
//                dataBufferManager = null;
//            }
//            if(readDataThread != null) {
//                readDataThread = null;
//            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeSocketServer() {
        disconnect();
        if(mBackgroundThread != null) {
            mBackgroundThread.quitSafely();
            try {
                mBackgroundThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mBackgroundThread = null;
        }
        handler = null;
    }

    public static DataInputStream getDataInputStream() throws IOException {
//        if(dataInputStream != null) {
//            int len = dataInputStream.skipBytes(dataInputStream.available());
//            Log.e(TAG, "getDataInputStream:skip " + len );
//            return dataInputStream;
//        }
        return null;
    }

    public void startReadDataThread() {
        if(readDataThread != null) {
            return ;
        }
        readDataThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int len;
                while(inputStream != null) {
                    try {
                        byte[] data = new byte[131072]; //256 K
                        if((len = inputStream.read(data,4,131068)) > 0) {
                            if(readDataState) {
                                try {
                                    data[0] = (byte) ((len >> 24) & 0XFF);
                                    data[1] = (byte) ((len >> 16) & 0XFF);
                                    data[2] = (byte) ((len >> 8) & 0XFF);
                                    data[3] = (byte) (len & 0XFF);
//                                    Log.e(TAG, "run: " + readDataBlockingDeque.size() );
                                    readDataBlockingDeque.offer(data,2, TimeUnit.SECONDS);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                    break;
                                }
                            }
                        } else {
                            Log.e(TAG, "run: 读取数据结束" );
                            break;
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "异常: 读取数据结束" );
                        e.printStackTrace();
                        break;
                    }
                }
                disconnect();
                readDataThread = null;
            }
        });
        readDataThread.start();
    }

//    public static DataBufferManager getDataBufferManager() {
//        readDataState = true;
////        dataBufferManager.clear();
//        return dataBufferManager;
//    }

    public static void clearDataBuffer() {
        readDataState = false;
//        dataBufferManager.clear();
    }

//    public JSONObject readJson() {
//
//    }

    public static BlockingDeque<byte[]> getReadBlockingDeque() {
//        dataList.clear();
        readDataBlockingDeque.clear();
        readDataState = true;
        return readDataBlockingDeque;
    }

    public static void clearReadBlockingDeque() {
        readDataState = false;
        readDataBlockingDeque.clear();
    }

    public static void blockingDequeOffer() {
        readDataBlockingDeque.offer(new byte[]{0,0,0,0});
    }

    public static void stopReadData() {
        readDataState = false;
//        readDataList.clear();
    }


    public static boolean sendData(byte[] data) throws IOException {
        if(outputStream != null) {
            outputStream.write(data);
            outputStream.flush();
            return true;
        }
        return false;
    }

    /**
     * 发送数据
     * @param data
     */
    public static boolean sendDataFlush(byte[] data) {
        if(outputStream != null) {
            try {
                outputStream.write(data);
                outputStream.flush();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }
    /**
     * 读取数据
     */
    public static int readData(byte[] data,int offset,int len) throws IOException {
        if(inputStream != null) {
            return inputStream.read(data,offset,len);
        }
        return -1;
    }

    /**
     *
     */
    private static BufferedReader bufferedReader;
    public static String readString()  throws  IOException {
        if(inputStream != null) {
            if(bufferedReader == null) {
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            }
            String str = bufferedReader.readLine();
            if(str != null && str.equals("EOF")) {
                bufferedReader = null;
                return str;
            } else if(str == null) {
                bufferedReader = null;
            }
            return str;
        }
        return null;
    }
}
