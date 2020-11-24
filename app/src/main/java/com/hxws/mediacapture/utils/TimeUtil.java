package com.hxws.mediacapture.utils;

import android.annotation.SuppressLint;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeUtil {

    public static String secToTime (int i) {
        String retStr = null;
        int hour = 0;
        int minute = 0;
        int second = 0;
        if (i <= 0) {
            return "00:00:00";
        }
        else {
            minute = i / 60;
            if (minute < 60) {
                second = i % 60;
                retStr = "00:" + unitFormat (minute) + ":" + unitFormat (second);
            }
            else {
                hour = minute / 60;
                if (hour > 99) {
                    return "99:59:59";
                }
                minute = minute % 60;
                second = i % 60;
                retStr = unitFormat (hour) + ":" + unitFormat (minute) + ":" + unitFormat (second);
            }
        }
        return retStr;
    }

    private static String unitFormat (int i) {
        String retStr = null;
        if (i >= 0 && i < 10) {
            retStr = "0" + Integer.toString (i);
        }
        else {
            retStr = Integer.toString (i);
        }
        return retStr;
    }

    public static String getCurTime(){
        String rel = "";
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
        Date curDate = new Date(System.currentTimeMillis());
        rel = formatter.format(curDate);

        return rel;
    }

}
