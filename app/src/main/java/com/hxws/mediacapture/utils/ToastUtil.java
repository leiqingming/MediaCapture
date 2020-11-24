package com.hxws.mediacapture.utils;

import android.content.Context;
import android.widget.Toast;

public class ToastUtil {

    private static Toast mToast;

    public static void showShortToast(Context context,String message){
        showToastMessage(context,message,Toast.LENGTH_SHORT);

    }


    /**
     * 弹出Toast提示
     * @param context 上下文
     * @param message 要显示的message
     * @param duration 时间长短
     */
    public static void showToastMessage(Context context,String message,int duration){
        if(mToast == null){
            mToast = Toast.makeText(context,message,duration);
        }else{
            mToast.setText(message);
            mToast.setDuration(duration);
        }
        mToast.show();

    }

}
