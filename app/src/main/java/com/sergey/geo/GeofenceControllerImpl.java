package com.sergey.geo;

import android.app.PendingIntent;
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

public class GeofenceControllerImpl implements GeofenceController, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public final static String TAG = GeofenceController.class.getSimpleName();

    private static GeofenceControllerImpl instance = new GeofenceControllerImpl();

    public final static IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");

    private Context context;
    private List<GeofenceEventListener> listeners = new ArrayList<>();
    private GoogleApiClient mGoogleApiClient;

    private ExecutorService resultExecutor = Executors.newSingleThreadExecutor();

    private GeofenceDataSource geofenceDataSource = GeofenceDataSourceImpl.getInstance();
    private Map<String, GeofenceModel> addingInProcessGeoIds = new ConcurrentHashMap<>();
    private volatile boolean isAddingGeofencesInProgress = false;

    private Map<String, GeofenceModel> deletingInProcessGeoIds = new ConcurrentHashMap<>();
    private volatile boolean isDeletingGeofencesInProgress = false;

    private volatile Network currentNetwork;

    public static GeofenceControllerImpl getInstance() {
        return instance;
    }

    private GeofenceControllerImpl() {
    }

    public void init(Context c) {
        context = c;
        context.registerReceiver(networkStateReceiver, intentFilter);
        currentNetwork = NetworkUtil.updateNetworkInfo();
    }

    @Override
    public synchronized void addGeoFence(final GeofenceModel geofenceModel) {
        Log.d(TAG, "addGeoFence");
        if(isAddingGeofencesInProgress) {
            List<String> list = new ArrayList<>();
            list.add(geofenceModel.getId());
            notifyOnGeofenceAddedFailed(list);
            return;
        }
        isAddingGeofencesInProgress = true;
        addingInProcessGeoIds.put(geofenceModel.getId(), geofenceModel);
        if(mGoogleApiClient == null) {
            Log.d(TAG, "addGeoFenceInternal:create client");
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(GeofenceControllerImpl.this)
                    .addOnConnectionFailedListener(GeofenceControllerImpl.this)
                    .build();
        }
        if(mGoogleApiClient.isConnected()) {
            Log.d(TAG, "addGeoFenceInternal:client is connected. trying add to service");
            addGeoFenceToService(geofenceModel);
        } else {
            if(!mGoogleApiClient.isConnecting()) {
                Log.d(TAG, "addGeoFenceInternal:client is not connected. connecting");
                mGoogleApiClient.connect();
            }
        }
    }

    private void addGeoFenceToService(GeofenceModel geofence) {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER|GeofencingRequest.INITIAL_TRIGGER_DWELL);
        builder.addGeofence(geofence.newGeofence());
        GeofencingRequest build = builder.build();
        try {
            LocationServices.GeofencingApi.addGeofences(mGoogleApiClient, build, getPendingIntent())
                    .setResultCallback(addGeofenceCallback);
        } catch (SecurityException e) {
            Log.e(TAG, "call location service Error:" + e.getMessage());
            notifyOnMessage(null, "call location service Error:" + e.getMessage());
        }
    }

    private ResultCallback<Status> addGeofenceCallback = new ResultCallback<Status>() {
        @Override
        public void onResult(@NonNull final Status status) {
            resultExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    List<String> list = new ArrayList<>(addingInProcessGeoIds.keySet());
                    if (status.isSuccess()) {
                        geofenceDataSource.saveGeofences(new ArrayList<>(addingInProcessGeoIds.values()));
                        notifyOnGeofenceAddedSuccess(list);
                    } else {
                        notifyOnGeofenceAddedFailed(list);
                    }
                    addingInProcessGeoIds.clear();
                    isAddingGeofencesInProgress = false;
                }
            });

        }
    };

    @Override
    public synchronized void removeGeoFence(GeofenceModel geofenceModel) {
        Log.d(TAG, "addGeoFence");
        if(isDeletingGeofencesInProgress) {
            List<String> list = new ArrayList<>();
            list.add(geofenceModel.getId());
            notifyOnGeofenceDeletedFailed(list);
            return;
        }
        isDeletingGeofencesInProgress = true;
        deletingInProcessGeoIds.put(geofenceModel.getId(), geofenceModel);
        if(mGoogleApiClient == null) {
            Log.d(TAG, "addGeoFenceInternal:create client");
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(GeofenceControllerImpl.this)
                    .addOnConnectionFailedListener(GeofenceControllerImpl.this)
                    .build();
        }
        if(mGoogleApiClient.isConnected()) {
            Log.d(TAG, "addGeoFenceInternal:client is connected. trying add to service");
            deleteGeoFenceFromService(geofenceModel);
        } else {
            if(!mGoogleApiClient.isConnecting()) {
                Log.d(TAG, "addGeoFenceInternal:client is not connected. connecting");
                mGoogleApiClient.connect();
            }
        }
    }

    private void deleteGeoFenceFromService(GeofenceModel geofence) {
        List<String> deleted = new ArrayList<>();
        deleted.add(geofence.getId());
        LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient, deleted).setResultCallback(deleteGeofenceCallback);
    }

    private ResultCallback<Status> deleteGeofenceCallback = new ResultCallback<Status>() {
        @Override
        public void onResult(@NonNull final Status status) {
            resultExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    List<String> list = new ArrayList<>(deletingInProcessGeoIds.keySet());
                    if (status.isSuccess()) {
                        geofenceDataSource.removeGeofences(list);
                        notifyOnGeofenceDeletedSuccess(list);
                    } else {
                        notifyOnGeofenceDeletedFailed(list);
                    }
                    deletingInProcessGeoIds.clear();
                    isDeletingGeofencesInProgress = false;
                }
            });

        }
    };

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
                handleResult(triggeredGeofences, transitionType);
//                notifyOnMessage(null, "triggered transition:" + GeofenceUtil.getTransionName(transitionType) + " size:" + triggeredGeofences.size());
            }
        });
    }

    private void handleResult(List<Geofence> triggeredGeofences, int transitionType) {
        if(triggeredGeofences == null || triggeredGeofences.size() == 0) return;
        Geofence triggerredGeofence = triggeredGeofences.get(0);
        GeofenceModel gm = geofenceDataSource.getGeofenceById(triggerredGeofence.getRequestId());
        if(gm == null) {
            notifyOnMessage(null, "occurs event for object which does not exist in data source");
            return;
        }
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                notifyOnEvent(gm, transitionType);
                break;
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                String wifiNetwork = gm.getWifiNetwork();
                if(currentNetwork != null && currentNetwork == Network.WIFI && currentNetwork.getName() != null) {
                    if(!wifiNetwork.equals(currentNetwork.getName())) {
                        notifyOnEvent(gm, transitionType);
                    } else {
                        notifyOnMessage(gm, "You are leaving geo fence but still in wifi zone");
                    }
                } else {
                    notifyOnEvent(gm, transitionType);
                }
                break;
            case Geofence.GEOFENCE_TRANSITION_DWELL:
                notifyOnEvent(gm, transitionType);
                break;
            default:
                break;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected");
        for (GeofenceModel m : addingInProcessGeoIds.values()) {
            addGeoFenceToService(m);
        }

        for (GeofenceModel m : deletingInProcessGeoIds.values()) {
            deleteGeoFenceFromService(m);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended:" + String.valueOf(i));
        notifyOnMessage(null, "onConnectionSuspended:" + String.valueOf(i));
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult.getErrorMessage());
        notifyOnMessage(null, "onConnectionFailed:" + connectionResult.getErrorMessage());
    }

    private BroadcastReceiver networkStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            currentNetwork = NetworkUtil.updateNetworkInfo();
        }
    };

    @Override
    public void onDestroy() {
        context.unregisterReceiver(networkStateReceiver);
        geofenceDataSource.removeAllGeofences();
        addingInProcessGeoIds.clear();
        context = null;
    }

    private PendingIntent getPendingIntent() {
        Intent i = new Intent(GeoApp.getInstance(), GeofenceEventReceiver.class);
        return PendingIntent.getBroadcast(GeoApp.getInstance(), 1, i, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public Network getCurrentNetwork() {
        return currentNetwork;
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

    private void notifyOnEvent(GeofenceModel m, int transitionType) {
        synchronized (listeners) {
            for (GeofenceEventListener l : listeners) {
                l.onEvent(m, transitionType);
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

    private void notifyOnGeofenceAddedSuccess(List<String> ids) {
        synchronized (listeners) {
            for (GeofenceEventListener l : listeners) {
                l.onGeofenceAddedSuccess(ids);
            }
        }
    }

    private void notifyOnGeofenceAddedFailed(List<String> ids) {
        synchronized (listeners) {
            for (GeofenceEventListener l : listeners) {
                l.onGeofenceAddedFailed(ids);
            }
        }
    }

    private void notifyOnGeofenceDeletedSuccess(List<String> ids) {
        synchronized (listeners) {
            for (GeofenceEventListener l : listeners) {
                l.onGeofenceDeletedSuccess(ids);
            }
        }
    }

    private void notifyOnGeofenceDeletedFailed(List<String> ids) {
        synchronized (listeners) {
            for (GeofenceEventListener l : listeners) {
                l.onGeofenceDeletedFailed(ids);
            }
        }
    }

    /**
     * test method dont use it!!!!
     */
    public void testClearAllGeofences() {
        List<String> deleted = new ArrayList<>();
        for(int i = 0; i < 500; i++) {
            String id = "m" + i;
            deleted.add(id);
        }
        if(mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient, deleted);
        }
    }
}
