package com.android.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2014/10/27.
 */
public class AllAppsList {
    private static final int DEFAULT_APPLICATIONS_NUMBER = 42;
    private final IconCache mIconCache;
    private final AppFilter mAppFilter;

    /**
     * list of all app in the launcher
     */
    public ArrayList<AppInfo> data = new ArrayList<AppInfo>();
    /** The list of apps that have been added since the last notify() call. */
    public ArrayList<AppInfo> added =
            new ArrayList<AppInfo>(DEFAULT_APPLICATIONS_NUMBER);
    /** The list of apps that have been removed since the last notify() call. */
    public ArrayList<AppInfo> removed = new ArrayList<AppInfo>();
    /** The list of apps that have been modified since the last notify() call. */
    public ArrayList<AppInfo> modified = new ArrayList<AppInfo>();

    public AllAppsList(IconCache iconCache, AppFilter appFilter) {
        mIconCache = iconCache;
        mAppFilter = appFilter;
    }

    public void addPackage(Context context, String packageName) {
        final List<ResolveInfo> matchs = findActivitiesForPackage(context, packageName);

        if(matchs.size() > 0){
            for (ResolveInfo info : matchs){
                add(new AppInfo(context.getPackageManager(), info, mIconCache, null));
            }
        }
    }

    private void add(AppInfo appInfo) {
        if(mAppFilter != null && mAppFilter.shouldShowApp(appInfo.componentName)){
            return;
        }
        if(findActivity(data, appInfo.componentName)){
            return;
        }

        data.add(appInfo);
        added.add(appInfo);
    }

    /**
     * return whether the apps container the component
     */
    private static boolean findActivity(ArrayList<AppInfo> apps, ComponentName componentName) {
        final int N  = apps.size();
        for(int i=0; i<N; i++){
            final AppInfo info = apps.get(i);
            if(info.componentName.equals(componentName)){
                return true;
            }
        }
        return false;
    }

    /**
     * Query package manager for MAIN/LAUNCHER activities in supplied package
     */
    private List<ResolveInfo> findActivitiesForPackage(Context context, String packageName) {
        final PackageManager packageManager = context.getPackageManager();

        final Intent mainIntent = new Intent(Intent.ACTION_MAIN);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        mainIntent.setPackage(packageName);

        final List<ResolveInfo> apps = packageManager.queryIntentActivities(mainIntent, 0);
        return apps != null ? apps : new ArrayList<ResolveInfo>();
    }

    /**  */
    private boolean findActivity(List<ResolveInfo> apps, ComponentName component) {
        final String className = component.getClassName();
        final int N = apps.size();
        for(int i=0; i<N; i++){
            final ActivityInfo activityInfo = apps.get(i).activityInfo;
            if(className.equals(activityInfo.name)){
                return true;
            }
        }
        return false;
    }

    public void updatepackage(Context context, String packageName) {
        final List<ResolveInfo> matchs = findActivitiesForPackage(context, packageName);

        if(matchs.size() > 0){
            //find unavailable activities and remove form data
            for(int i = data.size(); i>=0; i--){
                final AppInfo info = data.get(i);
                final ComponentName component = info.intent.getComponent();

                if(packageName.equals(component.getPackageName())){
                    if(!findActivity(matchs, component)){
                        removed.add(info);
                        mIconCache.remove(component);
                        data.remove(i);
                    }
                }
            }

            //update the available activities with new labels/icons
            int N = matchs.size();
            for (int i=0; i<N; i++){
                final ResolveInfo resolveInfo = matchs.get(i);
                final String pkgName = resolveInfo.activityInfo.packageName;
                final String className = resolveInfo.activityInfo.name;

                AppInfo info = findApplicationInfoLocked(pkgName, className);
                if(info == null){
                    add(new AppInfo(context.getPackageManager(), resolveInfo, mIconCache, null));
                }else {
                    mIconCache.remove(info.componentName);
                    mIconCache.getTitleAndIcon(info, resolveInfo, null);
                    modified.add(info);
                }
            }

        }else {
            //remove all data for this package
            for(int i=data.size(); i>=0; i--){
                final AppInfo info = data.get(i);
                final ComponentName componentName = info.intent.getComponent();
                if(packageName.equals(componentName.getPackageName())){
                    removed.add(info);
                    mIconCache.remove(componentName);
                    data.remove(i);
                }
            }
        }

    }

    public void removePackage(Context context, String packageName) {
        final List<AppInfo> data = this.data;

        for(int i=data.size()-1; i>=0; i--){
            AppInfo appInfo = data.get(i);
            final ComponentName componentName = appInfo.intent.getComponent();
            if(packageName.equals(componentName.getPackageName())){
                data.remove(i);
                removed.add(appInfo);
            }
        }

        mIconCache.flush();
    }

    private AppInfo findApplicationInfoLocked(String pkgName, String className) {
        for (AppInfo info : data){
            ComponentName componentName = info.intent.getComponent();
            if (componentName.getPackageName().equals(pkgName) && className.equals(componentName.getClassName())){
                return info;
            }
        }
        return null;
    }
}
