package com.xc.XTool.ClientService;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import top.zibin.luban.Luban;
import top.zibin.luban.OnCompressListener;

public class FileManager {

    private static final String TAG = "FileManager";
    private Thread getListFileThread;
    private Thread uploadFileThread;
    private Context context;
    private boolean uploadFileState;
//    private FileInputStream fileInputStream;

    private static final int UNKNOWN_TYPE = 0;
    private static final int VIDEO_TYPE = 1;
    private static final int PICTURE_TYPE = 2;
    private static final int AUDIO_TYPE = 3;
    private static final int ZIP_TYPE = 4;
    private static final int DIR_TYPE = 5;

    private static final int TEXT_TYPE = 6;
    private static final int PPT_TYPE = 7;
    private static final int PDF_TYPE = 8;
    private static final int WORD_TYPE = 9;
    private static final int WPS_TYPE = 10;
    private static final int EXCEL_TYPE = 11;
    private static final int APK_TYPE = 12;

    private final String[] zipType = {".zip",".rar",".7z"};
    private final String[] audioType = {".mp3",".m4a",".wmv",".aac",".flac",".wav",".ape",".amr"};
    private final String[] fileType = {".txt",".pptx",".pdf",".docx",".wps",".xlsx",".apk"};

//    private SocketClient socketClient;

    /**
     * 文件头部16进制匹配
     * @param head
     * @return
     */
    public int parseHeadCode(String head) {
        if (head == null) {
            return UNKNOWN_TYPE;
        }
        head = head.toUpperCase();
        if (head.startsWith("0000001866")) { //MP4
            return VIDEO_TYPE;
        } else if (head.startsWith("41564920")) {   //AVI
            return VIDEO_TYPE;
        } else if (head.startsWith("0000002066")) { //M4V
            return VIDEO_TYPE;
        } else if (head.startsWith("000001B")) {    //MPG
            return VIDEO_TYPE;
        } else if (head.startsWith("57415645")) {   //WAV
            return AUDIO_TYPE;
        } else if (head.startsWith("FFFB50")) {     //MP3
            return AUDIO_TYPE;
        } else if (head.startsWith("494433")) {     //MP3
            return AUDIO_TYPE;
        } else if (head.startsWith("504B0304")) {   //ZIP
            return ZIP_TYPE;
        } else if (head.startsWith("52617221")) {   //RAR
            return ZIP_TYPE;
        } else if (head.startsWith("377ABCAF271C")) {   //7-ZIP
            return ZIP_TYPE;
        }
        else {
            return UNKNOWN_TYPE;
        }
    }

    public byte[] readStreamBytes(InputStream inputStream, int readCount) {
        if (inputStream == null) {
            return null;
        }
        try {
            byte[] buffer = new byte[readCount];
            int temp;
            int offset = 0;
            int maxTime = 10000;
            while (offset < readCount) {
                if (maxTime < 0) {
                    throw new IOException("failed to complete after 10000 reads;");
                }
                temp = inputStream.read(buffer, offset, readCount - offset);
                if (temp < 0) {
                    break;
                }
                offset += temp;
                maxTime--;
            }
            return buffer;
        } catch (Exception e) {
            e.printStackTrace();
            try {
                inputStream.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return null;
    }

    private String bytesToHexString(byte[] src) {
        if (src == null || src.length <= 0) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (byte b : src) {
            int v = b & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    public String readFileHeadString(File file) {
        InputStream inputStream;
        try {
            inputStream = new FileInputStream(file);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        byte[] bytes = readStreamBytes(inputStream, 16);
        return bytesToHexString(bytes);
    }

    public int getFileTypeCode(File file) {
        return parseHeadCode(readFileHeadString(file));
    }

    /**
     * 文件后缀名匹配
     * @param name
     * @param file
     * @return
     */
    private char getFileType(String name,File file) {
        if(name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".gif")) {
            return PICTURE_TYPE;
        }
        if(name.endsWith(".mp4")) {
            return VIDEO_TYPE;
        }
        char i = 6;
        for(String s : fileType) {
            if(name.endsWith(s))
                return i;
            i++;
        }
        for(String s : zipType) {
            if(name.endsWith(s))
                return ZIP_TYPE;
        }
        for(String s : audioType) {
            if(name.endsWith(s))
                return AUDIO_TYPE;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getPath(), options);
        if(options.outMimeType != null)
            return PICTURE_TYPE;
        if(file.length() > 1024)
            return (char) getFileTypeCode(file);
        return UNKNOWN_TYPE;
    }

    public FileManager(Context context) {
        this.context = context;
//        this.socketClient = socketClient;
    }

    /**
     * 文件排序
     * @param fileList
     */
    private void fileSort(File[] fileList) {
        Arrays.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                if (o1.isDirectory() && o2.isFile())
                    return -1;
                if (o1.isFile() && o2.isDirectory())
                    return 1;
                return o1.getName().compareTo(o2.getName());
            }
        });
    }

    /**
     * 文件大小格式化
     * @param file
     * @return
     */
    @SuppressLint("DefaultLocale")
    private String getFileSize(File file) {
        String file_size = " ";
        if (file.isFile()) {
            long size = file.length();
            if (size >= 1073741820) {
                file_size = String.format("%.2f GB", (double) size / 1073741820.0);
            } else if (size >= 1048576) {
                file_size = String.format("%.2f MB", (double) size / 1048576.0);
            } else if (size >= 1024) {
                file_size = String.format("%.2f KB", (double) size / 1024.0);
            } else {
                file_size = size + " B";
            }
        } else {
            File[] fileSum = file.listFiles();
            if (fileSum != null)
                file_size = fileSum.length + "个项目";
        }
        return file_size;
    }
//
//    private int getFileType() {
//
//    }



    public void getImageThumbnail(String imagePath, int width, int height) {
        String path = context.getExternalCacheDir().getAbsolutePath();
        Luban.with(context).load(imagePath).ignoreBy(100).setTargetDir(path).setCompressListener(new OnCompressListener() {
            @Override
            public void onStart() {

            }
            @Override
            public void onSuccess(File file) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        uploadFile(file);
//                        Log.e(TAG, "run: " + file.getPath() );
//                        Log.e(TAG, "run: " + path);
                        if(file.getPath().startsWith(path)) {
                            file.delete();
                        }
                    }
                }).start();
            }

            @Override
            public void onError(Throwable e) {

            }
        }).launch();

//
//
//        Bitmap bitmap;
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        /*
//         * inJustDecodeBounds: If set to true, the decoder will return null (no
//         * bitmap), but the out... fields will still be set, allowing the caller
//         * to query the bitmap without having to allocate the memory for its
//         * pixels.
//         */
//        options.inJustDecodeBounds = true;
//        // 获取图片的宽、高，但是：bitmap为null！可以看一下上面的内容体会一下
//        BitmapFactory.decodeFile(imagePath, options);
//        if (options.outMimeType != null) {
//            // 计算缩放比
//            int sampleSize = (options.outWidth / width + options.outHeight / height) / 2;
//            Log.e(TAG, "getImageThumbnail:sampleSize " + sampleSize );
//            options.inDither = false;
//            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
//            options.inSampleSize = sampleSize;
//            options.inJustDecodeBounds = false;
//            // 真正读入图片
//            bitmap = BitmapFactory.decodeFile(imagePath, options);
//        } else {
//            bitmap = ThumbnailUtils.createVideoThumbnail(imagePath, MediaStore.Video.Thumbnails.MINI_KIND);
//        }
//        // 利用ThumbnailUtils 根据原来的图创建缩略图
////        bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
//        if (bitmap != null) {
//            SocketClient.sendDataState = true;
//            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            try {
//                baos.write(new byte[]{0,0,0,0});
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
//            byte[] bytes = baos.toByteArray();
//            Log.e(TAG, "getImageThumbnail: " + Arrays.toString(bytes) );
//            int length = bytes.length - 4;
//            bytes[0] = (byte) 0XFF;
//            bytes[1] = (byte)((length >> 16) & 0XFF);
//            bytes[2] = (byte) ((length >> 8) & 0XFF);
//            bytes[3] = (byte) (length & 0XFF);
//
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    SocketClient.sendDataFlush(bytes);
//                    SocketClient.sendDataState = false;
//                }
//            }).start();
//            Log.e(TAG, "getImageThumbnail: " + Arrays.toString(bytes) );
////            Log.e(TAG, "getImageThumbnail: " + Arrays.toString(bytes));
//            Log.e(TAG, "getImageThumbnail: " + bytes.length);
////            return bytes;
//        }

//        return null;
//        return bitmap;
    }

//    private Bitmap getVideoThumbnail(String videoPath, int width, int height) {
////        Bitmap bitmap = null;
//        // 获取视频的缩略图
//        // kind could be MINI_KIND or MICRO_KIND
//        Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(videoPath, MediaStore.Video.Thumbnails.MICRO_KIND);
////        bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
////        int bytes = bitmap.getByteCount();
//        if(bitmap != null) {
//            return bitmap;
//        }
////            ByteArrayOutputStream baos = new ByteArrayOutputStream();
////            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
////            byte[] bytes = baos.toByteArray();
////            Log.e(TAG, "getVideoThumbnail: video " + bytes.length);
////            return bytes;
////        }
//        return null;
//    }

    private boolean getListFileState;
    public void getListFile(String filePath) {
        if(getListFileThread != null) {
            return;
        }
        getListFileState = true;
        getListFileThread = new Thread(new Runnable() {
            @Override
            public void run() {
                SocketClient.startSend();
//                SocketClient.sendState = true;
                File file = new File(filePath);
                if (file.exists() && file.isDirectory()) {
                    File[] files = file.listFiles();
                    if (files != null) {
                        fileSort(files);
                        for (File value : files) {
//                            byte[] icon = null;
                            String file_name = value.getName();
                            String file_size = getFileSize(value);
                            String file_date = new SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.CHINESE).format(new Date(value.lastModified()));
                            char type = 0; // 0 代表未知文件
                            if(value.isFile()) {
                                type = getFileType(file_name.toLowerCase(),value);
//                                        icon = getImageThumbnail(value.getPath(), 100, 100);
                            } else if(value.isDirectory()) {
                                type = DIR_TYPE;
                            }
                            String str_pakg = "\n\n\n\n" + type + file_name + '\n' + file_date + '\n' + file_size + '\n';
                            byte[] bytes = str_pakg.getBytes(StandardCharsets.UTF_8);
                            int length = bytes.length;
                            bytes[0] = (byte) 0XFF;
                            bytes[1] = (byte) (byte) ((length >> 16) & 0XFF);
                            bytes[2] = (byte) (byte) ((length >> 8) & 0XFF);
                            bytes[3] = (byte) (byte) (length & 0XFF);
//                            byte[] head = {(byte) 0XFF,(byte) ((length >> 16) & 0XFF),(byte) ((length >> 8) & 0XFF),(byte) (length & 0XFF)};
//                            System.arraycopy(head, 0, bytes, 0, 4);

//                            Log.e(TAG, "run: " + length );

                            if(!getListFileState) {
                                break;
                            }
                            try {
                                SocketClient.putSendData(bytes);
                            } catch (Exception e) {
                                e.printStackTrace();
                                break;
                            }
//                            if(!SocketClient.sendData(bytes)) {
//                                break;
//                            }
                        }
                    }
                    byte[] head = {(byte) 0XFF, 0, 0, 0};
                    try {
                        SocketClient.putSendData(head);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
//                    SocketClient.sendDataFlush(head);

                }
//                getListFileState = true;
                getListFileThread = null;
                SocketClient.stopSend();
//                SocketClient.sendState = false;
//                SocketClient.clearSendDataBlockingDeque();
            }
        });
        getListFileThread.start();
    }

    public void deleteFile(String fileName) {
        File file = new File(fileName);
        Log.e(TAG, "deleteFile: " + fileName );
        if(file.isFile()) {
            Log.e(TAG, "deleteFile: 删除文件" );
            if(file.delete()) {
                Log.e(TAG, "删除成功");
            }
        }
    }

    public void uploadFile(File file) {
        SocketClient.startSend();
//        SocketClient.sendState = true;
        uploadFileState = true;
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] data = new byte[1024];
            data[0] = (byte) 0XFF;
            ByteBuffer byteBuffer = ByteBuffer.allocate(8);
            byteBuffer.putLong(0,file.length());
            byteBuffer.position(0);
            byteBuffer.limit(8);
            byteBuffer.get(data,1,8);
            int len = 9;
            if ((len += fileInputStream.read(data,9,data.length - 9)) > 9) {
                while(uploadFileState) {
                    if(data.length == len) {
                        SocketClient.putSendData(data);
                    } else if(len > 0) {
                        byte[] buff = new byte[len];
                        System.arraycopy(data,0,buff,0,len);
                        SocketClient.putSendData(buff);
                    }
                    data = new byte[1024];
                    if ((len = fileInputStream.read(data,0,data.length)) < 0) {
                        break;
                    }
                }
            }
            fileInputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
//        SocketClient.sendState = false;
        SocketClient.stopSend();
        uploadFileState = false;
    }

    public void uploadFile(String fileName) {
        if(uploadFileThread != null) {
            return ;
        }
        File file = new File(fileName);
        if(!file.isFile()) {
            return ;
        }
        uploadFileThread = new Thread(new Runnable() {
            @Override
            public void run() {
                uploadFile(file);
                uploadFileThread = null;
            }
        });
        uploadFileThread.start();
    }

    public void stopUploadFile() {
        uploadFileState = false;
        getListFileState = false;
    }
}
