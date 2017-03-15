/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.casasw.sunshinewatchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class SunshineWatchFace extends CanvasWatchFaceService {
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
    private static final Typeface ITALIC_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC);
    private static final Typeface BOLD_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);

    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;
    private static final String TAG = SunshineWatchFace.class.getSimpleName();

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<SunshineWatchFace.Engine> mWeakReference;

        public EngineHandler(SunshineWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            SunshineWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine implements SharedPreferences.OnSharedPreferenceChangeListener{
        final Handler mUpdateTimeHandler = new EngineHandler(this);
        boolean mRegisteredTimeZoneReceiver = false;
        Paint mBackgroundPaint, mIconPaint;
        Paint mHoursPaint, mMinutesPaint;
        Paint mDatePaint, mHighPaint, mLowPaint;
        String mHighText, mLowText;
        int mWeatherId;
        Bitmap mIcon;
        boolean mAmbient;
        Calendar mCalendar;
        SimpleDateFormat mDateFormat;
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };
        float mXOffset;
        float mYOffset;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(SunshineWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());
            Resources resources = SunshineWatchFace.this.getResources();
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.background));

            mIconPaint = new Paint();
            mHighPaint = new Paint();
            mHighPaint = createTextPaint(resources.getColor(R.color.digital_text), BOLD_TYPEFACE);
            mLowPaint  = new Paint();
            mLowPaint = createTextPaint(resources.getColor(R.color.digital_text), NORMAL_TYPEFACE);

            mHoursPaint = new Paint();
            mHoursPaint = createTextPaint(resources.getColor(R.color.digital_text), BOLD_TYPEFACE);
            mMinutesPaint = new Paint();
            mMinutesPaint = createTextPaint(resources.getColor(R.color.digital_text), NORMAL_TYPEFACE);
            mDatePaint = new Paint();
            mDatePaint = createTextPaint(resources.getColor(R.color.digital_text), NORMAL_TYPEFACE);

            mCalendar = Calendar.getInstance();
            int[] todayWeather = SunshineWatchFacePreferences.getTodayWeather(getApplicationContext());
            mWeatherId = todayWeather[0];
            mHighText = todayWeather[1]+"°"; //string format
            mLowText = todayWeather[2]+"°";  //string format
            mIcon = BitmapFactory.decodeResource(getResources(), getSmallArtResourceIdForWeatherCondition(mWeatherId));
            mIcon = Bitmap.createScaledBitmap(mIcon,
                    (int) getResources().getDimension(R.dimen.image_size),
                    (int) getResources().getDimension(R.dimen.image_size),
                    true);
            //"FRI, JUL 14 2017";
            mDateFormat = new SimpleDateFormat("EEE, MMM dd yyyy", Locale.ENGLISH);
        }


        /**
         * Helper method to provide the icon resource id according to the weather condition id returned
         * by the OpenWeatherMap call. This method is very similar to
         *
         *
         * The difference between these two methods is that this method provides smaller assets, used
         * in the list item layout for a "future day", as well as
         *
         * @param weatherId from OpenWeatherMap API response
         *                  See http://openweathermap.org/weather-conditions for a list of all IDs
         *
         * @return resource id for the corresponding icon. -1 if no relation is found.
         */
        public int getSmallArtResourceIdForWeatherCondition(int weatherId) {

        /*
         * Based on weather code data for Open Weather Map.
         */
            if (weatherId >= 200 && weatherId <= 232) {
                return R.drawable.ic_storm;
            } else if (weatherId >= 300 && weatherId <= 321) {
                return R.drawable.ic_light_rain;
            } else if (weatherId >= 500 && weatherId <= 504) {
                return R.drawable.ic_rain;
            } else if (weatherId == 511) {
                return R.drawable.ic_snow;
            } else if (weatherId >= 520 && weatherId <= 531) {
                return R.drawable.ic_rain;
            } else if (weatherId >= 600 && weatherId <= 622) {
                return R.drawable.ic_snow;
            } else if (weatherId >= 701 && weatherId <= 761) {
                return R.drawable.ic_fog;
            } else if (weatherId == 761 || weatherId == 771 || weatherId == 781) {
                return R.drawable.ic_storm;
            } else if (weatherId == 800) {
                return R.drawable.ic_clear;
            } else if (weatherId == 801) {
                return R.drawable.ic_light_clouds;
            } else if (weatherId >= 802 && weatherId <= 804) {
                return R.drawable.ic_cloudy;
            } else if (weatherId >= 900 && weatherId <= 906) {
                return R.drawable.ic_storm;
            } else if (weatherId >= 958 && weatherId <= 962) {
                return R.drawable.ic_storm;
            } else if (weatherId >= 951 && weatherId <= 957) {
                return R.drawable.ic_clear;
            }

            Log.e(TAG, "Unknown Weather: " + weatherId);
            return R.drawable.ic_storm;
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private Paint createTextPaint(int textColor, Typeface typeface) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(typeface);
            paint.setAntiAlias(true);
            return paint;
        }



        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            SunshineWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            SunshineWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = SunshineWatchFace.this.getResources();
            boolean isRound = insets.isRound();
            mXOffset = resources.getDimension(isRound ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);

            float textSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);

            mHoursPaint.setTextSize(textSize);
            mMinutesPaint.setTextSize(textSize);
            textSize = getResources().getDimension(R.dimen.temp_text_size);
            mHighPaint.setTextSize(textSize);
            mLowPaint.setTextSize(textSize);
            textSize = resources.getDimension(R.dimen.date_text_size);
            mDatePaint.setTextSize(textSize);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mHoursPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
            }

            // Draw H:MM in ambient mode or H:MM:SS in interactive mode.
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            String text = String.format(Locale.ENGLISH,"%d:", mCalendar.get(Calendar.HOUR));
            //canvas.drawText(text, mXOffset, mYOffset, mHoursPaint); -digital text size
            canvas.drawText(text,
                    bounds.centerX()-mHoursPaint.measureText(text),
                    getResources().getDimension(R.dimen.vertical_margin),
                    mHoursPaint);
            //Log.d(TAG, "onDraw: x: "+bounds.centerX() +" y: "+bounds.centerY());
            //Log.d(TAG, "onDraw: Text size"+getResources().getDimension(R.dimen.digital_text_size));
            text = String.format(Locale.ENGLISH,"%02d", mCalendar.get(Calendar.MINUTE));
            //canvas.drawText(text, mXOffset+getResources().getDimension(R.dimen.digital_text_size), mYOffset, mMinutesPaint);
            canvas.drawText(text,
                    bounds.centerX(),
                    getResources().getDimension(R.dimen.vertical_margin),
                    mMinutesPaint);

            if (!isInAmbientMode()) {
                text = mDateFormat.format(mCalendar.getTime());
                float posX = bounds.centerX()- (mDatePaint.measureText(text)/2);
                canvas.drawText(text,
                        posX,
                        bounds.centerY(),
                        mDatePaint);


                //(float startX, float startY, float stopX, float stopY, Paint paint)
                Paint line = new Paint();
                line.setColor(getColor(R.color.digital_text));
                float size = getResources().getDimension(R.dimen.line_size);
                canvas.drawLine(bounds.centerX() - size,
                        bounds.centerY() + getResources().getDimension(R.dimen.line_top_padding),
                        bounds.centerX() + size,
                        bounds.centerY() + getResources().getDimension(R.dimen.line_top_padding),
                        line );


                canvas.drawBitmap(mIcon,
                        posX - getResources().getDimension(R.dimen.image_end_padding),
                        bounds.centerY()+ getResources().getDimension(R.dimen.image_top_padding),
                        mIconPaint);
                posX = (float) (posX + (getResources().getDimension(R.dimen.image_size)/2) + (mHighPaint.measureText(mHighText)*0.25));
                canvas.drawText(mHighText,
                        posX,
                        bounds.centerY()+ getResources().getDimension(R.dimen.temp_top_padding),
                        mHighPaint
                );
                posX = posX +
                        (mLowPaint.measureText(mLowText)) +
                        getResources().getDimension(R.dimen.temp_inner_spacing);
                canvas.drawText(mLowText,
                        posX,
                        bounds.centerY()+ getResources().getDimension(R.dimen.temp_top_padding),
                        mLowPaint
                );
            }
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            mWeatherId = sharedPreferences.getInt(SunshineWatchFacePreferences.PREF_TODAY_WEATHER_ID, mWeatherId);
            int high = sharedPreferences.getInt(SunshineWatchFacePreferences.PREF_TODAY_HIGH, Integer.parseInt(mHighText));
            int low = sharedPreferences.getInt(SunshineWatchFacePreferences.PREF_TODAY_LOW, Integer.parseInt(mLowText));
            mHighText = high+"";
            if (BuildConfig.DEBUG){
                Log.d(TAG, "onSharedPreferenceChanged: New weather info to be draw.");
            }
            invalidate();
        }
    }

    /**
     * Sunshine Listener Service receives a message from phone
     */

    public static class SunshineListenerService extends WearableListenerService {
        public static final String TAG = SunshineListenerService.class.getSimpleName();
        @Override
        public void onMessageReceived(MessageEvent messageEvent) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "onMessageReceived: "+messageEvent.getPath());
            }
            int[] todayWeather = weatherToInt(messageEvent.getPath());
            SunshineWatchFacePreferences.setTodayWeather(this, todayWeather[0], todayWeather[1], todayWeather[2]);

        }

        private int[] weatherToInt(String message) {
            int[] ret = new int[3];
            StringTokenizer stringTokenizer = new StringTokenizer(message);
            int i = 0;
            while (stringTokenizer.hasMoreTokens()){
                ret[i] = Integer.parseInt(stringTokenizer.nextToken());
                i++;
            }
            return ret;
        }
    }
}
