package com.sergey.geo;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.sergey.geo.data.GeofenceDataSource;
import com.sergey.geo.data.GeofenceDataSourceImpl;
import com.sergey.geo.googleapi.GeofenceController;
import com.sergey.geo.googleapi.GeofenceControllerImpl;
import com.sergey.geo.googleapi.GeofenceEventListener;
import com.sergey.geo.model.GeofenceModel;
import com.sergey.geo.wifi.WifiController;
import com.sergey.geo.wifi.WifiControllerImpl;
import com.sergey.geo.wifi.WifiEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.sergey.geo.wifi.WifiController.WIFI_TRANSITION_ENTER;
import static com.sergey.geo.wifi.WifiController.WIFI_TRANSITION_EXIT;

/**
 * Created by sober on 10.08.2017.
 */

public class BusinessLogicController implements GeofenceController {
    private final static String TAG = BusinessLogicController.class.getSimpleName();
    private static final BusinessLogicController instance = new BusinessLogicController();
    private Context context;

    private GeofenceController geoController;
    private GeofenceDataSource geofenceDataSource;
    private WifiController wifiController;

    private Map<String, Integer> geoEventMap = new ConcurrentHashMap<>();
    private Map<String, Integer> wifiEventMap = new ConcurrentHashMap<>();

    private List<GeofenceEventListener> listeners = new ArrayList<>();

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private BusinessLogicController() {
    }

    public static BusinessLogicController getInstance() {
        return instance;
    }

    public void init(Context c) {
        context = c;
        geoController = GeofenceControllerImpl.getInstance();
        geoController.registerListener(geofenceEventListener);
        wifiController = WifiControllerImpl.getInstance();
        wifiController.registerListener(wifiEventListener);
        geofenceDataSource = GeofenceDataSourceImpl.getInstance();

    }


    private WifiEventListener wifiEventListener = new WifiEventListener() {
        @Override
        public void onEvent(final List<GeofenceModel> models, final int transitionType) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "onEvent from wifi size:" + models.size());
                    for (GeofenceModel m : models) {
                        wifiEventMap.put(m.getId(), transitionType);
                        switch (transitionType) {
                            case WIFI_TRANSITION_ENTER :
                                Log.d(TAG, "onEvent from wifi:ENTER");
                                if(insideGeo(m)) {
                                    notifyOnMessage(m, "You are entering to WIFI zone:" + m.getWifiNetwork()
                                            + " but already located in Geo:" + m.getName() + " with this wifi");
                                } else {
                                    notifyOnEvent(m, Geofence.GEOFENCE_TRANSITION_ENTER, GeofenceEventListener.EVENT_TYPE_DETECTOR_WIFI);
                                }
                                break;
                            case WIFI_TRANSITION_EXIT :
                                Log.d(TAG, "onEvent from wifi:EXIT");
                                if(insideGeo(m)) {
                                    notifyOnMessage(m, "You are leaving WIFI zone:" + m.getWifiNetwork()
                                            + " but still located in Geo:" + m.getName() + " with this wifi");
                                } else {
                                    notifyOnEvent(m, Geofence.GEOFENCE_TRANSITION_EXIT, GeofenceEventListener.EVENT_TYPE_DETECTOR_WIFI);
                                }
                                break;
                        }
                    }
                }
            });

        }

        @Override
        public void onMessage(GeofenceModel geofenceModel, String message) {

        }

        @Override
        public void onGeofenceAddedSuccess(List<String> ids) {

        }

        @Override
        public void onGeofenceAddedFailed(List<String> ids) {

        }

        @Override
        public void onGeofenceDeletedSuccess(List<String> ids) {

        }

        @Override
        public void onGeofenceDeletedFailed(List<String> ids) {

        }
    };

    private GeofenceEventListener geofenceEventListener = new GeofenceEventListener() {
        @Override
        public void onEvent(final GeofenceModel geofenceModel, final int transitionType, final int eventTypeDetector) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "onEvent from geo");
                    if(geofenceModel == null) {
                        notifyOnMessage(null, "from geo controller get null object");
                    }
                    geoEventMap.put(geofenceModel.getId(), transitionType);
                    switch (transitionType) {
                        case Geofence.GEOFENCE_TRANSITION_ENTER:
                            if(!insideWifi(geofenceModel)) {
                                notifyOnEvent(geofenceModel, transitionType, eventTypeDetector);
                            } else {
                                notifyOnMessage(geofenceModel, "You are entered in GEO but already entered with WIFI");
                            }
                            break;
                        case Geofence.GEOFENCE_TRANSITION_EXIT:
                            if(!insideWifi(geofenceModel)) {
                                notifyOnEvent(geofenceModel, transitionType, eventTypeDetector);
                            } else {
                                notifyOnMessage(geofenceModel, "You are leaving GEO but still in wifi zone");
                            }
                            break;
                        case Geofence.GEOFENCE_TRANSITION_DWELL:
                            notifyOnEvent(geofenceModel, transitionType, eventTypeDetector);
                            break;
                        default:
                            break;
                    }
                }
            });
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
            geofenceDataSource.removeGeofences(ids);
            notifyOnGeofenceAddedFailed(ids);
        }

        @Override
        public void onGeofenceDeletedSuccess(final List<String> ids) {
            geofenceDataSource.removeGeofences(ids);
            notifyOnGeofenceDeletedSuccess(ids);
        }

        @Override
        public void onGeofenceDeletedFailed(List<String> ids) {
            notifyOnGeofenceDeletedFailed(ids);
        }
    };

    private boolean insideGeo(GeofenceModel m) {
        Integer transition = geoEventMap.get(m.getId());
        if(transition == null) return false;
        if(transition == Geofence.GEOFENCE_TRANSITION_ENTER || transition == Geofence.GEOFENCE_TRANSITION_DWELL) {
            return true;
        }
        return false;
    }

    private boolean insideWifi(GeofenceModel m) {
        Integer transition = wifiEventMap.get(m.getId());
        if(transition == null) return false;
        if(transition == WIFI_TRANSITION_ENTER) {
            return true;
        }
        return false;
    }

    @Override
    public void addGeoFence(GeofenceModel geofenceModel) {
        geofenceDataSource.saveGeofence(geofenceModel);
        wifiController.addGeoFence(geofenceModel);
        geoController.addGeoFence(geofenceModel);
    }

    @Override
    public void removeGeoFence(GeofenceModel geofenceModel) {
        wifiController.removeGeoFence(geofenceModel);
        geoController.removeGeoFence(geofenceModel);
    }
    
    @Override
    public void onGeofenceEvent(Intent intent) {
        // TODO: 10.08.2017 do not implement this method
    }

    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        executorService.shutdown();
        executorService.shutdownNow();
        geofenceDataSource.removeAllGeofences();
        geoController.onDestroy();
        wifiController.onDestroy();
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

    private void notifyOnEvent(GeofenceModel m, int transitionType, int eventTypeDetector) {
        synchronized (listeners) {
            for (GeofenceEventListener l : listeners) {
                l.onEvent(m, transitionType, eventTypeDetector);
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
