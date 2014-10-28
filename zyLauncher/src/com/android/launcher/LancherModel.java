package com.android.launcher;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class LancherModel extends BroadcastReceiver {
    private static  String TAG = "zy.LancherModel";

    private final boolean mAppsCanBeOnRemoveableStorage;
    private final LauncherAppState mApp;
    private final IconCache mIconCache;
    private final AllAppsList mBgAllAppsList;
    private Bitmap mDefaultIcon;

    private static final HandlerThread sWorkerThread = new HandlerThread("lancher-loader");
    static {
        sWorkerThread.start();
    }
    private static final Handler sWorker = new Handler(sWorkerThread.getLooper());

    private WeakReference<Callbacks> mCallbacks;
    public interface Callbacks{
        //TBD
    }


    public LancherModel(LauncherAppState app, IconCache iconCache, AppFilter appFilter){
        Context context = app.getContext();

        mAppsCanBeOnRemoveableStorage = Environment.isExternalStorageRemovable();
        mApp = app;
        mIconCache = iconCache;

        mBgAllAppsList = new AllAppsList(iconCache, appFilter);

        mDefaultIcon = Utilities.creatIconBitmap( iconCache.getFullResDefaultActivityIcon(), context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive, intent= " + intent);

        String action = intent.getAction();

        if(Intent.ACTION_PACKAGE_ADDED.equals(action)
                || Intent.ACTION_PACKAGE_CHANGED.equals(action)
                || Intent.ACTION_PACKAGE_REMOVED.equals(action)){
            String packageName = intent.getData().getSchemeSpecificPart();
            boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);

            int op = PackageUpdateTask.OP_NONE;

            if(TextUtils.isEmpty(packageName))
                return;

            //confirm the operation
            if(Intent.ACTION_PACKAGE_CHANGED.equals(action)){
                op = PackageUpdateTask.OP_UPDATE;
            }else if(Intent.ACTION_PACKAGE_REMOVED.equals(action)){
                if(!replacing){
                    op = PackageUpdateTask.OP_REMOVE;
                }
            }else if(Intent.ACTION_PACKAGE_ADDED.equals(action)){
                if(!replacing){
                    op = PackageUpdateTask.OP_ADD;
                }else {
                    op = PackageUpdateTask.OP_UPDATE;
                }
            }

            if(op != PackageUpdateTask.OP_NONE){
                enqueuePackageUpdate(new PackageUpdateTask(op, new String[]{packageName}));
            }

        }else if(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE.equals(action)){
            //TBD

        }else if(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE.equals(action)){

        }
        //TBD

    }

    private void enqueuePackageUpdate(PackageUpdateTask task) {
        sWorker.post(task);
    }

    public static ComponentName getComponentNameFromResolveInfo(ResolveInfo resolveInfo) {
        if (resolveInfo.activityInfo != null){
            return new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
        }else {
            return new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);
        }
    }


    private class PackageUpdateTask implements Runnable{
        final int mOp;
        final String[] mPackages;

        public static final int OP_NONE = 0;
        public static final int OP_ADD = 1;
        public static final int OP_UPDATE = 2;
        public static final int OP_REMOVE = 3;
        public static final int OP_UNAVAILABLE = 4;

        public PackageUpdateTask(int op, String[] packageNames) {
            mOp = op;
            mPackages = packageNames;
        }

        @Override
        public void run() {
            final Context context = mApp.getContext();
            final String[] packages = mPackages;

            final int N = packages.length;
            switch (mOp){
                case OP_ADD:
                    for(int i=0; i<N; i++){
                        mBgAllAppsList.addPackage(context, packages[i]);
                    }
                    break;
                case OP_UPDATE:
                    for (int i=0; i<N; i++){
                        mBgAllAppsList.updatepackage(context, packages[i]);
                        //fixme appwidget need handle
                    }
                    break;
                case OP_REMOVE:
                case OP_UNAVAILABLE:
                    for(int i=0; i<N; i++){
                        mBgAllAppsList.removePackage(context, packages[i]);
                        //fixme appwidget need remove at same time
                    }
                    break;
            }

            ArrayList<AppInfo> added = null;
            ArrayList<AppInfo> modified = null;
            ArrayList<AppInfo> removedApps = new ArrayList<AppInfo>();

            if (mBgAllAppsList.added.size() > 0){
                added = new ArrayList<AppInfo>(mBgAllAppsList.added);
                mBgAllAppsList.added.clear();
            }
            if(mBgAllAppsList.modified.size() > 0 ){
                modified = new ArrayList<AppInfo>(mBgAllAppsList.modified);
                mBgAllAppsList.modified.clear();
            }
            if(mBgAllAppsList.removed.size() > 0){
                removedApps.addAll(mBgAllAppsList.removed);
                mBgAllAppsList.removed.clear();
            }

            final Callbacks callbacks = mCallbacks != null ? mCallbacks.get() : null;
            if(callbacks == null){
                return;
            }

            if(added != null){
                Callbacks cb  = mCallbacks!=null ? mCallbacks.get() : null;

            //TBD
            }

        }
    }
}
