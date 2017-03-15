package com.casasw.sunshinewatchface;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Junior on 15/03/2017.
 * Handle store and get shared preferences on watch side
 */

public class SunshineWatchFacePreferences {

    public static void setTodayWeather(Context context, int id, int high, int low) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();

        editor.putInt(context.getString(R.string.pref_today_weather_id), id);
        editor.putInt(context.getString(R.string.pref_today_high), high);
        editor.putInt(context.getString(R.string.pref_today_low), low);
        editor.apply();
    }
    public static int[] getTodayWeather(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        int[] ret = new int[3];
        ret[0] = sp.getInt(context.getString(R.string.pref_today_weather_id),951);
        ret[1] = sp.getInt(context.getString(R.string.pref_today_high),80);
        ret[2] = sp.getInt(context.getString(R.string.pref_today_low),80);

        return ret;
    }
}
