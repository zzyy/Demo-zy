package com.android.launcher;

import android.content.ContentValues;

import java.util.ArrayList;

/**
 * Created by Administrator on 2014/11/3.
 */
public class FolderInfo extends ItemInfo {
    /**
     * Whether this folder has been opened
     */
    boolean opened;

    /**
     * The apps and shortcuts
     */
    ArrayList<ShortcutInfo> contents = new ArrayList<ShortcutInfo>();

    ArrayList<FolderListener> listeners = new ArrayList<FolderListener>();

    FolderInfo() {
        itemType = LauncherSettings.Favorites.ITEM_TYPE_FOLDER;
    }

    /**
     * Add an app or shortcut
     *
     * @param item
     */
    public void add(ShortcutInfo item) {
        contents.add(item);
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onAdd(item);
        }
        itemsChanged();
    }

    /**
     * Remove an app or shortcut. Does not change the DB.
     *
     * @param item
     */
    public void remove(ShortcutInfo item) {
        contents.remove(item);
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onRemove(item);
        }
        itemsChanged();
    }

    public void setTitle(CharSequence title) {
        this.title = title;
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onTitleChanged(title);
        }
    }

    @Override
    void onAddToDatabase(ContentValues values) {
        super.onAddToDatabase(values);
        values.put(LauncherSettings.Favorites.TITLE, title.toString());
    }

    void addListener(FolderListener listener) {
        listeners.add(listener);
    }

    void removeListener(FolderListener listener) {
        if (listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }

    void itemsChanged() {
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).onItemsChanged();
        }
    }

    @Override
    void unbind() {
        super.unbind();
        listeners.clear();
    }

    interface FolderListener {
        public void onAdd(ShortcutInfo item);
        public void onRemove(ShortcutInfo item);
        public void onTitleChanged(CharSequence title);
        public void onItemsChanged();
    }

    @Override
    public String toString() {
        return "FolderInfo(id=" + this.id + " type=" + this.itemType
                + " container=" + this.container + " screen=" + screenId
                + " cellX=" + cellX + " cellY=" + cellY + " spanX=" + spanX
                + " spanY=" + spanY + " dropPos=" + dropPos + ")";
    }
}
