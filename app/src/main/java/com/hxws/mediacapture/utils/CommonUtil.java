package com.hxws.mediacapture.utils;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.Display;

public class CommonUtil {

    private static int mScreenWidth = 720;
    private static int mScreenHeight = 1080;

    private static int mScreenDpi;

    public static void init(Activity activity){

        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        mScreenWidth = metrics.widthPixels;
        mScreenHeight = metrics.heightPixels;
        mScreenDpi = metrics.densityDpi;
    }

    public static int getScreenWidth(){
        return mScreenWidth;
    }

    public static int getScreenHeight(){
        return mScreenHeight;
    }

    public static int getScreenDpi(){
        return mScreenDpi;
    }
}
