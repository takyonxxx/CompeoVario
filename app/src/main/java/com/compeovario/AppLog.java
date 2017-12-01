package com.compeovario;

import android.util.Log;

class AppLog {

    private static final String APP_TAG = "TurkayWidget";

    public static int logString(String message) {
        return Log.i(APP_TAG, message);
    }
}
