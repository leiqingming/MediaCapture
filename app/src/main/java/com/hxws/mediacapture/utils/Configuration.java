package com.hxws.mediacapture.utils;

import android.os.Environment;

public class Configuration {

    //这是app内部存储 格式如下 /data/data/包名/xxx/
    public static String insidePath = "/data/data/com.hxws.mediacapture/pic/";
    //外部路径
    public static String OUTPATH = Environment.getExternalStorageDirectory() + "/拍照-相册/";
}
