package com.sergey.geo;

import java.util.List;

/**
 * Created by sober on 07.08.2017.
 */

public interface GeofenceDataSource {
    GeofenceModel getGeofenceById(String id);
    List<GeofenceModel> getGeofences();
    void saveGeofences(List<GeofenceModel> models);
    void removeGeofence(String id);
    void removeGeofences(List<String> ids);
    void removeAllGeofences();
}
