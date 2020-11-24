package com.hxws.mediacapture.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.hxws.mediacapture.R;
import com.hxws.mediacapture.camera1.CustomCameraActivity;


public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private final static String TAG = "MainActivity";

    public static Context mContext;

    public static MainActivity sInstance;

    Bundle mBundle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;

        sInstance = this;

        mBundle = new Bundle();

        initViews();
    }

    private void initViews(){

        Button btn_camera = (Button) findViewById(R.id.btn_camera);
        Button btn_split = (Button) findViewById(R.id.btn_split);

        btn_camera.setOnClickListener(this);
        btn_split.setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.btn_camera:
                chooseActivity(1);

                break;

            case R.id.btn_split:
                chooseActivity(2);

                break;

            default:
                break;
        }
    }

    private void chooseActivity(int value){

        if (value == 1){
            Intent intent = new Intent(this, CustomCameraActivity.class);

            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

            startActivity(intent);
        }
        else if (value == 2){
            Intent intent = new Intent(this, SplitScreenActivity.class);

            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

            startActivity(intent);
        }
    }

}
