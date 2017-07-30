package com.sergey.geo;

/**
 * Created by user on 30.07.2017.
 */

public interface GeofenceEventListener {
    void onEvent(GeofenceModel geofenceModel);
    void onMessage(String message);
}
