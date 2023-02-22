package com.example.server;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Scroller;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.BlockingDeque;

public class FaceActivity extends AppCompatActivity {

    private static final String TAG = "FaceActivity";
    private static String FACE_PATH;
    public static final int GET_FACETEXT = 12;
    public static final int STOP_GETFACETEXT = 14;
    public static final int GET_FACEALLTEXT = 15;
//    public static final int TYPE_VIEW_CLICKED = 1;
//    public static final int TYPE_VIEW_TEXT_CHANGED = 2;
//    public static final int TYPE_VIEW_SCROLLED = 3;

    private Thread getFaceTextThread;
    private boolean getFaceTextState;
    private BlockingDeque<byte[]> dataQueue;
    private Handler handler;
//    private ScrollView scrollView;
    private TextView textView;
//    private LinearLayout layout;
//    private FileWriter fileWriter;
    private FileOutputStream fileOutputStream;
    private ByteBuffer byteBuffer;

//    private List<String> clickedText,textChangedText,scrolledText;

    private void sendText(String text) {
//        textView = new TextView(this);
//        textView.setTextColor(Color.BLACK);
//        textView.setText(text);
//        textView.append(text);
        if(handler != null) {
            Message message = new Message();
//            message.what = msg;
            message.obj = text;
            handler.sendMessage(message);
        }
    }

    private int getTextViewHeight(TextView pTextView) {
        Layout layout = pTextView.getLayout();
        int desired = layout.getLineTop(pTextView.getLineCount());
        int padding = pTextView.getCompoundPaddingTop() + pTextView.getCompoundPaddingBottom();
        return desired + padding;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FACE_PATH = MainActivity.MAIN_PATH + MainActivity.MODEL + "/FaceText/";
        File file = new File(FACE_PATH);
        if(!file.exists()) {
            if(!file.mkdir()) {
                return;
            }
        }
        byteBuffer = ByteBuffer.allocateDirect(1024 * 256);

        setContentView(R.layout.activity_face);
        TextView title = findViewById(R.id.title);
        title.setText("界 面");
//        layout = findViewById(R.id.linearLayout);
        textView = findViewById(R.id.textView);
        textView.setMovementMethod(ScrollingMovementMethod.getInstance());


//        scrollView = findViewById(R.id.scrollView);

//        TextView textChangedView = findViewById(R.id.textChangedText);
//        TextView scrolledView = findViewById(R.id.scrolledText);

        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message message) {
                String data = (String)message.obj;
//                Log.e(TAG, "run:str " + data );
                textView.append(data);
//                scrollView.addView(textView);
//                layout = (LinearLayout) scrollView.getChildAt(0);
//                layout.add;
                try {
                    fileOutputStream.write(data.getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                int offset=textView.getLineCount() * (textView.getLineHeight()+6);
////                Log.e(TAG, "handleMessage:行数 " + textView.getLineCount() );
//                Log.e(TAG, "handleMessage:高度 " + textView.getHeight() );
////                Log.e(TAG, "handleMessage:行高 " + textView.getLineHeight() );
//                Log.e(TAG, "handleMessage:偏移量 " + offset );
//                if (offset > textView.getHeight()) {
//                    textView.scrollTo(0, offset - textView.getHeight());
//                }

                int offset = getTextViewHeight(textView) - textView.getHeight();
                if(offset > 0) {
                    textView.scrollTo(0,offset);
                }

//                int offset = textView.getMeasuredHeight() * 5 - scrollView.getMeasuredHeight();
//                if (offset < 0) {
//                    offset = 0;
//                }
//                //scrollview开始滚动
//                scrollView.scrollTo(0, offset);

//                scrollView.fullScroll(ScrollView.FOCUS_DOWN);

//                if(message.what == TYPE_VIEW_CLICKED) {
//                    clickedView.setText((String)message.obj);
//                } else if (message.what == TYPE_VIEW_TEXT_CHANGED) {
//                    textChangedView.append((String)message.obj);
//                } else if (message.what == TYPE_VIEW_SCROLLED) {
//                    scrolledView.append((String)message.obj);
//                }
//                Log.e(TAG, "handleMessage: " + message.what );
                return false;
            }
        });

        Button button = findViewById(R.id.allTextButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getFaceAllText();
            }
        });

        getFaceText();

        TextView textView1 = findViewById(R.id.textView3);
        textView1.setText("正在获取...");
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        getFaceTextState = false;
        if(getFaceTextThread != null) {
            getFaceTextThread = null;
        }
        if(stopGetFaceTextThread != null) {
            stopGetFaceTextThread = null;
        }
        if(byteBuffer != null) {
            byteBuffer.clear();
            byteBuffer = null;
        }
//        if(fileOutputStream != null) {
//            try {
//                fileOutputStream.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            fileOutputStream = null;
//        }
        stopGetFaceText();
        if(handler != null) {
            handler = null;
        }
        Log.e(TAG, "onDestroy: 销毁" );
    }

    private Thread getFaceAllTextThread;
    private void getFaceAllText() {
        if(getFaceAllTextThread != null) {
            return;
        }
        getFaceAllTextThread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] pakg = {SocketServer.CLIENT_ID,GET_FACEALLTEXT,SocketServer.EOF};
                try {
                    SocketServer.sendData(pakg);
                    Log.e(TAG, "run: 开始获取整个界面" );
                    Thread.sleep(200);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                getFaceAllTextThread = null;
            }
        });
        getFaceAllTextThread.start();
    }

    private void getFaceText() {
        if(getFaceTextThread != null) {
            return ;
        }
        getFaceTextThread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] pakg = {SocketServer.CLIENT_ID,GET_FACETEXT,SocketServer.EOF};
                try {
                    SocketServer.sendData(pakg);
                    String time = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss", Locale.CHINESE).format(new Date(System.currentTimeMillis()));
                    File file = new File(FACE_PATH,time + ".txt");
                    try {
                        fileOutputStream = new FileOutputStream(file);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }
                    Log.e(TAG, "run: 开始获取界面" );
                    dataQueue = SocketServer.getReadBlockingDeque();
                    getFaceTextState = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                byte[] data;
                int len = 0,length;
                byte[] head = new byte[4];
//                ByteBuffer byteBuffer = ByteBuffer.allocate(10240);
                while(getFaceTextState) {
                    if((data = dataQueue.poll()) != null) {
                        len = (data[0] << 24) | ((data[1] << 16) & 0XFFFFFF) | ((data[2] << 8) & 0XFFFF) | (data[3] & 0XFF);

                        byteBuffer.put(data, 4, len);
                        byteBuffer.flip();
                        while (byteBuffer.limit() - byteBuffer.position() >= 4) {
                            byteBuffer.get(head);
                            if ((head[0] & 0XFF) == 0XFF) {
                                length = (((head[1] << 16) & 0XFFFFFF) | ((head[2] << 8) & 0XFFFF) | (head[3] & 0XFF)) - 4;
                                if (byteBuffer.limit() - byteBuffer.position() >= length) {
                                    byte[] buff = new byte[length];
                                    byteBuffer.get(buff);
                                    String str = new String(buff, StandardCharsets.UTF_8);
                                    sendText(str);
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
                        Log.e(TAG, "run: 重置" );
                        byteBuffer.compact();
//
//                        if(tempData != null) {
//                            byte[] t = new byte[tempData.length + data.length];
//                            System.arraycopy(tempData,0,t,0,tempData.length);
//                            System.arraycopy(data,0,t,tempData.length,data.length);
//                            data = t;
//                        }
////                        Log.e(TAG, "run:len " + data.length );
////                        byteBuffer.put(data);
////                        byteBuffer.flip();
////                        Log.e(TAG, "run:data " + Arrays.toString(data));
////                        Log.e(TAG, "run:byte " + byteBuffer.toString());
//                        String str = new String(data, StandardCharsets.UTF_8);
//                        while(begin < str.length()) {
//                            end = str.indexOf('\n', begin) + 1;
//                            if(end <= 0) {
//                                tempData = str.substring(begin).getBytes();
////                                tempData = new byte[data.length];
////                                byteBuffer.clear();
////                                byteBuffer.put(str.substring(begin).getBytes());
//                                Log.e(TAG, "run: 没有了");
//                                break;
//                            } else {
////                                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
//                                tempData = null;
//                                String text = str.substring(begin,end);
//                                begin = end ;
//                                sendText(text);
//                            }
//
//                        }
//                        if(end >= 0) {
//                            byteBuffer.clear();
//                        }
                    } else {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                fileOutputStream = null;
                SocketServer.clearReadBlockingDeque();
                getFaceTextThread = null;
            }
        });
        getFaceTextThread.start();
    }


    private Thread stopGetFaceTextThread;
    private void stopGetFaceText() {
        if(stopGetFaceTextThread != null) {
            return ;
        }
        stopGetFaceTextThread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] pakg = {SocketServer.CLIENT_ID,STOP_GETFACETEXT,SocketServer.EOF};
                try {
                    SocketServer.sendData(pakg);
                    Log.e(TAG, "pakg: " + Arrays.toString(pakg) );
                } catch (IOException e) {
                    e.printStackTrace();
                }
                stopGetFaceTextThread = null;
            }
        });
        stopGetFaceTextThread.start();
    }

//    private void printFaceText(String text) {
////        Log.e(TAG, "printFaceText: " + text.codePointAt(0) );
//        if(text.codePointAt(0) == TYPE_VIEW_CLICKED) {
////            clickedText = text.substring(1);
//            sendMsg(TYPE_VIEW_CLICKED,text.substring(1));
//        } else if(text.codePointAt(0) == TYPE_VIEW_TEXT_CHANGED) {
////            textChangedText = text.substring(1);
//            sendMsg(TYPE_VIEW_TEXT_CHANGED,text.substring(1));
//        } else if(text.codePointAt(0) == TYPE_VIEW_SCROLLED) {
////            scrolledText = text.substring(1);
//            sendMsg(TYPE_VIEW_SCROLLED,text.substring(1));
//        }
//    }
}