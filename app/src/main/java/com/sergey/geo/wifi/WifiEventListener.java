package com.sergey.geo.wifi;

import com.sergey.geo.model.GeofenceModel;

import java.util.List;

/**
 * Created by user on 10.08.2017.
 */

public interface WifiEventListener {
    void onEvent(List<GeofenceModel> model, int transitionType);
    void onMessage(GeofenceModel geofenceModel, String message);
    void onGeofenceAddedSuccess(List<String> ids);
    void onGeofenceAddedFailed(List<String> ids);
    void onGeofenceDeletedSuccess(List<String> ids);
    void onGeofenceDeletedFailed(List<String> ids);
}
