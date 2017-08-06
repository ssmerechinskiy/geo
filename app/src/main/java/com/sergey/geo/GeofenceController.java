package com.sergey.geo;

import android.content.Intent;

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
    Network getCurrentNetwork();
}
