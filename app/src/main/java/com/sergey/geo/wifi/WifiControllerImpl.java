package com.sergey.geo.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.sergey.geo.data.GeofenceDataSource;
import com.sergey.geo.data.GeofenceDataSourceImpl;
import com.sergey.geo.NetworkUtil;
import com.sergey.geo.model.GeofenceModel;
import com.sergey.geo.model.Network;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by user on 10.08.2017.
 */

public class WifiControllerImpl implements WifiController{
    private final static String TAG = WifiControllerImpl.class.getSimpleName();
    private static final WifiControllerImpl instance = new WifiControllerImpl();

    public final static IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");

    private Context context;
    private volatile Network currentNetwork;
    private GeofenceDataSource geofenceDataSource = GeofenceDataSourceImpl.getInstance();
    private List<WifiEventListener> listeners = new ArrayList<>();
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private WifiControllerImpl() {
        executor = Executors.newSingleThreadExecutor();
    }

    public static WifiControllerImpl getInstance() {
        return instance;
    }

    public void init(Context c) {
        context = c;
        currentNetwork = NetworkUtil.updateNetworkInfo();
        context.registerReceiver(networkStateReceiver, intentFilter);
    }

    private BroadcastReceiver networkStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive network state");
            Network previousNetwork = currentNetwork;
            if(previousNetwork != null && previousNetwork.getName() != null) {
                /** FOR PREVIOUS WIFI WIFI_TRANSITION_EXIT*/
                List<GeofenceModel> list = geofenceDataSource.getGeofencesByWifiNetworkName(previousNetwork.getName());
                if(list != null && list.size() > 0) {
                    notifyOnEvent(list, WIFI_TRANSITION_EXIT);
                }
            }
            currentNetwork = NetworkUtil.updateNetworkInfo();
            if(currentNetwork != null && currentNetwork.getName() != null) {
                /** FOR NEW WIFI WIFI_TRANSITION_ENTER*/
                List<GeofenceModel> list = geofenceDataSource.getGeofencesByWifiNetworkName(currentNetwork.getName());
                if(list != null && list.size() > 0) {
                    notifyOnEvent(list, WIFI_TRANSITION_ENTER);
                }
            }
        }
    };

    @Override
    public void addGeoFence(final GeofenceModel geofenceModel) {
        synchronized (executor) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "adding geofence");
                    if(geofenceModel.getWifiNetwork() != null && currentNetwork != null) {
                        if(geofenceModel.getWifiNetwork().equals(currentNetwork.getName())) {
                            List<GeofenceModel> list = geofenceDataSource.getGeofencesByWifiNetworkName(currentNetwork.getName());
                            notifyOnEvent(list, WIFI_TRANSITION_ENTER);
                        }
                    }
                }
            });
        }
    }

    @Override
    public void removeGeoFence(GeofenceModel geofenceModel) {

    }

    @Override
    public void registerListener(WifiEventListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    @Override
    public void unregisterListener(WifiEventListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    @Override
    public void onGeofenceEvent(Intent intent) {
        // TODO: 10.08.2017 do not implement
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        context.unregisterReceiver(networkStateReceiver);
        releaseExecutor();
        context = null;
    }

    private void releaseExecutor() {
        Log.d(TAG, "releaseExecutor");
        executor.shutdown();
        executor.shutdownNow();
        executor = null;
    }

    private void notifyOnEvent(List<GeofenceModel> models, int transitionType) {
        synchronized (listeners) {
            for (WifiEventListener l : listeners) {
                l.onEvent(models, transitionType);
            }
        }
    }

    private void notifyOnMessage(GeofenceModel m, String message) {
        synchronized (listeners) {
            for (WifiEventListener l : listeners) {
                l.onMessage(m, message);
            }
        }
    }

    private void notifyOnGeofenceAddedSuccess(List<String> ids) {
        synchronized (listeners) {
            for (WifiEventListener l : listeners) {
                l.onGeofenceAddedSuccess(ids);
            }
        }
    }

    private void notifyOnGeofenceAddedFailed(List<String> ids) {
        synchronized (listeners) {
            for (WifiEventListener l : listeners) {
                l.onGeofenceAddedFailed(ids);
            }
        }
    }

    private void notifyOnGeofenceDeletedSuccess(List<String> ids) {
        synchronized (listeners) {
            for (WifiEventListener l : listeners) {
                l.onGeofenceDeletedSuccess(ids);
            }
        }
    }

    private void notifyOnGeofenceDeletedFailed(List<String> ids) {
        synchronized (listeners) {
            for (WifiEventListener l : listeners) {
                l.onGeofenceDeletedFailed(ids);
            }
        }
    }
}
