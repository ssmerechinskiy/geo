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
    private GeofenceController geofenceController = null;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        geofenceController = new GeofenceControllerImpl(sInstance);
    }

    public static GeoApp getInstance() {
        return sInstance;
    }

    public GeofenceController getGeofenceController() {
        return geofenceController;
    }

    @Override
    public void onTerminate() {
        geofenceController.onDestroy();
        super.onTerminate();
    }

}
