package com.sergey.geo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by user on 30.07.2017.
 */

public class GeofenceControllerImpl implements GeofenceController, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public final static String TAG = GeofenceController.class.getSimpleName();

    IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");

    private Context context;
    private List<GeofenceEventListener> listeners = new ArrayList<>();
    private GoogleApiClient mGoogleApiClient;

    private ExecutorService requestExecutor = Executors.newSingleThreadExecutor();
    private ExecutorService resultExecutor = Executors.newSingleThreadExecutor();

    private Map<String, GeofenceModel> pendingGeofences = new ConcurrentHashMap<>();

    public GeofenceControllerImpl(Context c) {
        context = c;
        LocalBroadcastManager.getInstance(context).registerReceiver(networkStateReceiver, intentFilter);
    }

    @Override
    public void addGeofence(final GeofenceModel geofenceModel) {
        requestExecutor.execute(new Runnable() {
            @Override
            public void run() {
                addGeofenceInternal(geofenceModel);
            }
        });
    }

    private synchronized void addGeofenceInternal(GeofenceModel geofenceModel) {
        if(mGoogleApiClient == null || !mGoogleApiClient.isConnected()) {
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(GeofenceControllerImpl.this)
                    .addOnConnectionFailedListener(GeofenceControllerImpl.this)
                    .build();
            pendingGeofences.put(geofenceModel.getId(), geofenceModel);
            mGoogleApiClient.connect();
            // TODO: 30.07.2017 then waiting for connection
        } else {
            addGeofenceToService(geofenceModel);
        }
    }

    private void addGeofenceToService(GeofenceModel geofence) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // TODO: 30.07.2017 convert map objects and create as list
        addGeofenceToService(new GeofenceModel());
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended:" + String.valueOf(i));
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult.getErrorMessage());
    }

    @Override
    public void registerListener(GeofenceEventListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    @Override
    public void unregisterListener(GeofenceEventListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    private BroadcastReceiver networkStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    };

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(networkStateReceiver);
        context = null;
    }


}
