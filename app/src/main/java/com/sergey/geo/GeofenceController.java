package com.sergey.geo;

/**
 * Created by user on 30.07.2017.
 */

public interface GeofenceController {
    void addGeofence(GeofenceModel geofenceModel);
    void registerListener(GeofenceEventListener listener);
    void unregisterListener(GeofenceEventListener listener);
    void onDestroy();
}
