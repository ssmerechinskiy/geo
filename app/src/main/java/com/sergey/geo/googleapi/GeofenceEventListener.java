package com.sergey.geo.googleapi;

import com.sergey.geo.model.GeofenceModel;

import java.util.List;

/**
 * Created by user on 30.07.2017.
 */

public interface GeofenceEventListener {
    int EVENT_TYPE_DETECTOR_GEO_API = 1;
    int EVENT_TYPE_DETECTOR_WIFI = 2;
    void onEvent(GeofenceModel geofenceModel, int transitionType, int eventTypeDetector);
    void onMessage(GeofenceModel geofenceModel, String message);
    void onGeofenceAddedSuccess(List<String> ids);
    void onGeofenceAddedFailed(List<String> ids);
    void onGeofenceDeletedSuccess(List<String> ids);
    void onGeofenceDeletedFailed(List<String> ids);
}
