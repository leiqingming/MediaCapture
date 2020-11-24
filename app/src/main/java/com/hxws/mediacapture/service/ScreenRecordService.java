package com.hxws.mediacapture.service;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.RequiresApi;

import com.hxws.mediacapture.R;
import com.hxws.mediacapture.utils.CommonUtil;
import com.hxws.mediacapture.utils.FileUtil;
import com.hxws.mediacapture.utils.TimeUtil;

import java.io.IOException;

import static com.hxws.mediacapture.utils.FileUtil.getSaveDirectory;

public class ScreenRecordService extends Service implements Handler.Callback{

  private MediaProjectionManager mProjectionManager;
  private MediaProjection mMediaProjection;
  private MediaRecorder mMediaRecorder;
  private VirtualDisplay mVirtualDisplay;

  private boolean mIsRunning;
  //private int width = 720;
  //private int height = 1080;
  //private int dpi;

  private int mRecordWidth = CommonUtil.getScreenWidth();
  private int mRecordHeight = CommonUtil.getScreenHeight();
  private int mScreenDpi = CommonUtil.getScreenDpi();

  private int mResultCode;
  private Intent mResultData;

  //录屏文件的保存地址
  private String mRecordFilePath;

  private Handler mHandler;

  private static final int MSG_TYPE_COUNT_DOWN = 110;

  //已经录制多少秒了
  private int mRecordSeconds = 0;

  @Override
  public boolean handleMessage(Message msg) {

      switch (msg.what) {

          case MSG_TYPE_COUNT_DOWN: {

              String str = null;
              boolean enough = FileUtil.getSDFreeMemory() / (1024 * 1024) < 4;
              if (enough) {
                //空间不足，停止录屏
                str = getString(R.string.record_space_tip);
                stopRecord(str);
                mRecordSeconds = 0;
                break;
              }

              mRecordSeconds++;
              int minute = 0, second = 0;
              if (mRecordSeconds >= 60) {
                minute = mRecordSeconds / 60;
                second = mRecordSeconds % 60;
              } else {
                second = mRecordSeconds;
              }

              if (mRecordSeconds >= 10 * 60){
                  ScreenUtil.onRecording( minute + ":" + (second < 10 ? "0" + second : second + ""));
              }
              else {
                  ScreenUtil.onRecording("0" + minute + ":" + (second < 10 ? "0" + second : second + ""));
              }


              //if (mRecordSeconds < 3 * 60) {
                mHandler.sendEmptyMessageDelayed(MSG_TYPE_COUNT_DOWN, 1000);
              //} else if (mRecordSeconds == 3 * 60) {
              //  str = getString(R.string.record_time_end_tip);
              //  stopRecord(str);
              //  mRecordSeconds = 0;
              //}

              break;
          }
      }

    return false;
  }

  @Override
  public IBinder onBind(Intent intent) {
    return new RecordBinder();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    return START_STICKY;
  }

  @Override
  public void onCreate() {
    super.onCreate();
//    HandlerThread serviceThread = new HandlerThread("service_thread",
//            android.os.Process.THREAD_PRIORITY_BACKGROUND);
//
//    serviceThread.start();

    mIsRunning = false;
    mMediaRecorder = new MediaRecorder();

    mHandler = new Handler(Looper.getMainLooper(), this);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

  }

  public void setMediaProject(MediaProjection project) {
      mMediaProjection = project;
  }

  public boolean isReady(){
      return  mMediaProjection != null && mResultData != null;
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  public void clearRecordElement(){
      clearAll();
      if (mMediaRecorder != null){
        mMediaRecorder.reset();
        mMediaRecorder.release();
        mMediaRecorder = null;
      }
      mResultData = null;
      mIsRunning =false;
  }

  public boolean isRunning() {
      return mIsRunning;
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public void setResultData(int resultCode, Intent resultData){
    mResultCode = resultCode;
    mResultData = resultData;

    mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    if (mProjectionManager != null){
        mMediaProjection = mProjectionManager.getMediaProjection(mResultCode,mResultData);
    }
  }

  public void setConfig(int width, int height, int dpi) {
      this.mRecordWidth = width;
      this.mRecordHeight = height;
      this.mScreenDpi= dpi;
  }

  public boolean startRecord() {
    if (mMediaProjection == null || mIsRunning) {
      return false;
    }

    setUpMediaRecorder();

    createVirtualDisplay();

    mMediaRecorder.start();

    ScreenUtil.startRecord();

    //最多录制三分钟
    mHandler.sendEmptyMessageDelayed(MSG_TYPE_COUNT_DOWN,1000);

    mIsRunning = true;

    return true;
  }

  public boolean stopRecord(String tip) {
    if (!mIsRunning) {
      return false;
    }
    mIsRunning = false;

    try {
      mMediaRecorder.stop();
      mMediaRecorder.reset();
      mMediaRecorder = null;
      mVirtualDisplay.release();
      mMediaProjection.stop();

    }catch (Exception e){
      e.printStackTrace();
      mMediaRecorder.release();
      mMediaRecorder = null;
    }

    mMediaProjection = null;

    mHandler.removeMessages(MSG_TYPE_COUNT_DOWN);

    ScreenUtil.stopRecord(tip);

    if (mRecordSeconds <= 2 ){
        FileUtil.deleteSDFile(mRecordFilePath);
    } else {
        //通知系统图库更新
        FileUtil.fileScanVideo(this,mRecordFilePath,mRecordWidth,mRecordHeight,mRecordSeconds);
    }

    mRecordSeconds = 0;

    return true;
  }

  public void pauseRecord(){
      if (mMediaRecorder != null ){
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mMediaRecorder.pause();
          }
      }

  }

  public void resumeRecord(){
      if (mMediaRecorder != null ){
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mMediaRecorder.resume();
          }
      }
  }

  private void createVirtualDisplay() {
    mVirtualDisplay = mMediaProjection.createVirtualDisplay("MainScreen", mRecordWidth, mRecordHeight, mScreenDpi,
        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder.getSurface(), null, null);
  }

  private void setUpMediaRecorder() {

      mRecordFilePath = getSaveDirectory()  + TimeUtil.getCurTime() + ".mp4";

      if (mMediaRecorder == null){
          mMediaRecorder = new MediaRecorder();
      }

      mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
      mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
      mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
      mMediaRecorder.setOutputFile(mRecordFilePath);
      mMediaRecorder.setVideoSize(mRecordWidth, mRecordHeight);
      mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
      mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
      mMediaRecorder.setVideoEncodingBitRate(5 * 1024 * 1024);//(int) (mRecordWidth * mRecordHeight * 3.6));
      mMediaRecorder.setVideoFrameRate(30);
      try {
        mMediaRecorder.prepare();
      } catch (IOException e) {
        e.printStackTrace();
      }
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  public void clearAll(){
    if (mMediaProjection != null){
      mMediaProjection.stop();
      mMediaProjection = null;
    }
  }

  public String getRecordFilePath(){

    return mRecordFilePath;
  }

  public class RecordBinder extends Binder {
    public ScreenRecordService getRecordService() {
      return ScreenRecordService.this;
    }
  }
}