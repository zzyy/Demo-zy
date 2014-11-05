package com.android.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.util.Log;

/**
 * Created by Administrator on 2014/11/4.
 */
public class ShortcutInfo extends ItemInfo {
    Intent intent;

    /**
     * Indicates whether the icon comes from an application's resource (if false)
     * or from a custom Bitmap (if true.)
     */
    boolean customIcon;

    /**
     * Indicates whether we're using the default fallback icon instead of something from the
     * app.
     */
    boolean usingFallbackIcon;

    /**
     * If isShortcut=true and customIcon=false, this contains a reference to the
     * shortcut icon as an application's resource.
     */
    Intent.ShortcutIconResource iconResource;

    /**
     * The application icon.
     */
    private Bitmap mIcon;

    long firstInstallTime;
    int flags = 0;

    ShortcutInfo() {
        itemType = LauncherSettings.BaseLauncherColumns.ITEM_TYPE_SHORTCUT;
    }

    protected Intent getIntent() {
        return intent;
    }

    public ShortcutInfo(Context context, ShortcutInfo info) {
        super(info);
        title = info.title.toString();
        intent = new Intent(info.intent);
        if (info.iconResource != null) {
            iconResource = new Intent.ShortcutIconResource();
            iconResource.packageName = info.iconResource.packageName;
            iconResource.resourceName = info.iconResource.resourceName;
        }
        mIcon = info.mIcon; // TODO: should make a copy here.  maybe we don't need this ctor at all
        customIcon = info.customIcon;
        initFlagsAndFirstInstallTime(
                getPackageInfo(context, intent.getComponent().getPackageName()));
    }

    /** TODO: Remove this.  It's only called by ApplicationInfo.makeShortcut. */
    public ShortcutInfo(AppInfo info) {
        super(info);
        title = info.title.toString();
        intent = new Intent(info.intent);
        customIcon = false;
        flags = info.flags;
        firstInstallTime = info.firstInstallTime;
    }

    public static PackageInfo getPackageInfo(Context context, String packageName) {
        PackageInfo pi = null;
        try {
            PackageManager pm = context.getPackageManager();
            pi = pm.getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            Log.d("ShortcutInfo", "PackageManager.getPackageInfo failed for " + packageName);
        }
        return pi;
    }

    void initFlagsAndFirstInstallTime(PackageInfo pi) {
        flags = AppInfo.initFlags(pi);
        firstInstallTime = AppInfo.initFirstInstallTime(pi);
    }

    public void setIcon(Bitmap b) {
        mIcon = b;
    }

        //TBD

    @Override
    public String toString() {
        return "ShortcutInfo(title=" + title.toString() + "intent=" + intent + "id=" + this.id
                + " type=" + this.itemType + " container=" + this.container + " screen=" + screenId
                + " cellX=" + cellX + " cellY=" + cellY + " spanX=" + spanX + " spanY=" + spanY
                + " dropPos=" + dropPos + ")";
    }
}
