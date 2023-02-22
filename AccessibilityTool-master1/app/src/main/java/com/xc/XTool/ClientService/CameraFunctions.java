package com.xc.XTool.ClientService;

import android.Manifest;

import android.media.Image;
import android.os.Environment;
import android.os.Handler;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.HandlerThread;

import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CameraFunctions {

    private static String TAG = "CameraFunctions";
    private static final int MAX_WIDTH = 1920;
    private static final int MAX_HEIGHT = 1080;
    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAITING_LOCK = 1;
    private static final int STATE_WAITING_PRECAPTURE = 2;
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;
    private static final int STATE_PICTURE_TAKEN = 4;
    private boolean mFlashSupported;
    private int mState = STATE_PREVIEW;
//    private int MaxWidth;
//    private int MaxHeight;
    private Context context;
    private CameraManager manager;
    private CameraCaptureSession mCaptureSession;
    private CameraDevice mCameraDevice;
    private String mCameraId;
//    private ImageReader mImageReader;
    private ImageReader mImageJpeg;
    private CaptureRequest mPreviewRequest;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private Semaphore mCameraOpenCloseLock;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    private AvcEncoder avcEncoder;
    private int width;
    private int height;
    private int bitRate;
//    private int cameraId;

    private boolean flashLightState;
//    private AvcEncoder.UdpSendTask udpSendTask;

    public CameraFunctions(Context context) {
        this.context = context;

//        this.mBackgroundHandler = handler;
//        startBackgroundThread();
    }

    /**
     * 启动后台线程
     */
//    private void startBackgroundThread() {
////        if(mBackgroundThread != null && mBackgroundHandler != null) {
////            stopBackgroundThread();
////        }
////        mBackgroundThread = new HandlerThread("CameraFunctions");
////        mBackgroundThread.start();
//        mBackgroundHandler = new Handler(Looper.getMainLooper());
//    }
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }


    /**
     * 停止后台线程
     */
    private void stopBackgroundThread() {
        if(mBackgroundThread != null) {
            mBackgroundThread.quitSafely();
            try {
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
//    private void stopBackgroundThread() {
////        if(mBackgroundThread != null && mBackgroundHandler != null) {
////            mBackgroundThread.quitSafely();
////            try {
////                mBackgroundThread.join();
////                Log.e(TAG, "openCamera: 停止线程");
////                mBackgroundThread = null;
////                mBackgroundHandler = null;
////            } catch (InterruptedException e) {
////                e.printStackTrace();
////            }
////        }
//        mBackgroundHandler = null;
//    }

    /**
     * 相机打开状态回调
     */
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            ClientService.sendMsg(ClientService.CLOSE_CAMERA);
            Log.e(TAG, "onDisconnected: 相机断开连接");
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            ClientService.sendMsg(ClientService.CLOSE_CAMERA);
            Log.e(TAG, "onDisconnected: 相机错误");
        }
    };
    /**
     * 拍照图像处理
     */
    private final ImageReader.OnImageAvailableListener mOnJpegAvailableListener
            = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            String time = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.CHINESE).format(new Date(System.currentTimeMillis()));
            File mFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_DCIM), time);
            mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), mFile));
            Log.e(TAG, "onImageAvailable: 拍照成功" );
        }
    };

    /**
     * 图像数据处理回调
     */
//    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
//            = new ImageReader.OnImageAvailableListener() {
//
//        @Override
//        public void onImageAvailable(ImageReader reader) {
////            mBackgroundHandler.post(new ImageSaver(image, mFile));
////            Log.e(TAG, "onImageAvailable: " + reader.getMaxImages() );
//            Image image = reader.acquireNextImage();
////            Image image = reader.acquireLatestImage();
//            if (image == null) {
//                Log.e(TAG, "onImageAvailable: 没有数据"  );
//                return;
//            }
//
//            avcEncoder.putEncodeData(image);
//
////            Log.e(TAG, "camera width:" + image.getWidth() + " camera height:" + image.getHeight());
//            image.close();
//        }
//
//    };
    /**
     * 拍照回调
     */
    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
//            Log.e(TAG, "process: " + mState );
            switch (mState) {
                case STATE_PREVIEW: {
                    // We have nothing to do when the camera preview is working normally.
                    break;
                }
                case STATE_WAITING_LOCK: {
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
//                    Log.e(TAG, "process: " + afState );
                    if (afState == null) {
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null ||
                                aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            process(result);
        }

    };

    /**
     * 比较区域大小
     */
    public static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    /**
     * 设置输出图像的大小
     *
     * @param CameraId
     */
    private int mSensorOrientation;
    private void setUpCameraOutputs(String CameraId) {
        try {
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(CameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) {
                return;
            }
            Size largest_jpeg = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)), new CompareSizesByArea());

            mImageJpeg = ImageReader.newInstance(largest_jpeg.getWidth(), largest_jpeg.getHeight(), ImageFormat.JPEG, /*maxImages*/2);
            mImageJpeg.setOnImageAvailableListener(mOnJpegAvailableListener, mBackgroundHandler);
            Log.e(TAG, "jpegwidth: " + largest_jpeg.getWidth() + " " + "Jpegheight: " + largest_jpeg.getHeight());

            if(width > MAX_WIDTH) {
                width = MAX_WIDTH;
            }
            if(height > MAX_HEIGHT) {
                height = MAX_HEIGHT;
            }
            mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

//            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
//                    rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
//                    maxPreviewHeight, largest_jpeg);
//            Size largest = Collections.max(
//                    Arrays.asList(map.getOutputSizes(ImageFormat.YUV_420_888)), new CompareSizesByArea());
//            int MaxWidth = largest.getWidth();
//            int MaxHeight = largest.getHeight();
//            if (width > MaxWidth || height > MaxHeight) {
//                width = MaxWidth;
//                height = MaxHeight;
//            }
//            Point displaySize = new Point();
//            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
//            windowManager.getDefaultDisplay().getSize(displaySize);
//            int displayRotation =  windowManager.getDefaultDisplay().getRotation();
//            Log.e(TAG, "setUpCameraOutputs:displayRotation: " + displayRotation);
//            Log.e(TAG, "setUpCameraOutputs: " + displaySize.x + " " + displaySize.y );
//
//            Log.e(TAG, "setUpCameraOutputs: " + width + " " + height );
//            Log.e(TAG, "setUpCameraOutputs: " + mSensorOrientation );

//
//            mImageReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, /*maxImages*/2);
//            mImageReader.setOnImageAvailableListener(
//                    mOnImageAvailableListener, mBackgroundHandler);
//            Log.e(TAG, "Maxwidth: " + MaxWidth + " " + "Maxheight: " + MaxHeight);

            Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
            mFlashSupported = available == null ? false : available;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            Log.e(TAG, "不支持Camera2");
        }
    }

    /**
     * 打开相机
     * @param CameraId 相机ID（前后摄像头）
     */
    private void openCamera(String CameraId) {

        if (context.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "没有相机权限");
            return;
        }
        mCameraOpenCloseLock = new Semaphore(1);
        startBackgroundThread();

        setUpCameraOutputs(CameraId);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                Log.e(TAG, "相机开启超时");
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(CameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            Log.e(TAG, "相机被占用");
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    public void stopCamera() {
        if(avcEncoder != null) {
            avcEncoder.closeEncoder();
            avcEncoder = null;
        } else {
            closeCamera();
        }
    }
    /**
     * 关闭相机
     */
    private void closeCamera() {
            try {
                if (null != mCaptureSession) {
                    mCaptureSession.close();
                    mCaptureSession = null;
                }
                if (null != mCameraDevice) {
                    mCameraDevice.close();
                    mCameraDevice = null;
                }
//            if (null != mImageReader) {
//                mImageReader.close();
//                mImageReader = null;
//            }
                if (null != mImageJpeg) {
                    mImageJpeg.close();
                    mImageJpeg = null;
                }
                if (mCameraOpenCloseLock != null) {
                    mCameraOpenCloseLock.acquire();
                    mCameraOpenCloseLock.release();
                    mCameraOpenCloseLock = null;
                }
//                if (avcEncoder != null) {
////                    avcEncoder.closeEncoder();
//                    avcEncoder = null;
//                }
                stopBackgroundThread();
                Log.e(TAG, "closeCamera: 关闭相机" );
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
            }

    }

    /**
     * 创建相机预览
     */
    private void createCameraPreviewSession() {
        try {
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            avcEncoder = new AvcEncoder(width, height, 60, bitRate) {
                @Override
                public void closeEncoder() {
                    closeCamera();
                    super.closeEncoder();
                }
            };
            Surface surface = avcEncoder.getSurface();

            if(surface == null || mImageJpeg == null) {
                throw new Exception("空对象");
            }
            mPreviewRequestBuilder.addTarget(surface);

            // Here, we create a CameraCaptureSession for camera preview.
//            mCameraDevice.createCaptureSession(Arrays.asList(mImageReader.getSurface(),mImageJpeg.getSurface()),
            mCameraDevice.createCaptureSession(Arrays.asList(surface,mImageJpeg.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // Flash is automatically enabled when necessary.
//                                setAutoFlash(mPreviewRequestBuilder);

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        mCaptureCallback, mBackgroundHandler);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }


                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            Log.e(TAG, "预览失败");
                            stopCamera();
//                            showToast("Failed");
                        }
                    }, null
            );
        } catch (Exception e) {
            e.printStackTrace();
//            closeCamera();
        }
    }

    /**
     * 拍照
     */
    public void takePicture() {
        if(mCameraDevice != null)
            lockFocus();
    }

    /**
     * 聚焦
     */
    private void lockFocus() {
        try {
            if (mCaptureSession == null)
                return;
            // This is how to tell the camera to lock focus.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the lock.
            mState = STATE_WAITING_LOCK;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
            Log.e(TAG, "lockFocus: " );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 运行预览拍照
     */
    private void runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    /**
     * 捕获静态图片
     */
    private void captureStillPicture() {
        try {
//            final AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (null == mCameraDevice) {
                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageJpeg.getSurface());

            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
//            setAutoFlash(captureBuilder);

            // Orientation
//            int rotation = context.getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, (90 + mSensorOrientation + 270) % 360);

            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
//                    showToast("Saved: " + mFile);
//                    Log.d(TAG, mFile.toString());
                    unlockFocus();
                }
            };
            Log.e(TAG, "captureStillPicture: " );
            mCaptureSession.stopRepeating();
            mCaptureSession.abortCaptures();
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 解锁焦点
     * 当静止图像捕获序列为已完成
     */
    private void unlockFocus() {
        try {
            // Reset the auto-focus trigger
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
//            setAutoFlash(mPreviewRequestBuilder);
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
            // After this, the camera will go back to the normal state of preview.
            mState = STATE_PREVIEW;
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        if (mFlashSupported) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
    }

    /**
     * 图像数据处理服务
     */
    private static class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final Image mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                output.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    /**
     * 获取摄像头数量
     *
     * @return
     */

    public void openCamera(int width, int height, int cameraId,int bitRate) {
        if (mBackgroundThread != null) {
            return;
        }
        try {
//            udpSendTask = new AvcEncoder.UdpSendTask();
//            avcEncoder = new AvcEncoder(320, 240, 15, 2000000);
            this.width = width;
            this.height = height;
            this.bitRate = bitRate;
//            this.cameraId = cameraId1;
            manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            String[] cameraIdList = manager.getCameraIdList();
            if(cameraId >= cameraIdList.length) {
                cameraId = cameraIdList.length - 1;
            }
            openCamera(cameraIdList[cameraId]);
//            return manager.getCameraIdList();
        } catch (CameraAccessException e) {
            e.printStackTrace();
//            return null;
        }
    }

//    private void sendImage(byte[] data) {
//        try {
//            SocketClient.outputStream.write(data);
//        } catch (IOException e) {
//            e.printStackTrace();
//            closeCamera();
//            ClientService.socketClient.connect();
//        }
//    }
}