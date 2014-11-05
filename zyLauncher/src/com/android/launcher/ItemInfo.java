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

    long id = NO_ID;
    /**
     * the type: app, shortcut, folder or appwidget;
     */
    int itemType;

    /**
     * Iindicates the screen in which the shortcut appears.
     */
    long screenId = -1;

    /**
     * Indicates the X position of the associated cell.
     */
    int cellX = -1;

    /**
     * Indicates the Y position of the associated cell.
     */
    int cellY = -1;

    /**
     * Indicates the X cell span.
     */
    int spanX = 1;

    /**
     * Indicates the Y cell span.
     */
    int spanY = 1;

    /**
     * Indicates the minimum X cell span.
     */
    int minSpanX = 1;

    /**
     * Indicates the minimum Y cell span.
     */
    int minSpanY = 1;

    /**
     * Indicates that this item needs to be updated in the db
     */
    boolean requiresDbUpdate = false;

    /**
     * Title of the item
     */
    CharSequence title;

    /**
     * The position of the item in a drag-and-drop operation.
     */
    int[] dropPos = null;


    /**
     * M: the position of the application icon in all app list page, add for
     * op09.
     */
    int pos;

    ItemInfo() {
    }

    ItemInfo(ItemInfo info) {
        id = info.id;
        cellX = info.cellX;
        cellY = info.cellY;
        spanX = info.spanX;
        spanY = info.spanY;
        screenId = info.screenId;
        itemType = info.itemType;
        container = info.container;
        // tempdebug:
        LancherModel.checkItemInfo(this);
    }
}
