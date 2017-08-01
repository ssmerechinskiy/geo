package com.sergey.geo;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by sober on 01.08.2017.
 */

public class MapPresenter {

    public final static String CURRENT_LOCATION_TITLE = "Your location";
    public final static float CURRENT_LOCATION_DEFAULT_ZOOM = 18.0f;
    public final static double DEFAULT_GEOFENCE_RADIUS = 20.d;

    private MapsActivity activity;
    private LocationManager locationManager;
    private String mprovider;

    private Location currentLocation;
//    private Marker currentLocationMarker;
//    private Circle currentLocationGeofenceCircle;
    private boolean mapReady;
    private GoogleMap mGoogleMap;
    private Map<String, GeoFenceUIModel> uiGeoModels = new HashMap<>();


    public MapPresenter(MapsActivity a) {
        activity = a;
        locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
    }

    public void onStart() {
        updateCurrentLocation();
    }

    public void onMapReady(GoogleMap googleMap) {
        mapReady = true;
        mGoogleMap = googleMap;

        if (ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mGoogleMap.setMyLocationEnabled(true);

        if (currentLocation != null) {
//            currentLocationMarker = activity.displayMarker(currentLocation, CURRENT_LOCATION_TITLE, true);
            activity.animateCameraToLocation(currentLocation, CURRENT_LOCATION_DEFAULT_ZOOM);
        }
    }

    public void onStop() {

    }

    public void onDestroy() {
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.removeUpdates(locationListener);
        }
        activity = null;
    }

    private void updateCurrentLocation() {
        Criteria criteria = new Criteria();
        mprovider = locationManager.getBestProvider(criteria, false);
        if (mprovider != null && !mprovider.equals("")) {
            if (ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            currentLocation = locationManager.getLastKnownLocation(mprovider);
            locationManager.requestLocationUpdates(mprovider, 15000, 1, locationListener);
        }
    }

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            activity.showMessage("Location has changed");

            currentLocation = location;

            if(!mapReady) {
                activity.showMessage("Map not ready");
                return;
            }

//            if(currentLocationMarker != null) {
//                activity.removeMarker(currentLocationMarker);
//            }
//            currentLocationMarker = activity.displayMarker(currentLocation, CURRENT_LOCATION_TITLE, true);
            activity.animateCameraToLocation(currentLocation, CURRENT_LOCATION_DEFAULT_ZOOM);
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };

    public boolean onMarkerClick(final Marker marker) {
        final GeoFenceUIModel model = uiGeoModels.get(marker.getId());
        activity.showPopupMenuForMarker(model, new MapsActivity.PopupMenuListener() {
            @Override
            public void onCreateGeofence(int radius) {
//                GeoFenceUIModel model = uiGeoModels.get(marker.getId());
                activity.animateCameraToLatLng(model.marker.getPosition(), CURRENT_LOCATION_DEFAULT_ZOOM);
                Circle circle = activity.displayCircle(model.marker.getPosition(), radius);
                model.geoCircle = circle;
            }

            @Override
            public void onDeleteGeofence() {
//                GeoFenceUIModel model = uiGeoModels.get(marker.getId());
//                activity.animateCameraToLatLng(model.marker.getPosition(), CURRENT_LOCATION_DEFAULT_ZOOM);
                activity.removeMarker(model.marker);
                if(model.geoCircle != null) activity.removeCircle(model.geoCircle);
                uiGeoModels.remove(marker.getId());
            }
        });
        return true;
    }

    public void onMapLongClick(LatLng latLng) {
        Marker marker = activity.displayMarker(latLng, "Tap on marker for geo fence action", true);
        GeoFenceUIModel uiModel = new GeoFenceUIModel();
        uiModel.marker = marker;
        uiGeoModels.put(marker.getId(), uiModel);
    }

    public static class GeoFenceUIModel {
        public Marker marker;
        public Circle geoCircle;
    }
}
