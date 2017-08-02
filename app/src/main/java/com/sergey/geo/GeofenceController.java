package com.sergey.geo;

import android.content.Intent;

/**
 * Created by user on 30.07.2017.
 */

public interface GeoFenceController {
    void addGeoFence(GeoFenceModel geoFenceModel);
    void registerListener(GeoFenceEventListener listener);
    void unregisterListener(GeoFenceEventListener listener);
    void onGeofenceEvent(Intent intent);
    void onDestroy();
    Network getCurrentNetwork();
}
