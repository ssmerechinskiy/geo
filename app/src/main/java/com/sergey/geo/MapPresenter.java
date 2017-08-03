package com.sergey.geo;

import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.google.android.gms.maps.model.BitmapDescriptorFactory.fromResource;
import static com.google.android.gms.plus.PlusOneDummyView.TAG;

/**
 * Created by sober on 01.08.2017.
 */

public class MapPresenter {

    public final static String GEO_FENCE_MARKER_TITLE = "Tap on marker for geo fence action";
    public final static float CURRENT_LOCATION_DEFAULT_ZOOM = 18.0f;
    public final static double DEFAULT_GEOFENCE_RADIUS = 20.d;

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;
    private static final int REQUEST_CHECK_SETTINGS = 0x1;
    private volatile boolean mRequestingLocationUpdates;
    private FusedLocationProviderClient mFusedLocationClient;
    private SettingsClient mSettingsClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private LocationSettingsRequest mLocationSettingsRequest;

    private MapsActivity activity;
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
        geoController = GeoApp.getInstance().getGeoFenceController();
        geoController.registerListener(geoFenceEventListener);
        currentLocationBitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.ic_smiley);
        initLocationParams();
    }

    private void initLocationParams() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(activity);
        mSettingsClient = LocationServices.getSettingsClient(activity);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                currentLocation = locationResult.getLastLocation();
                if(!mapReady) {
                    activity.showMessage("Map not ready");
                    return;
                }
                displayCurrentLocation();
            }
        };

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();

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

    public void onStop() {
    }

    public void onDestroy() {
        mainThreadHandler.removeCallbacksAndMessages(null);
        geoController.unregisterListener(geoFenceEventListener);
        activity = null;
    }

    private void updateCurrentLocation() {
        startLocationUpdates();
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
                GeoFenceModel geoModel = createGeofenceModelFromUIModel(uiModel);
                geoController.addGeoFence(geoModel);
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

    public static GeoFenceModel createGeofenceModelFromUIModel(GeoFenceUIModel uiModel) {
        if(uiModel == null) return null;
        GeoFenceModel geoModel = new GeoFenceModel();
        geoModel.setId(uiModel.marker.getId());
        geoModel.setLatitude(uiModel.marker.getPosition().latitude);
        geoModel.setLongitude(uiModel.marker.getPosition().longitude);
        geoModel.setRadius((float) uiModel.geoCircle.getRadius());
        geoModel.setTransitionType(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT | Geofence.GEOFENCE_TRANSITION_DWELL);

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

    private void startLocationUpdates() {
        mRequestingLocationUpdates = true;
        // Begin by checking if the device has the necessary location settings.
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(activity, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i(TAG, "All location settings are satisfied.");
                        //noinspection MissingPermission
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                    }
                })
                .addOnFailureListener(activity, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(activity, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "PendingIntent unable to execute request.");
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e(TAG, errorMessage);
                                activity.showMessage(errorMessage);
                                mRequestingLocationUpdates = false;
                        }
                    }
                });
    }

    private void stopLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            Log.d(TAG, "stopLocationUpdates: updates never requested, no-op.");
            return;
        }
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
                .addOnCompleteListener(activity, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        mRequestingLocationUpdates = false;
                    }
                });
    }
}
