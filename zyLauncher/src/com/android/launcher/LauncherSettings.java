package com.android.launcher;

import android.content.Intent;
import android.net.Uri;
import android.provider.BaseColumns;

import java.lang.String;

/**
 * Created by Administrator on 2014/10/27.
 */
public class LauncherSettings {

    public static interface ChangeLogColumns extends BaseColumns{
        static final String MODIFIED = "modified";
    }


    public static interface BaseLauncherColumns extends ChangeLogColumns{

        static final String TITLE = "title";

        /** Intent#parseUri(String, int) to create Intent */
        static final String INTENT = "intent";


        static final String ITEM_TYPE = "itemType";
        /** The gesture is an application */
        static final int ITEM_TYPE_APPLICATION = 0;

        /** The gesture is an application created shortcut */
        static final int ITEM_TYPE_SHORTCUT = 1;

        /**
         * The icon type.
         * <P>Type: INTEGER</P>
         */
        static final String ICON_TYPE = "iconType";

        static final int ICON_TYPE_RESOURCE = 0;
        static final int ICON_TYPE_BITMAP = 1;
        /** The icon package name and resource id, if icon type is ICON_TYPE_RESOURCE */
        static final String ICON_PACKAGE = "iconPackage";
        static final String ICON_RESOURCE = "iconResource";
        /** icon bitmap, Type: BLOB */
        static final String ICON = "icon";
    }


    static final class WorkspaceScreens implements ChangeLogColumns{

        static final Uri CONTENT_URI = Uri.parse("content://" + LauncherProvider.AUTHORITY
                + "/" + LauncherProvider.TABLE_WORKSPACE_SCREENS
                + "?" + LauncherProvider.PARAMETER_NOTIFY + "=true");

        //排序方法
        static final String SCREEN_RANK = "screenRank";
    }


    static final class Favourites implements BaseLauncherColumns{

        static final Uri CONTENT_URI = Uri.parse("content://" + LauncherProvider.AUTHORITY
                + "/" + LauncherProvider.TABLE_FAVOURITES
                + "?" + LauncherProvider.PARAMETER_NOTIFY + "=true");

        static final Uri CONTENT_URI_NO_NOTIFICATION = Uri.parse("content://" + LauncherProvider.AUTHORITY
                + "/" + LauncherProvider.TABLE_FAVOURITES
                + "?" + LauncherProvider.PARAMETER_NOTIFY + "=false");

        static Uri getContentUri(long id, boolean notify){
            return Uri.parse("content://" + LauncherProvider.AUTHORITY
                    + "/" + LauncherProvider.TABLE_FAVOURITES + "/" + id
                    + "?" + LauncherProvider.PARAMETER_NOTIFY + "=" + notify);
        }

        static final String CONTAINER = "container";

        static final int CONTAINER_DESKTOP = -100;
        static final int CONTAINER_HOTSEAT = -101;

        static final String SCREEN = "screen";
        static final String CELLX = "cellX";
        static final String CELLY = "cellY";

        static final String SPANX = "spanX";
        static final String SPANY = "spanY";

        static final int ITEM_TYPE_FOLDER = 2;
        static final int ITEM_TYPE_LIVE_FOLDER = 3;

        static final int ITEM_TYPE_APPWIDGET = 4;
        static final int ITEM_TYPE_WIDGET_CLOCK = 1000;
        static final int ITEM_TYPE_WIDGET_SEARCH = 1001;
        static final int ITEM_TYPE_WIDGET_PHOTO_FRAME = 1002;

        static final String APPWIDGET_ID = "appWidgetId";
        public static final String APPWIDGET_PROVIDER = "appWidgetProvider";

        /**
         * Indicates whether this favorite is an application-created shortcut or not.
         * If the value is 0, the favorite is not an application-created shortcut, if the
         * value is 1, it is an application-created shortcut.
         * <P>Type: INTEGER</P>
         */
        @Deprecated
        static final String IS_SHORTCUT = "isShortcut";

        /**
         * The URI associated with the favorite. It is used, for instance, by
         * live folders to find the content provider.
         * <P>Type: TEXT</P>
         */
        static final String URI = "uri";

        /**
         * The display mode if the item is a live folder.
         * <P>Type: INTEGER</P>
         *
         * @see android.provider.LiveFolders#DISPLAY_MODE_GRID
         * @see android.provider.LiveFolders#DISPLAY_MODE_LIST
         */
        static final String DISPLAY_MODE = "displayMode";

    }

}
