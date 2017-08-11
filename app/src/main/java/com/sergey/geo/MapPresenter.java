package com.sergey.geo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.sergey.geo.data.GeofenceDataSource;
import com.sergey.geo.data.GeofenceDataSourceImpl;
import com.sergey.geo.googleapi.GeofenceEventListener;
import com.sergey.geo.model.GeofenceModel;
import com.sergey.geo.model.Network;
import com.sergey.geo.wifi.WifiControllerImpl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.android.gms.plus.PlusOneDummyView.TAG;

/**
 * Created by sober on 01.08.2017.
 */

public class MapPresenter {

    public static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    public static final int REQUEST_CHECK_SETTINGS = 0x1;

    public final static String GEO_FENCE_MARKER_TITLE = "Tap on marker for geo fence action";
    public final static float CURRENT_LOCATION_DEFAULT_ZOOM = 18.0f;

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 6000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    private volatile boolean mRequestingLocationUpdates;
    private FusedLocationProviderClient mFusedLocationClient;
    private SettingsClient mSettingsClient;
    private LocationRequest mLocationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;

    private MapsActivity activity;
    private Location currentLocation;
    private boolean mapReady;
    private GoogleMap mGoogleMap;
    private Map<String, GeoFenceUIModel> uiGeoModels = new ConcurrentHashMap<>();

    private BusinessLogicController businessLogicController;
    private GeofenceDataSource geofenceDataSource;
    private Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    private boolean isCameraAutoMovingMode = true;


    private BitmapDescriptor currentLocationBitmapDescriptor;
    private Marker currentLocationMarker;

    private volatile Network currentNetwork;

    private float currentZoom = CURRENT_LOCATION_DEFAULT_ZOOM;

    private final String wifiStatusTitle;

    public MapPresenter(MapsActivity a) {
        activity = a;
        businessLogicController = BusinessLogicController.getInstance();
        businessLogicController.registerListener(geofenceEventListener);
        geofenceDataSource = GeofenceDataSourceImpl.getInstance();
        currentLocationBitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.ic_smiley);
        prepareLocationServices();
        activity.registerReceiver(networkStateReceiver, WifiControllerImpl.intentFilter);
        wifiStatusTitle = activity.getString(R.string.wifi_status_title);
    }

    public void onStart() {
//        mainThreadHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                activity.showTipsDialog();
//            }
//        }, 5000);
    }

    public void onResume() {
        if(!mRequestingLocationUpdates) {
            if(checkPermissions()) {
                startLocationUpdates();
                activity.prepareMap();
            } else {
                requestPermissions();
            }
        }
    }

    public void onPause() {
        stopLocationUpdates();
    }

    public void onStop() {
        activity.hidePopupMenuForMarker();
    }

    public void onDestroy() {
        mainThreadHandler.removeCallbacksAndMessages(null);
        activity.unregisterReceiver(networkStateReceiver);
        businessLogicController.unregisterListener(geofenceEventListener);
        businessLogicController.onDestroy();
        activity = null;
    }

    public void onMapReady(GoogleMap googleMap) {
        mapReady = true;
        mGoogleMap = googleMap;
        try {
            mGoogleMap.setMyLocationEnabled(true);
            if(currentLocation != null) activity.animateCameraToLocation(currentLocation, CURRENT_LOCATION_DEFAULT_ZOOM);
            currentNetwork = NetworkUtil.updateNetworkInfo();
            showNetworkStatus();

        } catch (SecurityException e) {
            activity.showMessage("Permissions not granted");
            requestPermissions();
        }
    }

    private void showNetworkStatus() {
        if(currentNetwork != null && currentNetwork.getName() != null) {
            activity.updateWifiStatus(currentNetwork.getName().toString());
        } else {
            activity.updateWifiStatus("Not connected");
        }
    }

    private void prepareLocationServices() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(activity);
        mSettingsClient = LocationServices.getSettingsClient(activity);

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();

    }

    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            currentLocation = locationResult.getLastLocation();
            if (isCameraAutoMovingMode && currentLocation != null) {
                activity.animateCameraToLocation(currentLocation, currentZoom);
                mainThreadHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        currentZoom = mGoogleMap.getCameraPosition().zoom;
                    }
                }, 3000);
            }
            displayCurrentLocation();
        }
    };

    private void displayCurrentLocation() {
//        if (currentLocation != null) {
//            if(currentLocationMarker != null) activity.removeMarker(currentLocationMarker);
//            LatLng point = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
//            currentLocationMarker = activity.displayMarker(point, "You", false, currentLocationBitmapDescriptor);
//        }
    }

    private GeofenceEventListener geofenceEventListener = new GeofenceEventListener() {
        @Override
        public void onEvent(final GeofenceModel geofenceModel, final int transitionType) {
            mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    String m1 = "YOU ARE " + GeofenceUtil.getTransionName(transitionType);
                    String m2 = " GEOFENCE:" + geofenceModel.getName();
                    activity.showSnackbar(m1, m2, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                        }
                    });
                }
            });
        }

        @Override
        public void onMessage(final GeofenceModel geofenceModel, final String message) {
            mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    activity.showMessage(message);
                }
            });
        }

        @Override
        public void onGeofenceAddedSuccess(final List<String> ids) {
            mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    String id = ids.get(0);
                    if(id == null) return;
                    activity.hideProgress();
                    final GeoFenceUIModel uiModel = uiGeoModels.get(id);
                    Circle circle = activity.displayCircle(uiModel.marker.getPosition(), uiModel.radius);
                    uiModel.geoCircle = circle;
                    activity.animateCameraToLatLng(uiModel.marker.getPosition(), CURRENT_LOCATION_DEFAULT_ZOOM);
//                    GeofenceControllerImpl.getInstance().testClearAllGeofences();
                }
            });
        }

        @Override
        public void onGeofenceAddedFailed(final List<String> ids) {
            mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    String id = ids.get(0);
                    if(id == null) return;
                    uiGeoModels.remove(id);
                    activity.hideProgress();
                }
            });
        }

        @Override
        public void onGeofenceDeletedSuccess(final List<String> ids) {
            mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    activity.hideProgress();
                    String id = ids.get(0);
                    if(id == null) return;
                    GeoFenceUIModel uiModel = uiGeoModels.get(id);
                    activity.removeMarker(uiModel.marker);
                    if(uiModel.geoCircle != null) activity.removeCircle(uiModel.geoCircle);
                    uiGeoModels.remove(id);
                }
            });
        }

        @Override
        public void onGeofenceDeletedFailed(List<String> ids) {
            mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    activity.hideProgress();
                    activity.showMessage("geofence delete error");
                }
            });
        }
    };

    public void onMyLocationButtonClick() {
        isCameraAutoMovingMode = true;
    }

    public void onUserDragCamera() {
        isCameraAutoMovingMode = false;
    }

    public boolean onMarkerClick(final Marker marker) {
        final GeoFenceUIModel uiModel = uiGeoModels.get(marker.getId());
        if(uiModel == null) {
            activity.removeMarker(marker);
        }

        if(currentNetwork != null && currentNetwork.getName() != null && uiModel != null) {
            uiModel.networkName = currentNetwork.getName();
        }
        activity.showPopupMenuForMarker(uiModel, new MapsActivity.PopupMenuListener() {
            @Override
            public void onCreateGeofence(GeoFenceUIModel model, double radius, String geofenceName, String networkName) {
                if(model == null) return;
                if(TextUtils.isEmpty(geofenceName)) {
                    activity.showMessage("geofence name is null");
                    return;
                }
                if(TextUtils.isEmpty(networkName)) {
                    //for geofence without wifi radius can not be less 100
                    if(!GeofenceModel.validateRadius(radius, false)) {
                        activity.showMessage("radius less than:" + GeofenceModel.USE_WITHOUT_WIFI_MIN_RADIUS);
                        return;
                    }
                } else {
                    networkName = GeofenceUtil.addQuotes(networkName);
                }
                model.radius = radius;
                model.geofenceName = geofenceName;
                model.networkName = networkName;

//                activity.showMessage("create with network:" + model.networkName);
                activity.showProgress();
                GeofenceModel geoModel = GeofenceUtil.createGeofenceModelFromUIModel(uiModel);
                businessLogicController.addGeoFence(geoModel);
            }

            @Override
            public void onDeleteGeofence(GeoFenceUIModel model) {
                if(model == null) return;
                GeofenceModel geoModel = geofenceDataSource.getGeofenceById(uiModel.marker.getId());
                if(geoModel != null) {
                    activity.showProgress();
                    businessLogicController.removeGeoFence(geoModel);
                } else {
                    activity.removeMarker(uiModel.marker);
                    if(uiModel.geoCircle != null) activity.removeCircle(uiModel.geoCircle);
                }
            }
        });
        return true;
    }


    public void onMapLongClick(LatLng latLng) {
        Marker marker = activity.displayMarker(latLng, GEO_FENCE_MARKER_TITLE, true);
        GeoFenceUIModel uiModel = new GeoFenceUIModel(marker);
        uiModel.marker = marker;
        uiGeoModels.put(marker.getId(), uiModel);
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
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        mRequestingLocationUpdates = false;
        //something strange behavior on test device(Xiaomi note 3 pro Android 5.1) callback is not trigerred
//                .addOnCompleteListener(activity, new OnCompleteListener<Void>() {
//                    @Override
//                    public void onComplete(@NonNull Task<Void> task) {
//                        mRequestingLocationUpdates = false;
//                    }
//                });
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mRequestingLocationUpdates) {
                    Log.i(TAG, "Permission granted, updates requested, starting location updates");
                    startLocationUpdates();
                    activity.prepareMap();
                }
            } else {
                activity.showSnackbar(R.string.permission_denied_explanation, R.string.settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                activity.startActivity(intent);
                            }
                        });
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.i(TAG, "User agreed to make required location settings changes.");
                        // Nothing to do. startLocationupdates() gets called in onResume again.
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.i(TAG, "User chose not to make required location settings changes.");
                        mRequestingLocationUpdates = false;
                        break;
                }
                break;
        }
    }

    private boolean checkPermissions() {
        if (ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return true;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, android.Manifest.permission.ACCESS_FINE_LOCATION);
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            activity.showSnackbar(R.string.permission_rationale, android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    });
        } else {
            Log.i(TAG, "Requesting permission");
            ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    private BroadcastReceiver networkStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            currentNetwork = NetworkUtil.updateNetworkInfo();
            showNetworkStatus();
        }
    };


    public static class GeoFenceUIModel {
        private Marker marker;
        public double radius;
        public Circle geoCircle;
        public String networkName;
        public String geofenceName;

        public GeoFenceUIModel(Marker marker) {
            this.marker = marker;
        }

        public Marker getMarker() {
            return marker;
        }
    }

}
