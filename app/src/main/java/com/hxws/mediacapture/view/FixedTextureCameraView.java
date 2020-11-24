/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hxws.mediacapture.view;

import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

/**
 * A {@link TextureView} that can be adjusted to a specified aspect ratio.
 */
public class FixedTextureCameraView extends TextureView {

    private final static String TAG = "FixedTextureCameraView";

    private int widthMeasure;
    private int heightMeasure;

    private static int fixedWidth;
    private static int fixedHeight;
    private Matrix matrix;

    private int mRatioWidth = 0;
    private int mRatioHeight = 0;

    public FixedTextureCameraView(Context context) {
        this(context, null);
    }

    public FixedTextureCameraView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FixedTextureCameraView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
     * calculated from the parameters. Note that the actual sizes of parameters don't matter, that
     * is, calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
     *
     * @param width  Relative horizontal size
     * @param height Relative vertical size
     */
    public void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {

            throw new IllegalArgumentException("Size cannot be negative.");
        }
        Log.d(TAG,"setAspectRatio: width = "+width+",height = "+height);
        mRatioWidth = width;
        mRatioHeight = height;
        Log.d(TAG, "setAspectRatio: mRatioWidth: "
                +mRatioWidth+" , mRatioHeight: "+mRatioHeight);

        fixedWidth = width;
        fixedHeight = height;

        requestLayout();
    }

    public int getmRatioWidth(){
        mRatioWidth = fixedWidth;
        return mRatioWidth;
    }

    public int getmRatioHeight(){
        mRatioHeight = fixedHeight;
        return mRatioHeight;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        widthMeasure = MeasureSpec.getSize(widthMeasureSpec) ;
        heightMeasure = MeasureSpec.getSize(heightMeasureSpec) ;
        Log.d(TAG, "onMeasure: widthMeasure: "+widthMeasure+" ,heightMeasure : "+heightMeasure);
        Log.d(TAG, "onMeasure: mRatioWidth: "+mRatioWidth+" ,mRatioHeight : "+mRatioHeight);
        Log.d(TAG, "onMeasure: fixedWidth: "+fixedWidth+" ,fixedHeight : "+fixedHeight);

        if ( 0 == mRatioWidth || 0 == mRatioHeight ) {
            defaultMeasure(widthMeasureSpec,heightMeasureSpec);
            //setMeasuredDimension(widthMeasure, heightMeasure);
        } else {
            transformCamera(fixedWidth,fixedHeight);
            setMeasuredDimension(widthMeasure, heightMeasure);
//            if (widthMeasure < heightMeasure * mRatioWidth / mRatioHeight) {
//                setMeasuredDimension(widthMeasure, widthMeasure * mRatioHeight / mRatioWidth);
//                Log.d(TAG, "2. centraScreen: width: "+widthMeasure+" ,after height : "
//                        +widthMeasure * mRatioHeight / mRatioWidth);
//            }

        }

    }

    public int getResizedWidth() {
//        if (fixedWidth == 0) {
//            return getWidth();
//        } else {
//            return fixedWidth;
//        }
        return getWidth();
    }

    public int getResizedHeight() {
//        if (fixedHeight== 0) {
//            return getHeight();
//        } else {
//            return fixedHeight;
//        }
        return getHeight();
    }

    public void transformCamera(int cameraWidth, int cameraHeight) {
        if (getResizedHeight() == 0 || getResizedWidth() == 0) {
            Log.d(TAG, "transformCamera, getResizedHeight=" + getResizedHeight() + "," + "getResizedWidth=" + getResizedWidth());
            return;
        }
        float sx = (float) getResizedWidth() / (float) cameraWidth;
        float sy = (float) getResizedHeight() / (float) cameraHeight;
        Log.d(TAG, "transformCamera, sx=" + sx);
        Log.d(TAG, "transformCamera, sy=" + sy);

        float maxScale = Math.max(sx, sy);
        if (this.matrix == null) {
            matrix = new Matrix();
        } else {
            matrix.reset();
        }

        //第2步:把图像区移动到View区,使两者中心点重合.
        matrix.preTranslate((getResizedWidth() - cameraWidth) / 2, (getResizedHeight() - cameraHeight) / 2);

        //第1步:因为默认是fitXY的形式显示的,所以首先要缩放还原回来.
        matrix.preScale(cameraWidth / (float) getResizedWidth(), cameraHeight / (float) getResizedHeight());

        //第3步,等比例放大或缩小,直到图像区的一边超过View一边, 另一边与View的另一边相等. 因为超过的部分超出了View的范围,所以是不会显示的,相当于裁剪了.
        matrix.postScale(maxScale, maxScale, getResizedWidth() / 2, getResizedHeight() / 2);//后两个参数坐标是以整个View的坐标系以参考的

        Log.d(TAG, "transformCamera, maxScale=" + maxScale);

        setTransform(matrix);
        postInvalidate();
        Log.d(TAG, "transformCamera, cameraWidth=" + cameraWidth + "," + "cameraHeight=" + cameraHeight);
    }

    protected void defaultMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //Log.i("@@@@", "onMeasure(" + MeasureSpec.toString(widthMeasureSpec) + ", "
        //        + MeasureSpec.toString(heightMeasureSpec) + ")");

        int width = getDefaultSize(mRatioWidth, widthMeasureSpec);
        int height = getDefaultSize(mRatioHeight, heightMeasureSpec);
        if (mRatioWidth > 0 && mRatioHeight > 0) {

            int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
            int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
            int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
            int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);

            if (widthSpecMode == MeasureSpec.EXACTLY && heightSpecMode == MeasureSpec.EXACTLY) {
                // the size is fixed
                width = widthSpecSize;
                height = heightSpecSize;

                // for compatibility, we adjust size based on aspect ratio
                if ( mRatioWidth * height  < width * mRatioHeight ) {
                    //Log.i("@@@", "image too wide, correcting");
                    width = height * mRatioWidth / mRatioHeight;
                } else if ( mRatioWidth * height  > width * mRatioHeight ) {
                    //Log.i("@@@", "image too tall, correcting");
                    height = width * mRatioHeight / mRatioWidth;
                }
            } else if (widthSpecMode == MeasureSpec.EXACTLY) {
                // only the width is fixed, adjust the height to match aspect ratio if possible
                width = widthSpecSize;
                height = width * mRatioHeight / mRatioWidth;
                if (heightSpecMode == MeasureSpec.AT_MOST && height > heightSpecSize) {
                    // couldn't match aspect ratio within the constraints
                    height = heightSpecSize;
                }
            } else if (heightSpecMode == MeasureSpec.EXACTLY) {
                // only the height is fixed, adjust the width to match aspect ratio if possible
                height = heightSpecSize;
                width = height * mRatioWidth / mRatioHeight;
                if (widthSpecMode == MeasureSpec.AT_MOST && width > widthSpecSize) {
                    // couldn't match aspect ratio within the constraints
                    width = widthSpecSize;
                }
            } else {
                // neither the width nor the height are fixed, try to use actual video size
                width = mRatioWidth;
                height = mRatioHeight;
                if (heightSpecMode == MeasureSpec.AT_MOST && height > heightSpecSize) {
                    // too tall, decrease both width and height
                    height = heightSpecSize;
                    width = height * mRatioWidth / mRatioHeight;
                }
                if (widthSpecMode == MeasureSpec.AT_MOST && width > widthSpecSize) {
                    // too wide, decrease both width and height
                    width = widthSpecSize;
                    height = width * mRatioHeight / mRatioWidth;
                }
            }
        } else {
            // no size yet, just adopt the given spec sizes
        }
        setMeasuredDimension(width, height);
    }

}
