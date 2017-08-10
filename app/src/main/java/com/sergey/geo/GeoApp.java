package com.sergey.geo;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

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
