package com.sergey.geo;

import android.app.Application;

import com.sergey.geo.googleapi.GeofenceControllerImpl;
import com.sergey.geo.wifi.WifiControllerImpl;

/**
 * Created by user on 30.07.2017.
 */

public class GeoApp extends Application {

    private static GeoApp sInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        GeofenceControllerImpl.getInstance().init(sInstance);
        WifiControllerImpl.getInstance().init(sInstance);
        BusinessLogicController.getInstance().init(sInstance);
    }

    public static GeoApp getInstance() {
        return sInstance;
    }


    @Override
    public void onTerminate() {
        super.onTerminate();
    }

}
