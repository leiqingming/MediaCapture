package com.hxws.mediacapture.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.hxws.mediacapture.receiver.USBReceiver;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileUtil {

    private final static String TAG = "FileUtil";

    /**
     * 删除SD卡中的文件或目录
     *
     * @param path
     * @return
     */
    public static boolean deleteSDFile(String path) {
        return deleteSDFile(path, false);
    }

    /**
     * 删除SD卡中的文件或目录
     *
     * @param path
     * @param deleteParent true为删除父目录
     * @return
     */
    public static boolean deleteSDFile(String path, boolean deleteParent) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }

        File file = new File(path);
        if (!file.exists()) {
            //不存在
            return true;
        }
        return deleteFile(file, deleteParent);
    }

    /**
     * @param file
     * @param deleteParent true为删除父目录
     * @return
     */
    public static boolean deleteFile(File file, boolean deleteParent) {
        boolean flag = false;
        if (file == null) {
            return flag;
        }
        if (file.isDirectory()) {
            //是文件夹
            File[] files = file.listFiles();
            if (files.length > 0) {
                for (int i = 0; i < files.length; i++) {
                    flag = deleteFile(files[i], true);
                    if (!flag) {
                        return flag;
                    }
                }
            }
            if (deleteParent) {
                flag = file.delete();
            }
        } else {
            flag = file.delete();
        }
        file = null;
        return flag;
    }

    /**
     * 添加到媒体数据库
     *
     * @param context 上下文
     */
    public static Uri fileScanVideo(Context context, String videoPath, int videoWidth, int videoHeight,
                                    int videoTime) {

        File file = new File(videoPath);
        if (file.exists()) {

            Uri uri = null;

            long size = file.length();
            String fileName = file.getName();
            long dateTaken = System.currentTimeMillis();

            ContentValues values = new ContentValues(11);
            values.put(MediaStore.Video.Media.DATA, videoPath); // 路径;
            values.put(MediaStore.Video.Media.TITLE, fileName); // 标题;
            values.put(MediaStore.Video.Media.DURATION, videoTime * 1000); // 时长
            values.put(MediaStore.Video.Media.WIDTH, videoWidth); // 视频宽
            values.put(MediaStore.Video.Media.HEIGHT, videoHeight); // 视频高
            values.put(MediaStore.Video.Media.SIZE, size); // 视频大小;
            values.put(MediaStore.Video.Media.DATE_TAKEN, dateTaken); // 插入时间;
            values.put(MediaStore.Video.Media.DISPLAY_NAME, fileName);// 文件名;
            values.put(MediaStore.Video.Media.DATE_MODIFIED, dateTaken / 1000);// 修改时间;
            values.put(MediaStore.Video.Media.DATE_ADDED, dateTaken / 1000); // 添加时间;
            values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");

            ContentResolver resolver = context.getContentResolver();

            if (resolver != null) {
                try {
                    uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
                } catch (Exception e) {
                    e.printStackTrace();
                    uri = null;
                }
            }

            if (uri == null) {
                MediaScannerConnection.scanFile(context, new String[]{videoPath}, new String[]{"video/*"}, new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri) {

                    }
                });
            }

            return uri;
        }

        return null;
    }

    /**
     * SD卡存在并可以使用
     */
    public static boolean isSDExists() {
        return Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
    }

    /**
     * 获取SD卡的剩余容量，单位是Byte
     *
     * @return
     */
    public static long getSDFreeMemory() {
        try {
            if (isSDExists()) {
                File pathFile = Environment.getExternalStorageDirectory();
                // Retrieve overall information about the space on a filesystem.
                // This is a Wrapper for Unix statfs().
                StatFs statfs = new StatFs(pathFile.getPath());
                // 获取SDCard上每一个block的SIZE
                long nBlockSize = statfs.getBlockSize();
                // 获取可供程序使用的Block的数量
                // long nAvailBlock = statfs.getAvailableBlocksLong();
                long nAvailBlock = statfs.getAvailableBlocks();
                // 计算SDCard剩余大小Byte
                long nSDFreeSize = nAvailBlock * nBlockSize;
                return nSDFreeSize;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return 0;
    }

    private static String getUsbDiskPath(){

        String root = "/storage";

        String dirsPath = null;

        //ArrayList<String> PathInfoFiles = new ArrayList<String>();

        File parentDir = new File(root);

        File[] files = parentDir.listFiles();

        if (files == null) {
            //if (DEBUG) Log.e(TAG, root+" No file listed in " + root);
            return null;
        }

        for (File file : files) {

            //if (DEBUG) Log.i(TAG,root + " files: " + file.getName());

            if (file.isDirectory()) {
                String dirsName = file.getName();
                dirsPath = file.getPath();
                //if (DEBUG) Log.i(TAG,root+" dirsName " + dirsName);
                //if (DEBUG) Log.i(TAG,root+" dirsPath " + dirsPath);

                if (dirsName.equals("emulated") || dirsName.equals("self")) {

                } else {
                    //if (DEBUG) Log.i(TAG,root+" dirsPath " + file.getPath());
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

    public static boolean isVideoFile(String fileName) {

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

    public static String getUsbPath(){

        String usbPath = USBReceiver.mountPath;

        String videoFilePath = null;

        String externalStoragePath = Environment
                .getExternalStorageDirectory().getPath();//+ "/Movies/";

        //Log.d(TAG, "usbPath " + usbPath);

        if (usbPath == null){

            String mnt_usb_path = getUsbDiskPath();

            if (mnt_usb_path != null){

                videoFilePath = mnt_usb_path + "/";
            }
            else {
                videoFilePath = externalStoragePath;
            }
        }
        else {

            videoFilePath = usbPath + "/";// + "/Android/" ;

            //if (DEBUG) Log.i(TAG,"VideoInfoFilePath：" + videoFilePath);
        }
        return videoFilePath;
    }

    public static File[] getUsbFiles(){
        String usbPath = FileUtil.getUsbPath();

        File fileVideoInfoDir = new File(usbPath);

        return fileVideoInfoDir.listFiles();
    }

    public static String getSaveDirectory() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            String rootDir = Environment.getExternalStorageDirectory()
                    .getAbsolutePath() + "/" + "ScreenRecord" + "/";

            File file = new File(rootDir);
            if (!file.exists()) {
                if (!file.mkdirs()) {
                    return null;
                }
            }

            return rootDir;
        } else {
            return null;
        }
    }

    /**
     * 检测外部存储是否存在
     */
    public static boolean checkSDCard() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }


    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    /**
     * 创建一个文件来保存图片或者视频
     */
    public static File getOutputMediaFile(Context mContext, int type) {

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Camera2Examples");

        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }
        return mediaFile;
    }


    public static File getTimeStampMediaFile(String parentPath, int type) {
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(parentPath + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(parentPath + File.separator +
                    "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }
        return mediaFile;
    }


}
