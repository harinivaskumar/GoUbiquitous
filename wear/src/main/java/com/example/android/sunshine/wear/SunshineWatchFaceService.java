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

package com.example.android.sunshine.wear;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.example.android.sunshine.lib.SunshineConstants;
import com.example.android.sunshine.lib.SunshineUtility;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class SunshineWatchFaceService extends CanvasWatchFaceService {
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    private static final String LOG_TAG = SunshineWatchFaceService.class.getSimpleName();

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine implements
            GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener {

        final int TEXT_SIZE_NOT_APPLICABLE = 0;
        final int PAINT_TYPE_TEXT = 1;
        final int PAINT_TYPE_BACKGROUND = 2;
        final String TIME_FORMAT = "%02d:%02d";
        final String DATE_FORMAT = "%s, %s %d, %d";
        final String EXAMPLE_TIME_STRING = "12:00";
        final String EXAMPLE_DATE_STRING = "WED, JUN 19, 2016";
        // Load Resources that have alternate values for round watches.
        Resources mResources = SunshineWatchFaceService.this.getResources();
        Calendar mCalendar;
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                mCalendar.setTimeInMillis(System.currentTimeMillis());
            }
        };
        Paint mBackgroundPaint;
        Paint mTimeTextPaint;
        Paint mDateTextPaint;
        Paint mDividerTextPaint;
        Paint mTempHighPaint;
        Paint mTempLowPaint;
        int mTapCount;
        //Temperatures Text
        String mTempHighStr;
        String mTempLowStr;
        int mWeatherId = 200;
        //Temperatures Text Sizes
        float mTempHighTextSize;
        float mTempLowTextSize;
        //OffSets
        float mTimeXOffset;
        float mTimeYOffset;
        float mDateXOffset;
        float mDateYOffset;
        float mDividerXOffset;
        float mDividerYOffset;
        float mWeatherIconXOffset;
        float mWeatherIconYOffset;
        float mTempHighXOffset;
        float mTempHighYOffset;
        float mTempLowXOffset;
        float mTempLowYOffset;
        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;
        boolean mAmbient;
        boolean mRegisteredTimeZoneReceiver = false;
        // Google API Client
        private GoogleApiClient mGoogleApiClient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            setWatchFaceStyle(getWatchFaceStyle());
            mGoogleApiClient = new GoogleApiClient.Builder(SunshineWatchFaceService.this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Wearable.API)
                    .build();
            initPaintObjects();
            initXYOffsets();
            mCalendar = Calendar.getInstance();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();
                mGoogleApiClient.connect();

                // Update time zone in case it changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
                mCalendar.setTimeInMillis(System.currentTimeMillis());
            } else {
                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    Wearable.DataApi.removeListener(mGoogleApiClient, this);
                    mGoogleApiClient.disconnect();
                }
                unregisterReceiver();
            }
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);
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
                    mTimeTextPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    mTapCount++;
                    mBackgroundPaint.setColor(
                            getColorFromRes(mTapCount % 2 == 0 ?
                                    R.color.primary_dark :
                                    R.color.primary
                            )
                    );
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            Log.d(LOG_TAG, "Entering onDraw");

            // Set current time in calendar
            mCalendar.setTimeInMillis(System.currentTimeMillis());

            drawBackground(canvas, bounds);
            drawTimeText(canvas, bounds);
            drawDateText(canvas, bounds);
            drawDivider(canvas, bounds);

            String weatherStr = SunshineUtility.getWeatherId(getApplicationContext());
            mTempHighStr = SunshineUtility.getHighTemp(getApplicationContext());
            mTempLowStr = SunshineUtility.getLowTemp(getApplicationContext());

            if (!(weatherStr == null || weatherStr.equals("")) &&
                    !(mTempHighStr == null || mTempHighStr.equals("")) &&
                    !(mTempLowStr == null || mTempLowStr.equals(""))) {

                Log.d(LOG_TAG, "Before Formatting : WeatherId - " + weatherStr + " High Temp - " + mTempHighStr + " Low Temp - " + mTempLowStr);

                mWeatherId = Integer.parseInt(weatherStr);

                mTempHighStr = String.format(mResources.getString(R.string.format_temperature),
                        Float.parseFloat(mTempHighStr));

                mTempLowStr = String.format(mResources.getString(R.string.format_temperature),
                        Float.parseFloat(mTempLowStr));

                Log.d(LOG_TAG, "After Formatting : WeatherId - " + mWeatherId + " High Temp - " + mTempHighStr + " Low Temp - " + mTempLowStr);
                mTempHighTextSize = mTempHighPaint.measureText(mTempHighStr);
                mTempLowTextSize = mTempLowPaint.measureText(mTempLowStr);

                drawIconBitmap(canvas, bounds);
                drawHighTemperature(canvas, bounds);
                drawLowTemperature(canvas, bounds);
            } else {
                requestWeatherUpdateFromApp();
            }

            Log.d(LOG_TAG, "Exiting onDraw!");
        }

        private WatchFaceStyle getWatchFaceStyle() {
            return new WatchFaceStyle
                    .Builder(SunshineWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            SunshineWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            SunshineWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        private void initPaintObjects() {
            Log.d(LOG_TAG, "Entering initPaintObjects!");
            mBackgroundPaint = getPaintObject(PAINT_TYPE_BACKGROUND, R.color.primary_dark, TEXT_SIZE_NOT_APPLICABLE);

            mTimeTextPaint = getPaintObject(PAINT_TYPE_TEXT, R.color.accent_watch_face_dark, R.dimen.time_text_size);
            mDateTextPaint = getPaintObject(PAINT_TYPE_TEXT, R.color.accent_watch_face_light, R.dimen.date_text_size);

            mDividerTextPaint = getPaintObject(PAINT_TYPE_TEXT, R.color.accent_watch_face_light, R.dimen.divider_line_size);

            mTempHighPaint = getPaintObject(PAINT_TYPE_TEXT, R.color.accent_watch_face_dark, R.dimen.temp_text_size);
            mTempLowPaint = getPaintObject(PAINT_TYPE_TEXT, R.color.accent_watch_face_light, R.dimen.temp_text_size);
            Log.d(LOG_TAG, "Exiting initPaintObjects!");
        }

        private void initXYOffsets() {
            Log.d(LOG_TAG, "Entering initXYOffsets!");
            //mTimeXOffset = getDimensionFromRes(R.dimen.time_y_offset);
            mTimeXOffset = mTimeTextPaint.measureText(EXAMPLE_TIME_STRING) / 2;
            mTimeYOffset = getDimensionFromRes(R.dimen.time_y_offset);

            //mDateXOffset = getDimensionFromRes(R.dimen.date_y_offset);
            mDateXOffset = mDateTextPaint.measureText(EXAMPLE_DATE_STRING) / 2;
            mDateYOffset = getDimensionFromRes(R.dimen.date_y_offset);

            mDividerXOffset = getDimensionFromRes(R.dimen.divider_x_offset);
            mDividerYOffset = getDimensionFromRes(R.dimen.divider_y_offset);

            mWeatherIconXOffset = getDimensionFromRes(R.dimen.weather_icon_y_offset);
            mWeatherIconYOffset = getDimensionFromRes(R.dimen.weather_icon_y_offset);

            mTempHighXOffset = getDimensionFromRes(R.dimen.temp_high_x_offset);
            mTempHighYOffset = getDimensionFromRes(R.dimen.temp_high_y_offset);

            mTempLowXOffset = getDimensionFromRes(R.dimen.temp_low_x_offset);
            mTempLowYOffset = getDimensionFromRes(R.dimen.temp_low_y_offset);
            Log.d(LOG_TAG, "Exiting initXYOffsets!");
        }

        private Paint getPaintObject(int paintType, int textColorResId, int textSizeResId) {
            Paint paint = new Paint();

            switch (paintType) {
                case PAINT_TYPE_TEXT:
                    paint.setTextSize(getDimensionFromRes(textSizeResId));
                    paint.setTypeface(NORMAL_TYPEFACE);
                    paint.setAntiAlias(true);
                    break;
                case PAINT_TYPE_BACKGROUND:
                    break;
            }

            paint.setColor(getColorFromRes(textColorResId));
            return paint;
        }

        private float getDimensionFromRes(int dimensionResId) {
            return mResources.getDimension(dimensionResId);
        }

        private int getColorFromRes(int colorResId) {
            return mResources.getColor(colorResId);
        }

        private Drawable getDrawableFromRes(int drawableResId) {
            return mResources.getDrawable(drawableResId);
        }

        private Bitmap getIconBitmap() {
            return ((BitmapDrawable) getDrawableFromRes(
                    SunshineUtility.getIconResourceForWeatherCondition(mWeatherId))
            ).getBitmap();
        }

        private void rePaintUIObjects() {
            Log.d(LOG_TAG, "Entering rePaintUIObjects!");
            int color = getColorFromRes(isInAmbientMode() || (mTapCount % 2 != 0) ?
                    R.color.accent_watch_face_dark :
                    R.color.accent_watch_face_light
            );
            mDateTextPaint.setColor(color);
            mDividerTextPaint.setColor(color);
            mTempLowPaint.setColor(color);
            Log.d(LOG_TAG, "Exiting rePaintUIObjects!");
        }

        private void drawBackground(Canvas canvas, Rect bounds) {
            Log.d(LOG_TAG, "Entering drawBackground!");
            rePaintUIObjects();

            if (isInAmbientMode()) {
                canvas.drawColor(getColorFromRes(R.color.ambient_primary_dark));
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
            }
            Log.d(LOG_TAG, "Exiting drawBackground!");
        }

        private void drawTimeText(Canvas canvas, Rect bounds) {
            Log.d(LOG_TAG, "Entering drawTimeText!");
            int hourOfDay = mCalendar.get(Calendar.HOUR_OF_DAY);
            int minuteOfDay = mCalendar.get(Calendar.MINUTE);
            String timeString = String.format(TIME_FORMAT, hourOfDay, minuteOfDay);

            canvas.drawText(timeString,
                    bounds.centerX() - mTimeXOffset,
                    mTimeYOffset,
                    mTimeTextPaint
            );
            Log.d(LOG_TAG, "Exiting drawTimeText!");
        }

        private void drawDateText(Canvas canvas, Rect bounds) {
            Log.d(LOG_TAG, "Entering drawTimeText!");
            int dayOfMonth = mCalendar.get(Calendar.DAY_OF_MONTH);
            String dayName = mCalendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault());
            String monthName = mCalendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault());
            int year = mCalendar.get(Calendar.YEAR);
            String dateString = String.format(DATE_FORMAT, dayName.toUpperCase(), monthName.toUpperCase(), dayOfMonth, year);

            canvas.drawText(dateString,
                    bounds.centerX() - mDateXOffset,
                    mDateYOffset,
                    mDateTextPaint
            );
            Log.d(LOG_TAG, "Exiting drawTimeText!");
        }

        private void drawDivider(Canvas canvas, Rect bounds) {
            Log.d(LOG_TAG, "Entering drawDivider!");
            canvas.drawLine(
                    bounds.centerX() - mDividerXOffset, mDividerYOffset,
                    bounds.centerX() + mDividerXOffset, mDividerYOffset,
                    mDividerTextPaint);
            Log.d(LOG_TAG, "Exiting drawDivider!");
        }

        private void drawIconBitmap(Canvas canvas, Rect bounds) {
            if (!isInAmbientMode()) {
                Log.d(LOG_TAG, "Entering drawIconBitmap!");

                Bitmap iconBitmap = getIconBitmap();
                float scaledWidth = (mTempHighPaint.getTextSize() / iconBitmap.getHeight()) * iconBitmap.getWidth();
                Bitmap weatherIcon = Bitmap.createScaledBitmap(
                        iconBitmap,
                        (int) scaledWidth,
                        (int) mTempHighPaint.getTextSize(),
                        true);
                mWeatherIconXOffset = bounds.centerX() - ((mTempHighTextSize / 2) + weatherIcon.getWidth() + 30);

                canvas.drawBitmap(weatherIcon,
                        mWeatherIconXOffset,
                        mWeatherIconYOffset - weatherIcon.getHeight(),
                        null
                );
                Log.d(LOG_TAG, "Entering drawIconBitmap!");
            }
        }

        private void drawHighTemperature(Canvas canvas, Rect bounds) {
            Log.d(LOG_TAG, "Entering drawHighTemperature!");
            float highTempBaseOffset;
            if (isInAmbientMode()) {
                highTempBaseOffset = (mTempHighTextSize + mTempLowTextSize + 20);
            } else {
                highTempBaseOffset = mTempHighTextSize;
            }
            mTempHighXOffset = bounds.centerX() - (highTempBaseOffset / 2);

            canvas.drawText(mTempHighStr,
                    mTempHighXOffset,
                    mTempHighYOffset,
                    mTempHighPaint
            );
            Log.d(LOG_TAG, "Exiting drawHighTemperature!");
        }

        private void drawLowTemperature(Canvas canvas, Rect bounds) {
            Log.d(LOG_TAG, "Entering drawLowTemperature!");
            float lowTempBaseOffset;
            if (isInAmbientMode()) {
                lowTempBaseOffset = bounds.centerX() - (mTempLowTextSize / 2) + 10;
            } else {
                lowTempBaseOffset = bounds.centerX() + 20;
            }
            mTempLowXOffset = lowTempBaseOffset + (mTempHighTextSize / 2);

            canvas.drawText(mTempLowStr,
                    mTempLowXOffset,
                    mTempLowYOffset,
                    mTempLowPaint
            );
            Log.d(LOG_TAG, "Exiting drawLowTemperature!");
        }

        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Log.d(LOG_TAG, "onConnected!");
            Wearable.DataApi.addListener(mGoogleApiClient, this);
            SunshineUtility.setAppConnReq(getApplicationContext(), true);
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.d(LOG_TAG, "onConnectionSuspended!");
        }

        @Override
        public void onDataChanged(DataEventBuffer dataEventBuffer) {
            for (DataEvent dataEvent : dataEventBuffer) {
                if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                    DataItem dataItem = dataEvent.getDataItem();
                    if (dataItem.getUri().getPath().compareTo(SunshineConstants.WEATHER_DATA_PATH) == 0) {
                        readWeatherDataInWear(dataItem);
                    }
                }
            }
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            Log.d(LOG_TAG, "onConnectionFailed! - " + connectionResult.getErrorMessage());
            SunshineUtility.setAppConnReq(getApplicationContext(), false);
        }

        private void requestWeatherUpdateFromApp() {
            Log.d(LOG_TAG, "requestWeatherUpdateFromApp!");
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    ConnectionResult connectionResult = mGoogleApiClient.blockingConnect(30, TimeUnit.SECONDS);
                    if (connectionResult.isSuccess()) {
                        SunshineUtility.setAppConnReq(getApplicationContext(), true);
                        writeWeatherUpdateReqDataFromWear();
                    }
                    return null;
                }
            };
        }

        private void writeWeatherUpdateReqDataFromWear() {
            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(SunshineConstants.WEATHER_INFO_REQ_PATH);

            PutDataRequest putDataRequest = putDataMapRequest.asPutDataRequest();
            Wearable.DataApi.putDataItem(mGoogleApiClient, putDataRequest);

            Log.d(LOG_TAG, "writeWeatherUpdateReqDataFromWear : Weather Data Requested Successfully!");
        }

        private void readWeatherDataInWear(DataItem dataItem) {
            DataMap dataMap = DataMapItem.fromDataItem(dataItem).getDataMap();

            SunshineUtility.setAllWeatherValues(getApplicationContext(),
                    dataMap.getString(SunshineConstants.KEY_WEATHER_ID),
                    dataMap.getString(SunshineConstants.KEY_HIGH_TEMP),
                    dataMap.getString(SunshineConstants.KEY_LOW_TEMP)
            );

            Log.d(LOG_TAG,
                    "\nWeatherId : " + SunshineUtility.getWeatherId(getApplicationContext()) +
                            "\nHighTemp  : " + SunshineUtility.getHighTemp(getApplicationContext()) +
                            "\nLowTemp   : " + SunshineUtility.getLowTemp(getApplicationContext())
            );

            SunshineUtility.setAppConnReq(getApplicationContext(), false);
            Log.d(LOG_TAG, "readWeatherDataInWear : Weather Data Requested Successfully!");
            invalidate();
        }
    }
}
