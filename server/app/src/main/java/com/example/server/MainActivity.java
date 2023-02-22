package com.example.server;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
//import android.support.v4.content.ContextCompat;

import android.text.Layout;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.BlockingDeque;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "MainActivity";
    public static final int CLIENT_NULL = 29;
    public static final int PRINT_CLIENT = 30;
    public static final int GET_CLIENT = 31;
    public static final int CONNECT_ERROR = 32;
    public static final int CONNECT_WAIT = 33;
    public static final int GET_CLIENT_ERROR = 34;
    public static final byte[] REQ_CLIENT = {(byte) 0X11};
    public static String MAIN_PATH = "storage/emulated/0/setTool/";
    public static String MODEL;
    private ArrayList<String> clientList;
    private ListView listView;
    private BaseAdapter baseAdapter;
    private Thread getClientThread = null;
    private HandlerThread handlerThread;
    public static Handler handler;
    public TextView title;
//    private boolean getClientState = false;

    public static int clientWidth;
    public static int clientHeight;

    public static void sendMsg(int msg) {
        if(handler != null) {
            handler.sendEmptyMessage(msg);
//            Message message = new Message();
//            message.what = msg;
//            handler.sendMessage(message);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {//第一次打开软件可以进来
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "请授予存储权限", Toast.LENGTH_LONG).show();
            finish();
        } else {
            File file = new File(MAIN_PATH);
            if(!file.exists()) {
                if(!file.mkdir()) {
                    Toast.makeText(this, "目录创建失败", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.activity_main);
//        TextView titleText = (TextView) findViewById(R.id);
//        view.setText("123");
//        view.setGravity(Gravity.CENTER);
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        View view = findViewById(R.id.include);
        title = view.findViewById(R.id.title);

        clientList = new ArrayList<String>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
            }
        }
//        if(!ForegroundService.ForegroundServiceState) {
//            Intent serviceIntent = new Intent(this, ForegroundService.class);
//            serviceIntent.putExtra("input", "服务正在运行");
//            ContextCompat.startForegroundService(this, serviceIntent);
//        }
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message message) {
                switch (message.what) {
                    case CLIENT_NULL:
                        title.setText("没有可用客户端");
                        break;
                    case PRINT_CLIENT:
                        baseAdapter.notifyDataSetChanged();
                        title.setText("可用客户端");
//                        SocketServer.readData();
                        break;
                    case GET_CLIENT:
                        title.setText("正在获取客户端...");
                        get_client();
                        break;
                    case CONNECT_WAIT:
                        title.setText("正在连接服务器...");
                        break;
                    case CONNECT_ERROR:
                        title.setText("服务器连接失败");
                        break;
                    case GET_CLIENT_ERROR:
                        title.setText("客户端获取失败!");
                        break;
                }
            }
        };

        baseAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return clientList.size();
            }

            @Override
            public Object getItem(int position) {
                return position;
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
                    convertView = layoutInflater.inflate(R.layout.client_item_layout, null);
                    int start = 1,end;
                    String clientInfo = clientList.get(position);
                    TextView id = convertView.findViewById(R.id.id);
                    id.setText(clientInfo.codePointAt(0) + "");
                    TextView brand = convertView.findViewById(R.id.brand);
                    end = clientInfo.indexOf('\r',start);
                    brand.setText(clientInfo.substring(1,end));
                    start = end + 1;
                    end = clientInfo.indexOf('\r',start);
                    TextView model = convertView.findViewById(R.id.model);
                    model.setText(clientInfo.substring(start,end));
                    start = end + 1;
                    end = clientInfo.indexOf('\r',start);
                    TextView version = convertView.findViewById(R.id.version);
                    version.setText(clientInfo.substring(start,end));

                    TextView ipPost = convertView.findViewById(R.id.ipPost);
                    ipPost.setText(clientList.get(position).substring(end + 1));
                    convertView.setTag(convertView);
                } else {
                    return (View) convertView.getTag();
                }
                return convertView;
            }
        };

        listView = findViewById(R.id.listView);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.e(TAG, "onItemClick: " + position);
                Intent intent = new Intent();
                intent.setClass(MainActivity.this,ControlActivity.class);
                startActivity(intent);
                SocketServer.CLIENT_ID = (byte) clientList.get(position).codePointAt(0);
                TextView model = view.findViewById(R.id.model);
                MODEL = model.getText().toString();
                File file = new File(MAIN_PATH + MODEL);
                if(!file.exists()) {
                    if(!file.mkdir()) {
                        Toast.makeText(MainActivity.this, "目录创建失败", Toast.LENGTH_LONG).show();
                    }
                }
                Log.e(TAG, "onItemClick: " + MAIN_PATH);
            }
        });
        Button res_button = findViewById(R.id.res_button);
        res_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMsg(GET_CLIENT);
//                get_client();
            }
        });
        listView.setAdapter(baseAdapter);

//        get_client();
    }

    private boolean permissionStatus = false;
    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent();
        intent.setClass(MainActivity.this,MyService.class);
        startService(intent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!ForegroundService.ForegroundServiceState) {
                Intent serviceIntent = new Intent(this, ForegroundService.class);
                startForegroundService(serviceIntent);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 适配android11读写权限
            if (!Environment.isExternalStorageManager()) {
                //已获取android读写权限
                if(permissionStatus) {
                    Toast.makeText(this, "请授予所有文件访问权限", Toast.LENGTH_LONG).show();
                    permissionStatus = false;
                    finish();
                } else {
                    intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + getPackageName()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    permissionStatus = true;
                }
            } else {
                permissionStatus = false;
            }

        }

        File file = new File(MAIN_PATH);
        if(!file.exists()) {
            if(!file.mkdir()) {
                return;
            }
        }
        Log.e(TAG, "onStop: 开启" );
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.e(TAG, "onStop: 关闭" );
    }

    public void get_client() {
        if(getClientThread != null) {
            Log.e(TAG, "get_client: 获取未退出" );
            return ;
        }
        clientList.clear();
        baseAdapter.notifyDataSetChanged();
        getClientThread = new Thread(new Runnable() {
            @Override
            public void run() {
                ByteBuffer byteBuffer = ByteBuffer.allocate(1024 * 64);
                BlockingDeque<byte[]> dataQueue = SocketServer.getReadBlockingDeque();
                if(SocketServer.sendDataFlush(REQ_CLIENT)) {
                    int i = 100;
                    Log.e(TAG, "run: 获取中" );
                    while(i != 0) {
                        byte[] data = dataQueue.poll();
                        if(data == null) {
                            try {
                                Thread.sleep(5);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            i--;
                            continue;
                        }
                        int len = (data[0] << 24) | ((data[1] << 16) & 0XFFFFFF) | ((data[2] << 8) & 0XFFFF) | (data[3] & 0XFF);
                        byteBuffer.put(data, 4, len);
                        int len2 = byteBuffer.getInt(0) - 4;
                        clientWidth = byteBuffer.getInt(4);
                        clientHeight = byteBuffer.getInt(8);
//                        Log.e(TAG, "run: " + len2 );
//                        Log.e(TAG, "run: " + clientWidth + " " + clientHeight );
//                        Log.e(TAG, "run: " + Arrays.toString(byteBuffer.array()));
                        if(len2 == 0) {
                            sendMsg(CLIENT_NULL);
                            break;
                        } else if (len2 == byteBuffer.position() - 4) {
                            len2 -= 8;
                            byte[] buff = new byte[len2];
                            System.arraycopy(byteBuffer.array(), 12, buff, 0, len2);
                            int start = 0, end;
                            Log.e(TAG, "run: " + Arrays.toString(buff));
                            String client = new String(buff, StandardCharsets.UTF_8);
                            Log.e(TAG, "run: " + client );
                            while (start < client.length()) {
                                end = client.indexOf('\n', start);
                                if (end > -1) {
                                    clientList.add(client.substring(start, end));
                                    start = end + 9;
                                } else {
                                    break;
                                }
                            }
                            sendMsg(PRINT_CLIENT);
                            break;
                        }
                    }
                    if(i == 0) {
                        sendMsg(GET_CLIENT_ERROR);
                    }
                } else {
                    MyService.socketServer.disconnect();
                    MyService.socketServer.connect();
                }
                SocketServer.clearReadBlockingDeque();
                getClientThread = null;
                Log.e(TAG, "get_client: 获取结束" );
            }
        });
        getClientThread.start();
    }
}