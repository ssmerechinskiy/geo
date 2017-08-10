package com.sergey.geo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.google.android.gms.location.Geofence;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sober on 10.08.2017.
 */

public class BusinessLogicController implements GeofenceController {
    private static BusinessLogicController instance = new BusinessLogicController();
    private Context context;
    public final static IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");

    public final static int WIFI_TRANSITION_ENTER = 8;
    public final static int WIFI_TRANSITION_EXIT = 16;

    private volatile Network currentNetwork;
    private GeofenceController geoController;
    private GeofenceDataSource geofenceDataSource = GeofenceDataSourceImpl.getInstance();

    private List<GeofenceEventListener> listeners = new ArrayList<>();

    private BusinessLogicController() {
    }

    public static BusinessLogicController getInstance() {
        return instance;
    }

    public void init(Context c) {
        context = c;
        context.registerReceiver(networkStateReceiver, intentFilter);
        geoController = GeofenceControllerImpl.getInstance();
        currentNetwork = NetworkUtil.updateNetworkInfo();
        geoController.registerListener(geofenceEventListener);
    }

    private BroadcastReceiver networkStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Network previousNetwork = currentNetwork;
            currentNetwork = NetworkUtil.updateNetworkInfo();
            /** IF ENTER TO WIFI ZONE*/
            if(currentNetwork != null && currentNetwork.getName() != null) {
                /** IF WIFI EXIST*/
                GeofenceModel m = getGeofenceWithWifi(currentNetwork.getName());
                //check if this geofence already entered or not
                boolean alreadyEnteredToGeo = alreadyEnteredToGeo(m);
                if(m != null && alreadyEnteredToGeo) {
                    notifyOnMessage(m, "You are entering to WIFI zone:" + currentNetwork.getName()
                            + " but already located in Geo:" + m.getName() + " with this wifi");
                    return;
                }
                if(m != null && !alreadyEnteredToGeo) {
                    notifyOnEvent(m, Geofence.GEOFENCE_TRANSITION_ENTER);
                    return;
                }
            } else {/** IF EXIT FROM WIFI ZONE*/
                if(previousNetwork != null && previousNetwork.getName() != null) {
                    GeofenceModel m = getGeofenceWithWifi(previousNetwork.getName());
                    boolean stillInGeo = stillInGeo(m);
                    if(m != null && stillInGeo) {
                        notifyOnMessage(m, "You are leaving WIFI zone:" + previousNetwork.getName()
                                + " but still located in Geo:" + m.getName() + " with this wifi");
                        return;
                    }
                    if(m != null && !stillInGeo) {
                        notifyOnEvent(m, Geofence.GEOFENCE_TRANSITION_EXIT);
                        return;
                    }
                }
            }
        }
    };

    //check methods for wifi broadcasting events
    private GeofenceModel getGeofenceWithWifi(String wifiName) {
        // TODO: 10.08.2017 get from data source
        return null;
    }

    private boolean alreadyEnteredToGeo(GeofenceModel m) {
        // TODO: 10.08.2017 get from last geo event map
        return true;
    }

    private boolean stillInGeo(GeofenceModel m) {
        // TODO: 10.08.2017 get from last geo event map
        return true;
    }


    //check methods for google api client events
    private boolean alreadyEnteredToWifi(GeofenceModel m) {
        // TODO: 10.08.2017 get from last wifi event map
        return false;
    }

    private boolean stillInWifiZone(GeofenceModel m) {
        // TODO: 10.08.2017 get from current network state
//        String wifiNetwork = geofenceModel.getWifiNetwork();
//        if(currentNetwork != null && currentNetwork == Network.WIFI && currentNetwork.getName() != null) {
//            if(!wifiNetwork.equals(currentNetwork.getName())) {
//                notifyOnEvent(geofenceModel, transitionType);
//            } else {
//                notifyOnMessage(geofenceModel, "You are leaving geo fence but still in wifi zone");
//            }
//        } else {
//            notifyOnEvent(geofenceModel, transitionType);
//        }
        return false;
    }

    private GeofenceEventListener geofenceEventListener = new GeofenceEventListener() {
        @Override
        public void onEvent(final GeofenceModel geofenceModel, final int transitionType) {
            if(geofenceModel == null) {
                notifyOnMessage(null, "from geo controller get null object");
            }
            switch (transitionType) {
                case Geofence.GEOFENCE_TRANSITION_ENTER:
                    if(!alreadyEnteredToWifi(geofenceModel)) {
                        notifyOnEvent(geofenceModel, transitionType);
                    } else {
                        notifyOnMessage(geofenceModel, "You are entered in GEO but already entered with WIFI");
                    }
                    break;
                case Geofence.GEOFENCE_TRANSITION_EXIT:
                    if(!stillInWifiZone(geofenceModel)) {
                        notifyOnEvent(geofenceModel, transitionType);
                    } else {
                        notifyOnMessage(geofenceModel, "You are leaving GEO but still in wifi zone");
                    }
                    break;
                case Geofence.GEOFENCE_TRANSITION_DWELL:
                    notifyOnEvent(geofenceModel, transitionType);
                    break;
                default:
                    break;
             }
        }

        @Override
        public void onMessage(final GeofenceModel geofenceModel, final String message) {
            notifyOnMessage(null, message);
        }

        @Override
        public void onGeofenceAddedSuccess(final List<String> ids) {
            notifyOnGeofenceAddedSuccess(ids);
        }

        @Override
        public void onGeofenceAddedFailed(final List<String> ids) {
            notifyOnGeofenceAddedFailed(ids);
        }

        @Override
        public void onGeofenceDeletedSuccess(final List<String> ids) {
            notifyOnGeofenceDeletedSuccess(ids);
        }

        @Override
        public void onGeofenceDeletedFailed(List<String> ids) {
            notifyOnGeofenceDeletedFailed(ids);
        }
    };



    @Override
    public void addGeoFence(GeofenceModel geofenceModel) {
        geoController.addGeoFence(geofenceModel);
    }

    @Override
    public void removeGeoFence(GeofenceModel geofenceModel) {
        geoController.removeGeoFence(geofenceModel);
    }
    
    @Override
    public void onGeofenceEvent(Intent intent) {
        // TODO: 10.08.2017 do not implement this method
    }

    public void onDestroy() {
        geofenceDataSource.removeAllGeofences();
        geoController.onDestroy();
        context.unregisterReceiver(networkStateReceiver);
        context = null;
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
                l.onMessage(m, message);
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
}
