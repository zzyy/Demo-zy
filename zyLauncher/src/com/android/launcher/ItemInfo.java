package com.android.launcher;

import android.content.Intent;

/**
 * Created by Administrator on 2014/10/27.
 *
 * represent a item in the launcher
 */
public class ItemInfo {
    static final int NO_ID = -1;

    long container = NO_ID;

    /** the item title */
    CharSequence title;
    /**
     * the type: app, shortcut, folder or appwidget;
     */
    int itemType;
}
