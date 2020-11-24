package com.hxws.mediacapture.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hxws.mediacapture.R;
import com.hxws.mediacapture.camera1.CameraView;
import com.hxws.mediacapture.camera2.Camera2Fragment;
import com.hxws.mediacapture.receiver.USBReceiver;
import com.hxws.mediacapture.service.ScreenRecordService;
import com.hxws.mediacapture.service.ScreenUtil;
import com.hxws.mediacapture.utils.CommonUtil;
import com.hxws.mediacapture.utils.FileUtil;
import com.hxws.mediacapture.utils.TimeUtil;
import com.hxws.mediacapture.view.FixedTextureCameraView;
import com.hxws.mediacapture.view.FixedTextureVideoView;
import com.hxws.mediacapture.view.SelectVideoPopupWindow;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class SplitScreenActivity extends AppCompatActivity implements View.OnClickListener,
        SeekBar.OnSeekBarChangeListener
{
    private final static String TAG = "SplitScreenActivity";

    private boolean DEBUG = true;

    Context mContext;

    @SuppressLint("StaticFieldLeak")
    public static SplitScreenActivity sInstance = null;

    Bundle mBundle;

    //BroadcastReceiver
    private MyBroadcastReceiver mBroadcastReceiver;
    public final static String ACTION_CAMERA_STATUS = "com.hx.action.CAMERA_STATUS";
    public final static String ACTION_CAMERA_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    public final static String ACTION_CAMERA_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";

    public static final String START_INFO = "com.intent.SplitScreenActivity.start.info";

    private static final int REQUEST_FILE_PERMISSION = 1;
    private static final int REQUEST_ALL_PERMISSION = 123;

    private static final int VIDEO_REQUEST_CODE  = 100;

    private static final int RECORD_REQUEST_CODE  = 101;

    private static final String[] FILE_PERMISSIONS = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    //Layout
    // 外围的RelativeLayout容器
    private RelativeLayout llContentView;

    //扫描提示框
    private ProgressDialog mPdWatingScan = null;
    public static final String VIDEOPATH = "videopath";

    private boolean scanStop = false;

    private static final int MSG_HIDE_SHOW_DIALOG = 3;
    private static final int MSG_SHOW_WAIT_DIALOG = 4;
    private static final int MSG_NOTIFY_DATACHANGE = 5;
    private static final int WAITDIALOG_DISPALY_TIME = 500;
    private File[] mFiles;
    private ArrayList<File> files = new ArrayList();

    //scan thread
    public final static int END_OPERATION = 4;
    public final static int NEW_VIDEO = 5;
    protected int mStatus = -1;

    SelectVideoPopupWindow mVideoPopupWindow;

    //video

    public static String videoFilePath = null;


    //Camera1
    private CameraView mCameraPreview;
    //private Camera1Preview mCamera1View;
    private FrameLayout frameView;

    FixedTextureCameraView mCameraView;

    FixedTextureVideoView mVideoView;

    private TextView tv_NoCamDev;
    private TextView tv_choose_file;

    //control
    private ImageButton btn_search;
    private Button btn_start_pause;
    private Button btn_loop;

    private ImageButton btn_settings;

    private ImageButton btn_photo;

    private Button btn_cam_red_Bgtns;
    private Button btn_cam_add_Bgtns;
    private ImageButton btn_record;

    private TextView mTvTime;

    public SeekBar mSeekBar;
    private TextView startTime;
    public TextView endTime;

    private boolean shift_flag = false;

    private static int lastLength = 0;

    long quitTime = 0;

    public static final int UPDATE_TIME = 0x0001;
    public static final int OPEN_CAMERA = 0x0002;

    @SuppressLint("HandlerLeak")
    private  Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_TIME:
                    updateTime();
                    mHandler.sendEmptyMessageDelayed(UPDATE_TIME, 500);
                    break;

                case OPEN_CAMERA:
                    //loadCamera();
                    break;

                //扫描文件弹窗
                case MSG_SHOW_WAIT_DIALOG:

                    mPdWatingScan.show();

                    break;

                case MSG_HIDE_SHOW_DIALOG:
                    removeMessages ( MSG_SHOW_WAIT_DIALOG );

                    if ( mPdWatingScan != null ) {
                        mPdWatingScan.dismiss();
                        mPdWatingScan = null;
                    }
                    if (files != null && files.size() > 0){
                        if (!scanStop){
                            mVideoPopupWindow = new SelectVideoPopupWindow(mContext,files);
                        }
                    }
                    else {
                        SplitScreenActivity.showToast(mContext.getResources().getString(R.string.noVideoFile));
                    }

                    break;

                case MSG_NOTIFY_DATACHANGE:
                    removeMessages ( MSG_NOTIFY_DATACHANGE );

                    break;

                default:
                    break;

            }
        }
    };

    private void startScanThread() {
        Message nmsg = mHandler.obtainMessage ( MSG_SHOW_WAIT_DIALOG );
        mHandler.sendMessageDelayed ( nmsg, WAITDIALOG_DISPALY_TIME );

        scanStop = false;

        new Thread() {
            public void run() {
                //ArrayList<File> files = mPrefUtil.getStorageList(false);

                File[] files = FileUtil.getUsbFiles();

                if (files == null){
                    USBReceiver.mountPath = null;
                }
                files = FileUtil.getUsbFiles();
                if (DEBUG) Log.i(TAG,"files：" + Arrays.toString(files));

                if (files != null){
                    getList ( files );
                }

                mHandler.sendEmptyMessage ( MSG_HIDE_SHOW_DIALOG );
            }
        } .start();

        mPdWatingScan = ProgressDialog.show ( SplitScreenActivity.this,
                SplitScreenActivity.this
                        .getResources().getString ( R.string.scan_title ),
                SplitScreenActivity.this
                        .getResources().getString ( R.string.scan_tip ) ,
                false,true);

        mPdWatingScan.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_BACK){
                    scanStop = true;
                    mHandler.sendEmptyMessage ( MSG_HIDE_SHOW_DIALOG );
                }

                return false;
            }
        });

    }

    public void setCurrentList ( File directory ) {
        File[] tempFiles = directory.listFiles (new VideoFileFilter());
        for ( int i = 0; ( tempFiles != null ) && ( i < tempFiles.length );
              i++ ) {
            if ( tempFiles[i].isDirectory() ) {
                setCurrentList ( tempFiles[i] );
            } else {
                files.add ( tempFiles[i] );
            }
        }
    }

    public void getList ( ArrayList<File> dir ) {
        if ( dir == null) {
            return;
        }
        for (int i = 0; i < dir.size(); i++ ) {
            File directory = dir.get(i);
            setCurrentList ( directory );
        }
        mFiles = new File[files.size()];
        for ( int i = 0; i < files.size(); i++ ) {
            mFiles[i] = ( File ) files.get ( i );
        }
        mHandler.sendEmptyMessage ( MSG_NOTIFY_DATACHANGE );
    }

    public void getList ( File[] dir ) {
        for ( int j = 0; j < dir.length; j++ ) {
            setCurrentList ( dir[j] );
        }
        mFiles = new File[files.size()];
        for ( int i = 0; i < files.size(); i++ ) {
            mFiles[i] = ( File ) files.get ( i );
        }
        mHandler.sendEmptyMessage ( MSG_NOTIFY_DATACHANGE );
    }

    static class VideoFileFilter implements FilenameFilter {
        public boolean accept ( File directory, String file ) {
            String dir = directory.getPath();
            if ( new File ( directory, file ).isDirectory() ) {
                return true;
            } else if (  FileUtil.isVideoFile(file.toLowerCase()) ) {
                return true;
            } else {
                return false;
            }
        }
    }

    public class myRunnable implements Runnable{

        @Override
        public void run() {

        }
    }

    public static SplitScreenActivity getInstance(){

        if(sInstance == null){
            sInstance = new SplitScreenActivity();
        }
        return sInstance;
    }

    public static boolean isActivityExist(){

        if (sInstance == null){
            return false;
        }

        return !sInstance.isFinishing() && !sInstance.isDestroyed();
    }

    @Override
    public void onBackPressed() {

        if (System.currentTimeMillis() - quitTime <= 2 * 1000) {

            super.onBackPressed();
            if (DEBUG) Log.d(TAG, "----onBackPressed----");
        } else {

            SplitScreenActivity.showToast(getResources().getString(R.string.press_exit_again));
            quitTime = System.currentTimeMillis();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (DEBUG) Log.d(TAG, "----onPause----");

        if (mVideoView != null){
            mVideoView.pause();
            lastLength = mVideoView.getCurrentPosition();
        }

    }

    @Override
    protected void onResume() {

        super.onResume();

        if (DEBUG) Log.d(TAG, "----onResume----");

        if (mVideoView != null){

            if (lastLength > 0){
                mVideoView.seekTo(lastLength);
                mVideoView.start();
            }
        }
    }

    @Override
    protected void onDestroy() {

        if (DEBUG) Log.d(TAG, "----onDestroy----");

        mVideoView.suspend();
        mHandler.removeMessages(UPDATE_TIME);
        unregisterReceiver(mBroadcastReceiver);
        unbindService(mServiceConnection);

        sInstance = null;

        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (DEBUG) Log.d(TAG, "----onCreate----");

        setContentView(R.layout.activity_main_surface_default);

        sInstance = this;
        mContext = this;

        mBundle = savedInstanceState;

        initView();

        bindVideoView();

        mCameraView = new FixedTextureCameraView(mContext);

        //getPermission();
        if (getReadWritePermssions()){
            loadCamera();
        }
        startScreenRecordService();

        registerMyReceiver();
    }

    private ServiceConnection mServiceConnection;

    /**
     * 开启录制 Service
     */
    private void startScreenRecordService(){

        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                ScreenRecordService.RecordBinder recordBinder = (ScreenRecordService.RecordBinder) service;
                ScreenRecordService screenRecordService = recordBinder.getRecordService();
                ScreenUtil.setScreenService(screenRecordService);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };

        Intent intent = new Intent(this, ScreenRecordService.class);
        bindService(intent, mServiceConnection, BIND_AUTO_CREATE);

        CommonUtil.init(this);

        ScreenUtil.addRecordListener(recordListener);
    }

    void registerMyReceiver(){

        mBroadcastReceiver = new MyBroadcastReceiver();

        IntentFilter filter = new IntentFilter();

        filter.addAction(ACTION_CAMERA_STATUS);//ACTION_CAMERA_ATTACHED
        filter.addAction(ACTION_CAMERA_ATTACHED);
        filter.addAction(ACTION_CAMERA_DETACHED);

        registerReceiver(mBroadcastReceiver,filter);
    }

    public static void showToast(final String text) {
        final Activity activity = getInstance();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, text, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    public void loadCamera(){

        //if (null == mBundle) {

            //getSupportFragmentManager().beginTransaction().remove(Camera2Fragment.getInstance()).commit();
            //getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            (getSupportFragmentManager().beginTransaction())
                    .replace(R.id.container, Camera2Fragment.newInstance())
                    .commit();
        //}
//
        //camera1
//        mCameraPreview = new Camera1Preview(this);
//        //mCamera1View = new Camera1Preview(this);
//        frameView.addView((View) mCameraPreview);
    }
    public void reLoadFragView(){
        (getSupportFragmentManager().beginTransaction())
                .replace(R.id.container, Camera2Fragment.newInstance())
                .commit();
        getSupportFragmentManager().findFragmentById(R.id.container).onResume();
    }

    private int getStartInfo(){

        return getIntent().getIntExtra(START_INFO,1);
    }

    private void initView(){

        //seekBar
        mSeekBar = (SeekBar) findViewById(R.id.tv_progess);

        startTime = (TextView) findViewById(R.id.tv_start_time);
        endTime = (TextView) findViewById(R.id.tv_end_time);

        btn_start_pause = (Button) findViewById(R.id.btn_play_pause);
        btn_search = (ImageButton) findViewById(R.id.btn_search);
        btn_loop = (Button) findViewById(R.id.btn_loop);

        btn_settings = (ImageButton)findViewById(R.id.btn_settings);

        //camera1
        frameView = (FrameLayout) findViewById(R.id.container);

        //camera2
        //btn_cam_red_Bgtns = findViewById(R.id.btn_cam_reduce_Brightness);
        //btn_cam_add_Bgtns = findViewById(R.id.btn_cam_add_Brightness);
        btn_photo = (ImageButton)findViewById(R.id.btn_photo);

        btn_record = (ImageButton)findViewById(R.id.btn_record);
        mTvTime = findViewById(R.id.tv_record_time);

        tv_NoCamDev = (TextView)findViewById(R.id.NoCameraDevice);
        tv_choose_file = (TextView)findViewById(R.id.tip_choose_file);

        btn_search.setOnClickListener(this);
        btn_search.setFocusable(true);

        btn_start_pause.setOnClickListener(this);
        btn_loop.setOnClickListener(this);

        btn_settings.setOnClickListener(this);

        //camera2
//        btn_cam_red_Bgtns.setOnClickListener(this);
//        btn_cam_add_Bgtns.setOnClickListener(this);
        btn_photo.setOnClickListener(this);

        btn_record.setOnClickListener(this);

        mSeekBar.setOnSeekBarChangeListener(this);
    }

    private void bindVideoView() {

        mVideoView = (FixedTextureVideoView) findViewById(R.id.video_sfv_show);

        mVideoView.setMediaController(new MediaController(mContext));
    }

    void postVideoView(final int videoWidth,final int videoHeight){
        mVideoView.post(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG,"mVideoView.post---Width: "+mVideoView.getWidth()
                        +" ,Height: "+ mVideoView.getHeight());

                mVideoView.setFixedSize(videoWidth, videoHeight);
                mVideoView.transformVideo(mVideoView.getWidth(),mVideoView.getHeight());
                //mVideoView.requestLayout();
                mVideoView.invalidate();
            }
        });
    }

    void postCameraView(final int Width,final int Height){
//        mCameraView.post(new Runnable() {
//            @Override
//            public void run() {
//                Log.d(TAG,"mCameraView.post---Width: "+mCameraView.getWidth()
//                        +" ,Height: "+ mCameraView.getHeight());
//
//                mCameraView.setAspectRatio(Width, Height);
//                //mCameraView.requestLayout();
//                mCameraView.invalidate();
//            }
//        });
        mCameraView.setAspectRatio(Width, Height);
        mCameraView.invalidate();
    }

    public void changeVideoSize(int width,int height){

        RelativeLayout RL_SvfView = findViewById(R.id.RL_SurfaceView);

        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) RL_SvfView.getLayoutParams();
        layoutParams.width = width;
        layoutParams.height = height;
        //layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        RL_SvfView.setLayoutParams(layoutParams);
    }

    public void changeCameraSize(int width,int height){

        if (width > 0 && height > 0){

            RelativeLayout FL_Camera_area = findViewById(R.id.RL_camera_area);
            //FL_Camera_area.removeView(frameView);
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) FL_Camera_area.getLayoutParams();
            layoutParams.width = width;
            layoutParams.height = height;
            //layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
            FL_Camera_area.setLayoutParams(layoutParams);

            //FL_Camera_area.addView(frameView,0,layoutParams);
        }

        loadCamera();

    }

    public void shiftView(int video_pos,int camera_pos){

        RelativeLayout RL_video_sfv_area = findViewById(R.id.video_sfv);

        RelativeLayout.LayoutParams RL_video_area_LP =
                //(RelativeLayout.LayoutParams) RL_video_sfv_area.getLayoutParams();
                new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);

        RL_video_area_LP.addRule(video_pos,R.id.second_all_center);

        RL_video_sfv_area.setLayoutParams(RL_video_area_LP);

        //
        RelativeLayout RL_camera_sfv_area = findViewById(R.id.camera_sfv);
        RelativeLayout.LayoutParams RL_camera_area_LP =
                //(RelativeLayout.LayoutParams) RL_camera_sfv_area.getLayoutParams();
                new RelativeLayout.LayoutParams
                (ViewGroup.LayoutParams.MATCH_PARENT,
                 ViewGroup.LayoutParams.MATCH_PARENT);
        RL_camera_area_LP.addRule(camera_pos,R.id.second_all_center);
        RL_camera_sfv_area.setLayoutParams(RL_camera_area_LP);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.btn_search:
                searchFiles();
                break;

            case R.id.btn_play_pause:

                if (mVideoView.isInPlaybackState()){
                    if (mVideoView.isPlaying()){
                        mVideoView.pause();
                        btn_start_pause.setText(R.string.play);
                    } else {
                        mVideoView.start();
                        btn_start_pause.setText(R.string.pause);
                    }
                }

                break;

            case R.id.btn_loop:

                if (mVideoView.isInPlaybackState()){
                    if (mVideoView.getmMediaPlayer().isLooping()){
                        mVideoView.setLoop(false);
                        btn_loop.setText(R.string.order);
                        SplitScreenActivity.showToast(
                                mContext.getResources().getString(R.string.order));
                    } else {
                        mVideoView.setLoop(true);
                        btn_loop.setText(R.string.loop);
                        SplitScreenActivity.showToast(
                                mContext.getResources().getString(R.string.loop));
                    }
                }

                break;

            case R.id.btn_settings:

                showSettingDialog();

                break;
            /*
            case R.id.btn_cam_add_Brightness:
                Camera2Fragment.getInstance().addBrightness();
                //Camera2Fragment.getInstance().addOpticalZoom();
                //Camera1Fragment.getInstance().addCamera1Zoom();
                break;

            case R.id.btn_cam_reduce_Brightness:
                Camera2Fragment.getInstance().reduceBrightness();
                //Camera2Fragment.getInstance().reduceOpticalZoom();
                //Camera1Fragment.getInstance().reduceCamera1Zoom();
                break;
            */
            case R.id.btn_photo:
                Camera2Fragment.getInstance().takePicture();
                break;

            case R.id.btn_record:

                if (ScreenUtil.isCurrentRecording()){
                    ScreenUtil.stopScreenRecord(this);
                }
                else {
                    ScreenUtil.startScreenRecord(this,RECORD_REQUEST_CODE);
                }

                break;

            default:
                break;
        }
    }

    public void resetVideoUrl(String path){

        if (path != null && !path.equals("")){
            mVideoView.setVideoPath(path);
            mVideoView.start();
            btn_start_pause.setText(R.string.pause);
            mHandler.sendEmptyMessageDelayed(UPDATE_TIME, 500);

            tv_choose_file.setVisibility(View.INVISIBLE);
        } else {
            tv_choose_file.setVisibility(View.VISIBLE);
            SplitScreenActivity.showToast
                    (this.getResources().getString(R.string.Video_Parsing_failed));
        }
    }

    private void updateTime() {

        if (mVideoView != null){
            startTime.setText(TimeUtil.secToTime(mVideoView.getCurrentPosition() / 1000));
            //Log.d(TAG, "updateTime: "+ mVideoView.getCurrentPosition());

            mSeekBar.setProgress(mVideoView.getCurrentPosition());
        }
    }

    //OnSeekBarChangeListener
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
        //Log.d(TAG,"onProgressChanged: b: "+ b);
        if(mVideoView != null && b){
            //Log.d(TAG,"onProgressChanged: seekTo: "+ progress);

            //Log.d(TAG,"onProgressChanged: setProgress: "+ mVideoView.getCurrentPosition() / 1000);

            mVideoView.seekTo(progress);
            mSeekBar.setProgress(mVideoView.getCurrentPosition());
        }
    }
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    void getPermission()
    {

//        for (String str : FILE_PERMISSIONS) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                if (this.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
//                    this.requestPermissions(FILE_PERMISSIONS, REQUEST_FILE_PERMISSION);
//                    return;
//                }
//            }
//        }

//        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
//            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(this, FILE_PERMISSIONS, REQUEST_FILE_PERMISSION);
//            }
//        }

        //PermissionUtils.checkPermission(this);

    }

    boolean getReadWritePermssions(){

        List<String> permissionList = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if (DEBUG) Log.i(TAG,"add WRITE_EXTERNAL_STORAGE");
            }

            if (checkSelfPermission(Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {

                permissionList.add(Manifest.permission.CAMERA);
                if (DEBUG) Log.i(TAG,"add CAMERA");
            }

            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {

                permissionList.add(Manifest.permission.RECORD_AUDIO);
                if (DEBUG) Log.i(TAG,"add RECORD_AUDIO");
            }

            if (!permissionList.isEmpty()) {
                requestPermissions(
                        permissionList.toArray(new String[permissionList.size()])
                        , REQUEST_ALL_PERMISSION);
                if (DEBUG) Log.i(TAG,"toArray = "+permissionList);
                return false;
            } else {
                if (DEBUG) Log.i(TAG,"permissionList.isEmpty()");
                return true;
            }
        }

        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        if (requestCode == REQUEST_FILE_PERMISSION) {
//            if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED))
//            {
//                //Log.d(TAG, "获取到权限了");
//
//            } else {
//                //Log.d(TAG, "未获取到权限");
//            }

//            if (grantResults.length == FILE_PERMISSIONS.length) {
//                for (int result : grantResults) {
//                    if (result != PackageManager.PERMISSION_GRANTED) {
//                        break;
//                    }
//                }
//            }
            Log.d(TAG,"onRequestPermissionsResult: REQUEST_FILE");
        }
        else if (requestCode == REQUEST_ALL_PERMISSION){
            Log.d(TAG,"onRequestPermissionsResult: REQUEST_ALL");
            loadCamera();
        }
        else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            Log.d(TAG,"onRequestPermissionsResult else");
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_MENU)
        {
            showSettingDialog();
        }

        return super.onKeyDown(keyCode, event);
    }

    private void changeplayerSpeed(float speed) {
        // this checks on API 23 and up
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (mVideoView.getmMediaPlayer() != null){
                if (mVideoView.isPlaying()) {
                    mVideoView.getmMediaPlayer().setPlaybackParams
                            (mVideoView.getmMediaPlayer().getPlaybackParams().setSpeed(speed));
                } else {
                    mVideoView.getmMediaPlayer().setPlaybackParams
                            (mVideoView.getmMediaPlayer().getPlaybackParams().setSpeed(speed));
                    mVideoView.pause();
                }
            }
        }
    }

    private void showPlaySpeedSettingDialog() {

        final String[] items = getResources().getStringArray(R.array.list_speed_setting);
        AlertDialog.Builder listDialog =
                new AlertDialog.Builder(SplitScreenActivity.this,R.style.AlertDialog);
        listDialog.setTitle(R.string.title_speed_settings);
        listDialog.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                switch (which) {

                    case 0:
                        changeplayerSpeed((float)0.5);
                    break;

                    case 1:
                        changeplayerSpeed((float)0.75);
                    break;

                    case 2:
                        changeplayerSpeed((float)1);
                        break;

                    case 3:
                        changeplayerSpeed((float)1.25);
                        break;

                    case 4:
                        changeplayerSpeed((float)1.5);
                        break;

                    case 5:
                        changeplayerSpeed((float)2);
                        break;
                    default:
                    break;
                }
            }
        });
        listDialog.show();
    }

    void searchFiles(){

        if (!files.isEmpty()){
            files.clear();
        }

        startScanThread();


//        mVideoPopupWindow = new SelectVideoPopupWindow(mContext);
//
//        mVideoPopupWindow.showAtLocation
//                (SplitScreenActivity.this.findViewById(R.id.video_sfv),
//                        Gravity.BOTTOM| Gravity.CENTER_HORIZONTAL, 0, 0);

//        Intent intent = new Intent(this, ScanActivity.class);
//
//        //intent.putExtra(SplitScreenActivity.START_INFO,value);
//
//        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
//
//        //startActivity(intent);
//        startActivityForResult(intent,VIDEO_REQUEST_CODE);
//
//        overridePendingTransition(R.anim.enter_anim,R.anim.out_anim);

    }

    public void setLightness(float lightness){
        WindowManager.LayoutParams layoutParams =getWindow().getAttributes();
        //屏幕的亮度,最大是255
        layoutParams.screenBrightness =layoutParams.screenBrightness+lightness/255f;
        if(layoutParams.screenBrightness>1){
            layoutParams.screenBrightness=1;
        }else if(layoutParams.screenBrightness<0.2){
            layoutParams.screenBrightness=0.2f;
        }
        getWindow().setAttributes(layoutParams);
    }


    private void showSettingDialog() {

        final String[] items = getResources().getStringArray(R.array.list_setting);
        AlertDialog.Builder listDialog =
                new AlertDialog.Builder(SplitScreenActivity.this,R.style.AlertDialog);
        listDialog.setTitle(R.string.title_settings);
        listDialog.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                switch (which){

                    case 0:

                        searchFiles();

//                        new Thread(new Runnable() {
//                            @Override
//                            public void run() {
//
//
//                                SplitScreenActivity.this.runOnUiThread(new Runnable() {
//                                    @Override
//                                    public void run() {
//
//                                    }
//                                });
//                            }
//                        }).start();

                        break;
                    case 1:
                        if (shift_flag){
                            shiftView(RelativeLayout.START_OF,RelativeLayout.END_OF);
                            shift_flag = false;
                        }
                        else {
                            shiftView(RelativeLayout.END_OF,RelativeLayout.START_OF);
                            shift_flag = true;
                        }

                        break;
                    case 2:
                        showPlaySpeedSettingDialog();
                        break;
                    case 3:
                        showSizeSettingDialog(1);
                        break;
                    case 4:
                        showSizeSettingDialog(2);
                    default:
                        break;
                }
            }
        });
        listDialog.show();
    }

    private void showSizeSettingDialog(final int isVideo1OrCamera2) {
        final String[] items = getResources().getStringArray(R.array.list_size_setting);
        AlertDialog.Builder listDialog =
                new AlertDialog.Builder(SplitScreenActivity.this,R.style.AlertDialog);
        listDialog.setTitle(R.string.title_size_settings);

        listDialog.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                switch (which){

                    case 0:
                        //centre
                        if (isVideo1OrCamera2 == 1){
                            //AutoChangeVideoSize(mCurrentMediaPlayer, true);

                            if (mVideoView.isInPlaybackState()){
                                changeVideoSize(1280,600);
                                postVideoView(0,0);
                            }
                        }
                        else if (isVideo1OrCamera2 == 2){
                            changeCameraSize(1280,525);
                            postCameraView(0,0);
                        }

                        break;
                    case 1:
                        //full screen
                        if (isVideo1OrCamera2 == 1) {

                            if (mVideoView.isInPlaybackState()){
                                changeVideoSize(1280,850);
                                postVideoView(1280,850);
                            }
                        }
                        else if (isVideo1OrCamera2 == 2){
                            changeCameraSize(1280,1080);
                            postCameraView(850,1080);
                        }
                        break;
                    default:
                        break;

                }
            }
        });
        listDialog.show();
    }

    private class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            boolean isCameraOpen = intent.getBooleanExtra("CAMERA_STATUS",true);

            if(ACTION_CAMERA_STATUS.equals(intent.getAction())){

                Log.d(TAG, "onReceive: isCameraOpen is "+isCameraOpen);
                if (isCameraOpen){

                    tv_NoCamDev.setVisibility(View.GONE);
                }
                else {
                    tv_NoCamDev.setVisibility(View.VISIBLE);
                }

            }

            if (ACTION_CAMERA_ATTACHED.equals(intent.getAction())){
                Log.d(TAG, "ACTION_CAMERA_ATTACHED");
                //Log.d(TAG, "onReceive: isCameraOpen is "+isCameraOpen);

                //tv_NoCamDev.setVisibility(View.GONE);
                //loadCamera();
                reLoadFragView();
            }

            if (ACTION_CAMERA_DETACHED.equals(intent.getAction())){
                Log.d(TAG, "ACTION_CAMERA_DETACHED");
                //Log.d(TAG, "onReceive: DETACHED isCameraOpen is "+isCameraOpen);

                //tv_NoCamDev.setVisibility(View.VISIBLE);
                //loadCamera();
                reLoadFragView();

//                if (isCameraOpen){
//                    tv_NoCamDev.setVisibility(View.INVISIBLE);
//                }
//                else {
//                    tv_NoCamDev.setVisibility(View.VISIBLE);
//                }

            }
        }
    }

    private ScreenUtil.RecordListener recordListener = new ScreenUtil.RecordListener() {
        @Override
        public void onStartRecord() {
            //btn_record.setText(R.string.stop_record);
            mTvTime.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPauseRecord() {

        }

        @Override
        public void onResumeRecord() {

        }

        @Override
        public void onStopRecord(String stopTip) {
            //SplitScreenActivity.showToast(stopTip);
            //btn_record.setText(R.string.start_record);
            mTvTime.setVisibility(View.INVISIBLE);

            SplitScreenActivity.showToast("文件保存在： "+ScreenUtil.getScreenRecordFilePath());
        }

        @Override
        public void onRecording(String timeTip) {
            mTvTime.setText(timeTip);
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d(TAG,"onActivityResult: "+requestCode+" , "+resultCode+" , ");

        if (requestCode == RECORD_REQUEST_CODE && resultCode == RESULT_OK) {

            try {
                ScreenUtil.setUpData(resultCode,data);
            } catch (Exception e) {
                e.printStackTrace();
            }

            //Log.d(TAG,"onActivityResult RESULT_OK");
        }

    }

}
