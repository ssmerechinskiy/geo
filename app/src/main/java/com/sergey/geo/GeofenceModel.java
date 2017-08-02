package com.sergey.geo;

import com.google.android.gms.location.Geofence;

/**
 * Created by user on 30.07.2017.
 */

public class GeoFenceModel {

    private static final int EXPIRATION_TIME = 600000;

    private String id;
    private double latitude;
    private double longitude;
    private float radius;
    private int transitionType;
    private String wifiNetwork;

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

    public Geofence newGeofence() {
        return new Geofence.Builder()
                .setRequestId(id)
                .setTransitionTypes(transitionType)
                .setCircularRegion(latitude, longitude, radius)
                .setExpirationDuration(EXPIRATION_TIME)
                .build();
    }
}
