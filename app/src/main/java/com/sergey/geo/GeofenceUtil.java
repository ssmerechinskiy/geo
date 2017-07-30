package com.sergey.geo;

import com.google.android.gms.location.Geofence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by user on 30.07.2017.
 */

public class GeofenceUtil {
    public static List<Geofence> convertToList(Map<String, GeofenceModel> map) {
        if(map == null || map.values().size() == 0) return null;
        List<Geofence> list = new ArrayList<>();
        for (GeofenceModel m : map.values()) {
            list.add(m.newGeofence());
        }
        return list;
    }
}
