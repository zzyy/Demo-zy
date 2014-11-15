package com.android.launcher.ui;

import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Administrator on 2014/11/7.
 */

interface Page {
    public int getPageChildCount();
    public View getChildOnPageAt(int i);
    public void removeAllViewsOnPage();
    public void removeViewOnPageAt(int i);
    public int indexOfChildOnPage(View v);
}

public class PagedView extends ViewGroup implements ViewGroup.OnHierarchyChangeListener {
    private static final String TAG = "PagedView";
    protected static final int INVALID_PAGE = -1;


}
