package com.android.launcher;

import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.util.Log;
import com.example.zylauncher.R;

import java.lang.ref.WeakReference;

/**
 * Created by Administrator on 2014/10/24.
 */
public class LauncherAppState {
    private static String TAG = "zy.LauncherAppState";

    private static final String SHARED_PREFERENCES_KEY = "com.android.launcher.prefs";
    private static LauncherAppState INSTANCE;
    private static WeakReference<LauncherProvider> sLauncherProvider;
    private static Context sContext;
    private final AppFilter mAppFilter;
    private final LancherModel mModel;
    private float mScreenDensity;
    private IconCache mIconCache;


    public static void setApplicationContext(Context mContext) {
        sContext = mContext;
    }

    public static LauncherAppState getInstance(){
        if (INSTANCE == null){
            INSTANCE = new LauncherAppState();
        }
        return INSTANCE;
    }

    private LauncherAppState(){
        Log.d(TAG, "LauncherAppState init");
        if (sContext == null)
            throw new IllegalStateException("sContext need init before contract LauncherAppState");

        mScreenDensity = sContext.getResources().getDisplayMetrics().density;

        mIconCache = new IconCache(sContext);

        mAppFilter = AppFilter.loadByName(sContext.getString(R.string.app_filter_class));
        mModel = new LancherModel(this, mIconCache, mAppFilter);

        //register intents receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");
        sContext.registerReceiver(mModel, filter);

        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
        filter.addAction(Intent.ACTION_LOCALE_CHANGED);
        filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
        sContext.registerReceiver(mModel, filter);

        filter = new IntentFilter();
        filter.addAction(SearchManager.INTENT_GLOBAL_SEARCH_ACTIVITY_CHANGED);
        sContext.registerReceiver(mModel, filter);

        filter = new IntentFilter();
        filter.addAction(SearchManager.INTENT_ACTION_SEARCHABLES_CHANGED);
        sContext.registerReceiver(mModel, filter);

        //observes the favorites table
        ContentResolver resolver = sContext.getContentResolver();
        resolver.registerContentObserver(LauncherSettings.Favorites.CONTENT_URI, true, mFavoritesObserver);

    }

    private final ContentObserver mFavoritesObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            Log.d(TAG, "mFavoritesObserver onChange: selfChange=" + selfChange);

            // If the database has ever changed, then we really need to force a reload of the
            // workspace on the next load
            mModel.resetLoadedState(false, true);
            mModel.startLoaderFromBackground();
        }
    };

    public static String getSharedPreferencesKey() {
        return SHARED_PREFERENCES_KEY;
    }

    public Context getContext() {
        return sContext;
    }

    public static LauncherProvider getLauncherProvider() {
        return sLauncherProvider.get();
    }

    public static void setLauncherProvider(LauncherProvider launcherProvider) {
        sLauncherProvider = new WeakReference<LauncherProvider>(launcherProvider);
    }
}
