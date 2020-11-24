package com.hxws.mediacapture.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.hxws.mediacapture.R;
import com.hxws.mediacapture.activity.SplitScreenActivity;

import java.io.File;
import java.util.ArrayList;

public class SelectVideoPopupWindow extends PopupWindow {

    private final static String TAG = "SelectVideoPopupWindow";

    Context mContext;

    public static SelectVideoPopupWindow sInstance;

    private boolean DEBUG = false;

    private View mPopupView;

    //protected ArrayList<VideoInfo> mVideoList = new ArrayList<VideoInfo>();
    //private ArrayList<VideoInfo> mData = null;

    private ListView mLvVideoInfo;
    private VideoInfosAdapter mVideoInfosAdapter;// = new VideoInfosAdapter();
    private static ArrayList<File> filelist = new ArrayList<>();


    @SuppressLint("HandlerLeak")
    public Handler mHandler = new Handler() {

        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            switch (msg.what) {

                default:
                    break;

            }
        }
    };

    public SelectVideoPopupWindow(Context context,ArrayList<File> files) {
        mContext = context;
        filelist = files;

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mPopupView = inflater.inflate(R.layout.popup_window, null);

        //设置SelectPicPopupWindow的View
        this.setContentView(mPopupView);
        //设置SelectPicPopupWindow弹出窗体的宽
        this.setWidth(LinearLayout.LayoutParams.MATCH_PARENT);
        //设置SelectPicPopupWindow弹出窗体的高
        this.setHeight(LinearLayout.LayoutParams.WRAP_CONTENT);
        //设置SelectPicPopupWindow弹出窗体可点击
        this.setFocusable(true);
        //设置SelectPicPopupWindow弹出窗体动画效果
        this.setAnimationStyle(R.style.Animation);
        //实例化一个ColorDrawable颜色为半透明
        //ColorDrawable dw = new ColorDrawable(0xb0000000);
        ColorDrawable dw = new ColorDrawable(0x30FFFFFF);
        //设置SelectPicPopupWindow弹出窗体的背景
        this.setBackgroundDrawable(dw);


        mLvVideoInfo = (ListView) mPopupView.findViewById(R.id.lv_popup);

        mLvVideoInfo.setOnItemClickListener(new ItemClickEvent());

        //bind Adapter

        mVideoInfosAdapter = new VideoInfosAdapter(files);

        mLvVideoInfo.setAdapter(mVideoInfosAdapter);

        mVideoInfosAdapter.notifyDataSetChanged();

        showAtLocation(SplitScreenActivity.getInstance().findViewById(R.id.video_sfv),
                                Gravity.BOTTOM| Gravity.CENTER_HORIZONTAL, 0, 0);

        //mMenuView添加OnTouchListener监听判断获取触屏位置如果在选择框外面则销毁弹出框
        mPopupView.setOnTouchListener(new OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {

                int height = mPopupView.findViewById(R.id.pop_layout).getTop();
                int y=(int) event.getY();
                if(event.getAction() == MotionEvent.ACTION_UP){
                    if (y < height){
                        dismiss();

                    }
                }
                return true;
            }
        });
    }

    /*
    //find VideoInfo
    private boolean isVideoInfoFile(String fileName) {

        if (fileName.endsWith(".mp4")
                || fileName.endsWith(".mkv")
                || fileName.endsWith(".avi")
                || fileName.endsWith(".mpg")
                || fileName.endsWith(".mpeg")
                || fileName.endsWith(".ts")
                || fileName.endsWith(".rmvb")
                || fileName.endsWith(".m2ts")
                ||fileName.endsWith(".f4v")){
            return true;
        }
        return false;
    }

    class VideoFileFilter implements FilenameFilter {
        public boolean accept ( File directory, String file ) {
            String dir = directory.getPath();
            if ( new File ( directory, file ).isDirectory() ) {
                return true;
            } else if ( isVideoInfoFile ( file.toLowerCase() )) {
                return true;
            } else {
                return false;
            }
        }
    }

        private List<String> getVideoInfoFileList(String directory) {
            ArrayList<String> VideoInfoFiles = new ArrayList<String>();

            File parentDir = new File(directory);

            File[] files = parentDir.listFiles();

            if (files == null) {
                if (DEBUG) Log.e(TAG, "No file listed in " + directory);

                return null;
            }

            //Log.i(TAG,"files: " + files);

            for (File file : files) {

                if (DEBUG) Log.i(TAG,"files " + file.getName());

                if (file.isDirectory()) {
                    String dirsName = file.getName();
                    String dirsPath = file.getPath();
                    if (DEBUG) Log.i(TAG,"dirsPath " + dirsPath);
                    if (getVideoInfoFileList(dirsPath) != null){
                        VideoInfoFiles.addAll(getVideoInfoFileList(dirsPath));
                    }
                    continue;
                } else {
                    String fileName = file.getName();
                    String dirsPath = file.getPath();
                    if (DEBUG) Log.i(TAG,"dirsPath " + dirsPath);
                    if (isVideoInfoFile(fileName)){
                        VideoInfoFiles.add(dirsPath);
                        if (DEBUG) Log.i(TAG,"VideoInfoFiles.add  " + dirsPath);
                    }

                }

            }

            //Log.i(TAG,"VideoInfoFiles  " + VideoInfoFiles);
            return VideoInfoFiles;
        }

        private String getUsbPath(){

            String root = "/storage";

            String dirsPath = null;

            //ArrayList<String> PathInfoFiles = new ArrayList<String>();

            File parentDir = new File(root);

            File[] files = parentDir.listFiles();

            if (files == null) {
                if (DEBUG) Log.e(TAG, root+" No file listed in " + root);
                return null;
            }

            for (File file : files) {

                if (DEBUG) Log.i(TAG,root + " files: " + file.getName());

                if (file.isDirectory()) {
                    String dirsName = file.getName();
                    dirsPath = file.getPath();
                    if (DEBUG) Log.i(TAG,root+" dirsName " + dirsName);
                    if (DEBUG) Log.i(TAG,root+" dirsPath " + dirsPath);

                    if (dirsName.equals("emulated") || dirsName.equals("self")) {

                    } else {
                        if (DEBUG) Log.i(TAG,root+" dirsPath " + file.getPath());
                        dirsPath = file.getPath();
                        return dirsPath;
                    }

    //                if (getUsbPathList(dirsPath) != null){
    //
    //                    PathInfoFiles.addAll(getUsbPathList(dirsPath));
    //                }

                } else {
    //                String fileName = file.getName();
    //
    //                Log.i(TAG,root+" dirsName " + fileName);
    //
    //                PathInfoFiles.add(fileName);
                }
            }

            return null;

        }

        private void refreshList(){

            if (!mData.isEmpty()){
                mVideoInfosAdapter.clear();
            }

            String usbPath = USBReceiver.mountPath;

            String VideoInfoRootPath = Environment
                    .getExternalStorageDirectory().getPath()+ "/Movies/";

    //        SharedPreferences sp = getSharedPreferences("data", 0);
    //        SharedPreferences.Editor editor = sp.edit();
    //
    //        editor.putString("VIDEO_PATH", usbPath);
    //        editor.apply();

            if (DEBUG) Log.d(TAG, "onCreate: usbPath " + usbPath);

            if (usbPath == null){
                //usbPath = getUsbPath();
    //            if (usbPath == null){
    //                Log.i(TAG, "No usb found! bye!");
    //                finish();
    //            }

                String mnt_usb_path = getUsbPath();

                if (mnt_usb_path != null){

                    videoFilePath = mnt_usb_path + "/";
                }
                else {

                    if (DEBUG) Log.d(TAG,"VideoInfoRootPath: "+VideoInfoRootPath);
                    videoFilePath = VideoInfoRootPath;// + "/Movies/";
                }

            }
            else {

                videoFilePath = usbPath + "/";// + "/Android/" ;

                //if (DEBUG) Log.i(TAG,"VideoInfoFilePath：" + videoFilePath);
            }

            File fileVideoInfoDir = new File(videoFilePath);

            if (!fileVideoInfoDir.exists()) {
                if (DEBUG) Log.e(TAG, "No dir found in USB!");

                if (DEBUG) Log.d(TAG,"VideoInfoRootPath: "+VideoInfoRootPath);
                videoFilePath = VideoInfoRootPath;// + "/Movies/";

            }
            if (DEBUG) Log.i(TAG,"videoFilePath：" + videoFilePath);
            //List<String> VideoInfoFiles = new List<String>();
            //List<String> VideoInfoFiles = getVideoInfoFileList(VideoInfoRootPath + moviesPath);
            List<String> VideoInfoFiles = getVideoInfoFileList(videoFilePath);

            if (VideoInfoFiles != null && VideoInfoFiles .size() > 0) {

                int count = VideoInfoFiles.size();

                for (int i = 0; i < count; i++) {

                    String s = VideoInfoFiles.get(i);

                    addVideoInfo(s);

                }
            }

            if (!mData.isEmpty()){

                mVideoInfosAdapter = new VideoInfosAdapter();

                mLvVideoInfo.setAdapter(mVideoInfosAdapter);
                mVideoInfosAdapter.notifyDataSetChanged();
            }else {
                SplitScreenActivity.showToast(mContext.getResources().getString(R.string.noVideoFile));
            }

        }

        private void addVideoInfo(String VideoInfoName){

            VideoInfo videoInfo = new VideoInfo(VideoInfoName);

            if (!mData.contains(videoInfo)){
                mData.add(videoInfo);
            }
            else {
                return;
            }

        }

        public class VideoInfo {
            private String VideoInfoName;

            private int aIcon;


            VideoInfo(String VideoInfoName) {
                this.VideoInfoName = VideoInfoName;

                //this.aIcon = aIcon;
            }

            String getVideoInfoName() {
                return VideoInfoName;
            }

            public int getaIcon() {
                return aIcon;
            }

            public void setVideoInfoName(String macAddr) {
                this.VideoInfoName = VideoInfoName;
            }

            public void setaIcon(int aIcon) {
                this.aIcon = aIcon;
            }

        }
    */
    public class VideoInfosAdapter extends BaseAdapter {
        private static final String TAG = "VideoInfosAdapter";

        private boolean D = true;

        ArrayList<File> myFiles = new ArrayList<>();

        VideoInfosAdapter( ArrayList<File> files){
            myFiles = files;
        }

        @Override
        public int getCount() {
            return myFiles.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @SuppressLint("ViewHolder")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            //View view;
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.listitem_videos, parent, false);
                holder = new ViewHolder();
                //ImageView img_icon = (ImageView) convertView.findViewById(R.id.img_icon);
                holder.VideoInfoName = (TextView) convertView.findViewById(R.id.video_name);
                //img_icon.setBackgroundResource(mData.get(position).getaIcon());
                convertView.setTag(holder);   //将Holder存储到convertView中
            } else {
                //view = convertView;
                holder = (ViewHolder) convertView.getTag();
            }

            String fileName = myFiles.get(position).getName();

            holder.VideoInfoName.setText(fileName);

            return convertView;
        }

        void clear() {
            if (myFiles != null) {
                myFiles.clear();
            }
            notifyDataSetChanged();
        }

        class ViewHolder {
            TextView VideoInfoName;
        }

    }
    private static int lastPosition = 0;

    public class ItemClickEvent implements OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> l, View v, int position,
                                long id) {
            // TODO Auto-generated method stub

            String path = filelist.get(position).getPath();

            lastPosition = position;

            if (DEBUG) Log.d(TAG,"lastPosition: "+lastPosition);

            SplitScreenActivity.getInstance().resetVideoUrl(path);

            dismiss();
        }
    }

    public static void playNextPath(){

        if (filelist != null && !filelist.isEmpty()){

            lastPosition ++;

            if (lastPosition > filelist.size() - 1)
            {
                lastPosition = 0;
            }

            String path = filelist.get(lastPosition).getPath();
            Log.d(TAG,"lastPosition: " + lastPosition);

            if (path != null){
                SplitScreenActivity.getInstance().resetVideoUrl(path);
            }

        }

    }

}