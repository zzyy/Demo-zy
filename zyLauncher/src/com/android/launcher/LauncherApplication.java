package com.android.launcher;

import android.app.Application;
import android.util.Log;

/**
 * Created by Administrator on 2014/10/24.
 */
public class LauncherApplication extends Application {
    private static String TAG = "zy";

    @Override
    public void onCreate() {
        Log.d(TAG, "LauncherApplication --> onCreate start");
        super.onCreate();

        LauncherAppState.setApplicationContext(this);
        LauncherAppState.getInstance();
    }


}
