package com.android.launcher;

import android.view.ViewGroup;

/**
 * Created by Administrator on 2014/11/7.
 */
public class ShortcutAndWidgetContainer extends ViewGroup{


    private int mCellWidth;
    private int mCellHeight;
    private int mWidthGap;
    private int mHeightGap;
    private int mCountX;
    private int mCountY;



    public void setCellDimensions(int cellWidth, int cellHeight, int widthGap, int heightGap,
                                  int countX, int countY) {
        mCellWidth = cellWidth;
        mCellHeight = cellHeight;
        mWidthGap = widthGap;
        mHeightGap = heightGap;
        mCountX = countX;
        mCountY = countY;
    }
}
