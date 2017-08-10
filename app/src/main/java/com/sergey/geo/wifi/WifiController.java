package com.sergey.geo.wifi;

import android.content.Intent;

import com.sergey.geo.model.GeofenceModel;

/**
 * Created by user on 30.07.2017.
 */

public interface WifiController {

    int WIFI_TRANSITION_ENTER = 8;
    int WIFI_TRANSITION_EXIT = 16;

    void addGeoFence(GeofenceModel geofenceModel);
    void removeGeoFence(GeofenceModel geofenceModel);
    void registerListener(WifiEventListener listener);
    void unregisterListener(WifiEventListener listener);
    void onGeofenceEvent(Intent intent);
    void onDestroy();
}
