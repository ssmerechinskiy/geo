package com.sergey.geo;

import com.google.android.gms.location.Geofence;
import com.sergey.geo.googleapi.GeofenceEventListener;
import com.sergey.geo.model.GeofenceModel;

/**
 * Created by user on 30.07.2017.
 */

public class GeofenceUtil {

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

    public static String getEventTypeDetectorName(int eventTypeDetector) {
        switch (eventTypeDetector) {
            case GeofenceEventListener.EVENT_TYPE_DETECTOR_GEO_API :
                return "GEO API";
            case GeofenceEventListener.EVENT_TYPE_DETECTOR_WIFI :
                return "WIFI API";
            default:
                return "Unknown";
        }
    }

    public static GeofenceModel createGeofenceModelFromUIModel(MapPresenter.GeoFenceUIModel uiModel) {
        if(uiModel == null) return null;
        GeofenceModel geoModel = new GeofenceModel();
        geoModel.setId(uiModel.getMarker().getId());
        geoModel.setLatitude(uiModel.getMarker().getPosition().latitude);
        geoModel.setLongitude(uiModel.getMarker().getPosition().longitude);
        geoModel.setRadius((float) uiModel.radius);
        geoModel.setTransitionType(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT | Geofence.GEOFENCE_TRANSITION_DWELL);
        geoModel.setWifiNetwork(uiModel.networkName);
        geoModel.setName(uiModel.geofenceName);
        return geoModel;
    }

    public static String addQuotes(String s) {
        if(s.startsWith("\"") && s.endsWith("\"")) {
            return s;
        } else {
            if(!s.startsWith("\"")) {
                s = "\"" + s;
            }
            if(!s.endsWith("\"")) {
                s = s + "\"";
            }
            return s;
        }
    }
}
