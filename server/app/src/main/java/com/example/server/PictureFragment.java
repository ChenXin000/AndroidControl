package com.example.server;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Arrays;

public class PictureFragment extends Fragment implements View.OnTouchListener {

    private static final String TAG = "PictureViewActivity";

    private float lastX[] = {0, 0};//用来记录上一次两个触点的横坐标
    private float lastY[] = {0, 0};//用来记录上一次两个触点的纵坐标

    private float windowWidth, windowHeight;//当前窗口的宽和高
    private float imageHeight, imageWidth;//imageview中图片的宽高（注意：不是imageview的宽和高）

    private ImageView imageView;

    private Matrix currentMatrix;//保存当前窗口显示的矩阵
    private Matrix touchMatrix, mMatrix;
    private boolean flag = false;//用来标记是否进行过移动前的首次点击
    private float moveLastX, moveLastY;//进行移动时用来记录上一个触点的坐标

    private float max_scale ;//缩放的最大值
    private float min_scale;//缩放的最小值
    private String picturePath;
    private TextView size_text;
    private String fileSize = null;
    private Button button;
    public static boolean pictureState;

    public PictureFragment() {

    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pictureState = true;
////        setContentView(R.layout.activity_picture_view);
//        currentMatrix = new Matrix();
////        imageView = findViewById(R.id.picture_imageView);
//        imageView.setOnTouchListener(this);
//
//        if(pictureBitmap != null) {
//            imageView.setImageBitmap(pictureBitmap);
//            Log.e(TAG, "onCreate: 设置" );
//            center(pictureBitmap);
//        }

    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        currentMatrix = new Matrix();
        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.activity_picture_view, null);

        imageView = view.findViewById(R.id.picture_imageView);
        imageView.setOnTouchListener(this);
        imageView.post(new Runnable() {
            @Override
            public void run() {
                if(picturePath !=null) {
                    setImageBitmap(picturePath);
                }
            }
        });

        size_text = view.findViewById(R.id.size_text);

        button = view.findViewById(R.id.button);
        button.setBackgroundColor(0X00000000);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(imageView.getWidth() != 0) {
                    center();
                }
            }
        });

        return view;
    }

    public void setLoadSize(String size) {
        if(size_text != null) {
            size_text.setText(size);
        } else {
            fileSize = size;
        }
    }

//    @Override
//    protected void onStart() {
//        super.onStart();
//        if(pictureBitmap != null) {
//            imageView.setImageBitmap(pictureBitmap);
//            Log.e(TAG, "onCreate: 设置" );
//            center(pictureBitmap);
//        }
//    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        pictureState = false;
        currentMatrix = null;
        imageView = null;
        picturePath = null;
        fileSize = null;
        FileManager.stopWriteFile();
        Log.e(TAG, "onDestroy: 退出" );
    }

//    public static void setImageBitmap(Bitmap bitmap) {
//        if(imageView != null) {
//            imageView.setImageBitmap(bitmap);
//            center(bitmap);
//        } else {
//            pictureBitmap = bitmap;
//            Log.e(TAG, "setImageBitmap: 空" );
//        }
//    }


    public void setImagePath(String path) {
        if(imageView != null && imageView.getWidth() != 0) {
            setImageBitmap(path);
        } else {
            picturePath = path;
        }
    }

    /**
     * 开始时将图片居中显示
     */
    private void center() {
        //变换矩阵，使其移动到屏幕中央
        float scale = 1.0f;
        Matrix matrix = new Matrix();
        if(imageWidth > windowWidth) {
            scale = windowWidth / imageWidth;
        }
        if(imageHeight > windowHeight) {
            scale = Math.min(scale, windowHeight / imageHeight);
        }
        min_scale = scale / 1.5f;
        max_scale = scale * 6;
        matrix.preScale(scale,scale);
        matrix.postTranslate((windowWidth / 2 - (imageWidth * scale)  / 2) , (windowHeight / 2 - (imageHeight * scale) / 2));
        //保存到currentMatrix
        currentMatrix.set(matrix);
        imageView.setImageMatrix(matrix);
    }
    private void setImageBitmap(String path) {

        //获取imageview中图片的实际高度
//        Bitmap bitmap = ((BitmapDrawable) (imageView).getDrawable()).getBitmap();
        Bitmap bitmap = BitmapFactory.decodeFile(path);
        if(bitmap == null) {
            return ;
        }
        if(fileSize != null) {
            size_text.setText(fileSize);
        }
        imageView.setImageBitmap(bitmap);
        imageHeight = bitmap.getHeight();
        imageWidth = bitmap.getWidth();

        windowWidth = imageView.getWidth();
        windowHeight = imageView.getHeight();
        center();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        //注意这一句的写法，用在多点触控中
        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
            // 在这里解释一下，在程序中我们将单点控制移动和双点控制缩放区分开（但是双点也是可以
            // 控制移动的)flag 的作用很简单，主要是用在单点移动时判断是否此次点击是否将要移动（不好描述，请读者自行细想一下）
            // 否则容易与双点操作混乱在一起，给用户带来较差的用户体验
            /*
             *
             *
             *
             * */
            case MotionEvent.ACTION_DOWN://第一个触点按下，将第一次的坐标保存下来
                lastX[0] = motionEvent.getX(0);
                lastY[0] = motionEvent.getY(0);
                moveLastX = motionEvent.getX();
                moveLastY = motionEvent.getY();
                flag = true;//第一次点击，说明有可能要进行单点移动，flag设为true
                break;
            case MotionEvent.ACTION_POINTER_DOWN://第二个触点按下，保存下来
                lastX[1] = motionEvent.getX(1);
                lastY[1] = motionEvent.getY(1);
                flag = false;//第二次点击，说明要进行双点操作,而不是单点移动，所以设为false
                break;
            case MotionEvent.ACTION_MOVE:
                //计算上一次触点间的距离
                float lastDistance = getDistance(lastX[0], lastY[0], lastX[1], lastY[1]);

                //如果有两个触点，进行放缩操作
                if (motionEvent.getPointerCount() == 2) {

                    //得到当前触点之间的距离
                    float currentDistance = getDistance(motionEvent.getX(0), motionEvent.getY(0), motionEvent.getX(1), motionEvent.getY(1));
                    touchMatrix = new Matrix();
                    //矩阵初始化为当前矩阵
                    touchMatrix.set(currentMatrix);

                    float pp[] = new float[9];
                    touchMatrix.getValues(pp);
                    float leftPosition = pp[2];//图片左边的位置
                    float upPostion = pp[5];//图片顶部的位置
                    /*
                     * 缩放之前对图片进行平移，将缩放中心平移到将要缩放的位置
                     * */

                    float l = (motionEvent.getX(0) + motionEvent.getX(1)) / 2 - leftPosition;
                    float t = (motionEvent.getY(0) + motionEvent.getY(1)) / 2 - upPostion;

                    touchMatrix.postTranslate(-(currentDistance / lastDistance - 1) * l,
                            -(currentDistance / lastDistance - 1) * t);
                    float p[] = new float[9];
                    touchMatrix.getValues(p);
                    //根据判断当前缩放的大小来判断是否达到缩放边界
                    if (p[0] * currentDistance / lastDistance < min_scale || p[0] * currentDistance / lastDistance > max_scale) {
                        //超过边界值时，设置为先前记录的矩阵
                        touchMatrix.set(mMatrix);
                    } else {
                        //图像缩放
                        touchMatrix.preScale(currentDistance / lastDistance, currentDistance / lastDistance);

                        //根据两个触点移动的距离实现位移（双触点平移）
                        float movex = (motionEvent.getX(0) - lastX[0] + motionEvent.getX(1) - lastX[1]) / 2;
                        float movey = (motionEvent.getY(0) - lastY[0] + motionEvent.getY(1) - lastY[1]) / 2;
                        touchMatrix.postTranslate(movex, movey);
                        //保存最后的矩阵，当缩放超过边界值时就设置为此矩阵
                        mMatrix = touchMatrix;
                    }
                    imageView.setImageMatrix(touchMatrix);

                } else {
                    if (flag) {

                        //只有一个触点时进行位移
                        Matrix tmp = new Matrix();//临时矩阵用来判断此次平移是否会导致平移越界
                        tmp.set(currentMatrix);
                        tmp.postTranslate(-moveLastX + motionEvent.getX(0), -moveLastY + motionEvent.getY(0));

                        if (!isTranslateOver(tmp)) {
                            //如果不越界就进行平移
                            touchMatrix = new Matrix();
                            touchMatrix.set(currentMatrix);
                            touchMatrix.postTranslate(-moveLastX + motionEvent.getX(0), -moveLastY + motionEvent.getY(0));
                        } else {
                            //如果会越界就保存当前位置，并且不进行矩阵变换
                            currentMatrix = touchMatrix;
                            moveLastX = motionEvent.getX(0);
                            moveLastY = motionEvent.getY(0);
                        }
                        imageView.setImageMatrix(touchMatrix);


                    }
//                    else  {
//                        center();
//                    }

                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:

                //松开手时，保存当前矩阵，此时的位置保存下来
                //flag设为控制
                currentMatrix = touchMatrix;
                moveLastX = motionEvent.getX(0);
                moveLastY = motionEvent.getY(0);
                flag = false;

                break;
        }
        return true;
    }

    //得到两点之间的距离
    private float getDistance(float x1, float y1, float x2, float y2) {
        return (float) Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
    }

    //判断平移是否越界
    private boolean isTranslateOver(Matrix matrix) {
        float p[] = new float[9];
        matrix.getValues(p);
//        Log.e(TAG, "isTranslateOver: " + Arrays.toString(p));

        float scaleX = p[0];
        float scaleY = p[4];
        float X = p[2];
        float Y = p[5];
        float X2 = X + imageWidth * scaleX;
        float Y2 = Y + imageHeight * scaleY;
//        if(X2 > windowWidth) {
//            return (X > 0 || Y > 0 || Y2 < windowHeight);
//        } else if(Y2 > imageHeight) {
//            return (X > 0 || Y > 0 || Y2 < windowHeight);
//        }
//        return true;
        return  (X > windowWidth / 4 * 3 || Y > windowHeight / 4 * 3 || X2 < windowWidth / 4 || Y2 < windowHeight / 4);


////
//        float leftPosition = p[2];
//        float rightPosition = (p[2] + imageWidth * p[0]);
//
//        float upPostion = p[5];
//        float downPostion = p[5] + imageHeight * p[0];
//
//        float leftSide, rightSide, upSide, downSide;
//        leftSide = windowWidth ;
//        rightSide = 0;
//        upSide = windowHeight ;
//        downSide = 0;
//        return (leftPosition > rightSide || rightPosition < leftSide || upPostion > downSide || downPostion < upSide);
    }


}