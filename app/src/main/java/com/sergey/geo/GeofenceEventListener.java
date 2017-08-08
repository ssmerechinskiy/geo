package com.sergey.geo;

import java.util.Collection;
import java.util.List;

/**
 * Created by user on 30.07.2017.
 */

public interface GeofenceEventListener {
    void onEvent(GeofenceModel geofenceModel, int transitionType);
    void onMessage(String message);
    void onGeofenceAddedSuccess(List<String> ids);
    void onGeofenceAddedFailed(List<String> ids);
    void onGeofenceDeletedSuccess(List<String> ids);
    void onGeofenceDeletedFailed(List<String> ids);
}
