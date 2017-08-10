package com.sergey.geo;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by sober on 07.08.2017.
 */

public class GeofenceDataSourceImpl implements GeofenceDataSource {
    private final static String TAG = GeofenceDataSourceImpl.class.getSimpleName();

    private static GeofenceDataSource instance = new GeofenceDataSourceImpl();
    private static Map<String, GeofenceModel> geofenceCache = new ConcurrentHashMap<>();

    public static GeofenceDataSource getInstance() {
        return instance;
    }

    @Override
    public GeofenceModel getGeofenceById(String id) {
        return geofenceCache.get(id);
    }

    @Override
    public GeofenceModel getGeofenceByWifiNetworkName(String name) {
        Log.d(TAG, "getGeofenceByWifiNetworkName finding name:" + name);
        for (String gid : geofenceCache.keySet()) {
            GeofenceModel m = geofenceCache.get(gid);
            Log.d(TAG, "getGeofenceByWifiNetworkName:" + m.getWifiNetwork());
            if(m.getWifiNetwork().equals(name)) {
                Log.d(TAG, "getGeofenceByWifiNetworkName SUCCESS" + m.getWifiNetwork());
                return m;
            }
        }
        return null;
    }

    @Override
    public List<GeofenceModel> getGeofences() {
        return new ArrayList<>(geofenceCache.values());
    }

    @Override
    public List<String> getGeofencesIds() {
        return new ArrayList<>(geofenceCache.keySet());
    }

    @Override
    public void saveGeofences(List<GeofenceModel> models) {
        if(models == null) return;
        for(GeofenceModel m : models) {
            geofenceCache.put(m.getId(), m);
        }
    }

    @Override
    public void removeGeofence(String id) {
        geofenceCache.remove(id);
    }

    @Override
    public void removeGeofences(List<String> ids) {
        if(ids == null) return;
        for (String id : ids) {
            geofenceCache.remove(id);
        }
    }

    @Override
    public void removeAllGeofences() {

    }
}
