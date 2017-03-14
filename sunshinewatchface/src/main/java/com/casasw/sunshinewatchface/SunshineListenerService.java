package com.casasw.sunshinewatchface;

import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by Junior on 14/03/2017.
 * Sunshine Listener Service receives a message from phone
 */

public class SunshineListenerService extends WearableListenerService {
    public static final String TAG = SunshineListenerService.class.getSimpleName();
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onMessageReceived: "+messageEvent.getPath());
        }
    }
}
