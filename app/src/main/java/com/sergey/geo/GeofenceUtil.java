package com.sergey.geo;

import com.google.android.gms.location.Geofence;

import java.util.ArrayList;
import java.util.Collection;
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

    public static GeofenceModel createGeofenceModelFromUIModel(MapPresenter.GeoFenceUIModel uiModel) {
        if(uiModel == null) return null;
        GeofenceModel geoModel = new GeofenceModel();
        geoModel.setId(uiModel.marker.getId());
        geoModel.setLatitude(uiModel.marker.getPosition().latitude);
        geoModel.setLongitude(uiModel.marker.getPosition().longitude);
        geoModel.setRadius((float) uiModel.radius);
        geoModel.setTransitionType(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT | Geofence.GEOFENCE_TRANSITION_DWELL);

        Network network = GeoApp.getInstance().getGeofenceController().getCurrentNetwork();
        if(network != null && network == Network.WIFI) {
            geoModel.setWifiNetwork(network.getName());
        }
        return geoModel;
    }
}
