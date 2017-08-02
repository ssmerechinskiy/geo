package com.sergey.geo;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.HashMap;
import java.util.Map;

import static com.google.android.gms.maps.model.BitmapDescriptorFactory.fromResource;

/**
 * Created by sober on 01.08.2017.
 */

public class MapPresenter {

    public final static String GEO_FENCE_MARKER_TITLE = "Tap on marker for geo fence action";
    public final static float CURRENT_LOCATION_DEFAULT_ZOOM = 18.0f;
    public final static double DEFAULT_GEOFENCE_RADIUS = 20.d;

    private MapsActivity activity;
    private LocationManager locationManager;
    private String mprovider;

    private Location currentLocation;
    private boolean mapReady;
    private GoogleMap mGoogleMap;
    private Map<String, GeoFenceUIModel> uiGeoModels = new HashMap<>();
    private GeoFenceController geoController;
    private Handler mainThreadHandler = new Handler(Looper.getMainLooper());


    private BitmapDescriptor currentLocationBitmapDescriptor;
    private Marker currentLocationMarker;

    private GeoFenceEventListener geoFenceEventListener = new GeoFenceEventListener() {
        @Override
        public void onEvent(final GeoFenceModel geoFenceModel) {
            mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    activity.showMessage("OnEvent:" + GeoFenceUtil.getTransionName(geoFenceModel.getTransitionType()) + " Geo fence:" + geoFenceModel.getId() + " wifi:" + geoFenceModel.getWifiNetwork());
                }
            });
        }

        @Override
        public void onMessage(final String message) {
            mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    activity.showMessage(message);
                }
            });
        }
    };


    public MapPresenter(MapsActivity a) {
        activity = a;
        locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        geoController = GeoApp.getInstance().getGeoFenceController();
        geoController.registerListener(geoFenceEventListener);
        currentLocationBitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.ic_smiley);

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

        displayCurrentLocation();
    }

    private void displayCurrentLocation() {
        if (currentLocation != null) {
            if(currentLocationMarker != null) activity.removeMarker(currentLocationMarker);
            LatLng point = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            currentLocationMarker = activity.displayMarker(point, "You", false, currentLocationBitmapDescriptor);
            activity.animateCameraToLocation(currentLocation, CURRENT_LOCATION_DEFAULT_ZOOM);
        }
    }

    private void navigateToCurrentLocation() {
        activity.animateCameraToLocation(currentLocation, CURRENT_LOCATION_DEFAULT_ZOOM);
    }

    public void onStop() {
    }

    public void onDestroy() {
        mainThreadHandler.removeCallbacksAndMessages(null);
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            } else {
                locationManager.removeUpdates(locationListener);
            }
        }
        geoController.unregisterListener(geoFenceEventListener);
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
            locationManager.requestLocationUpdates(mprovider, 2000, 10, locationListener);
        }
    }

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
//            activity.showMessage("Location has changed");

            currentLocation = location;

            if(!mapReady) {
                activity.showMessage("Map not ready");
                return;
            }
            displayCurrentLocation();
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
        final GeoFenceUIModel uiModel = uiGeoModels.get(marker.getId());
        activity.showPopupMenuForMarker(uiModel, new MapsActivity.PopupMenuListener() {
            @Override
            public void onCreateGeofence(int radius) {
                activity.animateCameraToLatLng(uiModel.marker.getPosition(), CURRENT_LOCATION_DEFAULT_ZOOM);
                Circle circle = activity.displayCircle(uiModel.marker.getPosition(), radius);
                uiModel.geoCircle = circle;
                GeoFenceModel geoModelEnter = createGeofenceModelFromUIModel(uiModel, Geofence.GEOFENCE_TRANSITION_ENTER);
                GeoFenceModel geoModelExit = createGeofenceModelFromUIModel(uiModel, Geofence.GEOFENCE_TRANSITION_EXIT);
                geoController.addGeoFence(geoModelEnter);
                geoController.addGeoFence(geoModelExit);
            }

            @Override
            public void onDeleteGeofence() {
                activity.removeMarker(uiModel.marker);
                if(uiModel.geoCircle != null) activity.removeCircle(uiModel.geoCircle);
                uiGeoModels.remove(marker.getId());
            }
        });
        return true;
    }

    public void onMapLongClick(LatLng latLng) {
        Marker marker = activity.displayMarker(latLng, GEO_FENCE_MARKER_TITLE, true);
        GeoFenceUIModel uiModel = new GeoFenceUIModel();
        uiModel.marker = marker;
        uiGeoModels.put(marker.getId(), uiModel);
    }

    public void onCurrentLocationClick() {
        navigateToCurrentLocation();
    }

    public static GeoFenceModel createGeofenceModelFromUIModel(GeoFenceUIModel uiModel, int transitionType) {
        if(uiModel == null) return null;
        GeoFenceModel geoModel = new GeoFenceModel();
        String idPart = "unknown transition";
        if(transitionType == Geofence.GEOFENCE_TRANSITION_ENTER) {
            idPart = "_enter";
        } else if (transitionType == Geofence.GEOFENCE_TRANSITION_EXIT) {
            idPart = "_exit";
        }
        String id = uiModel.marker.getId() + idPart;
        geoModel.setId(id);
        geoModel.setLatitude(uiModel.marker.getPosition().latitude);
        geoModel.setLongitude(uiModel.marker.getPosition().longitude);
        geoModel.setRadius((float) uiModel.geoCircle.getRadius());
        geoModel.setTransitionType(transitionType);

        Network network = GeoApp.getInstance().getGeoFenceController().getCurrentNetwork();
        if(network != null && network == Network.WIFI) {
            geoModel.setWifiNetwork(network.getName());
        }
        return geoModel;
    }

    public static class GeoFenceUIModel {
        public Marker marker;
        public Circle geoCircle;
    }
}
