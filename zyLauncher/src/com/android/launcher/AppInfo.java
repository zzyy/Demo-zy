package com.android.launcher;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;

import java.util.HashMap;

/**
 * Created by Administrator on 2014/10/27.
 */
public class AppInfo extends ItemInfo{
    private static final int DOWNLOADED_FLAG = 1;
    private static final int UPDATED_SYSTEM_APP_FLAG = 2;

    ComponentName componentName;

    /**
     * The intent used to start app
     */
    Intent intent;

    Bitmap iconBitmap;
    int flag;
    long firstInstallTime;

    public AppInfo(PackageManager pm, ResolveInfo info, IconCache iconCache, HashMap<Object, CharSequence> labelCache) {
        final String packageName = info.activityInfo.applicationInfo.packageName;
        this.componentName = new ComponentName(packageName, info.activityInfo.name);

        this.container = NO_ID;
        this.setActivity(componentName, Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        this.setFlagAndInstallTime(pm, packageName);
        iconCache.getTitleAndIcon(this, info, labelCache);
    }

    private void setFlagAndInstallTime(PackageManager pm, String packageName) {
        try {
            PackageInfo packageInfo = pm.getPackageInfo(packageName, 0);
            flag = initFlags(packageInfo);
            firstInstallTime = initFirstInstallTime(packageInfo);
        } catch (PackageManager.NameNotFoundException e) {
            //fixme exception
        }

    }

    private static long initFirstInstallTime(PackageInfo pi) {
        return pi.firstInstallTime;
    }

    private static int initFlags(PackageInfo packageInfo) {
        int appFlags = packageInfo.applicationInfo.flags;
        int flags = 0;

        if((appFlags & ApplicationInfo.FLAG_SYSTEM) ==0){
            flags |= DOWNLOADED_FLAG;
            if((appFlags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0){
                flags |= UPDATED_SYSTEM_APP_FLAG;
            }
        }

        return flags;
    }

    /**
     * create the application intent
     * @param className
     * @param launchFlags
     */
    final void setActivity(ComponentName className, int launchFlags){
        this.intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(className);
        intent.setFlags(launchFlags);

        this.itemType =LauncherSettings.BaseLauncherColumns.ITEM_TYPE_APPLICATION;
    }
}
