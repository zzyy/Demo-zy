package com.android.launcher;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import com.android.launcher.Launcher;
import com.android.launcher.LauncherAppState;

/**
 * Created by Administrator on 2014/11/7.
 */
public class CellLayout extends ViewGroup {
    static final String TAG = "CellLayout";

    private Launcher mLauncher;
    private int mCellWidth;
    private int mCellHeight;
    private int mFixedCellWidth;
    private int mFixedCellHeight;

    private int mCountX;
    private int mCountY;

    private int mOriginalWidthGap;
    private int mOriginalHeightGap;
    private int mWidthGap;
    private int mHeightGap;
    private int mMaxGap;

    // These values allow a fixed measurement to be set on the CellLayout.
    private int mFixedWidth = -1;
    private int mFixedHeight = -1;

    private final Rect mRect = new Rect();
    private final CellInfo mCellInfo = new CellInfo();

    // These are temporary variables to prevent having to allocate a new object just to
    // return an (x, y) value from helper functions. Do NOT use them to maintain other state.
    private final int[] mTmpXY = new int[2];
    private final int[] mTmpPoint = new int[2];
    int[] mTempLocation = new int[2];

    boolean[][] mOccupied;
    boolean[][] mTmpOccupied;
    private boolean mLastDownOnOccupiedCell = false;

    private float FOREGROUND_ALPHA_DAMPER = 0.65f;
    private int mForegroundAlpha = 0;
    private float mBackgroundAlpha;
    private float mBackgroundAlphaMultiplier = 1.0f;

    private Drawable mNormalBackground;
    private Drawable mActiveGlowBackground;
    private Drawable mOverScrollForegroundDrawable;
    private Drawable mOverScrollLeft;
    private Drawable mOverScrollRight;
    private Rect mBackgroundRect;
    private Rect mForegroundRect;
    private int mForegroundPadding;

    // If we're actively dragging something over this screen, mIsDragOverlapping is true
    private boolean mIsDragOverlapping = false;
    boolean mUseActiveGlowBackground = false;

    // Used as an index into the above 3 arrays; indicates which is the most current value.
    private int mDragOutlineCurrent = 0;
    private final Paint mDragOutlinePaint = new Paint();

    // These arrays are used to implement the drag visualization on x-large screens.
    // They are used as circular arrays, indexed by mDragOutlineCurrent.
    private Rect[] mDragOutlines = new Rect[4];
    private float[] mDragOutlineAlphas = new float[mDragOutlines.length];
    private InterruptibleInOutAnimator[] mDragOutlineAnims =
            new InterruptibleInOutAnimator[mDragOutlines.length];


    private OnTouchListener mInterceptTouchListener;


    private ShortcutAndWidgetContainer mShortcutsAndWidgets;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        LauncherAppState app = LauncherAppState.getInstance();
        DeviceProfile grid = app.getDynamicGrid().getDeviceProfile();

        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize =  MeasureSpec.getSize(heightMeasureSpec);
        int childWidthSize = widthSize - (getPaddingLeft() + getPaddingRight());
        int childHeightSize = heightSize - (getPaddingTop() + getPaddingBottom());

        if (mFixedCellWidth < 0 || mFixedCellHeight < 0) {
            int cw = grid.calculateCellWidth(childWidthSize, mCountX);
            int ch = grid.calculateCellHeight(childHeightSize, mCountY);
            if (cw != mCellWidth || ch != mCellHeight) {
                mCellWidth = cw;
                mCellHeight = ch;
                mShortcutsAndWidgets.setCellDimensions(mCellWidth, mCellHeight, mWidthGap,
                        mHeightGap, mCountX, mCountY);
            }
        }

        int newWidth = childWidthSize;
        int newHeight = childHeightSize;

        if (mFixedWidth > 0 && mFixedHeight > 0) {
            newWidth = mFixedWidth;
            newHeight = mFixedHeight;
        } else if (widthSpecMode == MeasureSpec.UNSPECIFIED || heightSpecMode == MeasureSpec.UNSPECIFIED) {
            throw new RuntimeException("CellLayout cannot have UNSPECIFIED dimensions");
        }

        int numWidthGaps = mCountX - 1;
        int numHeightGaps = mCountY - 1;

        if (mOriginalWidthGap < 0 || mOriginalHeightGap < 0) {
            int hSpace = childWidthSize;
            int vSpace = childHeightSize;
            int hFreeSpace = hSpace - (mCountX * mCellWidth);
            int vFreeSpace = vSpace - (mCountY * mCellHeight);
            mWidthGap = Math.min(mMaxGap, numWidthGaps > 0 ? (hFreeSpace / numWidthGaps) : 0);
            mHeightGap = Math.min(mMaxGap,numHeightGaps > 0 ? (vFreeSpace / numHeightGaps) : 0);
            mShortcutsAndWidgets.setCellDimensions(mCellWidth, mCellHeight, mWidthGap,
                    mHeightGap, mCountX, mCountY);
        } else {
            mWidthGap = mOriginalWidthGap;
            mHeightGap = mOriginalHeightGap;
        }

        int count = getChildCount();
        int maxWidth = 0;
        int maxHeight = 0;
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(newWidth,
                    MeasureSpec.EXACTLY);
            int childheightMeasureSpec = MeasureSpec.makeMeasureSpec(newHeight,
                    MeasureSpec.EXACTLY);
            child.measure(childWidthMeasureSpec, childheightMeasureSpec);
            maxWidth = Math.max(maxWidth, child.getMeasuredWidth());
            maxHeight = Math.max(maxHeight, child.getMeasuredHeight());
        }

        if (mFixedWidth > 0 && mFixedHeight > 0) {
            setMeasuredDimension(maxWidth, maxHeight);
        } else {
            setMeasuredDimension(widthSize, heightSize);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int offset = getMeasuredWidth() - getPaddingLeft() - getPaddingRight() -
                (mCountX * mCellWidth);
        int left = getPaddingLeft() + (int) Math.ceil(offset / 2f);
        int top = getPaddingTop();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            child.layout(left, top,
                    left + r - l,
                    top + b - t);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mBackgroundAlpha > 0.0f) {
            Drawable bg;

            if (mUseActiveGlowBackground) {
                // In the mini case, we draw the active_glow bg *over* the active background
                bg = mActiveGlowBackground;
            } else {
                bg = mNormalBackground;
            }

            bg.setAlpha((int) (mBackgroundAlpha * mBackgroundAlphaMultiplier * 255));
            bg.setBounds(mBackgroundRect);
            bg.draw(canvas);
        }

        final Paint paint = mDragOutlinePaint;
        for (int i = 0; i < mDragOutlines.length; i++) {
            final float alpha = mDragOutlineAlphas[i];
            if (alpha > 0) {
                final Rect r = mDragOutlines[i];
                mTempRect.set(r);
                Utilities.scaleRectAboutCenter(mTempRect, getChildrenScale());
                final Bitmap b = (Bitmap) mDragOutlineAnims[i].getTag();
                paint.setAlpha((int)(alpha + .5f));
                canvas.drawBitmap(b, null, mTempRect, paint);
            }
        }


    }

    // This class stores info for two purposes:
    // 1. When dragging items (mDragInfo in Workspace), we store the View, its cellX & cellY,
    //    its spanX, spanY, and the screen it is on
    // 2. When long clicking on an empty cell in a CellLayout, we save information about the
    //    cellX and cellY coordinates and which page was clicked. We then set this as a tag on
    //    the CellLayout that was long clicked
    static final class CellInfo {
        View cell;
        int cellX = -1;
        int cellY = -1;
        int spanX;
        int spanY;
        long screenId;
        long container;

        @Override
        public String toString() {
            return "Cell[view=" + (cell == null ? "null" : cell.getClass())
                    + ", x=" + cellX + ", y=" + cellY + "]";
        }
    }
}
