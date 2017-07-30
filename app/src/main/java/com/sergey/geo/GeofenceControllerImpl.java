package com.sergey.geo;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.content.Context.CONNECTIVITY_SERVICE;

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
            Log.d(TAG, "addGeofenceInternal:create client");
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(GeofenceControllerImpl.this)
                    .addOnConnectionFailedListener(GeofenceControllerImpl.this)
                    .build();
            pendingGeofences.put(geofenceModel.getId(), geofenceModel);
            mGoogleApiClient.connect();
        } else {
            Log.d(TAG, "addGeofenceInternal:send to service");
            pendingGeofences.put(geofenceModel.getId(), geofenceModel);
            addGeofenceToService(geofenceModel);
        }
    }

    private void addGeofenceToService(GeofenceModel geofence) {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(geofence.getTransitionType() == Geofence.GEOFENCE_TRANSITION_ENTER
                ? GeofencingRequest.INITIAL_TRIGGER_ENTER : GeofencingRequest.INITIAL_TRIGGER_EXIT);
        builder.addGeofence(geofence.newGeofence());
        GeofencingRequest build = builder.build();
        try {
            LocationServices.GeofencingApi.addGeofences(mGoogleApiClient, build, getPendingIntent())
                    .setResultCallback(addGeofenceCallback);
        } catch (SecurityException e) {
            Log.e(TAG, "call location service Error:" + e.getMessage());
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.e(TAG, "onConnected");
        for (GeofenceModel m : pendingGeofences.values()) {
            addGeofenceToService(m);
        }
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

    private void notifyOnEvent(GeofenceModel m) {
        synchronized (listeners) {
            for (GeofenceEventListener l : listeners) {
                l.onEvent(m);
            }
        }
    }

    private void notifyOnMessage(GeofenceModel m, String message) {
        synchronized (listeners) {
            for (GeofenceEventListener l : listeners) {
                l.onMessage(message);
            }
        }
    }

    @Override
    public void onGeofenceEvent(final Intent intent) {
        resultExecutor.execute(new Runnable() {
            @Override
            public void run() {
                GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
                if (geofencingEvent.hasError()) {
                    Log.e(TAG, "Location Services error: " + geofencingEvent.getErrorCode());
                    return;
                }
                int transitionType = geofencingEvent.getGeofenceTransition();
                List<Geofence> triggeredGeofences = geofencingEvent.getTriggeringGeofences();
                for (Geofence geofence : triggeredGeofences) {
                    handleResult(geofence);
                }
            }
        });
    }

    private void handleResult(Geofence geofence) {
        GeofenceModel gm = pendingGeofences.get(geofence.getRequestId());
        switch (gm.getTransitionType()) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                notifyOnEvent(gm);
                break;
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                String wifiNetwork = gm.getWifiNetwork();
                if(isConnectedToWifi(wifiNetwork)) {
                    notifyOnMessage(gm, "exit but wifi connected");
                } else {
                    notifyOnEvent(gm);
                }
                break;
            case Geofence.GEOFENCE_TRANSITION_DWELL:
                notifyOnMessage(gm, "you are inside geofence");
                break;
            default:
                break;
        }
    }

    private BroadcastReceiver networkStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    };

    private ResultCallback<Status> addGeofenceCallback = new ResultCallback<Status>() {
        @Override
        public void onResult(@NonNull Status status) {

        }
    };

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(networkStateReceiver);
        context = null;
    }

    private PendingIntent getPendingIntent() {
        Intent i = new Intent(GeoApp.getInstance(), GeofenceEventReceiver.class);
        return PendingIntent.getBroadcast(GeoApp.getInstance(), 1, i, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private boolean isConnectedToWifi(String wifiNetwork) {
        try {
            if (context != null) {
                ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                if(networkInfo.isConnected()) {
                    final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                    final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
                    if (connectionInfo != null && !(connectionInfo.getSSID().equals("")) && wifiNetwork.equals(connectionInfo.getSSID())) {
                        return true;
                    }
                }
                return false;
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "isConnectedToInternet:" + e.getMessage());
            return false;
        }
    }

}
