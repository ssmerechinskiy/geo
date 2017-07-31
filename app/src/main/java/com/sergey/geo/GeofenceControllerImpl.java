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
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
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

    private Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    private ExecutorService requestExecutor = Executors.newSingleThreadExecutor();
    private ExecutorService resultExecutor = Executors.newSingleThreadExecutor();

    private Map<String, GeofenceModel> pendingGeofences = new ConcurrentHashMap<>();

    private NetworkType currentNetworkType;

    public GeofenceControllerImpl(Context c) {
        context = c;
        LocalBroadcastManager.getInstance(context).registerReceiver(networkStateReceiver, intentFilter);
        currentNetworkType = NetworkUtil.updateNetworkInfo();
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
        pendingGeofences.put(geofenceModel.getId(), geofenceModel);
        if(mGoogleApiClient == null) {
            Log.d(TAG, "addGeofenceInternal:create client");
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(GeofenceControllerImpl.this)
                    .addOnConnectionFailedListener(GeofenceControllerImpl.this)
                    .build();
        }
        if(mGoogleApiClient.isConnected()) {
            addGeofenceToService(geofenceModel);
        } else {
            if(!mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
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
                if(!TextUtils.isEmpty(wifiNetwork) && currentNetworkType == NetworkType.WIFI && currentNetworkType.getName().equals(wifiNetwork)) {
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
            currentNetworkType = NetworkUtil.updateNetworkInfo();
        }
    };

    private ResultCallback<Status> addGeofenceCallback = new ResultCallback<Status>() {
        @Override
        public void onResult(@NonNull Status status) {
            // TODO: 31.07.2017 there we should to disconnect if we have no pending geofenes?
        }
    };

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(networkStateReceiver);
        context = null;
        mainThreadHandler.removeCallbacksAndMessages(null);
    }

    private PendingIntent getPendingIntent() {
        Intent i = new Intent(GeoApp.getInstance(), GeofenceEventReceiver.class);
        return PendingIntent.getBroadcast(GeoApp.getInstance(), 1, i, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public NetworkType getCurrentNetworkType() {
        return currentNetworkType;
    }

}
