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

    private static void saveBoolValuesToPrefs(Context context, String keyStr, boolean valueBool) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(keyStr, valueBool)
                .commit();

        Log.d(LOG_TAG, "Boolean Value - " + valueBool + " Saved for Key - " + keyStr);
    }

    private static boolean retrieveBoolValuesFromPrefs(Context context, String keyStr) {
        boolean valueBool = PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(keyStr, true);

        Log.d(LOG_TAG, "Boolean Value - " + valueBool + " Retrieved for Key - " + keyStr);
        return valueBool;
    }

    private static void saveStringValuesToPrefs(Context context, String keyStr, String valueStr) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(keyStr, valueStr)
                .commit();

        Log.d(LOG_TAG, "String Value - " + valueStr + " Saved for Key - " + keyStr);
    }

    private static String retrieveStringValuesFromPrefs(Context context, String keyStr) {
        String valueStr = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(keyStr, "");

        Log.d(LOG_TAG, "String Value - " + valueStr + " Retrieved for Key - " + keyStr);
        return valueStr;
    }

    public static void setAppConnReq(Context context, boolean appConnReq) {
        saveBoolValuesToPrefs(context, SunshineConstants.KEY_APP_CONN_REQ, appConnReq);
    }

    public static boolean getAppConnReq(Context context) {
        return retrieveBoolValuesFromPrefs(context, SunshineConstants.KEY_APP_CONN_REQ);
    }

    public static void setWearConnReq(Context context, boolean appConnReq) {
        saveBoolValuesToPrefs(context, SunshineConstants.KEY_WEARABLE_CONN_REQ, appConnReq);
    }

    public static boolean getWearConnReq(Context context) {
        return retrieveBoolValuesFromPrefs(context, SunshineConstants.KEY_WEARABLE_CONN_REQ);
    }

    public static void setWeatherId(Context context, String weatherId) {
        saveStringValuesToPrefs(context, SunshineConstants.KEY_WEATHER_ID, weatherId);
    }

    public static void setHighTemp(Context context, String highTemperature) {
        saveStringValuesToPrefs(context, SunshineConstants.KEY_HIGH_TEMP, highTemperature);
    }

    public static void setLowTemp(Context context, String lowTemperature) {
        saveStringValuesToPrefs(context, SunshineConstants.KEY_LOW_TEMP, lowTemperature);
    }

    public static void setAllWeatherValues(Context context, String weatherId,
                                           String highTemperature, String lowTemperature) {
        saveStringValuesToPrefs(context, SunshineConstants.KEY_WEATHER_ID, weatherId);
        saveStringValuesToPrefs(context, SunshineConstants.KEY_HIGH_TEMP, highTemperature);
        saveStringValuesToPrefs(context, SunshineConstants.KEY_LOW_TEMP, lowTemperature);
    }

    public static String getWeatherId(Context context) {
        return retrieveStringValuesFromPrefs(context, SunshineConstants.KEY_WEATHER_ID);
    }

    public static String getHighTemp(Context context) {
        return retrieveStringValuesFromPrefs(context, SunshineConstants.KEY_HIGH_TEMP);
    }

    public static String getLowTemp(Context context) {
        return retrieveStringValuesFromPrefs(context, SunshineConstants.KEY_LOW_TEMP);
    }

    public static String[] getAllWeatherValues(Context context) {
        return new String[]{
                retrieveStringValuesFromPrefs(context, SunshineConstants.KEY_WEATHER_ID),
                retrieveStringValuesFromPrefs(context, SunshineConstants.KEY_HIGH_TEMP),
                retrieveStringValuesFromPrefs(context, SunshineConstants.KEY_LOW_TEMP)
        };
    }
}
