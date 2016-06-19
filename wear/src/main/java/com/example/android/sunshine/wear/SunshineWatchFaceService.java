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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.util.TimeZone;

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

    private class Engine extends CanvasWatchFaceService.Engine {

        // Load Resources that have alternate values for round watches.
        Resources mResources = SunshineWatchFaceService.this.getResources();
        Time mTime;
        Paint mBackgroundPaint;
        Paint mTimeTextPaint;
        Paint mDateTextPaint;
        Paint mDividerTextPaint;
        Paint mTempHighPaint;
        Paint mTempLowPaint;

        final int TEXT_SIZE_NOT_APPLICABLE = 0;
        final int PAINT_TYPE_TEXT = 1;
        final int PAINT_TYPE_BACKGROUND = 2;
        final String DIGITAL_TIME_FORMAT = "%d:%02d";

        final String EXAMPLE_TIME_STRING = "12:00";
        final String EXAMPLE_DATE_STRING = "WED, JUN 19, 2016";

        int mTapCount;
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

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            setWatchFaceStyle(getWatchFaceStyle());
            initPaintObjects();
            initXYOffsets();
            mTime = new Time();
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

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
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
            drawBackground(canvas, bounds);
            drawTimeText(canvas, bounds);
            drawDateText(canvas, bounds);
            drawDivider(canvas, bounds);
            drawIconBitmap(canvas, bounds);
            drawHighTemperature(canvas, bounds);
            drawLowTemperature(canvas, bounds);
            Log.d(LOG_TAG, "Exiting onDraw!");
        }

        private WatchFaceStyle getWatchFaceStyle(){
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

        private void initPaintObjects(){
            Log.d(LOG_TAG, "Entering initPaintObjects!");
            mBackgroundPaint    = getPaintObject(PAINT_TYPE_BACKGROUND, R.color.primary_dark, TEXT_SIZE_NOT_APPLICABLE);

            mTimeTextPaint      = getPaintObject(PAINT_TYPE_TEXT, R.color.accent_watch_face_dark, R.dimen.time_text_size);
            mDateTextPaint      = getPaintObject(PAINT_TYPE_TEXT, R.color.accent_watch_face_light, R.dimen.date_text_size);

            mDividerTextPaint   = getPaintObject(PAINT_TYPE_TEXT, R.color.accent_watch_face_light, R.dimen.divider_line_size);

            mTempHighPaint      = getPaintObject(PAINT_TYPE_TEXT, R.color.accent_watch_face_dark, R.dimen.temp_text_size);
            mTempLowPaint       = getPaintObject(PAINT_TYPE_TEXT, R.color.accent_watch_face_light, R.dimen.temp_text_size);
            Log.d(LOG_TAG, "Exiting initPaintObjects!");
        }

        private void initXYOffsets(){
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

            switch (paintType){
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

        private float getDimensionFromRes(int dimensionResId){
            return mResources.getDimension(dimensionResId);
        }

        private int getColorFromRes(int colorResId){
            return mResources.getColor(colorResId);
        }

        private void drawBackground(Canvas canvas, Rect bounds){
            Log.d(LOG_TAG, "Entering drawBackground!");
            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(getColorFromRes(R.color.ambient_primary_dark));
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
            }
            Log.d(LOG_TAG, "Exiting drawBackground!");
        }

        private void drawTimeText(Canvas canvas, Rect bounds){
            Log.d(LOG_TAG, "Entering drawTimeText!");
            // Draw H:MM in both ambient and interactive modes.
            mTime.setToNow();
            String timeFormat = String.format(DIGITAL_TIME_FORMAT, mTime.hour, mTime.minute);
            canvas.drawText(timeFormat, bounds.centerX() - mTimeXOffset, mTimeYOffset, mTimeTextPaint);
            Log.d(LOG_TAG, "Exiting drawTimeText!");
        }

        private void drawDateText(Canvas canvas, Rect bounds){
            Log.d(LOG_TAG, "Entering drawTimeText!");
            // Draw H:MM in both ambient and interactive modes.
            mTime.setToNow();
            String timeFormat = String.format(DIGITAL_TIME_FORMAT, mTime.hour, mTime.minute);
            canvas.drawText(timeFormat, bounds.centerX() - mDateXOffset, mDateYOffset, mDateTextPaint);
            Log.d(LOG_TAG, "Exiting drawTimeText!");
        }

        private void drawDivider(Canvas canvas, Rect bounds){
            Log.d(LOG_TAG, "Entering drawDivider!");
            // Draw divider
            canvas.drawLine(
                    bounds.centerX() - mDividerXOffset, mDividerYOffset,
                    bounds.centerX() + mDividerXOffset, mDividerYOffset,
                    mDividerTextPaint);
            Log.d(LOG_TAG, "Exiting drawDivider!");
        }

        private void drawIconBitmap(Canvas canvas, Rect bounds){
            Log.d(LOG_TAG, "Entering drawIconBitmap!");
            Log.d(LOG_TAG, "Entering drawIconBitmap!");
        }

        private void drawHighTemperature(Canvas canvas, Rect bounds){
            Log.d(LOG_TAG, "Entering drawTemperature!");
            // Draw H:MM in both ambient and interactive modes.
            mTime.setToNow();
            String timeFormat = String.format(DIGITAL_TIME_FORMAT, mTime.hour, mTime.minute);
            canvas.drawText(timeFormat, mTempHighXOffset, mTempHighYOffset, mTempHighPaint);
            Log.d(LOG_TAG, "Exiting drawTemperature!");
        }

        private void drawLowTemperature(Canvas canvas, Rect bounds){
            Log.d(LOG_TAG, "Entering drawTemperature!");
            // Draw H:MM in both ambient and interactive modes.
            mTime.setToNow();
            String timeFormat = String.format(DIGITAL_TIME_FORMAT, mTime.hour, mTime.minute);
            canvas.drawText(timeFormat, mTempLowXOffset, mTempLowYOffset, mTempLowPaint);
            Log.d(LOG_TAG, "Exiting drawTemperature!");
        }

    }
}
