package com.sergey.geo.googleapi;

import android.content.Intent;

import com.sergey.geo.model.GeofenceModel;

/**
 * Created by user on 30.07.2017.
 */

public interface GeofenceController {
    void addGeoFence(GeofenceModel geofenceModel);
    void removeGeoFence(GeofenceModel geofenceModel);
    void registerListener(GeofenceEventListener listener);
    void unregisterListener(GeofenceEventListener listener);
    void onGeofenceEvent(Intent intent);
    void onDestroy();
}
