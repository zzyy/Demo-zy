package com.android.launcher;

import android.appwidget.AppWidgetManager;
import android.content.*;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.*;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class LancherModel extends BroadcastReceiver {
    private static  String TAG = "zy.LancherModel";

    private final Object mLock = new Object();

    private final boolean mAppsCanBeOnRemoveableStorage;
    private final LauncherAppState mApp;
    private final IconCache mIconCache;
    private final AllAppsList mBgAllAppsList;
    private Bitmap mDefaultIcon;

    static final ArrayList<ItemInfo> sBgWorkspaceItems = new ArrayList<ItemInfo>();
//    static final ArrayList<LauncherAppWidgetInfo> sBgAppWidgets = new ArrayList<LauncherAppWidgetInfo>();
    static final HashMap<Long, FolderInfo> sBgFolders = new HashMap<Long, FolderInfo>();
    // sBgItemsIdMap maps *all* the ItemInfos (shortcuts, folders, and widgets) created by
    // LauncherModel to their ids
    static final HashMap<Long, ItemInfo> sBgItemsIdMap = new HashMap<Long, ItemInfo>();
    // sBgDbIconCache is the set of ItemInfos that need to have their icons updated in the database
    static final HashMap<Object, byte[]> sBgDbIconCache = new HashMap<Object, byte[]>();
    // sBgWorkspaceScreens is the ordered set of workspace screens.
    static final ArrayList<Long> sBgWorkspaceScreens = new ArrayList<Long>();

    private static final HandlerThread sWorkerThread = new HandlerThread("lancher-loader");
    static {
        sWorkerThread.start();
    }
    private static final Handler sWorker = new Handler(sWorkerThread.getLooper());

    private LoaderTask mLoaderTask;
    private boolean mIsLoaderTaskRunning;

    private boolean mAllAppsLoaded;
    private boolean mWorkspaceLoaded;
    static final Object sBglock = new Object();

    public static void checkItemInfo(ItemInfo itemInfo) {
        final StackTraceElement[] stackTrace = new Throwable().getStackTrace();
        final long itemId = itemInfo.id;
        Runnable r = new Runnable() {
            @Override
            public void run() {
                synchronized (sBglock){
                    checkItemInfoLocked(itemId, item, stackTrace);
                }
            }
        };

        runOnWorkerThread(r);
    }

    private static void runOnWorkerThread(Runnable r) {
        if (sWorkerThread.getThreadId() == Process.myTid()) {
            r.run();
        } else {
            // If we are not on the worker thread, then post to the worker handler
            sWorker.post(r);
        }
    }

    /**
     * the thread used to load the contents of the launcher
     *   -workspace icon
     *   -widgets
     *   -all apps icon
     */
    private class LoaderTask implements Runnable{
        private Context mContext;
        /** The Launcher app is launching or have launched */
        private boolean mIsLaunching;
        private boolean mIsLoadingAndBindingWorkspace;
        private boolean mStopped;
        private boolean mLoadAndBindStepFinish;

        private HashMap<Objects, CharSequence> mLabelCache;

        private LoaderTask(Context mContext ,boolean mIsLaunching) {
            this.mIsLaunching = mIsLaunching;
            this.mContext = mContext;

            mLabelCache = new HashMap<Objects, CharSequence>();
        }

        @Override
        public void run() {
            boolean isUpgrade =false;

            synchronized (mLock){
                mIsLoaderTaskRunning = true;
            }

            keep_running:{
                // Elevate priority when Home launches for the first time to avoid
                // starving at boot time. Staring at a blank home is not cool.
                synchronized (mLock){
                    Process.setThreadPriority(mIsLaunching ? Process.THREAD_PRIORITY_DEFAULT : Process.THREAD_PRIORITY_BACKGROUND);
                }

                isUpgrade = loadAndBindWorkspace();
//TBD
            }

                //TBD
        }

        /** returns whether this is an upgrade path */
        private boolean loadAndBindWorkspace() {
            mIsLoadingAndBindingWorkspace = true;

            boolean isUpgradePath = false;
            if(!mWorkspaceLoaded){
                isUpgradePath = loadWorkspace();
                synchronized (LoaderTask.this){
                    if (mStopped){
                        return isUpgradePath;
                    }
                    mWorkspaceLoaded = true;
                }
            }
//TBD
            return isUpgradePath;
        }

        private boolean loadWorkspace() {
            final Context context = mContext;
            final ContentResolver contentResolver = context.getContentResolver();
            final PackageManager packageManager = context.getPackageManager();
            final AppWidgetManager widgets = AppWidgetManager.getInstance(context);
            final boolean isSafeMode = packageManager.isSafeMode();

            LauncherAppState app = LauncherAppState.getInstance();

            DeviceProfile grid = app.getDynamicGrid().getDeviceProfile();
            int countX = (int) grid.numColumns;
            int countY = (int) grid.numRows;

            LauncherAppState.getLauncherProvider().loadDefaultFavoritesIfNecessary(0);

            // Check if we need to do any upgrade-path logic
            //boolean loadedOldDb = LauncherAppState.getLauncherProvider().justLoadedOldDb();

            synchronized (sBglock){
                clearSBgDataStructures();

                final ArrayList<Long> itemsToRemove = new ArrayList<Long>();
                final Uri contentUri = LauncherSettings.Favorites.CONTENT_URI;
                final Cursor c = contentResolver.query(contentUri, null, null ,null, null);

                final HashMap<Long, ItemInfo[][]> occupied = new HashMap<Long, ItemInfo[][]>();
                try {
                    final int idIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites._ID);
                    final int intentIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.INTENT);
                    final int titleIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.TITLE);
                    final int iconTypeIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ICON_TYPE);
                    final int iconIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ICON);
                    final int iconPackageIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ICON_PACKAGE);
                    final int iconResourceIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ICON_RESOURCE);
                    final int containerIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CONTAINER);
                    final int itemTypeIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ITEM_TYPE);
                    final int appWidgetIdIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.APPWIDGET_ID);
                    final int appWidgetProviderIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.APPWIDGET_PROVIDER);
                    final int screenIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.SCREEN);
                    final int cellXIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLX);
                    final int cellYIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLY);
                    final int spanXIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.SPANX);
                    final int spanYIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.SPANY);

                    ShortcutInfo info;
                    String intentDescription;
                    LauncherAppWidgetInfo appWidgetInfo;
                    int container;
                    long id;
                    Intent intent;

                    while (!mStopped && c.moveToNext()){
                        AtomicBoolean deleteOnItemOverlap = new AtomicBoolean(false);

                        int itemType = c.getInt(itemTypeIndex);
                        switch (itemType){
                            case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
                            case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                                id = c.getLong(idIndex);
                                intentDescription = c.getString(intentIndex);
                                try {
                                    intent = Intent.parseUri(intentDescription, 0);
                                    ComponentName cn = intent.getComponent();
                                    if (cn != null && !isValidPackageComponent(packageManager, cn)){
                                        if (!mAppsCanBeOnRemoveableStorage){
                                            //invalid package, remove it from db
                                            itemsToRemove.add(id);
                                        }else {
                                            //if apps can be on external storage, leave them for the user to remove.
                                        }
                                    }
                                } catch (URISyntaxException e) {
                                    e.printStackTrace();
                                    continue;
                                }

                                if (itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION){
                                    info = getShortcutInfo(packageManager, intent, context, c, iconIndex, titleIndex, mLabelCache);
                                }else {
                                    info = getShortcutInfo(c, context, iconTypeIndex, iconPackageIndex, iconResourceIndex, iconIndex, titleIndex);

                                    if (intent.getAction() != null && intent.getCategories() != null
                                            && intent.getAction().equals(Intent.ACTION_MAIN) && intent.getCategories().contains(Intent.CATEGORY_LAUNCHER)){
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                                    }
                                }

                                if (info != null){
                                    info.id = id;
                                    info.intent = intent;
                                    container = c.getInt(containerIndex);
                                    info.container = container;
                                    info.screenId = c.getInt(screenIndex);
                                    info.cellX = c.getInt(cellXIndex);
                                    info.cellY = c.getInt(cellYIndex);
                                    info.spanX = 1;
                                    info.spanY = 1;

                                    if (container == LauncherSettings.Favorites.CONTAINER_DESKTOP){
                                        if (checkItemDimensions(info)){
                                            Log.d(TAG, "skipped load out of bound shortcut " + info);
                                            continue;
                                        }
                                    }

                                    deleteOnItemOverlap.set(false);
                                    if (!checkItemPlacement(occupied, info, deleteOnItemOverlap)){
                                        if (deleteOnItemOverlap.get()){
                                            itemsToRemove.add(id);
                                        }
                                        break;
                                    }

                                    switch (container){
                                        case LauncherSettings.Favorites.CONTAINER_DESKTOP:
                                        case LauncherSettings.Favorites.CONTAINER_HOTSEAT:
                                            sBgWorkspaceItems.add(info);
                                            break;
                                        default:
                                            // Item is in a user folder
                                            FolderInfo folderInfo =
                                                    findOrMakeFolder(sBgFolders, container);
                                            folderInfo.add(info);
                                            break
                                    }

                                    sBgItemsIdMap.put(info.id, info);
                                    queueIconToBeChecked(sBgDbIconCache, info, c, iconIndex);
                                }else {
                                    throw new RuntimeException("Unexpected null ShortcutInfo");
                                }
                                break;

                            case LauncherSettings.Favorites.ITEM_TYPE_FOLDER:
                                id = c.getLong(idIndex);
                                FolderInfo folderInfo = findOrMakeFolder(sBgFolders, id);

                                folderInfo.title = c.getString(titleIndex);
                                folderInfo.id = id;
                                container = c.getInt(containerIndex);
                                folderInfo.container = container;
                                folderInfo.screenId = c.getInt(screenIndex);
                                folderInfo.cellX = c.getInt(cellXIndex);
                                folderInfo.cellY = c.getInt(cellYIndex);
                                folderInfo.spanX = 1;
                                folderInfo.spanY = 1;


                                //TBD
                            case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:

                        }
                    }

                }finally {
                    if (c != null)
                        c.close();
                }

                // Break early if we've stopped loading
                if (mStopped) {
                    clearSBgDataStructures();
                    return false;
                }

                //TBD
            }

            //TBD
        }

        private boolean checkItemPlacement(HashMap<Long, ItemInfo[][]> occupied, ShortcutInfo item, AtomicBoolean deleteOnItemOverlap) {
            LauncherAppState app = LauncherAppState.getInstance();
            DeviceProfile grid = app.getDynamicGrid().getDeviceProfile();
            int countX = (int) grid.numColumns;
            int countY = (int) grid.numRows;

            long containerIndex = item.screenId;
            if (item.container == LauncherSettings.Favorites.CONTAINER_HOTSEAT){
                if (mCallbacks == null || mCallbacks.get().isAllAppsButtonRank((int)item.screenId)){
                    deleteOnItemOverlap.set(true);
                    return false;
                }

                if (occupied.containsKey(LauncherSettings.Favorites.CONTAINER_HOTSEAT)) {
                    if (occupied.get(LauncherSettings.Favorites.CONTAINER_HOTSEAT)[((int) item.screenId)][0] != null) {
                        Log.e(TAG, "Error loading shortcut into hotseat " + item
                                + " into position (" + item.screenId + ":" + item.cellX + ","
                                + item.cellY + ") occupied by "
                                + occupied.get(LauncherSettings.Favorites.CONTAINER_HOTSEAT)
                                [(int) item.screenId][0]);
                        return false;
                    }
                } else {
                    ItemInfo[][] items = new ItemInfo[countX + 1][countY + 1];
                    items[((int) item.screenId)][0] = item;
                    occupied.put((long) LauncherSettings.Favorites.CONTAINER_HOTSEAT, items);
                    return true;
                }
            }else if (item.container != LauncherSettings.Favorites.CONTAINER_DESKTOP){
                // Skip further checking if it is not the hotseat or workspace container
                return true;
            }

            if (occupied.containsKey(item.screenId)){
                ItemInfo[][] items = new ItemInfo[countX + 1][countY + 1];
                occupied.put(item.screenId, items);
            }

            //each screen
            ItemInfo[][] screens = occupied.get(item.screenId);
            // Check if any workspace icons overlap with each other
            for (int x=item.cellX; x<(item.cellX + item.spanX); x++){
                for (int y=item.cellY; y<(item.cellY+item.spanY); y++){
                    if (item[x][y] != null){
                        Log.e(TAG, "Error loading shortcut " + item
                                + " into cell (" + containerIndex + "-" + item.screenId + ":"
                                + x + "," + y
                                + ") occupied by "
                                + screens[x][y]);
                        return false;
                    }
                }
            }

            for (int x = item.cellX; x < (item.cellX+item.spanX); x++) {
                for (int y = item.cellY; y < (item.cellY+item.spanY); y++) {
                    screens[x][y] = item;
                }
            }

            return true;
        }

        private boolean checkItemDimensions(ShortcutInfo info) {
            LauncherAppState app = LauncherAppState.getInstance();
            DeviceProfile grid = app.getDynamicGrid().getDeviceProfile();
            return (info.cellX + info.spanX) > (int) grid.numColumns ||
                    (info.cellY + info.spanY) > (int) grid.numRows;
        }

        private boolean isValidPackageComponent(PackageManager packageManager, ComponentName componentName) {
            if (componentName==null){
                return false;
            }

            try {
                PackageInfo packageInfo = packageManager.getPackageInfo(componentName.getPackageName(), 0);
                if (!packageInfo.applicationInfo.enabled){
                    return false;
                }

                return packageManager.getActivityInfo(componentName, 0) != null;

            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }

        }

        private void clearSBgDataStructures() {
            synchronized (sBglock){
                sBgWorkspaceItems.clear();
//                sBgAppWidgets.clear();
                sBgFolders.clear();
                sBgItemsIdMap.clear();
                sBgDbIconCache.clear();
                sBgWorkspaceScreens.clear();
            }
        }

        public boolean isLaunching() {
            return mIsLaunching;
        }
        public boolean isLoadingWorkspace() {
            return mIsLoadingAndBindingWorkspace;
        }
    }

    private FolderInfo findOrMakeFolder(HashMap<Long, FolderInfo> folders, long id) {
        //TBD create folderInfo
        return null;
    }

    private ShortcutInfo getShortcutInfo(Cursor c, Context context, int iconTypeIndex, int iconPackageIndex, int iconResourceIndex, int iconIndex, int titleIndex) {
        //TBD
        return null;
    }

    private ShortcutInfo getShortcutInfo(PackageManager packageManager, Intent intent, Context context, Cursor c, int iconIndex, int titleIndex, HashMap<Objects, CharSequence> labelCache) {
        //TBD
        return null;
    }

    private WeakReference<Callbacks> mCallbacks;

    public interface Callbacks{
        boolean isAllAppsButtonRank(int rank);
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


    public void resetLoadedState(boolean resetAllAppsLoaded, boolean resetWorkspaceLoaded) {
        synchronized (mLock){
            
            // Stop any existing loaders first, so they don't set mAllAppsLoaded or
            // mWorkspaceLoaded to true later
            stopLoaderLocked();
            if (resetAllAppsLoaded) mAllAppsLoaded = false;
            if (resetWorkspaceLoaded) mWorkspaceLoaded = false;
        }
    }

    private boolean stopLoaderLocked() {
        boolean isLaunching  = false;
        //TBD

        return isLaunching;
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
