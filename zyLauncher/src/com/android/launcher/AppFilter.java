package com.android.launcher;

import android.content.ComponentName;
import android.text.TextUtils;

/**
 * Created by Administrator on 2014/10/27.
 */
public abstract class AppFilter {
    public abstract boolean shouldShowApp(ComponentName app);


    public static AppFilter loadByName(String className){
        if(TextUtils.isEmpty(className))
            return null;
        return null;
    }
}
