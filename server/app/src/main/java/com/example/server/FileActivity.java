package com.example.server;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.annotation.SuppressLint;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;


import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class FileActivity extends AppCompatActivity {

    private static final String TAG = "FileActivity";

//    public static final int GET_LISTFILE = 5;
//    private static final int REFRESH_FILE = 6;
//    private static final int UPDATE_FILE_SUM = 7;
//    public static final int DOWNLOAD_FILE = 9;
//    public static final int REFRESH_SIZE = 20;
//    public static final int DOWNLOAD_OK = 21;
//    public static final int STOP_UPLOAD_FILE = 16;
//    public static final int DELETE_FILE = 17;
//    public static final int UPLOAD_PICTURE = 18;

//    public static final int GET_START = 23;
//    public static final int GET_END = 24;

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


    private static HandlerThread mBackgroundThread;
    private static Handler mBackgroundHandler;
//    private ArrayList<String> fileList;
    private ArrayList<String> filePathList;
    private BaseAdapter baseAdapter;
    private ListView listView;
    private TextView downloadSizeTextView;
    private Button backButton;
    private long downloadSize = 0;

//    BlockingDeque<byte[]> dataQueue;
    ByteBuffer byteBuffer = ByteBuffer.allocate(1024 * 512);

    private Thread decodeDataThread;
    private boolean decodeDataState;
    private Handler handler;
    private String fileName;
    private String filePath;

    private FileManager fileManager;
    private PictureFragment pictureFragment;
    private ImageView pictureIcon;

//    private Point scrollPoint;
    private ArrayList<Point> scrollPointList;
    private boolean scrollState;
//    private BlockingDeque<byte[]> dataQueue;

//    private String fileSum;

    private static class FileInfo {
        private byte[] name;
        private byte[] date;
        private byte[] size;
        private byte[] icon = null;
//        private byte[] buff;
        private int type = 0;
        private int offset,length;
        public FileInfo(byte[] data,int offs,int len) {
            this.offset = offs;
            this.length = len + offs;
//            buff = new byte[len];
            type = data[offset++];
            for(int i=offset;i<length;i++) {
                if(data[i] == '\n') {
                    name = new byte[i - offset];
                    System.arraycopy(data,offset,name,0,name.length);
                    offset = i + 1;
//                    Log.e(TAG, "FileInfo: name " + new String(name,StandardCharsets.UTF_8) );
                    break;
                }
            }
            for(int i=offset;i<length;i++) {
                if(data[i] == '\n') {
                    date = new byte[i - offset];
                    System.arraycopy(data, offset, date, 0, date.length);
                    offset = i + 1;
//                    Log.e(TAG, "FileInfo: date " + new String(date, StandardCharsets.UTF_8));
                    break;
                }
            }
            for(int i=offset;i<length;i++) {
                if(data[i] == '\n') {
                    size = new byte[i - offset];
                    System.arraycopy(data,offset,size,0,size.length);
                    offset = i + 1;
                    break;
                }
            }
            if(offset < length) {
                icon = data;
//                icon = new byte[length - offset];
//                System.arraycopy(data, offset, icon, 0, icon.length);
            }
        }
        public int getType() {
            return type;
        }
        public String getName() {
            if(name != null)
                return new String(name,StandardCharsets.UTF_8);
            return " ";
        }
        public String getDate() {
            if(date != null)
                return new String(date,StandardCharsets.UTF_8);
            return " ";
        }
        public String getSize() {
            if(size != null)
                return new String(size,StandardCharsets.UTF_8);
            return " ";
        }
        public Bitmap getIcon() {
            if(icon != null) {
//                Log.e(TAG, "getIcon: icon " + icon.length);
//                Log.e(TAG, "getIcon: icon " + Arrays.toString(icon) );
                return BitmapFactory.decodeByteArray(icon, offset, length - offset);
            }
            return null;
        }
    }

    private void setFileTypeIcon(int type, ImageView imageView) {
        switch (type) {
            case DIR_TYPE:
                imageView.setImageResource(R.drawable.folder);
                break;
            case PICTURE_TYPE:
                imageView.setImageResource(R.drawable.tupian);
                break;
            case VIDEO_TYPE:
                imageView.setImageResource(R.drawable.video);
                break;
            case AUDIO_TYPE:
                imageView.setImageResource(R.drawable.music);
                break;
            case ZIP_TYPE:
                imageView.setImageResource(R.drawable.zip);
                break;
            case TEXT_TYPE:
                imageView.setImageResource(R.drawable.txt);
                break;
            case PPT_TYPE:
                imageView.setImageResource(R.drawable.ppt);
                break;
            case WORD_TYPE:
                imageView.setImageResource(R.drawable.word);
                break;
            case WPS_TYPE:
                imageView.setImageResource(R.drawable.wps);
                break;
            case PDF_TYPE:
                imageView.setImageResource(R.drawable.pdf);
                break;
            case UNKNOWN_TYPE:
                imageView.setImageResource(R.drawable.unknown);
                break;
            case EXCEL_TYPE:
                imageView.setImageResource(R.drawable.excel);
                break;
            case APK_TYPE:
                imageView.setImageResource(R.drawable.apk);
                break;
        }
    }

//    private void sendMsg(int msg) {
//        if(handler != null) {
//            Message message = new Message();
//            message.what = msg;
//            handler.sendMessage(message);
//        }
//    }

    private void sendMsg(int msg) {
        if(handler != null) {
            handler.sendEmptyMessage(msg);
        }
    }
    private void sendMsg(int msg,Object obj) {
        if(handler != null) {
            Message message = new Message();
            message.what = msg;
            message.obj = obj;
            handler.sendMessage(message);
        }
    }
//    private void bgSendMsg(int msg) {
//        if(mBackgroundHandler != null) {
//            mBackgroundHandler.sendEmptyMessage(msg);
//        }
//    }
//    private void bgSendMsg(int msg,Object obj) {
//        if(mBackgroundHandler != null) {
//            Message message = new Message();
//            message.what = msg;
//            message.obj = obj;
//            mBackgroundHandler.sendMessage(message);
//        }
//    }

//    public void sendMsg(int msg) {
//        if(mBackgroundHandler != null) {
//            mBackgroundHandler.sendEmptyMessage(msg);
//        }
//    }

//    private void startBackgroundThread() {
//        mBackgroundThread = new HandlerThread("SocketFunctions");
//        mBackgroundThread.start();
//        mBackgroundHandler = new Handler(mBackgroundThread.getLooper()) {
//            @Override
//            public void handleMessage(@NonNull Message message) {
////                if(message.what == UPDATE_FILE_SUM) {
////                    fileList.add((FileInfo) message.obj);
////                    baseAdapter.notifyDataSetChanged();
////                } else
//                if(message.what == GET_LISTFILE) {
//                    getFiles((String) message.obj);
//                } else if(message.what == DOWNLOAD_FILE) {
//                    downloadFile((String) message.obj);
//                }
//
//            }
//        };
//    }

//    private void stopBackgroundThread() {
//        if(mBackgroundThread != null) {
//            mBackgroundThread.quitSafely();
//            try {
//                mBackgroundThread.join();
//                mBackgroundThread = null;
//                mBackgroundHandler = null;
//                Log.e(TAG, "openCamera: 停止线程");
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//    }

//    private View download_file_layout;
//    private View view_picture_layout;

    @SuppressLint("DefaultLocale")
    private String getFileSize(long size) {
        String file_size;
        if (size >= 1073741820) {
            file_size = String.format("%.2f GB", (double) size / 1073741820.0);
        } else if (size >= 1048576) {
            file_size = String.format("%.2f MB", (double) size / 1048576.0);
        } else if (size >= 1024) {
            file_size = String.format("%.2f KB", (double) size / 1024.0);
        } else {
            file_size = size + " B";
        }
//        Log.e(TAG, "handleMessage: " + file_size );
        return file_size;
    }

    @SuppressLint("InflateParams")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file);
        TextView title = findViewById(R.id.title);
        title.setText("文 件");

        pictureFragment = new PictureFragment();

        filePathList = new ArrayList<>();
//        filePathList.add("storage/emulated/0/");

        TextView path_text = findViewById(R.id.path_text);
        TextView file_text = findViewById(R.id.file_text);

//        startBackgroundThread();
//        decodeData();

        handler = new Handler(Looper.getMainLooper()) {
            @SuppressLint("SetTextI18n")
            @Override
            public void handleMessage(@NonNull Message message) {
                 if(message.what == FileManager.UPDATE_FILE_SUM) {
                     filePathList = (ArrayList<String>) fileManager.getFileList().clone();
                     baseAdapter.notifyDataSetChanged();
                     file_text.setText("文件数: " + filePathList.size());
                     return ;
                }
                 switch (message.what) {
                     case FileManager.SCROLL_LIST:
                         if(scrollState) {
                             int p = scrollPointList.size();
                             if (p > 0) {
                                 Point point = scrollPointList.remove(p - 1);
                                 listView.setSelectionFromTop(point.x, point.y);
                                 Log.e(TAG, "onClick: 返回" + point);
                             }
                             scrollState = false;
                         }
                         break;
                     case FileManager.REFRESH_FILE_LIST:
                         filePathList.clear();
                         baseAdapter.notifyDataSetChanged();
                         path_text.setText(fileManager.getFilePath());
                         file_text.setText("文件数: " + filePathList.size());
                         break;
                     case FileManager.REFRESH_DOWNLOAD_SIZE:
                         if(PictureFragment.pictureState) {
                             pictureFragment.setLoadSize(getFileSize(fileManager.getDownloadSize()) + " / " + getFileSize(fileManager.getFileSize()));
                         } else {
                             downloadSizeTextView.setText("已下载: " + getFileSize(fileManager.getDownloadSize()));
                         }
                         break;
                     case FileManager.DOWNLOAD_OK:
//                         PictureFragment.setImageBitmap(fileManager.getPicture());
                         if(PictureFragment.pictureState) {
                             pictureFragment.setImagePath(fileManager.getPicturePath());
                             Bitmap bitmap = BitmapFactory.decodeFile(fileManager.getPicturePath());
                             if(bitmap != null) {
                                 pictureIcon.setImageBitmap(bitmap);
                             }
                         } else {
                             downloadSizeTextView.setText("下载完成");
                         }
//                         pictureImageView.setImageBitmap(fileManager.getPicture());
                         break;
                     case FileManager.DOWNLOAD_ERROR:
                         break;
                     case FileManager.GET_START:
                         backButton.setText("加载中...(点击取消)");
                         break;
                     case FileManager.GET_END:
                         backButton.setText("返回上一级");
                         break;
//                     case FileManager.GET_PICTURE_OK:
////                         showPictureUI();
//                         break;
                 }
            }
        };

        fileManager = new FileManager(handler);
//        fileList = fileManager.getFileList();

        backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(!fileManager.goBackPath()) {
                    scrollState = true;
                }
//                goBackPath();
            }
        });

        baseAdapter = new BaseAdapter() {

            @Override
            public int getCount() {
                return filePathList.size();
            }

            @Override
            public Object getItem(int position) {
                return position;
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @SuppressLint("SetTextI18n")
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    LayoutInflater layoutInflater = LayoutInflater.from(FileActivity.this);
                    convertView = layoutInflater.inflate(R.layout.file_item_layout, null);
//                    convertView = file_item_layout;

                    convertView.setTag(convertView);
                } else {
                    convertView = (View) convertView.getTag();
                }

                ImageView imageView = convertView.findViewById(R.id.imageView);
                TextView type_text = convertView.findViewById(R.id.type_text);
                int start = 1,end;

                if(filePathList.isEmpty()) {
                    return convertView;
                }
                String fileInfo = filePathList.get(position);
                int type = fileInfo.codePointAt(0);
                type_text.setText(type + "");
//                Bitmap bitmap = fileInfo.getIcon();
//                if(type == DIR_TYPE) {
//                    imageView.setImageResource(R.drawable.folder);
////                    imageView.setTag(R.drawable.folder);
//                } else {
//                    imageView.setTag(R.drawable.unknown);
//                }
//                else {
//                    imageView.setImageResource(R.drawable.folder1);
//                    imageView.setTag(R.drawable.folder1);
//                }
                end = fileInfo.indexOf('\n',start);
                String name = fileInfo.substring(start,end);
                File file = new File(FileManager.PICTURE_PATH,name);
                if(file.exists() && type == PICTURE_TYPE) {
                    Bitmap bitmap = BitmapFactory.decodeFile(FileManager.PICTURE_PATH + name);
                    if(bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                    }
                } else {
                    setFileTypeIcon(type,imageView);
                }

                TextView file_name = convertView.findViewById(R.id.file_name);
                file_name.setText(name);
//                String name = fileList.get(position).substring(start,end);
                start = end + 1;
//
                end = fileInfo.indexOf('\n',start);
                TextView file_date = convertView.findViewById(R.id.file_date);
                file_date.setText(fileInfo.substring(start,end));
//                String date = fileList.get(position).substring(start,end);
                start = end + 1;
//
//                String size = fileList.get(position).substring(start);
                TextView file_size = convertView.findViewById(R.id.file_size);
                file_size.setText(fileInfo.substring(start));

                return convertView;
            }

        };

        listView = findViewById(R.id.listView);
//        scrollPoint = new Point();
        scrollPointList = new ArrayList<>();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @SuppressLint("ResourceType")
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                pictureIcon = view.findViewById(R.id.imageView);
                TextView file_name = view.findViewById(R.id.file_name);
                TextView file_size = view.findViewById(R.id.file_size);
                TextView type_text = view.findViewById(R.id.type_text);
                String fileName = (String) file_name.getText();
                String type = (String) type_text.getText();
                if(type.equals(DIR_TYPE + "")) {
                    fileManager.sendMsg(FileManager.GET_FILE_LIST,fileName);
                    Point point = new Point();
                    point.x = listView.getFirstVisiblePosition();
                    point.y = listView.getChildAt(0).getTop();
                    scrollPointList.add(point);
                    Log.e(TAG, "onScrollStateChanged: "+ point );
//                    bgSendMsg(GET_LISTFILE,fileName);
                    Log.e(TAG, "onItemClick: 文件夹" );
//                    downloadFile();
                } else if(type.equals(PICTURE_TYPE + "")) {
                    showPictureUI(fileName);
//                    file_name.setTextColor(0XFFEEEEEE);
                    Log.e(TAG, "onItemClick: 图片" );
                } else {
                    downloadFileUI(fileName,(String) file_size.getText());
                }
                Log.e(TAG, "onItemClick: " + fileName);
//                Intent intent = new Intent();
//                intent.setClass(FileActivity.this,ControlActivity.class);
//                startActivity(intent);
//                SocketServer.CLIENT_ID = (byte) clientList.get(position).codePointAt(0);
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                TextView type_text = view.findViewById(R.id.type_text);
                TextView file_size = view.findViewById(R.id.file_size);
                TextView file_name = view.findViewById(R.id.file_name);
                String type = (String) type_text.getText();
                if(!type.equals(DIR_TYPE + "")) {
                    downloadFileUI((String) file_name.getText(),(String) file_size.getText());
                }
                return true;
            }
        });

//        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
//            @Override
//            public void onScrollStateChanged(AbsListView absListView, int scrollState) {
//                if(scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
//                    scrollPoint.x = absListView.getFirstVisiblePosition();
//                    scrollPoint.y = absListView.getChildAt(0).getTop();
//                    Log.e(TAG, "onScrollStateChanged: "+ scrollPoint );
//                }
//            }
//
//            @Override
//            public void onScroll(AbsListView absListView, int i, int i1, int i2) {
//
//            }
//        });

        listView.setAdapter(baseAdapter);

        fileManager.sendMsg(FileManager.GET_FILE_LIST,null);
//        bgSendMsg(GET_LISTFILE,null);

//        handler.sendEmptyMessage(GET_LISTFILE);
//        getFiles();
//        byte[] msg = {SocketServer.CLIENT_ID,GET_LISTFILE,SocketServer.EOF};
//        SocketServer.sendData();
    }

    private boolean downloadFileUIState;

    @SuppressLint("SetTextI18n")
    private void downloadFileUI(String  fileName, String fileSize) {
        downloadFileUIState = true;
        final LayoutInflater inflater = LayoutInflater.from(FileActivity.this);
        final View view = inflater.inflate(R.layout.download_file_layout, null);
//        View view = download_file_layout;
        TextView file_size_text = view.findViewById(R.id.file_size_text);
        file_size_text.setText("大小: " + fileSize);
        TextView file_name_text = view.findViewById(R.id.file_name_text);
        file_name_text.setText("文件: " + fileName);
        downloadSizeTextView = view.findViewById(R.id.download_size_text);

        final AlertDialog dialog_main = new AlertDialog.Builder(FileActivity.this).setCancelable(false).setView(view).create();
        dialog_main.show();

        Button cancelButton = view.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FileManager.stopWriteFile();
//                stopUploadFile();
//                downloadFileState = false;
                dialog_main.dismiss();
            }
        });

        Button downloadButton = view.findViewById(R.id.download_button);
        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fileManager.sendMsg(FileManager.DOWNLOAD_FILE,fileName);
//                bgSendMsg(DOWNLOAD_FILE,fileName);
//                downloadFile(fileName);
            }
        });

        Button deleteButton = view.findViewById(R.id.delete_button);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder dialog=new AlertDialog.Builder(FileActivity.this);
                dialog.setTitle("是否删除?");
                dialog.setMessage(fileName);
                dialog.setPositiveButton("删除", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int arg1) {
                        // TODO Auto-generated method stub
                        Point point = new Point();
                        point.x = listView.getFirstVisiblePosition();
                        point.y = listView.getChildAt(0).getTop();
                        scrollPointList.add(point);
                        scrollState = true;
                        fileManager.sendMsg(FileManager.DELETE_FILE,fileName);
                        fileManager.sendMsg(FileManager.GET_FILE_LIST,null);
                        dialog.dismiss();
                        dialog_main.dismiss();
//                        info.setText("确认退出");
                    }

                });
                dialog.setNegativeButton("取消", new DialogInterface.OnClickListener(){

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                        dialog.dismiss();
//                        info.setText("取消");
                    }

                });
                dialog.create().show();

//                fileManager.sendMsg(FileManager.DELETE_FILE,fileName);
//                dialog_main.dismiss();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.e(TAG, "onStop: 关闭" );
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        decodeDataState = false;
        if(decodeDataThread != null) {
            decodeDataThread = null;
        }
        if(handler != null) {
            handler = null;
        }
        if(fileManager != null) {
            fileManager.closeFileManager();
            fileManager = null;
        }
        if(scrollPointList != null) {
            scrollPointList.clear();
            scrollPointList = null;
        }
        pictureFragment = null;
        Log.e(TAG, "onDestroy: 销毁" );
    }

    private void getPicture() {

        String path = filePath + fileName + '\n';
    }

    private boolean showPictureUIState;
    private ImageView pictureImageView;


    private void showPictureUI(String fileName) {
//        Intent intent = new Intent();
//        intent.setClass(FileActivity.this,PictureViewActivity.class);
//        startActivity(intent);
//        fragmentManager = getFragmentManager();
        PictureFragment.pictureState = true;
        fileManager.sendMsg(FileManager.GET_PICTURE,fileName);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, pictureFragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();


//        final AlertDialog dialog_main = new AlertDialog.Builder(FileActivity.this).setCancelable(false).create();
//        dialog_main.show();

//        final LayoutInflater inflater = LayoutInflater.from(FileActivity.this);
//        final View view = inflater.inflate(R.layout.view_picture_layout, null);
//        final AlertDialog dialog_main = new AlertDialog.Builder(FileActivity.this).setCancelable(false).setView(view).create();
//        dialog_main.show();
//        pictureImageView = view.findViewById(R.id.image);
////        byte[] buff = fileManager.getPictureBuffer().array()
//        Log.e(TAG, "showPicture: " + buff.length );
//        Bitmap bitmap = BitmapFactory.decodeByteArray(buff,0,buff.length);
//        imageView.setImageBitmap(bitmap);
////        imageView.setImageResource(R.drawable.video);
//        Log.e(TAG, "showPicture:  显示图片" );
    }
//
//    private void decodeData(BlockingDeque<byte[]> dataQueue) {
////        if(decodeDataThread != null) {
////            return ;
////        }
//        if(decodeDataState) {
//            return ;
//        }
//        byteBuffer.clear();
//
//        decodeDataState = true;
//        byte[] head = new byte[4];
//        sendMsg(GET_START);
//        int length = 0;
//        while(decodeDataState) {
//            byte[] data = dataQueue.poll();
//            if(data == null) {
//                continue;
//            }
//            int len = (data[0] << 24) | ((data[1] << 16) & 0XFFFFFF) | ((data[2] << 8) & 0XFFFF) | (data[3] & 0XFF);
//            byteBuffer.put(data, 4, len);
//            byteBuffer.flip();
//            while (byteBuffer.limit() - byteBuffer.position() >= 4) {
//                byteBuffer.get(head);
//                if ((head[0] & 0XFF) == 0XFF) {
//                    length = (((head[1] << 16) & 0XFFFFFF) | ((head[2] << 8) & 0XFFFF) | (head[3] & 0XFF)) - 4;
//                    if (length <= 0 || length >= 10000) {
//                        decodeDataState = false;
//                        break;
//                    } else if (length <= byteBuffer.limit() - byteBuffer.position()) {
//                        byte[] buff = new byte[length];
//                        System.arraycopy(byteBuffer.array(),byteBuffer.position(),buff,0,length);
//                        sendMsg(UPDATE_FILE_SUM, new String(buff,StandardCharsets.UTF_8));
////                                fileList.add(new String(buff,StandardCharsets.UTF_8));
//                        byteBuffer.position(byteBuffer.position() + length);
//                    } else {
//                        byteBuffer.position(byteBuffer.position() - 4);
//                        Log.e(TAG, "run: 长度不够" );
//                        break;
//                    }
//                } else {
//                    byteBuffer.position(byteBuffer.position() - 3);
//                    Log.e(TAG, "run: 没有找到" );
//                }
//            }
//            if(length > 0) {
//                Log.e(TAG, "run: 重置" );
//                byteBuffer.compact();
//            }
//        }
//        SocketServer.clearReadBlockingDeque();
//        sendMsg(GET_END);
//        decodeDataState = false;
//
////        decodeDataThread = new Thread(new Runnable() {
////            @Override
////            public void run() {
////
////                decodeDataThread = null;
////            }
////        });
//        decodeDataThread.start();
//    }
//
//    private void getFiles(String fileName) {
//        if(fileName != null) {
//            filePathList.add(fileName + "/");
//        }
//        if(!decodeDataState)
//            sendMsg(REFRESH_FILE);
//        StringBuilder path = new StringBuilder();
//        path.append((char) SocketServer.CLIENT_ID);
//        path.append((char) GET_LISTFILE);
//        path.append(filePathList.get(0));
//        for(int i=1;i<filePathList.size();i++) {
//            path.append(filePathList.get(i));
//        }
//        filePath = path.substring(2);
//        path.append('\n');
//        byte[] pakg = path.toString().getBytes();
//        BlockingDeque<byte[]> dataQueue = SocketServer.getReadBlockingDeque();
//        if(SocketServer.sendDataFlush(pakg)) {
//            if(decodeDataState) {
//                return ;
//            }
//            decodeDataState = true;
//            byteBuffer.clear();
//            byte[] head = new byte[4];
//            sendMsg(GET_START);
//            int length = 0;
//            while(decodeDataState) {
//                byte[] data = dataQueue.poll();
//                if(data == null) {
//                    continue;
//                }
//                int len = (data[0] << 24) | ((data[1] << 16) & 0XFFFFFF) | ((data[2] << 8) & 0XFFFF) | (data[3] & 0XFF);
//                byteBuffer.put(data, 4, len);
//                byteBuffer.flip();
//                while (byteBuffer.limit() - byteBuffer.position() >= 4) {
//                    byteBuffer.get(head);
//                    if ((head[0] & 0XFF) == 0XFF) {
//                        length = (((head[1] << 16) & 0XFFFFFF) | ((head[2] << 8) & 0XFFFF) | (head[3] & 0XFF)) - 4;
//                        if (length <= 0 || length >= 10000) {
//                            decodeDataState = false;
//                            break;
//                        } else if (length <= byteBuffer.limit() - byteBuffer.position()) {
//                            byte[] buff = new byte[length];
//                            System.arraycopy(byteBuffer.array(),byteBuffer.position(),buff,0,length);
//                            sendMsg(UPDATE_FILE_SUM, new String(buff,StandardCharsets.UTF_8));
////                                fileList.add(new String(buff,StandardCharsets.UTF_8));
//                            byteBuffer.position(byteBuffer.position() + length);
//                        } else {
//                            byteBuffer.position(byteBuffer.position() - 4);
//                            Log.e(TAG, "run: 长度不够" );
//                            break;
//                        }
//                    } else {
//                        byteBuffer.position(byteBuffer.position() - 3);
//                        Log.e(TAG, "run: 没有找到" );
//                    }
//                }
//                if(length > 0) {
//                    Log.e(TAG, "run: 重置" );
//                    byteBuffer.compact();
//                }
//            }
//            SocketServer.clearReadBlockingDeque();
//            sendMsg(GET_END);
//            decodeDataState = false;
//        }
//    }

//    private void goBackPath() {
//        if(filePathList.size() > 1 && !decodeDataState) {
//            filePathList.remove(filePathList.size() - 1);
//        }
//        bgSendMsg(GET_LISTFILE,null);
//    }

//    private Thread deleteThread;
//
//    private void deleteFile() {
//        if(deleteThread != null || fileName == null) {
//            return ;
//        }
//        deleteThread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                String path  = '\n' + '\n' + filePath + fileName + '\n';
////                StringBuilder path = new StringBuilder(filePathList.get(0));
////                for(int i=1;i<filePathList.size();i++) {
////                    path.append(filePathList.get(i));
////                }
////                path.append(fileName);
////                path.append('\n');
//                byte[] pakg = path.getBytes();
////                byte[] pakg = new byte[data.length + 2];
//                pakg[0] = SocketServer.CLIENT_ID;
//                pakg[1] = DELETE_FILE;
//
//                if(SocketServer.sendDataFlush(pakg)) {
//                    try {
//                        Thread.sleep(200);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
////                System.arraycopy(pakg,0,pakg,2,data.length);
////                try {
////                    SocketServer.sendData(pakg);
////                } catch (IOException e) {
////                    e.printStackTrace();
////                }
//
////                fileName = null;
//                deleteThread = null;
//            }
//        });
//        deleteThread.start();
//    }

//    private Thread downloadFileThread;
//    private FileOutputStream fileOutputStream;
//    private boolean downloadFileState;
//
//    private void downloadFile(String fileName) {
//        if(downloadFileState || fileName == null) {
//            return ;
//        }
//        String path = '\n' + '\n' + filePath + fileName + '\n';
//        BlockingDeque<byte[]> dataQueue = SocketServer.getReadBlockingDeque();
//        byte[] pakg = path.getBytes(StandardCharsets.UTF_8);
//        pakg[0] = SocketServer.CLIENT_ID;
//        pakg[1] = DOWNLOAD_FILE;
//        Log.e(TAG, "downloadFile: " + Arrays.toString(pakg));
//
//        if(SocketServer.sendDataFlush(pakg)) {
//            downloadFileState = true;
//            File file = new File("storage/emulated/0/setTool/Download/");
//            if(!file.exists()) {
//                if(!file.mkdir()) {
//                    return;
//                }
//            }
//            file = new File("storage/emulated/0/setTool/Download/", fileName);
//            try {
//                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file));
//                long fileSize = 0;
//                int len,offset;
//                downloadSize = 0;
//                Log.e(TAG, "saveFile: 等待下载" );
//                while(downloadFileState) {
//                    byte[] data = dataQueue.poll();
//                    if(data == null) {
//                        continue;
//                    }
//                    offset = 4;
//                    len = (data[0] << 24) | ((data[1] << 16) & 0XFFFFFF) | ((data[2] << 8) & 0XFFFF) | (data[3] & 0XFF);
//                    if(fileSize == 0) {
//                        if((data[4] & 0XFF) != 0XFF)
//                            break;
//                        ByteBuffer byteBuffer = ByteBuffer.allocate(13);
//                        byteBuffer.put(data,5,8);
//                        fileSize = byteBuffer.getLong(0);
//                        offset += 9;
//                        len -= 9;
//                        Log.e(TAG, "saveFile: " + fileSize );
//                    }
//                    try {
//                        bufferedOutputStream.write(data,offset,len);
//                        sendMsg(REFRESH_SIZE);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                        break;
//                    }
//                    downloadSize += len;
//                    if(downloadSize == fileSize) {
//                        sendMsg(DOWNLOAD_OK);
//                        break;
//                    }
//
//                }
//                Log.e(TAG, "saveFile: " + downloadSize );
//                try {
//                    bufferedOutputStream.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                downloadFileState = false;
//                SocketServer.clearReadBlockingDeque();
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            }
//        }
//        downloadFileState = true;

//        File file = new File("storage/emulated/0/setTool/Download/");
//        if(!file.exists()) {
//            if(!file.mkdir()) {
//                return;
//            }
//        }
//        file = new File("storage/emulated/0/setTool/Download/", fileName);
//        try {
//            fileOutputStream = new FileOutputStream(file);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//            return ;
//        }
////        downloadFileState = false;
//        SocketServer.clearReadBlockingDeque();
//        downloadFileThread = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                saveFile(dataQueue);
//                byte[] data = path.toString().getBytes();
//                byte[] pakg = new byte[data.length + 2];
//                pakg[0] = SocketServer.CLIENT_ID;
//                pakg[1] = DOWNLOAD_FILE;
//                System.arraycopy(data,0,pakg,2,data.length);
//                try {
//                    SocketServer.sendData(pakg);
//                    downloadFileState = true;
//                    dataQueue = SocketServer.getReadBlockingDeque();
//                    int len = 0,off;
//                    long fileSize = 0;
//                    downloadSize = 0;
//                    while(downloadFileState) {
//                        off = 0;
//                        data = dataQueue.poll();
//                        if(data == null)
//                            continue;
////                        try {
////                            data = dataQueue.take();
//////                            Log.e(TAG, "run: " + dataQueue.size() );
////                        } catch (InterruptedException e) {
////                            e.printStackTrace();
////                            break;
////                        }
//                        len = data.length;
//                        if(fileSize == 0 && (data[0] & 0XFF) == 0XFF) {
//                            ByteBuffer byteBuffer =  ByteBuffer.allocate(8);
//                            byteBuffer.put(data,1,8);
//                            fileSize = byteBuffer.getLong(0);
//                            if(fileSize <= 0)
//                                break;
//                            off = 9;
//                            len -= 9;
//                            Log.e(TAG, "fileSize: " + fileSize );
//                        }
////                        if((data[len - 1] == 'F') && (data[len - 2] == 'O') && (data[len - 3] == 'E')) {
////                            len -= 3;
////                            downloadFileState = false;
////                            if(len <= 0) {
////                                break;
////                            }
////                        }
//                        try {
//                            fileOutputStream.write(data,off,len);
//                            fileOutputStream.flush();
//                            sendMsg(REFRESH_SIZE);
////                            handler.sendEmptyMessage(REFRESH_SIZE);
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                            break;
//                        }
//                        downloadSize += len;
//
//                        if(downloadSize == fileSize) {
//                            break;
//                        }
//                    }
//                    sendMsg(DOWNLOAD_OK);
////                    handler.sendEmptyMessage(DOWNLOAD_OK);
//                    Log.e(TAG, "size: " + downloadSize );
//                    Log.e(TAG, "下载完成 " );
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                try {
//                    fileOutputStream.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                downloadFileThread = null;
//                downloadFileState = false;
//                SocketServer.clearReadBlockingDeque();
//            }
//        });
//        downloadFileThread.start();
////        fileName = null;
//    }

//    private void saveFile() {
////            DataInputStream dataInputStream = SocketServer.getDataInputStream();
////            if(dataInputStream == null)
////                return;
////            byte head = dataInputStream.readByte();
////            if((head & 0XFF) != 0XFF)
////                return ;
//            File file = new File("storage/emulated/0/setTool/Download/");
//            if(!file.exists()) {
//                if(!file.mkdir()) {
//                    return;
//                }
//            }
//            file = new File("storage/emulated/0/setTool/Download/", fileName);
//            try {
//                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file));
//
////                FileOutputStream fileOutputStream = new FileOutputStream(file);
//                long fileSize = 0;
//                int len,offset;
////                byte[] buff = new byte[1024 * 8];
//                downloadSize = 0;
//                Log.e(TAG, "saveFile: 等待下载" );
//                while(downloadFileState) {
//                    byte[] data = dataQueue.poll();
//                    if(data == null) {
//                        continue;
//                    }
//                    offset = 4;
//                    len = (data[0] << 24) | ((data[1] << 16) & 0XFFFFFF) | ((data[2] << 8) & 0XFFFF) | (data[3] & 0XFF);
////                    Log.e(TAG, "saveFile: " + len );
//                    if(fileSize == 0) {
//                        if((data[4] & 0XFF) != 0XFF)
//                            break;
//                        ByteBuffer byteBuffer = ByteBuffer.allocate(13);
//                        byteBuffer.put(data,5,8);
//                        fileSize = byteBuffer.getLong(0);
//                        offset += 9;
//                        len -= 9;
//                        Log.e(TAG, "saveFile: " + fileSize );
//                    }
////                    len = SocketServer.inputStream.read(buff);
////                    dataInputStream.readFully(buff);
////                    len = buff.length;
////                    13751296
////                    13750680
//                    try {
//                        bufferedOutputStream.write(data,offset,len);
//                        sendMsg(REFRESH_SIZE);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                        break;
//                    }
//                    downloadSize += len;
//                    if(downloadSize == fileSize) {
//                        sendMsg(DOWNLOAD_OK);
//                        break;
//                    }
////                    Log.e(TAG, "saveFile: " + downloadSize );
//
//                }
//                                    Log.e(TAG, "saveFile: " + downloadSize );
//                try {
//                    bufferedOutputStream.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                downloadFileState = false;
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            }
//    }

//    private void stopUploadFile() {
//        if(downloadFileState) {
//            byte[] pakg = {SocketServer.CLIENT_ID, STOP_UPLOAD_FILE, SocketServer.EOF};
//            if(SocketServer.sendCtrlMsg(pakg)) {
//                downloadFileState = false;
//            }
////            SocketServer.clearReadBlockingDeque();
////            new Thread(new Runnable() {
////                @Override
////                public void run() {
////                    byte[] pakg = {SocketServer.CLIENT_ID, STOP_UPLOAD_FILE, SocketServer.EOF};
////                    try {
////                        SocketServer.sendData(pakg);
////                    } catch (IOException e) {
////                        e.printStackTrace();
////                    }
////                }
////            }).start();
//        }
//    }


}