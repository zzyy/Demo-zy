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

        /**
         * The gesture is an application
         */
        static final int ITEM_TYPE_APPLICATION = 0;

        /**
         * The gesture is an application created shortcut
         */
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



        static final String CONTAINER = "container";

        static final int CONTAINER_DESKTOP = -100;
        static final int CONTAINER_HOTSEAT = -101;

        static final String SCREEN = "screen";
        static final String CELLX = "cellX";
        static final String CELLY = "cellY";

    }

}
