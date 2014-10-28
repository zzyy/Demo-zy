package com.android.launcher;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.util.HashMap;

/**
 * Created by Administrator on 2014/10/24.
 */
public class IconCache {
    private static String TAG = "zy.IconCache";

    private int mIconDpi;
    private Bitmap mDefaultIcon;
    private Context mContext;
    private PackageManager mPackageManager;

    private final HashMap<ComponentName, CacheEntry> mCache = new HashMap<ComponentName, CacheEntry>();

    public void getTitleAndIcon(AppInfo appInfo, ResolveInfo info, HashMap<Object, CharSequence> labelCache) {
        CacheEntry entry = cacheLocked(appInfo.componentName, info, labelCache);

        appInfo.title = entry.titel;
        appInfo.iconBitmap = entry.icon;
    }

    private CacheEntry cacheLocked(ComponentName componentName, ResolveInfo resolveInfo, HashMap<Object, CharSequence> labelCache) {
        CacheEntry entry = mCache.get(componentName);
        if(entry == null)
            entry = new CacheEntry();
        mCache.put(componentName, entry);

        ComponentName key = LancherModel.getComponentNameFromResolveInfo(resolveInfo);
        if(labelCache !=null && labelCache.containsKey(key)){
            entry.titel = labelCache.get(key).toString();
        }else {
            //使用 label 标签名
            entry.titel = resolveInfo.loadLabel(mPackageManager).toString();
            if (labelCache != null){
                labelCache.put(key, entry.titel);
            }
        }
        //没有label标签时 使用class类名
        if (entry.titel == null){
            entry.titel = resolveInfo.activityInfo.name;
        }

        entry.icon = Utilities.creatIconBitmap(getFullResIcon(resolveInfo), mContext);
        return entry;
    }

    public void flush() {
        synchronized (mCache){
            mCache.clear();
        }
    }

    private static class CacheEntry{

        public Bitmap icon;
        public String titel;
    }
    public IconCache(Context context){
        Log.d(TAG, "init IconCache");
        mContext = context;

        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        mPackageManager = mContext.getPackageManager();

        mIconDpi = am.getLauncherLargeIconDensity();
        mDefaultIcon = makeDefaultIcon();
    }

    private Bitmap makeDefaultIcon() {
        Drawable d = getFullResDefaultActivityIcon();
        Bitmap b = Bitmap.createBitmap(Math.max(d.getIntrinsicWidth(), 1),
                    Math.max(d.getIntrinsicHeight(), 1), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        d.setBounds(0,0, b.getWidth(), b.getHeight());
        d.draw(c);
        c.setBitmap(null);
        return b;
    }

    public Drawable getFullResDefaultActivityIcon() {
        return getFullResIcon(Resources.getSystem(), android.R.mipmap.sym_def_app_icon);
    }

    private Drawable getFullResIcon(ResolveInfo resolveInfo) {
        return getFullResIcon(resolveInfo.activityInfo);
    }

    private Drawable getFullResIcon(ActivityInfo activityInfo) {
        Resources resources;
        try {
            resources = mPackageManager.getResourcesForApplication(activityInfo.applicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
            resources = null;
        }

        if (resources != null){
            int iconId = activityInfo.getIconResource();
            if (iconId != 0){
                return getFullResIcon(resources, iconId);
            }
        }
        return getFullResDefaultActivityIcon();
    }

    private Drawable getFullResIcon(Resources resources, int iconId) {
        Drawable d = null;
            d = resources.getDrawableForDensity(iconId, mIconDpi);

        return d!=null ? d : getFullResDefaultActivityIcon();
    }

    public void remove(ComponentName componentName) {
        synchronized (mCache){
            mCache.remove(componentName);
        }
    }
}
