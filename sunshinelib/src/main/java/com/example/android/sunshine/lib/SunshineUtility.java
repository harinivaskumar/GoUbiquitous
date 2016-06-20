package com.example.android.sunshine.lib;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by Hari Nivas Kumar R P on 6/20/2016.
 */
public class SunshineUtility {

    private static final String LOG_TAG = SunshineUtility.class.getSimpleName();

    /**
     * Helper method to provide the icon resource id according to the weather condition id returned
     * by the OpenWeatherMap call.
     *
     * @param weatherId from OpenWeatherMap API response
     * @return resource id for the corresponding icon. -1 if no relation is found.
     */
    public static int getIconResourceForWeatherCondition(int weatherId) {
        // Based on weather code data found at:
        // http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
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
        } else if (weatherId == 761 || weatherId == 781) {
            return R.drawable.ic_storm;
        } else if (weatherId == 800) {
            return R.drawable.ic_clear;
        } else if (weatherId == 801) {
            return R.drawable.ic_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.ic_cloudy;
        }
        return -1;
    }

    public static void saveValuesToPrefs(Context context, String keyStr, String valueStr) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(keyStr, valueStr)
                .commit();

        Log.d(LOG_TAG, "Value - " + valueStr + " Saved for Key - " + keyStr);
    }

    public static String retrieveValuesFromPrefs(Context context, String keyStr) {
        String valueStr = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(keyStr, "");

        Log.d(LOG_TAG, "Value - " + valueStr + " Retrieved for Key - " + keyStr);
        return valueStr;
    }

    public static void setWeatherId(Context context, String weatherId) {
        saveValuesToPrefs(context, SunshineConstants.KEY_WEATHER_ID, weatherId);
    }

    public static void setHighTemp(Context context, String highTemperature) {
        saveValuesToPrefs(context, SunshineConstants.KEY_HIGH_TEMP, highTemperature);
    }

    public static void setLowTemp(Context context, String lowTemperature) {
        saveValuesToPrefs(context, SunshineConstants.KEY_LOW_TEMP, lowTemperature);
    }

    public static void setAllWeatherValues(Context context, String weatherId,
                                           String highTemperature, String lowTemperature) {
        saveValuesToPrefs(context, SunshineConstants.KEY_WEATHER_ID, weatherId);
        saveValuesToPrefs(context, SunshineConstants.KEY_HIGH_TEMP, highTemperature);
        saveValuesToPrefs(context, SunshineConstants.KEY_LOW_TEMP, lowTemperature);
    }

    public static String getWeatherId(Context context) {
        return retrieveValuesFromPrefs(context, SunshineConstants.KEY_WEATHER_ID);
    }

    public static String getHighTemp(Context context) {
        return retrieveValuesFromPrefs(context, SunshineConstants.KEY_HIGH_TEMP);
    }

    public static String getLowTemp(Context context) {
        return retrieveValuesFromPrefs(context, SunshineConstants.KEY_LOW_TEMP);
    }

    public static String[] getAllWeatherValues(Context context) {
        return new String[]{
                retrieveValuesFromPrefs(context, SunshineConstants.KEY_WEATHER_ID),
                retrieveValuesFromPrefs(context, SunshineConstants.KEY_HIGH_TEMP),
                retrieveValuesFromPrefs(context, SunshineConstants.KEY_LOW_TEMP)
        };
    }
}
