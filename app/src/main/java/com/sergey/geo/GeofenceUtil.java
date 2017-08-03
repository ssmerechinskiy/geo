package com.sergey.geo;

import com.google.android.gms.location.Geofence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by user on 30.07.2017.
 */

public class GeoFenceUtil {
    public static List<Geofence> convertToList(Map<String, GeoFenceModel> map) {
        if(map == null || map.values().size() == 0) return null;
        List<Geofence> list = new ArrayList<>();
        for (GeoFenceModel m : map.values()) {
            list.add(m.newGeofence());
        }
        return list;
    }

    public static String getTransionName(int transitionType) {
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER :
                return "ENTER";
            case Geofence.GEOFENCE_TRANSITION_EXIT :
                return "EXIT";
            case Geofence.GEOFENCE_TRANSITION_DWELL :
                return "INSIDE";
            default:
                return "Unknown";
        }
    }
}
