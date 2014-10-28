package com.android.launcher;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

/**
 * Created by Alex on 2014/10/28.
 */
public class LauncherProvider extends ContentProvider{


    public static final String AUTHORITY = "com.android.launcher.setting";


    public static String TABLE_FAVOURITES = "favorites";
    public static final String TABLE_WORKSPACE_SCREENS = "workspaceScreens";
    public static final String PARAMETER_NOTIFY = "notify";

    @Override
    public boolean onCreate() {
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
