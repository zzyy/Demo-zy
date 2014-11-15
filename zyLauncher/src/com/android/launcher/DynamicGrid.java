package com.android.launcher;

/**
 * Created by Administrator on 2014/11/5.
 */
public class DynamicGrid {


    private DeviceProfile mProfile;

    public DeviceProfile getDeviceProfile() {
        return mProfile;
    }
}

class DeviceProfileQuery{

}

class DeviceProfile {

    float numRows;
    float numColumns;

    int hotseatAllAppsRank;

    int calculateCellWidth(int width, int countX) {
        return width / countX;
    }
    int calculateCellHeight(int height, int countY) {
        return height / countY;
    }
}