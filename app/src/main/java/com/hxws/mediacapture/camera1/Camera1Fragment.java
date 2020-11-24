package com.hxws.mediacapture.camera1;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.hxws.mediacapture.R;
import com.hxws.mediacapture.view.CircleButtonView;
import com.hxws.mediacapture.view.FaceDeteView;
import com.hxws.mediacapture.view.FixedTextureCameraView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Camera1Fragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Camera1Fragment extends Fragment implements View.OnClickListener, CameraPresenter.CameraCallBack {

    private static final String TAG = "Camera1Fragment";

    private static Camera1Fragment sInstance = null;


    //SurfaceView
    private SurfaceView sf_camera;

    FixedTextureCameraView mTextureView;

    private Size mPreviewSize;

    private static final int MAX_PREVIEW_WIDTH = 1920;

    private static final int MAX_PREVIEW_HEIGHT = 1080;

    private int mSensorOrientation;

    //逻辑层
    private CameraPresenter mCameraPresenter;

    private int mZoom = 0;

    //操作按钮
    //拍照
    private CircleButtonView tv_takephoto;

    //显示拍下来的图片
    private ImageView iv_photo;
    //更换摄像头
    private TextView tv_change_camera;
    //闪光灯
    private TextView tv_flash;
    //开启关闭人脸识别按钮
    private TextView tv_facedetect;
    //人脸检测框
    private FaceDeteView faceView;

    //全屏还是4：3
    private TextView tv_matchorwrap;

    //Recycleview
    private RecyclerView cy_photo;



    private RelativeLayout rl_parent;

    private boolean isFull = false;

    private ImageView iv_test;

    //屏幕尺寸
    private int screenWidth;
    private int screenHeight;


    public Camera1Fragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment Camera1Fragment.
     */
    // TODO: Rename and change types and number of parameters
    public static Camera1Fragment newInstance() {
        Camera1Fragment fragment = new Camera1Fragment();

        return fragment;
    }

    public static Camera1Fragment getInstance() {

        if (sInstance == null) {
            sInstance = new Camera1Fragment();
        }
        return sInstance;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Override
    public void onResume() {
        super.onResume();

        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCameraPresenter != null) {
            mCameraPresenter.releaseCamera();
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_camera1, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {

        sInstance = this;

        //获取屏幕宽度
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;

        getScreenBrightness();

        initBind(view);
        initListener();

        //初始化CameraPresenter
        //mCameraPresenter = new CameraPresenter(this, sf_camera);

        mCameraPresenter = new CameraPresenter(this,mTextureView);

        //设置后置摄像头
        mCameraPresenter.setFrontOrBack(Camera.CameraInfo.CAMERA_FACING_BACK);
        //添加监听
        mCameraPresenter.setCameraCallBack(this);
    }

    private void initBind(View view) {

        //tv_change_camera = view.findViewById(R.id.tv_change_camera);

        //tv_takephoto = view.findViewById(R.id.tv_takephoto);
        //sf_camera = view.findViewById(R.id.sfv_camera);
        //iv_photo = view.findViewById(R.id.iv_photo);
        //cy_photo = view.findViewById(R.id.cy_photo);

        //tv_flash = view.findViewById(R.id.tv_flash);
        //tv_facedetect = view.findViewById(R.id.tv_facedetect);
        faceView = view.findViewById(R.id.faceView);
        tv_matchorwrap = view.findViewById(R.id.tv_matchorwrap);
        rl_parent = view.findViewById(R.id.rl_parent);
        //iv_test = view.findViewById(R.id.iv_test);

        mTextureView = (FixedTextureCameraView) view.findViewById(R.id.camera1_texture);
        mTextureView.setAspectRatio(mTextureView.getWidth(),mTextureView.getWidth());
    }

    /**
     * 添加点击事件 触摸事件
     */
    private void initListener() {
        //sf_camera.setOnTouchListener(this);
        //iv_photo.setOnClickListener(this);
        //tv_change_camera.setOnClickListener(this);
        //tv_flash.setOnClickListener(this);
        //点击事件
//        tv_takephoto.setOnClickListener(new CircleButtonView.OnClickListener() {
//            @Override
//            public void onClick() {
//                //拍照的调用方法
//                mCameraPresenter.takePicture();
//            }
//        });

        //长按事件
//        tv_takephoto.setOnLongClickListener(new CircleButtonView.OnLongClickListener() {
//            @Override
//            public void onLongClick() {
//                //mCameraPresenter.startRecord(Configuration.OUTPATH, "video");
//
//            }
//
//            @Override
//            public void onNoMinRecord(int currentTime) {
//                //ToastUtil.showShortToast(CustomCameraActivity.this, "录制时间太短～");
//            }
//
//            @Override
//            public void onRecordFinishedListener() {
//                //mCameraPresenter.stopRecord();
//                //startActivity(new Intent(CustomCameraActivity.this, PlayAudioActivity.class));
//            }
//        });
        //tv_facedetect.setOnClickListener(this);
        tv_matchorwrap.setOnClickListener(this);

        //mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
    }


    /**
     * 加入调整亮度
     */
    private void getScreenBrightness() {
        WindowManager.LayoutParams lp = getActivity().getWindow().getAttributes();
        //screenBrightness的值是0.0-1.0 从0到1.0 亮度逐渐增大 如果是-1，那就是跟随系统亮度
        lp.screenBrightness = Float.valueOf(200) * (1f / 255f);
        getActivity().getWindow().setAttributes(lp);
    }

    private void setBrightness(int value){



    }

    public void addCamera1Zoom(){
        setCamera1Zoom(mZoom + 10);
    }

    public void reduceCamera1Zoom(){
        setCamera1Zoom(mZoom - 10);
    }

    private void setCamera1Zoom(int scale){

        if (scale >= 1 || scale <= -1) {

            int zoom = scale;       //mCameraPresenter.getZoom() + scale;
            //判断zoom是否超出变焦距离
            if (zoom > mCameraPresenter.getMaxZoom()) {
                zoom = mCameraPresenter.getMaxZoom();
                //mZoom = mCameraPresenter.getMaxZoom();
            }
            //如果系数小于0
            if (zoom < 0) {
                zoom = 0;
                //mZoom = 0;
            }

            mZoom = zoom;
            Log.d(TAG,"setCamera1Zoom: zoom = "+zoom);
            //设置焦距
            mCameraPresenter.setZoom(zoom);
        }

    }



    /**
     * 获取屏幕宽高
     */
    private int[] getScreen() {
        int[] screens = new int[2];
        //获取屏幕宽度
        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        //宽
        screens[0] = width;
        //高
        screens[1] = height;

        return screens;

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.tv_matchorwrap:
                rl_parent.removeView(sf_camera);

                int screen[] = getScreen();
                //定义布局参数
//                ConstraintLayout.LayoutParams layoutParams =
//                        new ConstraintLayout.LayoutParams(
//                                ConstraintLayout.LayoutParams.WRAP_CONTENT,
//                                ConstraintLayout.LayoutParams.WRAP_CONTENT);

                RelativeLayout.LayoutParams layoutParams =
                        new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                                RelativeLayout.LayoutParams.WRAP_CONTENT);

                if(isFull){
                    //是全屏 切换成4：3
                    //layoutParams.width = (int) (screen[0]);
                    //layoutParams.height = (int) (screen[0] * 4/3);

//                    layoutParams.width = (int) sf_camera.getWidth();
//                    layoutParams.height = (int) (sf_camera.getWidth()* 4/3);

                } else {
                    //不是全屏
                    //是全屏 切换成4：3
                    //layoutParams.width = (int) (screen[0]);
                    //layoutParams.height = (int) (screen[1]);
//                    layoutParams.width = sf_camera.getWidth();
//                    layoutParams.height = sf_camera.getHeight();
                }

                sf_camera.setLayoutParams(layoutParams);
                isFull = !isFull;
                mCameraPresenter.setFull(isFull);
                rl_parent.addView(sf_camera,0,layoutParams);

//                scaleSurfaceView(sf_camera, isFull);
//                if(isFull){
//                    mCameraPresenter.setFull(false);
//                    int s = screen[0] * 4/ 3;
//                    Log.d("sss进入",screen[1]+ "sdsd" +s+"");
//                    changeViewHeightAnimatorStart(sf_camera,screen[1],screen[0] * 4/3);
//                } else {
//                    mCameraPresenter.setFull(true);
//                    changeViewHeightAnimatorStart(sf_camera,screen[0] * 4/3,screen[1]);
//                }
//                isFull = !isFull;

                break;
            default:
                break;

        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

    }

    @Override
    public void onTakePicture(byte[] data, Camera Camera) {

    }

    @Override
    public void onFaceDetect(ArrayList<RectF> rectFArrayList, Camera camera) {

    }

    @Override
    public void getPhotoFile(String imagePath) {

    }

    /**
     * 放大缩小动画
     *
     * @param v
     */
    private void scaleSurfaceView(View v, boolean isFull) {
        ScaleAnimation scamleAni;
        if (isFull) {
            scamleAni = new ScaleAnimation(1f, 1, 1f, 0.5f, screenWidth / 2, screenHeight / 2);
            // scamleAni = new ScaleAnimation(screenWidth / 2,screenWidth / 2,screenHeight / 2,screenWidth  / 2 * 4 / 3, Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF,0.5f);
        } else {
            // scamleAni = new ScaleAnimation(v.getMeasuredWidth() / 2,screenWidth / 2,v.getMeasuredHeight() / 2,screenHeight / 2, Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF,0.5f);
            scamleAni = new ScaleAnimation(1, 1f, 1, 1.5f, screenWidth / 2, screenHeight / 2);
        }

        //设置动画执行的时间，单位是毫秒
        scamleAni.setDuration(1500);
        scamleAni.setFillAfter(true);
        v.startAnimation(scamleAni);
    }

    public  void changeViewHeightAnimatorStart(final View view, final int startHeight, final int endHeight) {
        if (view != null && startHeight >= 0 && endHeight >= 0) {

            ValueAnimator animator = ValueAnimator.ofInt(startHeight, endHeight);

            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                @Override

                public void onAnimationUpdate(ValueAnimator animation) {

                    ViewGroup.LayoutParams params = view.getLayoutParams();

                    params.height = (int) animation.getAnimatedValue();

                    String s  = String.valueOf(animation.getAnimatedValue());
                    //   params.width = (int) (Float.valueOf(s) / 4.0f * 3);
                    view.setLayoutParams(params);
                    Log.d("sssd-伸缩后的宽高",view.getMeasuredWidth() + "111"+ params.height +"");

                }

            });


            animator.start();

        }
    }

    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {

            mCameraPresenter.startCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            //configureTransform(width, height);
//            rl_parent.removeView(mTextureView);
//
//            RelativeLayout.LayoutParams layoutParams =
//                    new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
//                            RelativeLayout.LayoutParams.WRAP_CONTENT);
//
//            rl_parent.addView(mTextureView,0,layoutParams);


        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };

    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
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

    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

}
