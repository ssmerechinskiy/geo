package com.sergey.geo.model;

import com.google.android.gms.location.Geofence;

/**
 * Created by user on 30.07.2017.
 */

public class GeofenceModel {

    public final static double USE_WIFI_MIN_RADIUS = 20.0d;
    public final static double USE_WITHOUT_WIFI_MIN_RADIUS = 100.0d;

    private static final int EXPIRATION_TIME = 600000;

    private String id;
    private double latitude;
    private double longitude;
    private float radius;
    private int transitionType;
    private String wifiNetwork;
    private String name;

    private boolean addedToGeoService;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public int getTransitionType() {
        return transitionType;
    }

    public void setTransitionType(int transitionType) {
        this.transitionType = transitionType;
    }

    public String getWifiNetwork() {
        return wifiNetwork;
    }

    public void setWifiNetwork(String wifiNetwork) {
        this.wifiNetwork = wifiNetwork;
    }

    public boolean isAddedToGeoService() {
        return addedToGeoService;
    }

    public void setAddedToGeoService(boolean addedToGeoService) {
        this.addedToGeoService = addedToGeoService;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Geofence newGeofence() {
        return new Geofence.Builder()
                .setRequestId(id)
                .setTransitionTypes(transitionType)
                .setCircularRegion(latitude, longitude, radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setLoiteringDelay(60)
                .build();
    }

    public static boolean validateRadius(double radius, boolean useWifi) {
        if(useWifi) {
            if(radius >= USE_WIFI_MIN_RADIUS) return true;
            else return false;
        } else {
            if(radius >= USE_WITHOUT_WIFI_MIN_RADIUS) return true;
            else return false;
        }
    }
}
