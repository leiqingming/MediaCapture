package com.hxws.mediacapture.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.hxws.mediacapture.R;
import com.hxws.mediacapture.activity.SplitScreenActivity;

public class USBReceiver extends BroadcastReceiver {

    public static String mountPath = null;

    private final String TAG = "USBReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        Log.i(TAG,"onReceive: "+intent);

        if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {

            mountPath = intent.getData().getPath();
            if (mountPath.equals("/storage/emulated/0")){
                Log.d(TAG, "mountPath = " + "/storage/emulated/0");
                return;
            }
            Log.d(TAG, "mountPath = " + mountPath);
            if (!TextUtils.isEmpty(mountPath)) {
//                Intent intentVideo = new Intent();
//                intentVideo.setClass(context, MainActivity.class);
//                intentVideo.addFlags(FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
//
//                context.startActivity(intentVideo);

                 //SplitScreenActivity.showToast(context.getResources().getString(R.string.Storage_Mounted));

                if (SplitScreenActivity.isActivityExist()){
                    Toast.makeText(context,
                            context.getResources().getString(R.string.Storage_Mounted),
                            Toast.LENGTH_SHORT).show();
                }

            }

        } else if (action.equals(Intent.ACTION_MEDIA_UNMOUNTED) || action.equals(Intent.ACTION_MEDIA_EJECT)) {

            mountPath = null;

            if (SplitScreenActivity.isActivityExist()){
                Toast.makeText(context,
                        context.getResources().getString(R.string.Storage_Unmounted),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}
