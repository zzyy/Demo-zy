package com.android.launcher;

import android.content.*;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import com.example.zylauncher.R;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Alex on 2014/10/28.
 */
public class LauncherProvider extends ContentProvider{
    private static final String TAG = "zy.LauncherProvider";

    public static final String AUTHORITY = "com.android.launcher.setting";

    //default_workspace.xml tags.
    private static final String TAG_FAVORITES = "favorites";
    private static final String TAG_FAVORITE = "favorite";
    private static final String TAG_CLOCK = "clock";
    private static final String TAG_SEARCH = "search";
    private static final String TAG_APPWIDGET = "appwidget";
    private static final String TAG_SHORTCUT = "shortcut";
    private static final String TAG_FOLDER = "folder";
    private static final String TAG_EXTRA = "extra";
    private static final String TAG_INCLUDE = "include";

    public static String TABLE_FAVOURITES = "favorites";
    public static final String TABLE_WORKSPACE_SCREENS = "workspaceScreens";
    public static final String PARAMETER_NOTIFY = "notify";

    private static final String DATABASE_NAME = "launcher.db";
    private static final int DATABASE_VERSION = 1;

    private DatabaseHelper mOpenHelper;
    static final String EMPTY_DATABASE_CREATED = "EMPTY_DATABASE_CREATED";
    static final String DEFAULT_WORKSPACE_RESOURCE_ID = "DEFAULT_WORKSPACE_RESOURCE_ID";

    @Override
    public boolean onCreate() {
        Log.d(TAG, "LauncherProvider onCreate");

        final Context context = getContext();

        mOpenHelper = new DatabaseHelper(context);
        LauncherAppState.setLauncherProvider(this);

        return true;
    }

    @Override
    public String getType(Uri uri) {
        SqlArguments args = new SqlArguments(uri, null, null);
        if (TextUtils.isEmpty(args.where)){
            return "vnd.android.cursor.dir/" + args.table;
        }else {
            return "vnd.android.cursor.item/" + args.table;
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
        SQLiteQueryBuilder sqb = new SQLiteQueryBuilder();
        sqb.setTables(args.table);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Cursor result = sqb.query(db, projection, args.where, args.args, null, null, sortOrder);
        result.setNotificationUri(getContext().getContentResolver(), uri);
        return result;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SqlArguments args = new SqlArguments(uri);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        addModifiedTime(values);

        final long rowId = dbInsertAndCheck(mOpenHelper, db, args.table, null, values);
        if (rowId <= 0){
            return null;
        }

        uri = ContentUris.withAppendedId(uri, rowId);
        sendNotify(uri);
        return uri;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        SqlArguments args = new SqlArguments(uri);
        SQLiteDatabase db  = mOpenHelper.getWritableDatabase();

        db.beginTransaction();
        try {
            int valueNum = values.length;
            for (int i=0; i<valueNum; i++){
                addModifiedTime(values[i]);
                long rowId = dbInsertAndCheck(mOpenHelper, db, args.table, null, values[i]);
                if (rowId < 0){
                    return 0;
                }
            }
        }finally {
            db.endTransaction();
        }

        sendNotify(uri);
        return values.length;
    }

    private void sendNotify(Uri uri) {
        String notify = uri.getQueryParameter(PARAMETER_NOTIFY);
        if (notify == null || "true".equals(notify)){
            getContext().getContentResolver().notifyChange(uri, null);
        }

        //fixme backup
    }

    private static long dbInsertAndCheck(DatabaseHelper helper, SQLiteDatabase db, String table, String nullColumnHack, ContentValues values) {
        if (!values.containsKey(BaseColumns._ID)){
            throw new RuntimeException("Error: attempting to add item w/o specify an id");
        }
        long result = db.insert(table, nullColumnHack, values);
        return result;
    }

    private void addModifiedTime(ContentValues values) {
        values.put(LauncherSettings.ChangeLogColumns.MODIFIED, System.currentTimeMillis());
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = db.delete(args.table, args.where, args.args);
        if (count>0)
            sendNotify(uri);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SqlArguments args = new SqlArguments(uri, selection, selectionArgs);
        addModifiedTime(values);

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = db.update(args.table, values, args.where, args.args);
        if (count>0)sendNotify(uri);

        return count;
    }

    synchronized public void loadDefaultFavoritesIfNecessary(int orgiWorkspaceResId) {
        String spksy = LauncherAppState.getSharedPreferencesKey();
        SharedPreferences sp = getContext().getSharedPreferences(spksy, Context.MODE_PRIVATE);

        if (sp.getBoolean(EMPTY_DATABASE_CREATED, false)){
            int workspaceResId = orgiWorkspaceResId;

            //use default workspace resource if not provided
            if (workspaceResId == 0){
                workspaceResId = sp.getInt(DEFAULT_WORKSPACE_RESOURCE_ID, R.xml.default_workspace);
            }

            SharedPreferences.Editor editor = sp.edit();
            editor.remove(EMPTY_DATABASE_CREATED);
            if (orgiWorkspaceResId != 0 ){
                editor.putInt(DEFAULT_WORKSPACE_RESOURCE_ID, orgiWorkspaceResId);
            }

            mOpenHelper.loadFavorites(mOpenHelper.getWritableDatabase(), workspaceResId);
            //fixme load old db

            editor.commit();
        }
    }

    static class SqlArguments{
        public final String table;
        public final String where;
        public final String[] args;

        SqlArguments(Uri uri, String where, String[] args) {
            final List<String> paths = uri.getPathSegments();
            if (paths.size() ==1){
                this.table = paths.get(0);
                this.where = where;
                this.args = args;
            }else if (paths.size() != 2){
                throw new IllegalArgumentException("Invalid URI: " + uri);
            }else if (!TextUtils.isEmpty(where)){
                throw new UnsupportedOperationException("where not support: " + uri);
            }else {
                this.table = paths.get(0);
                this.where = "_id=" + ContentUris.parseId(uri);
                this.args = args;
            }
        }

        SqlArguments(Uri uri) {
            if (uri.getPathSegments().size() == 1){
                this.table = uri.getPathSegments().get(0);
                this.where = null;
                this.args = null;
            }else {
                throw new IllegalArgumentException("Invalid URI: " + uri);
            }
        }
    }

    private static class DatabaseHelper extends SQLiteOpenHelper{

        private final Context mContext;

        private long mMaxItemId = -1;
        private long mMaxScreenid = -1;

        DatabaseHelper(Context context){
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            Log.d(TAG, "create DatabaseHelper");
            mContext = context;
            //fixme appwidget

            if (mMaxItemId == -1){
                mMaxItemId = initializeMaxItemId(getWritableDatabase());
            }

            if (mMaxScreenid == -1){
                mMaxScreenid = initializeMaxScreenId(getWritableDatabase());
            }
        }

        private long initializeMaxScreenId(SQLiteDatabase db) {
            Cursor c = db.rawQuery("SELECT MAX(_id) FROM" + TABLE_WORKSPACE_SCREENS, null);
            long id = -1;
            if (c!=null && c.moveToNext()){
                id = c.getLong(0);
            }

            if ( id == -1){
                throw new RuntimeException("Error: could not query max screen id");
            }

            return id;
        }

        private long initializeMaxItemId(SQLiteDatabase db) {
            Cursor c = db.rawQuery("SELECT MAX(_id) FROM" + LauncherProvider.TABLE_FAVOURITES, null);

            final int index = 0;
            long id = -1;
            if(c !=null && c.moveToNext()){
                id = c.getLong(index);
            }

            if (id == -1){
                throw new RuntimeException("Error: could not query max item id from favorites");
            }

            return id;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.d(TAG, "create launcher database");

            mMaxItemId = 1;
            mMaxScreenid = 0;

            db.execSQL("CREATE TABLE " + TABLE_FAVOURITES + "(" +
                    LauncherSettings.Favorites._ID + " INTEGER PRIMARY KEY," +
                    LauncherSettings.Favorites.TITLE + " TEXT," +
                    LauncherSettings.Favorites.INTENT + " TEXT," +
                    LauncherSettings.Favorites.CONTAINER + " INTEGER," +
                    LauncherSettings.Favorites.SCREEN + " INTEGER," +
                    LauncherSettings.Favorites.CELLX + " INTEGER," +
                    LauncherSettings.Favorites.CELLY + " INTEGER," +
                    LauncherSettings.Favorites.SPANX + " INTEGER," +
                    LauncherSettings.Favorites.SPANY + " INTEGER," +
                    LauncherSettings.Favorites.ITEM_TYPE + " INTEGER," +
                    LauncherSettings.Favorites.APPWIDGET_ID + " INTEGER NOT NULL DEFAULT -1," +
                    LauncherSettings.Favorites.IS_SHORTCUT + " INTEGER," +
                    LauncherSettings.Favorites.ICON_TYPE + " INTEGER," +
                    LauncherSettings.Favorites.ICON_PACKAGE + " TEXT," +
                    LauncherSettings.Favorites.ICON_RESOURCE + " TEXT," +
                    LauncherSettings.Favorites.ICON + " BLOB," +
                    LauncherSettings.Favorites.URI + " TEXT," +
                    LauncherSettings.Favorites.DISPLAY_MODE + " INTEGER," +
                    LauncherSettings.Favorites.APPWIDGET_PROVIDER + " TEXT," +
                    LauncherSettings.Favorites.MODIFIED + " INTEGER NOT NULL DEFAULT -1," +
                    ");");
            addWorkspacesTable(db);

            //fixme wipe previous widget

            //fixme try to converting old database

//TBD
        }

        private void addWorkspacesTable(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_WORKSPACE_SCREENS + " (" +
                    LauncherSettings.WorkspaceScreens._ID + " INTEGER," +
                    LauncherSettings.WorkspaceScreens.SCREEN_RANK + " INTEGER," +
                    LauncherSettings.ChangeLogColumns.MODIFIED + " INTEGER NOT NULL DEFAULT 0" +
                    ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//TBD
        }

        /** Loads default set favorites from an xml(default_workspace.xml) */
        public int loadFavorites(SQLiteDatabase db, int workspaceResId) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            ContentValues values = new ContentValues();

            PackageManager packageManager = mContext.getPackageManager();
            int i = 0;
            XmlResourceParser parser = mContext.getResources().getXml(workspaceResId);
            AttributeSet attrs = Xml.asAttributeSet(parser);
            try {
                beginDocument(parser, TAG_FAVORITES);

                final int depth = parser.getDepth();
                int type;
                while ((type = parser.next()) != XmlPullParser.END_TAG ||
                        parser.getDepth() > depth && type != XmlPullParser.END_DOCUMENT){
                    if (type != XmlPullParser.START_TAG){
                        continue;
                    }

                    boolean added =false;
                    //标签名
                    final String name = parser.getName();

                    //the tag is <Include>
                    if (TAG_INCLUDE.equals(name)){
                        TypedArray a = mContext.obtainStyledAttributes(attrs, R.styleable.Include);
                        final int resId = a.getResourceId(R.styleable.Include_workspace, 0);

                        if (resId!=0 && resId != workspaceResId){
                            i += loadFavorites(db, resId);
                            added = false;
                            mMaxItemId = -1;
                        }else {
                            //fixme
                        }

                        a.recycle();
                        continue;
                    }

                    TypedArray a = mContext.obtainStyledAttributes(attrs, R.styleable.Favorite);

                    long container = LauncherSettings.Favorites.CONTAINER_DESKTOP;
                    if (a.hasValue(R.styleable.Favorite_container)){
                        container = Long.valueOf(a.getString(R.styleable.Favorite_container));
                    }
                    String screen = a.getString(R.styleable.Favorite_screen);
                    String x = a.getString(R.styleable.Favorite_x);
                    String y = a.getString(R.styleable.Favorite_y);

                    values.clear();
                    values.put(LauncherSettings.Favorites.CONTAINER, container);
                    values.put(LauncherSettings.Favorites.SCREEN, screen);
                    values.put(LauncherSettings.Favorites.CELLX, x);
                    values.put(LauncherSettings.Favorites.CELLY, y);

                    if (TAG_FAVORITE.equals(name)){
                        long id = addAppsShortcut(db, values, a, packageManager, intent);
                        added = id>=0;
                    }else if (TAG_SEARCH.equals(name)){

                    }else if (TAG_CLOCK.equals(name)){

                    }else if (TAG_APPWIDGET.equals(name)){

                    }else if (TAG_SHORTCUT.equals(name)){

                    }else if (TAG_FOLDER.equals(name)){
                        String title;
                        int titleResId = a.getResourceId(R.styleable.Favorite_title, -1);
                        if (titleResId != -1){
                            title = mContext.getResources().getString(titleResId);
                        }else {
                            title = mContext.getResources().getString(R.string.folder_name);
                        }
                        values.put(LauncherSettings.Favorites.TITLE, title);
                        long folderId = addFolder(db, values);
                        added = folderId >= 0;

                        ArrayList<Long> folderItems = new ArrayList<Long>();

                        int folderDepth = parser.getDepth();
                        while ((type = parser.next()) != XmlPullParser.END_TAG || parser.getDepth() > folderDepth){
                            if (type != XmlPullParser.START_TAG){
                                continue;
                            }
                            final String folder_item_name = parser.getName();

                            values.clear();
                            values.put(LauncherSettings.Favorites.CONTAINER, folderId);

                            if (TAG_FAVORITE.equals(folder_item_name) && folderId >= 0){
                                long id = addAppsShortcut(db, values, a, packageManager, intent);
                                if (id >0){
                                    folderItems.add(id);
                                }
                            } else if (TAG_SHORTCUT.equals(folder_item_name) && folderId >=0 ){
                                long id = addUriShortcut(db, values, a);
                                if (id >0 ){
                                    folderItems.add(id);
                                }
                            }else {
                                throw new RuntimeException("folder can contain only shortcut");
                            }
//                            a.recycle();
                        }

                        // We can only have folders with >= 2 items, so we need to remove the
                        // folder and clean up if less than 2 items were included, or some
                        // failed to add, and less than 2 were actually added
                        if (folderItems.size() < 2 && folderId >= 0){
                            deleteId(db, folderId);
                            if (folderItems.size() > 0){
                                deleteId(db, folderItems.get(0));
                            }
                                added = false;
                        }
                    }

                    if (added) i++;
                    a.recycle();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            }

            // Update the max item id after we have loaded the database
            if (mMaxItemId == -1){
                mMaxItemId = initializeMaxItemId(db);
            }

            return i;
        }

        private long addFolder(SQLiteDatabase db, ContentValues values) {
            values.put(LauncherSettings.Favorites.ITEM_TYPE, LauncherSettings.Favorites.ITEM_TYPE_FOLDER);
            values.put(LauncherSettings.Favorites.SPANX, 1);
            values.put(LauncherSettings.Favorites.SPANY, 1);
            long id = generateNewItemId();
            values.put(LauncherSettings.Favorites._ID, id);
            if (dbInsertAndCheck(this, db, TABLE_FAVOURITES, null, values) <=0 ){
                return -1;
            }else {
                return id;
            }
        }

        private void deleteId(SQLiteDatabase db, long id) {
            Uri uri = LauncherSettings.Favorites.getContentUri(id, false);
            SqlArguments args = new SqlArguments(uri, null, null);
            db.delete(TABLE_FAVOURITES, args.where, args.args);
        }

        private long addUriShortcut(SQLiteDatabase db, ContentValues values, TypedArray a) {
            Resources resources = mContext.getResources();

            final int iconResId = a.getInt(R.styleable.Favorite_icon, 0);
            final int titleResId = a.getInt(R.styleable.Favorite_title, 0);

            Intent intent;
            String uri = null;

            try {
                uri = a.getString(R.styleable.Favorite_uri);
                intent = Intent.parseUri(uri, 0);
            } catch (URISyntaxException e) {
                return -1;
            }

            if (iconResId == 0 || titleResId ==0){
                return  -1;
            }

            long id = generateNewItemId();
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            values.put(LauncherSettings.Favorites.INTENT, intent.toUri(0));
            values.put(LauncherSettings.Favorites.TITLE, resources.getString(titleResId));
            values.put(LauncherSettings.Favorites.ITEM_TYPE, LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT);
            values.put(LauncherSettings.Favorites.SPANX, 1);
            values.put(LauncherSettings.Favorites.SPANY, 1);
            values.put(LauncherSettings.Favorites.ICON_TYPE, LauncherSettings.Favorites.ICON_TYPE_RESOURCE);
            values.put(LauncherSettings.Favorites.ICON_PACKAGE, mContext.getPackageName());
            values.put(LauncherSettings.Favorites.ICON_RESOURCE, iconResId);
            values.put(LauncherSettings.Favorites._ID, id);

            if (dbInsertAndCheck(this, db, TABLE_FAVOURITES, null, values) < 0){
                return -1;
            }

            return id;
        }

        private long addAppsShortcut(SQLiteDatabase db, ContentValues values, TypedArray a, PackageManager packageManager, Intent intent) {

            long id = -1;
            ActivityInfo info;
            String packageName = a.getString(R.styleable.Favorite_packageName);
            String className = a.getString(R.styleable.Favorite_className);

            try {
                ComponentName cn;
                cn = new ComponentName(packageName, className);
                try {
                    info = packageManager.getActivityInfo(cn, 0);
                } catch (PackageManager.NameNotFoundException e) {
                    String[] packages = packageManager.currentToCanonicalPackageNames(new String[]{packageName});
                    cn = new ComponentName(packages[0], className);
                    info = packageManager.getActivityInfo(cn, 0);
                }
                id = generateNewItemId();
                intent.setComponent(cn);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

                values.put(LauncherSettings.Favorites.INTENT, intent.toUri(0));
                values.put(LauncherSettings.Favorites.TITLE, info.loadLabel(packageManager).toString());
                values.put(LauncherSettings.Favorites.ITEM_TYPE, LauncherSettings.Favorites.ITEM_TYPE_APPLICATION);
                values.put(LauncherSettings.Favorites.SPANX, 1);
                values.put(LauncherSettings.Favorites.SPANY, 1);
//                values.put(LauncherSettings.Favourites._ID, generateNewItemId());
                values.put(LauncherSettings.Favorites._ID, id);

                if (dbInsertAndCheck(this, db, TABLE_FAVOURITES, null, values) < 0){
                    return -1;
                }
            } catch (PackageManager.NameNotFoundException e) {

            }

            return id;
        }

        private long generateNewItemId() {
            if (mMaxItemId < 0){
                throw new RuntimeException("Error: mMaxItemId was not initialize");
            }
            mMaxItemId += 1;
            return mMaxItemId;
        }

        //检测xml文档是否以 firstElementName 开始
        private static final void beginDocument(XmlResourceParser parser, String firstElementName) throws IOException, XmlPullParserException {
            int type;
            while ((type = parser.next()) != XmlPullParser.START_TAG && type != XmlPullParser.END_DOCUMENT);

            if (type != XmlPullParser.START_TAG)
                throw new XmlPullParserException("No start tag found");

            if (!parser.getName().equals(firstElementName)){
                throw new XmlPullParserException("Unexpected start tag, Actual: " + parser.getName() + ", Expected: "+ firstElementName);
            }
        }
    }
}
