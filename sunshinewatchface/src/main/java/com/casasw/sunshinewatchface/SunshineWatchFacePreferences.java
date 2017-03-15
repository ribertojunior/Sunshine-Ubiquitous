package com.casasw.sunshinewatchface;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Junior on 15/03/2017.
 * Handle store and get shared preferences on watch side
 */

public class SunshineWatchFacePreferences {
    /*
     * Wearable preferences
     * need to use string.xml
     */
    public static final String PREF_TODAY_WEATHER_ID = "today_weather_id";
    public static final String PREF_TODAY_HIGH = "today_high";
    public static final String PREF_TODAY_LOW = "today_low";
    public static void setTodayWeather(Context context, int id, int high, int low) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sp.edit();

        editor.putInt(PREF_TODAY_WEATHER_ID, id);
        editor.putInt(PREF_TODAY_HIGH, high);
        editor.putInt(PREF_TODAY_LOW, low);
        editor.apply();
    }
    public static int[] getTodayWeather(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        int[] ret = new int[3];
        ret[0] = sp.getInt(PREF_TODAY_WEATHER_ID,951);
        ret[1] = sp.getInt(PREF_TODAY_HIGH,80);
        ret[2] = sp.getInt(PREF_TODAY_LOW,80);

        return ret;
    }
}
