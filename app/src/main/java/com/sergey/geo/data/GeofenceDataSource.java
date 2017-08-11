package com.sergey.geo.data;

import com.sergey.geo.model.GeofenceModel;

import java.util.List;

/**
 * Created by sober on 07.08.2017.
 */

public interface GeofenceDataSource {
    GeofenceModel getGeofenceById(String id);
    List<GeofenceModel> getGeofencesByWifiNetworkName(String id);
    List<GeofenceModel> getGeofences();
    List<String> getGeofencesIds();
    void saveGeofences(List<GeofenceModel> models);
    void saveGeofence(GeofenceModel model);
    void removeGeofence(String id);
    void removeGeofences(List<String> ids);
    void removeAllGeofences();
}
