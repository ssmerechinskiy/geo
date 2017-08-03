package com.sergey.geo;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

/**
 * Created by user on 30.07.2017.
 */

public class GeoFenceControllerImpl implements GeoFenceController, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public final static String TAG = GeoFenceController.class.getSimpleName();

    IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");

    private Context context;
    private List<GeoFenceEventListener> listeners = new ArrayList<>();
    private GoogleApiClient mGoogleApiClient;

    private Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    private ExecutorService requestExecutor = Executors.newSingleThreadExecutor();
    private ExecutorService resultExecutor = Executors.newSingleThreadExecutor();

    private Map<String, GeoFenceModel> pendingGeofences = new ConcurrentHashMap<>();

    private Network currentNetwork;

    public GeoFenceControllerImpl(Context c) {
        context = c;
        LocalBroadcastManager.getInstance(context).registerReceiver(networkStateReceiver, intentFilter);
        currentNetwork = NetworkUtil.updateNetworkInfo();
    }

    @Override
    public void addGeoFence(final GeoFenceModel geoFenceModel) {
        requestExecutor.execute(new Runnable() {
            @Override
            public void run() {
                addGeoFenceInternal(geoFenceModel);
            }
        });
    }

    private synchronized void addGeoFenceInternal(GeoFenceModel geoFenceModel) {
        Log.d(TAG, "addGeoFenceInternal");
        pendingGeofences.put(geoFenceModel.getId(), geoFenceModel);
        if(mGoogleApiClient == null) {
            Log.d(TAG, "addGeoFenceInternal:create client");
//            GeoApp.showMessage("addGeoFenceInternal:create client");
//            notifyOnMessage(null, "addGeoFenceInternal:create client");
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(GeoFenceControllerImpl.this)
                    .addOnConnectionFailedListener(GeoFenceControllerImpl.this)
                    .build();
        }
        if(mGoogleApiClient.isConnected()) {
            Log.d(TAG, "addGeoFenceInternal:client is connected. trying add to service");
//            GeoApp.showMessage("addGeoFenceInternal:client is connected. trying add to service");
//            notifyOnMessage(null, "addGeoFenceInternal:client is connected. trying add to service");
            addGeoFenceToService(geoFenceModel);
        } else {
            if(!mGoogleApiClient.isConnecting()) {
                Log.d(TAG, "addGeoFenceInternal:client is not connected. connecting");
//                GeoApp.showMessage("addGeoFenceInternal:client is not connected. connecting");
//                notifyOnMessage(null, "addGeoFenceInternal:client is not connected. connecting");
                mGoogleApiClient.connect();
            }
        }
    }

    private void addGeoFenceToService(GeoFenceModel geofence) {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
//        builder.setInitialTrigger(geofence.getTransitionType() == Geofence.GEOFENCE_TRANSITION_ENTER
//                ? GeofencingRequest.INITIAL_TRIGGER_ENTER : GeofencingRequest.INITIAL_TRIGGER_EXIT);
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER|GeofencingRequest.INITIAL_TRIGGER_DWELL);
        builder.addGeofence(geofence.newGeofence());
        GeofencingRequest build = builder.build();
        try {
            LocationServices.GeofencingApi.addGeofences(mGoogleApiClient, build, getPendingIntent())
                    .setResultCallback(addGeofenceCallback);
        } catch (SecurityException e) {
            Log.e(TAG, "call location service Error:" + e.getMessage());
//            GeoApp.showMessage("call location service Error:" + e.getMessage());
            notifyOnMessage(null, "call location service Error:" + e.getMessage());
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected");
//        GeoApp.showMessage("onConnected");
//        notifyOnMessage(null, "onConnected");
        for (GeoFenceModel m : pendingGeofences.values()) {
            addGeoFenceToService(m);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended:" + String.valueOf(i));
//        GeoApp.showMessage("onConnectionSuspended:" + String.valueOf(i));
        notifyOnMessage(null, "onConnectionSuspended:" + String.valueOf(i));
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult.getErrorMessage());
//        GeoApp.showMessage("onConnectionFailed:" + connectionResult.getErrorMessage());
        notifyOnMessage(null, "onConnectionFailed:" + connectionResult.getErrorMessage());
    }

    @Override
    public void registerListener(GeoFenceEventListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    @Override
    public void unregisterListener(GeoFenceEventListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    private void notifyOnEvent(GeoFenceModel m) {
        synchronized (listeners) {
            for (GeoFenceEventListener l : listeners) {
                l.onEvent(m);
            }
        }
    }

    private void notifyOnMessage(GeoFenceModel m, String message) {
        synchronized (listeners) {
            for (GeoFenceEventListener l : listeners) {
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
                    notifyOnMessage(null, "Location Services error: " + geofencingEvent.getErrorCode());
                    return;
                }
                int transitionType = geofencingEvent.getGeofenceTransition();
                List<Geofence> triggeredGeofences = geofencingEvent.getTriggeringGeofences();
                notifyOnMessage(null, "triggered transition:" + GeoFenceUtil.getTransionName(transitionType) + " size:" + triggeredGeofences.size());
                for (Geofence geofence : triggeredGeofences) {
                    handleResult(geofence);
                }
            }
        });
    }

    private void handleResult(Geofence geofence) {
//        notifyOnMessage(null, "handle res:" + geofence.getRequestId());
        GeoFenceModel gm = pendingGeofences.get(geofence.getRequestId());
        if(gm == null) return;
        switch (gm.getTransitionType()) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                notifyOnEvent(gm);
//                pendingGeofences.remove(gm.getId());
                break;
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                notifyOnEvent(gm);
//                String wifiNetwork = gm.getWifiNetwork();
//                if(!TextUtils.isEmpty(wifiNetwork) && currentNetwork == Network.WIFI && currentNetwork.getName().equals(wifiNetwork)) {
//                    notifyOnMessage(gm, "exit but wifi connected");
//                } else {
//                    notifyOnEvent(gm);
//                    pendingGeofences.remove(gm.getId());
//                }
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
            currentNetwork = NetworkUtil.updateNetworkInfo();
        }
    };

    private ResultCallback<Status> addGeofenceCallback = new ResultCallback<Status>() {
        @Override
        public void onResult(@NonNull Status status) {
            if (status.isSuccess()) {
                Log.d(TAG, "addGeofenceCallback success:");
            } else {
                Log.d(TAG, "addGeofenceCallback error:" + status.getStatusMessage());
            }
            String msg = "Geo fences add status: " + status.getStatusMessage();
            notifyOnMessage(null, msg);

//            if(pendingGeofences.keySet().size() == 0) {
//                mGoogleApiClient.disconnect();
//            }
        }
    };

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(networkStateReceiver);
        context = null;
        mainThreadHandler.removeCallbacksAndMessages(null);
    }

    private PendingIntent getPendingIntent() {
        Intent i = new Intent(GeoApp.getInstance(), GeoFenceEventReceiver.class);
        return PendingIntent.getBroadcast(GeoApp.getInstance(), 1, i, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public Network getCurrentNetwork() {
        return currentNetwork;
    }

}
