package com.android.launcher;

import android.provider.BaseColumns;

/**
 * Created by Administrator on 2014/10/27.
 */
public class LauncherSettings {

    public static interface ChangeLogColumns extends BaseColumns{
        static final String MODIFIED = "modified";
    }

    public static interface BaseLauncherColumns extends ChangeLogColumns{

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
    }
}
