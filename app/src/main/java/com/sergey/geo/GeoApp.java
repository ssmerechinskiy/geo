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
    private static Toast sToast;
    private GeoFenceController geoFenceController = null;
    private static Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        geoFenceController = new GeoFenceControllerImpl(sInstance);
    }

    public static GeoApp getInstance() {
        return sInstance;
    }

    public GeoFenceController getGeoFenceController() {
        return geoFenceController;
    }

    @Override
    public void onTerminate() {
        geoFenceController.onDestroy();
        super.onTerminate();
    }

//    public static void showMessage(final String message) {
//        mainThreadHandler.post(new Runnable() {
//            @Override
//            public void run() {
//                if(sToast == null) {
//                    sToast = Toast.makeText(GeoApp.getInstance(), message, Toast.LENGTH_SHORT);
//                }
//                sToast.setText(message);
//                sToast.show();
//            }
//        });
//    }
}
