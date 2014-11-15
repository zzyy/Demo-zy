package com.android.launcher;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import com.android.launcher.Launcher;
import com.android.launcher.LauncherAppState;
import com.example.zylauncher.R;

/**
 * Created by Administrator on 2014/11/7.
 */
public class Hotseat extends FrameLayout {
    private static final String TAG = "Hotseat";

    private Launcher mLauncher;

    private boolean mTransposeLayoutWithOrientation;
    private boolean mIsLandscape;
    private int mAllAppsButtonRank;

    public Hotseat(Context context) {
        super(context);
    }

    public Hotseat(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public Hotseat(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        Resources r = context.getResources();
        mTransposeLayoutWithOrientation = r.getBoolean(R.bool.hotseat_transpose_layout_with_orientation);
        mIsLandscape = r.getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    public void setup(Launcher launcher){
        mLauncher = launcher;
        //fixme setClickListener
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        LauncherAppState app = LauncherAppState.getInstance();
        DeviceProfile grid = app.getDynamicGrid().getDeviceProfile();

        mAllAppsButtonRank = grid.hotseatAllAppsRank;

    }
}
