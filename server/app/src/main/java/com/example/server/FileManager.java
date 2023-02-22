package com.example.server;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.BlockingDeque;

public class FileManager {
    private static final String TAG = "FileManager";
    private static String DOWNLOAD_PATH = "/Download/";
    public static String PICTURE_PATH;
    public static final int GET_FILE_LIST = 5;
    public static final int DOWNLOAD_FILE = 9;
    public static final int STOP_DOWNLOAD_FILE = 16;
    public static final int STOP_GET_FILE_LIST = STOP_DOWNLOAD_FILE;
    public static final int DELETE_FILE = 17;
    public static final int GET_PICTURE = 18;

    public static final int REFRESH_FILE_LIST = 6;
    public static final int UPDATE_FILE_SUM = 7;
    public static final int REFRESH_DOWNLOAD_SIZE = 20;
    public static final int DOWNLOAD_OK = 21;
    public static final int DOWNLOAD_ERROR = 22;
    public static final int DELETE_FILE_ERROR = 30;
    public static final int WAIT_FOR_RESULTS = 31;
    public static final int GET_PICTURE_OK = 32;
    public static final int SCROLL_LIST = 33;
    public static final int GET_START = 23;
    public static final int GET_END = 24;


    private static HandlerThread mBackgroundThread;
    private static Handler mBackgroundHandler;
    private final Handler handler;
    private ArrayList<String> fileList;
    private ArrayList<String> filePathList;
    private ByteBuffer byteBuffer;

    private boolean mBgHandlerState;

    public FileManager(Handler handler) {
        this.handler = handler;
        startBackgroundThread();
        filePathList = new ArrayList<>();
        fileList = new ArrayList<>();
        byteBuffer = ByteBuffer.allocate(1024 * 512);
        filePathList.add("storage/emulated/0/");
        PICTURE_PATH = MainActivity.MAIN_PATH + MainActivity.MODEL + "/Picture/";
    }
    public void closeFileManager() {
        if(filePathList != null) {
            filePathList.clear();
            filePathList = null;
        }
        if(fileList != null) {
            fileList.clear();
            fileList = null;
        }
        byteBuffer = null;
        getFileListState = false;
        stopWriteFile();
        stopBackgroundThread();
    }

    public void sendMsg(int msg) {
        if(mBackgroundHandler != null) {
            mBackgroundHandler.sendEmptyMessage(msg);
        }
    }
    public void sendMsg(int msg,Object obj) {
        if(mBackgroundHandler != null) {
            Message message = new Message();
            message.what = msg;
            message.obj = obj;
            mBackgroundHandler.sendMessage(message);
        }
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("SocketFunctions");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper()) {
            @Override
            public void handleMessage(@NonNull Message message) {
                switch (message.what) {
                    case GET_FILE_LIST:
                        getFileList((String) message.obj);
                        break;
                    case DOWNLOAD_FILE:
                        downloadFile((String) message.obj);
                        break;
                    case DELETE_FILE:
                        deleteFile((String) message.obj);
                        break;
                    case GET_PICTURE:
                        getPicture((String) message.obj);
                        break;
                }
                mBgHandlerState = false;
            }
        };
    }

    private void stopBackgroundThread() {
        if(mBackgroundThread != null) {
            mBackgroundThread.quitSafely();
            try {
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
                Log.e(TAG, "openCamera: 停止线程");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean getFileListState;
    private String filePath;

    private void decodeData(BlockingDeque<byte[]> dataQueue) {
        byteBuffer.clear();
        byte[] head = new byte[4],data;
        int length = 0,len;
        while(getFileListState) {
            if((data= dataQueue.poll()) == null) {
                continue;
            }
            len = (data[0] << 24) | ((data[1] << 16) & 0XFFFFFF) | ((data[2] << 8) & 0XFFFF) | (data[3] & 0XFF);
            byteBuffer.put(data, 4, len);
            byteBuffer.flip();
            while (byteBuffer.limit() - byteBuffer.position() >= 4) {
                byteBuffer.get(head);
                if ((head[0] & 0XFF) == 0XFF) {
                    length = (((head[1] << 16) & 0XFFFFFF) | ((head[2] << 8) & 0XFFFF) | (head[3] & 0XFF)) - 4;
                    if (length <= 0 || length >= 10000) {
                        getFileListState = false;
                        break;
                    } else if (length <= byteBuffer.limit() - byteBuffer.position()) {
                        byte[] buff = new byte[length];
                        System.arraycopy(byteBuffer.array(),byteBuffer.position(),buff,0,length);
//                        sendMsg(UPDATE_FILE_SUM, new String(buff, StandardCharsets.UTF_8));
                        fileList.add(new String(buff,StandardCharsets.UTF_8));
                        byteBuffer.position(byteBuffer.position() + length);
                    } else {
                        byteBuffer.position(byteBuffer.position() - 4);
                        Log.e(TAG, "run: 长度不够" );
                        break;
                    }
                } else {
                    byteBuffer.position(byteBuffer.position() - 3);
                    Log.e(TAG, "run: 没有找到" );
                }
            }
            handler.sendEmptyMessage(UPDATE_FILE_SUM);
            if(length > 0) {
                Log.e(TAG, "run: 重置" );
                byteBuffer.compact();
            }
        }
        handler.sendEmptyMessage(SCROLL_LIST);
    }

    public ArrayList<String> getFileList() {
        return fileList;
    }
    public String getFilePath() {
        return filePath;
    }

    private void getFileList(String fileName) {
        if(fileName != null) {
            filePathList.add(fileName + "/");
        }
        Log.e(TAG, "getFileList: 获取文件"  + mBgHandlerState + getFileListState);
        if(mBgHandlerState || getFileListState) {
            return ;
        }
        mBgHandlerState = true;
        fileList.clear();
        handler.sendEmptyMessage(REFRESH_FILE_LIST);
        StringBuilder path = new StringBuilder();
        path.append((char) SocketServer.CLIENT_ID);
        path.append((char) GET_FILE_LIST);
        path.append(filePathList.get(0));
        for(int i=1;i<filePathList.size();i++) {
            path.append(filePathList.get(i));
        }
        filePath = path.substring(2);
        path.append('\n');
        byte[] pakg = path.toString().getBytes();
        BlockingDeque<byte[]> dataQueue = SocketServer.getReadBlockingDeque();
        if(SocketServer.sendDataFlush(pakg)) {
            getFileListState = true;
            handler.sendEmptyMessage(GET_START);
//            sendMsg(GET_START);
            decodeData(dataQueue);
            handler.sendEmptyMessage(GET_END);
//            sendMsg(GET_END);
        }
        getFileListState = false;
        SocketServer.clearReadBlockingDeque();
    }

    public boolean goBackPath() {
        if(getFileListState) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] pakg = {SocketServer.CLIENT_ID, STOP_GET_FILE_LIST, SocketServer.EOF};
                    SocketServer.sendDataFlush(pakg);
                    getFileListState = false;
                }
            }).start();
        } else {
            if(filePathList.size() > 1) {
                filePathList.remove(filePathList.size() - 1);
            }
            sendMsg(GET_FILE_LIST,null);
        }
        return getFileListState;
    }

//    private boolean downloadFileState;
    private long downloadSize;
    private long fileSize;

    public long getDownloadSize() {
        return downloadSize;
    }

    public long getFileSize() {
        return fileSize;
    }

    private static boolean writeFileState;
    private void writeFile(File file) {
        BufferedOutputStream bufferedOutputStream = null;
        fileSize = 0;
        downloadSize = 0;
        int i = 10;
        try {
            writeFileState = true;
            BlockingDeque<byte[]> dataQueue = SocketServer.getReadBlockingDeque();
            bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file));
            int len;
            Log.e(TAG, "saveFile: 等待下载");
            while (writeFileState) {
                byte[] data = dataQueue.poll();
                if (data == null) {
                    continue;
                }
                len = (data[0] << 24) | ((data[1] << 16) & 0XFFFFFF) | ((data[2] << 8) & 0XFFFF) | (data[3] & 0XFF);
                if (fileSize == 0) {
                    if ((data[4] & 0XFF) != 0XFF)
                        break;
                    ByteBuffer byteBuffer = ByteBuffer.allocate(13);
                    byteBuffer.put(data, 5, 8);
                    fileSize = byteBuffer.getLong(0);
                    Log.e(TAG, "saveFile: " + fileSize);
                    bufferedOutputStream.write(data, 13, len - 9);
                    downloadSize = len - 9;
                } else {
                    bufferedOutputStream.write(data, 4, len);
                    downloadSize += len;
                }
                if(i-- == 0) {
                    handler.sendEmptyMessage(REFRESH_DOWNLOAD_SIZE);
                    i = 10;
                }
                if (downloadSize == fileSize) {
                    handler.sendEmptyMessage(REFRESH_DOWNLOAD_SIZE);
                    handler.sendEmptyMessage(DOWNLOAD_OK);
                    break;
                }
                Log.e(TAG, "saveFile: " + downloadSize);
            }
            Log.e(TAG, "saveFile: " + downloadSize);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(downloadSize == 0 || downloadSize != fileSize) {
            handler.sendEmptyMessage(DOWNLOAD_ERROR);
            file.delete();

        }
        if(bufferedOutputStream != null) {
            try {
                bufferedOutputStream.flush();
                bufferedOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        SocketServer.clearReadBlockingDeque();
        writeFileState = false;
    }

    private void downloadFile(String fileName) {
        if (writeFileState || fileName == null || mBgHandlerState) {
            return;
        }
        mBgHandlerState = true;
        String path = "\n\n" + filePath + fileName + '\n';
        byte[] pakg = path.getBytes(StandardCharsets.UTF_8);
        pakg[0] = SocketServer.CLIENT_ID;
        pakg[1] = DOWNLOAD_FILE;
        Log.e(TAG, "downloadFile: " + Arrays.toString(pakg));

        if (SocketServer.sendDataFlush(pakg)) {
            File file = new File(MainActivity.MAIN_PATH + MainActivity.MODEL + DOWNLOAD_PATH);
            if (!file.exists()) {
                if (!file.mkdir()) {
                    return;
                }
            }
            file = new File(MainActivity.MAIN_PATH + MainActivity.MODEL + DOWNLOAD_PATH, fileName);
            writeFile(file);
        }
    }

    public static void stopWriteFile() {
        if(writeFileState) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] pakg = {SocketServer.CLIENT_ID, STOP_DOWNLOAD_FILE, SocketServer.EOF};
                    SocketServer.sendDataFlush(pakg);
                    writeFileState = false;
                }
            }).start();
        }
    }

    private boolean deleteFileState;
    private void deleteFile(String fileName) {
        if(deleteFileState || fileName == null || mBgHandlerState) {
            return ;
        }
        mBgHandlerState = true;
        deleteFileState = true;
        String path  = '\n' + '\n' + filePath + fileName + '\n';
        Log.e(TAG, "deleteFile: " + path );
        byte[] pakg = path.getBytes();
        pakg[0] = SocketServer.CLIENT_ID;
        pakg[1] = DELETE_FILE;
        if(!SocketServer.sendDataFlush(pakg)) {
            handler.sendEmptyMessage(WAIT_FOR_RESULTS);
        } else {
            handler.sendEmptyMessage(DELETE_FILE_ERROR);
        }
        deleteFileState = false;
    }

//    private boolean getPictureState;

    private String pictureName;
    public Bitmap getPicture() {
        return BitmapFactory.decodeFile(PICTURE_PATH + pictureName);
    }
    public String getPicturePath() {
        return PICTURE_PATH + pictureName;
    }

    private void getPicture(String fileName) {
        if(writeFileState || mBgHandlerState) {
            return ;
        }
        mBgHandlerState = true;
//        getPictureState = true;
        pictureName = fileName;
        String path = "\n\n" + filePath + fileName + '\n';
        byte[] pakg = path.getBytes();
        pakg[0] = SocketServer.CLIENT_ID;
        pakg[1] = GET_PICTURE;
//        BlockingDeque<byte[]> dataQueue = SocketServer.getReadBlockingDeque();
//        int len,size = 0;
//        long pictureSize = 0;
//        byte[] data;
        Log.e(TAG, "getPicture: " + PICTURE_PATH );
        File file = new File(PICTURE_PATH);
        if (!file.exists()) {
            if (!file.mkdir()) {
                return;
            }
        }
        file = new File(PICTURE_PATH, fileName);
        if(file.exists()){
            Log.e(TAG, "getPicture: 图片存在" );
            downloadSize = file.length();
            fileSize = downloadSize;
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            handler.sendEmptyMessage(REFRESH_DOWNLOAD_SIZE);
            handler.sendEmptyMessage(DOWNLOAD_OK);
        }
        else if(SocketServer.sendDataFlush(pakg)) {
            writeFile(file);

//
//            try {
//                while (getPictureState) {
//                    if ((data = dataQueue.poll()) == null) {
//                        continue;
//                    }
//                    len = (data[0] << 24) | ((data[1] << 16) & 0XFFFFFF) | ((data[2] << 8) & 0XFFFF) | (data[3] & 0XFF);
//                    if (pictureSize == 0) {
//                        if ((data[4] & 0XFF) != 0XFF)
//                            break;
//                        ByteBuffer byteBuffer = ByteBuffer.allocate(13);
//                        byteBuffer.put(data, 5, 8);
//                        pictureSize = byteBuffer.getLong(0);
//
////                        pictureSize = ((data[5] << 16) & 0XFFFFFF) | ((data[6] << 8) & 0XFFFF) | (data[7] & 0xFF);
//                        if (pictureSize <= 0)
//                            break;
//                        Log.e(TAG, "getPicture: " + pictureSize );
//                        pictureBuffer = ByteBuffer.allocate(pictureSize);
//                        pictureBuffer.put(data, 8, len - 8);
//                        size = len - 9;
//                    } else {
//                        pictureBuffer.put(data, 4, len);
//                        size += len;
//                    }
//                    Log.e(TAG, "getPicture:size " + size );
//                    if(pictureSize == size) {
//                        handler.sendEmptyMessage(GET_PICTURE_OK);
//                        break;
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            Log.e(TAG, "getPicture:退出 " );
//            if(pictureSize != size || pictureSize == 0) {
//                handler.sendEmptyMessage(ERROR);
//            }
        }
//        SocketServer.clearReadBlockingDeque();
//        getPictureState = false;
    }

}
