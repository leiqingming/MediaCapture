/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hxws.mediacapture.camera2;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
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
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.hxws.mediacapture.R;
import com.hxws.mediacapture.activity.SplitScreenActivity;
import com.hxws.mediacapture.utils.TimeUtil;
import com.hxws.mediacapture.view.FixedTextureCameraView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static com.hxws.mediacapture.utils.FileUtil.getSaveDirectory;

public class Camera2Fragment extends Fragment
        implements View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback {

    public static Camera2Fragment sInstance = null;

    public static Camera2Fragment getInstance() {

        if (sInstance == null) {
            sInstance = new Camera2Fragment();
            Log.d(TAG, "getInstance = null");
        }
        return sInstance;
    }

    private static final int REQUEST_CAMERA_PERMISSION = 1;

    private static final String[] CAMERA_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };

    private static final String FRAGMENT_DIALOG = "dialog";

    /**
     * Conversion from screen rotation to JPEG orientation.
     */

    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;

    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();

    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }

    /**
     * Tag for the {@link Log}.
     */
    private static final String TAG = "Camera2Fragment";

    private boolean DEBUG = true;

    public static boolean isCameraOpen = false;

    /**
     * Camera state: Showing camera preview.
     */
    private static final int STATE_PREVIEW = 0;

    /**
     * Camera state: Waiting for the focus to be locked.
     */
    private static final int STATE_WAITING_LOCK = 1;

    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    private static final int STATE_WAITING_PRECAPTURE = 2;

    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;

    /**
     * Camera state: Picture was taken.
     */
    private static final int STATE_PICTURE_TAKEN = 4;

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    //camera2 some params settings

    CameraCharacteristics mCameraCharacteristics;

    private int brightnessValue = 0;
    private float contrastValue = 0.0F;

    private Rect activeArraySize;
    private float maxDigitalZoom;
    private float maxOpticalZoom;
    private float digitalZoom = 1.0F;
    private float opticalZoom = 0.0F;

    private float gammaValue = 1.0F;

    private float[][] channels;
    private float[] channelValues = { 0.0F, 0.0F, 0.0F };

    private boolean isAutoWhiteBalanceEnabled = true;

    private int maxCompensationRange;
    private int minCompensationRange;

    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            if (DEBUG) Log.d(TAG,"onSurfaceTextureAvailable");
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };

    /**
     * ID of the current {@link CameraDevice}.
     */
    private String mCameraId;

    /**
     * An {@link FixedTextureCameraView} for camera preview.
     */
    private FixedTextureCameraView mTextureView;

    /**
     * A {@link CameraCaptureSession } for camera preview.
     */
    private CameraCaptureSession mCaptureSession;

    /**
     * A reference to the opened {@link CameraDevice}.
     */
    private CameraDevice mCameraDevice;

    /**
     * The {@link Size} of camera preview.
     */
    private Size mPreviewSize;

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
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
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
//            Activity activity = getActivity();
//            if (null != activity) {
//                activity.finish();
//            }
            SplitScreenActivity.showToast(getResources().getString(R.string.noCameraDevice));
        }

    };

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    /**
     * An {@link ImageReader} that handles still image capture.
     */
    private ImageReader mImageReader;

    /**
     * This is the output file for our picture.
     */
    private File mFile;

    /**
     * This a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
     * still image is ready to be saved.
     */
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            //mFile = new File(getSaveDirectory() + System.currentTimeMillis() + ".jpg");
            mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), mFile));
        }

    };

    /**
     * {@link CaptureRequest.Builder} for the camera preview
     */
    private CaptureRequest.Builder mPreviewRequestBuilder;

    /**
     * {@link CaptureRequest} generated by {@link #mPreviewRequestBuilder}
     */
    private CaptureRequest mPreviewRequest;

    /**
     * The current state of camera state for taking pictures.
     *
     * @see #mCaptureCallback
     */
    private int mState = STATE_PREVIEW;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    /**
     * Whether the current camera device supports Flash or not.
     */
    private boolean mFlashSupported;

    /**
     * Orientation of the camera sensor
     */
    private int mSensorOrientation;

    /**
     * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG capture.
     */
    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
            switch (mState) {
                case STATE_PREVIEW: {
                    // We have nothing to do when the camera preview is working normally.

                    break;
                }
                case STATE_WAITING_LOCK: {

                    Log.d(TAG, "process: STATE_WAITING_LOCK");
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    Log.d(TAG, "afState: " + afState);
                    if (afState == null || afState == 0) {
                        captureStillPicture();
                        Log.d(TAG, "process: afState == null || afState == 0");
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null ||
                                aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN;
                            Log.d(TAG, "process: afState == null STATE_PICTURE_TAKEN");
                            captureStillPicture();
                        } else {
                            Log.d(TAG, "process: aeState != null");
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
                    Log.d(TAG, "process: STATE_WAITING_PRECAPTURE");
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
                    Log.d(TAG, "process: STATE_WAITING_NON_PRECAPTURE");
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
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                          int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();

        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();

        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    public static Camera2Fragment newInstance() {
        return new Camera2Fragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_camera2, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        //view.findViewById(R.id.picture).setOnClickListener(this);
        //view.findViewById(R.id.info).setOnClickListener(this);
        mTextureView = (FixedTextureCameraView) view.findViewById(R.id.texture);

        sInstance = this;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //mFile = new File(getActivity().getExternalFilesDir(null), "pic.jpg");
    }

    @Override
    public void onResume() {
        super.onResume();

        if (DEBUG) Log.i(TAG,"--- onResume ---");

        startBackgroundThread();

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).

        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
            //return;
        }
        else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }

    }

    @Override
    public void onPause() {

        Log.d(TAG, "--- onPause --- isCameraOpen: "+isCameraOpen);

        if (isCameraOpen) {
            closeCamera();
            stopBackgroundThread();
        }
        super.onPause();
    }

    private void requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } else {
            requestPermissions(CAMERA_PERMISSIONS, REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
//            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
//
//                ErrorDialog.newInstance(getString(R.string.permission_request))
//                        .show(getChildFragmentManager(), FRAGMENT_DIALOG);
//            }

            if (grantResults.length == CAMERA_PERMISSIONS.length) {
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        ErrorDialog.newInstance(getString(R.string.permission_request))
                                .show(getChildFragmentManager(), FRAGMENT_DIALOG);
                        break;
                    }
                }
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private void setUpCameraOutputs(int width, int height) {
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        //Log.d(TAG,"manager: " + manager);

        try {
            String[] camid = manager.getCameraIdList();

            Log.d(TAG,"camid: " + Arrays.toString(camid));

            for (String cameraId : camid) {
                CameraCharacteristics mCameraCharacteristics
                        = manager.getCameraCharacteristics(cameraId);

                int i = mCameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);

                Log.d(TAG,"SUPPORTED_HARDWARE_LEVEL: " + i);

                // We don't use a front facing camera in this sample.
                Integer facing = mCameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                StreamConfigurationMap map = mCameraCharacteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                    //throw new RuntimeException("Cannot get available preview/video sizes");
                }

                // For still image captures, we use the largest available size.
                Size largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());
                mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                        ImageFormat.JPEG, /*maxImages*/2);
                mImageReader.setOnImageAvailableListener(
                        mOnImageAvailableListener, mBackgroundHandler);

                // Find out if we need to swap dimension to get the preview size relative to sensor
                // coordinate.
                int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
                //noinspection ConstantConditions
                mSensorOrientation = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swappedDimensions = false;
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                            swappedDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                            swappedDimensions = true;
                        }
                        break;
                    default:
                        Log.e(TAG, "Display rotation is invalid: " + displayRotation);
                        break;
                }

                Point displaySize = new Point();
                activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;

                if (swappedDimensions) {
                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }

                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                }

                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                }

                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                        maxPreviewHeight, largest);

                // We fit the aspect ratio of TextureView to the size of preview we picked.
                int orientation = getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {

                    Log.d(TAG, "ORIENTATION_LANDSCAPE");
                    mTextureView.setAspectRatio(
                            mPreviewSize.getWidth(), mPreviewSize.getHeight());
                } else {
                    Log.d(TAG, "ORIENTATION_PORTRAIT");
                    mTextureView.setAspectRatio(
                            mPreviewSize.getHeight(), mPreviewSize.getWidth());
                }

                // Check if the flash is supported.
                Boolean available = mCameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                mFlashSupported = available == null ? false : available;

                mCameraId = cameraId;

                Log.d(TAG, "mCameraId a: "+mCameraId);

                return;
            }
            Log.d(TAG, "mCameraId b: "+mCameraId);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            SplitScreenActivity.showToast(getResources().getString(R.string.noCameraDevice));
            //activity.finish();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            ErrorDialog.newInstance(getString(R.string.camera_error))
                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);
        }
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    /**
     * Opens the camera specified by {@link Camera2Fragment#mCameraId}.
     */
    private void openCamera(int width, int height) {
        if (ContextCompat.checkSelfPermission(Objects.requireNonNull(getActivity()), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            if (DEBUG) Log.d(TAG,"openCamera return");
            return;
        }
        //if (DEBUG) Log.d(TAG,"openCamera");
        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {

            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                //SplitScreenActivity.showToast(getResources().getString(R.string.noCameraDevice));
                //isCameraOpen = false;
                //sendCameraStatus(isCameraOpen);
                //throw new RuntimeException("Time out waiting to lock camera opening.");
                if (DEBUG) Log.d(TAG,"!mCameraOpenCloseLock.tryAcquire");
                //return;
            }
            if (mCameraId != null) {
                manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);//mBackgroundHandler
                isCameraOpen = true;
                if (DEBUG) Log.d(TAG,"mCameraId != null");
                //sendCameraStatus(isCameraOpen);
            }
            else {
                isCameraOpen = false;
                if (DEBUG) Log.d(TAG,"mCameraId is null");
                //sendCameraStatus(isCameraOpen);

            }

            sendCameraStatus(isCameraOpen);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            SplitScreenActivity.showToast(getResources().getString(R.string.noCameraDevice));
        }
        catch (InterruptedException e) {
            e.printStackTrace();
            //throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
            //SplitScreenActivity.showToast(getResources().getString(R.string.noCameraDevice));
        }

    }

    private void sendCameraStatus(boolean isCameraOpen) {
        Intent intent = new Intent(SplitScreenActivity.ACTION_CAMERA_STATUS);

        intent.putExtra("CAMERA_STATUS", isCameraOpen);

        getActivity().sendBroadcast(intent);

        Log.d(TAG, "sendCameraStatus: isCameraOpen is " + isCameraOpen);
    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
            if (DEBUG) Log.d(TAG,"closeCamera() finally");
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;

            Log.d(TAG, "mPreviewSize.getWidth(): " + mPreviewSize.getWidth()
                    + " , mPreviewSize.getHeight(): " + mPreviewSize.getHeight());

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReader.getSurface()),
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
                                setAutoFlash(mPreviewRequestBuilder);

                                //setAutoWhiteBalance(mPreviewRequestBuilder);

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();

                                mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        mCaptureCallback, mBackgroundHandler);

                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                                SplitScreenActivity.showToast(getResources().getString(R.string.noCameraDevice));
                            }
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            SplitScreenActivity.showToast("Create Camera Preview Failed");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
            //SplitScreenActivity.showToast(getResources().getString(R.string.noCameraDevice));
        }
    }

    /**
     * Configures the necessary {@link Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        Object localObject = getActivity();
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);

//        int i = activity.getWindowManager().getDefaultDisplay().getRotation();
//        Matrix matrix = new Matrix();
//        RectF localRectF1 = new RectF(0.0F, 0.0F, viewWidth, viewHeight);
//        RectF localRectF2 = new RectF(0.0F, 0.0F, this.mPreviewSize.getHeight(), this.mPreviewSize.getWidth());
//        float f1 = localRectF1.centerX();
//        float f2 = localRectF1.centerY();
//        if ((1 == i) || (3 == i))
//        {
//            localRectF2.offset(f1 - localRectF2.centerX(), f2 - localRectF2.centerY());
//            matrix.setRectToRect(localRectF1, localRectF2, Matrix.ScaleToFit.FILL);
//            float f3 = Math.max(viewHeight / this.mPreviewSize.getHeight(), viewWidth / this.mPreviewSize.getWidth());
//            matrix.postScale(f3, f3, f1, f2);
//            matrix.postRotate((i - 2) * 90, f1, f2);
//        }
//        while (true)
//        {
//            this.mTextureView.setTransform(matrix);
//
//            if (2 == i) {
//                matrix.postRotate(180.0F, f1, f2);
//            }
//            return;
//        }
    }

    /**
     * Initiate a still image capture.
     */
    public void takePicture() {
        lockFocus();
    }

    /**
     * Lock the focus as the first step for a still image capture.
     */
    private void lockFocus() {
        try {
            // This is how to tell the camera to lock focus.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the lock.
            mState = STATE_WAITING_LOCK;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    null);//mBackgroundHandler
            //Log.d(TAG, "lockFocus");
        } catch (CameraAccessException | NullPointerException e) {
            e.printStackTrace();
            //Log.d(TAG, "CameraAccessException");
            SplitScreenActivity.showToast(getResources().getString(R.string.noCameraDevice));
        }
    }

    /**
     * Run the precapture sequence for capturing a still image. This method should be called when
     * we get a response in {@link #mCaptureCallback} from {@link #lockFocus()}.
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

            //Log.d(TAG, "runPrecaptureSequence");
        } catch (CameraAccessException | NullPointerException e) {
            e.printStackTrace();
            SplitScreenActivity.showToast(getResources().getString(R.string.noCameraDevice));
        }
    }

    /**
     * Capture a still picture. This method should be called when we get a response in
     * {@link #mCaptureCallback} from both {@link #lockFocus()}.
     */
    private void captureStillPicture() {
        try {
            final Activity activity = getActivity();
            if (null == activity || null == mCameraDevice) {

                Log.d(TAG,"activity: "+activity+"mCameraDevice: "+mCameraDevice);

                return;
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            setAutoFlash(captureBuilder);

            // Orientation
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

            mFile = new File(getSaveDirectory() + TimeUtil.getCurTime() + ".jpg");

            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {

                    //Log.d(TAG, mFile.toString());
                    unlockFocus();
                }
            };

            mCaptureSession.stopRepeating();
            mCaptureSession.abortCaptures();
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
            SplitScreenActivity.showToast(
                    getResources().getString(R.string.saveFilePath) + mFile);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            SplitScreenActivity.showToast(getResources().getString(R.string.noCameraDevice));
        }
    }

    /**
     * Retrieves the JPEG orientation from the specified screen rotation.
     *
     * @param rotation The screen rotation.
     * @return The JPEG orientation (one of 0, 90, 270, and 360)
     */
    private int getOrientation(int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from DEFAULT_ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (DEFAULT_ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }

    /**
     * Unlock the focus. This method should be called when still image capture sequence is
     * finished.
     */
    private void unlockFocus() {
        try {
            // Reset the auto-focus trigger
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            setAutoFlash(mPreviewRequestBuilder);
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
            // After this, the camera will go back to the normal state of preview.
            mState = STATE_PREVIEW;
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            SplitScreenActivity.showToast(getResources().getString(R.string.noCameraDevice));
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
//            case R.id.picture: {
//                takePicture();
//                break;
//            }
//
//            case R.id.info: {
//                Activity activity = getActivity();
//                if (null != activity) {
//                    new AlertDialog.Builder(activity)
//                            .setMessage(R.string.intro_message)
//                            .setPositiveButton(android.R.string.ok, null)
//                            .show();
//                }
//                break;
//            }
        }
    }

    private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        if (mFlashSupported) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
    }

    private void setAutoWhiteBalance(CaptureRequest.Builder requestBuilder){
        //CaptureRequest.Builder localBuilder = this.mPreviewRequestBuilder;
        //CaptureRequest.Key localKey = CaptureRequest.CONTROL_AWB_MODE;

        requestBuilder.set(CaptureRequest.CONTROL_AWB_MODE,CaptureRequest.CONTROL_AWB_MODE_AUTO);

        try {
            mCaptureSession.setRepeatingRequest(
                    mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
            Log.d(TAG,"setAutoWhiteBalance()");
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    public void setBrightness(int paramInt)
    {
        if ((paramInt >= this.minCompensationRange) && (paramInt <= this.maxCompensationRange))
        {
            Range<Integer> range1 = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
            int maxmax = range1.getUpper();
            int minmin = range1.getLower();
            int all = (-minmin) + maxmax;
            int time = 100 / all;
            int ae = ((paramInt / time) - maxmax) > maxmax ? maxmax
                    : ((paramInt / time) - maxmax) < minmin ? minmin
                    : ((paramInt / time) - maxmax);

            mPreviewRequestBuilder.set(
                    CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, ae);
            Log.d(TAG,"setBrightness: set");
            paramInt = ae;
        }

        try
        {
            mCaptureSession.setRepeatingRequest(
                    mPreviewRequest, mCaptureCallback, mBackgroundHandler);
            brightnessValue = paramInt;
            Log.e(TAG, "brightnessValue: " + brightnessValue);
        }
        catch (Exception localException)
        {
            localException.printStackTrace();
        }
    }

    public void addBrightness()
    {
        setBrightness(brightnessValue + 10);
    }
    public void reduceBrightness()
    {
        setBrightness(brightnessValue - 10);
    }
/*
    public void setContrast(float paramFloat)
    {
        if ((paramFloat < -1.0F) || (paramFloat > 1.0F) || (this.channels == null))
            return;
        Object localObject = new float[3][];
        int i = 0;
        while (i <= 2)
        {
            float[] arrayOfFloat = new float[this.channels[i].length];
            int j = 0;
            while (j < arrayOfFloat.length)
            {
                arrayOfFloat[j] = this.channels[i][j];
                j += 1;
            }
            j = 0;
            if (j < arrayOfFloat.length)
            {
                arrayOfFloat[j] += paramFloat;
                if (arrayOfFloat[j] < 0.0F)
                    arrayOfFloat[j] = 0.0F;
                while (true)
                {
                    j += 1;
                    break;
                    if (arrayOfFloat[j] > 1.0F)
                        arrayOfFloat[j] = 1.0F;
                }
            }
            localObject[i] = arrayOfFloat;
            i += 1;
        }
        localObject = new TonemapCurve(localObject[0], localObject[1], localObject[2]);
        this.mPreviewRequestBuilder.set(CaptureRequest.TONEMAP_MODE, Integer.valueOf(0));
        this.mPreviewRequestBuilder.set(CaptureRequest.TONEMAP_CURVE, localObject);
        this.mPreviewRequest = this.mPreviewRequestBuilder.build();
        try
        {
            this.mCaptureSession.setRepeatingRequest(this.mPreviewRequest, this.mCaptureCallback, this.mBackgroundHandler);
            this.contrastValue = paramFloat;
            Log.e(TAG, "contrastValue: " + this.contrastValue);
            return;
        }
        catch (Exception localException)
        {
            localException.printStackTrace();
        }
    }

    private void setChanelValue(int paramInt, float paramFloat)
    {
        if ((paramFloat < -1.0F) || (paramFloat > 1.0F) || (this.channels == null))
            return;
        Object localObject = new float[3][];
        int i = 0;
        while (i <= 2)
        {
            float[] arrayOfFloat = new float[this.channels[i].length];
            int j = 0;
            while (j < arrayOfFloat.length)
            {
                arrayOfFloat[j] = this.channels[i][j];
                j += 1;
            }
            if (i == paramInt)
            {
                j = 0;
                if (j < arrayOfFloat.length)
                {
                    arrayOfFloat[j] += paramFloat;
                    if (arrayOfFloat[j] < 0.0F)
                        arrayOfFloat[j] = 0.0F;
                    while (true)
                    {
                        j += 1;
                        break;
                        if (arrayOfFloat[j] > 1.0F)
                            arrayOfFloat[j] = 1.0F;
                    }
                }
            }
            localObject[i] = arrayOfFloat;
            i += 1;
        }
        localObject = new TonemapCurve(localObject[0], localObject[1], localObject[2]);
        this.mPreviewRequestBuilder.set(CaptureRequest.TONEMAP_MODE, Integer.valueOf(0));
        this.mPreviewRequestBuilder.set(CaptureRequest.TONEMAP_CURVE, localObject);
        this.mPreviewRequest = this.mPreviewRequestBuilder.build();
        try
        {
            this.mCaptureSession.setRepeatingRequest(this.mPreviewRequest, this.mCaptureCallback, this.mBackgroundHandler);
            this.channelValues[paramInt] = paramFloat;
            Log.e("Camera2Fragment", "" + paramInt + "channel value: " + this.channelValues[paramInt]);
            return;
        }
        catch (Exception localException)
        {
            localException.printStackTrace();
        }
    }
*/



    public void addDigitalZoom(){

        setDigitalZoom(digitalZoom + 0.25F);
    }

    public void reduceDigitalZoom(){

        setDigitalZoom(digitalZoom - 0.25F);
    }

    private void setDigitalZoom(float paramFloat)
    {
        if ((paramFloat >= 1.0F) && (paramFloat <= this.maxDigitalZoom))
        {
            Rect localRect = new Rect((int)(this.activeArraySize.centerX() - 1.0D / paramFloat * this.activeArraySize.centerX()), (int)(this.activeArraySize.centerY() - 1.0D / paramFloat * this.activeArraySize.centerY()), (int)(this.activeArraySize.centerX() + 1.0D / paramFloat * this.activeArraySize.centerX()), (int)(this.activeArraySize.centerY() + 1.0D / paramFloat * this.activeArraySize.centerY()));
            this.mPreviewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, localRect);
            this.mPreviewRequest = this.mPreviewRequestBuilder.build();
        }
        try
        {
            this.mCaptureSession.setRepeatingRequest(this.mPreviewRequest, this.mCaptureCallback, this.mBackgroundHandler);
            this.digitalZoom = paramFloat;
            Log.e("Camera2Fragment", "digitalZoom: " + this.digitalZoom);
            return;
        }
        catch (Exception localException)
        {
            localException.printStackTrace();
        }
    }

    private void setGamma(float paramFloat)
    {
        if ((paramFloat >= 1.0F) && (paramFloat <= 5.0F))
        {
            this.mPreviewRequestBuilder.set(CaptureRequest.TONEMAP_MODE, Integer.valueOf(3));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                this.mPreviewRequestBuilder.set(CaptureRequest.TONEMAP_GAMMA, Float.valueOf(paramFloat));
            }
            this.mPreviewRequest = this.mPreviewRequestBuilder.build();
        }
        try
        {
            this.mCaptureSession.setRepeatingRequest(this.mPreviewRequest, this.mCaptureCallback, this.mBackgroundHandler);
            this.gammaValue = paramFloat;
            Log.e("Camera2Fragment", "gammaValue: " + this.gammaValue);
            return;
        }
        catch (Exception localException)
        {
            localException.printStackTrace();
        }
    }

    public void addOpticalZoom(){

        setOpticalZoom(opticalZoom + 0.25F);
    }

    public void reduceOpticalZoom(){

        setOpticalZoom(opticalZoom - 0.25F);
    }

    private void setOpticalZoom(float paramFloat)
    {
//        this.mPreviewRequestBuilder.set(CaptureRequest.LENS_FOCAL_LENGTH, Float.valueOf(paramFloat));
//        this.mPreviewRequest = this.mPreviewRequestBuilder.build();
//        try
//        {
//            this.mCaptureSession.setRepeatingRequest(this.mPreviewRequest, this.mCaptureCallback, this.mBackgroundHandler);
//            this.opticalZoom = paramFloat;
//            Log.e("Camera2Fragment", "opticalZoom: " + this.opticalZoom);
//            return;
//        }
//        catch (Exception localException)
//        {
//            localException.printStackTrace();
//        }

        float minimumLens = mCameraCharacteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
        float num = (((float) paramFloat) * minimumLens / 100);
        mPreviewRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, num);
        mPreviewRequest = mPreviewRequestBuilder.build();
        try {
            mCaptureSession.setRepeatingRequest(this.mPreviewRequest, this.mCaptureCallback, this.mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        opticalZoom = paramFloat;

    }

    /**
     * Saves a JPEG {@link Image} into the specified {@link File}.
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
     * Shows an error message dialog.
     */
    public static class ErrorDialog extends DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }

    }

    /**
     * Shows OK/Cancel confirmation dialog about camera permission.
     */
    public static class ConfirmationDialog extends DialogFragment {

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Fragment parent = getParentFragment();
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.permission_request)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            parent.requestPermissions(new String[]{Manifest.permission.CAMERA},
                                    REQUEST_CAMERA_PERMISSION);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Activity activity = parent.getActivity();
                                    if (activity != null) {
                                        activity.finish();
                                    }
                                }
                            })
                    .create();
        }
    }

}
