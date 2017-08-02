package com.sergey.geo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class GeoFenceEventReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        GeoApp.getInstance().getGeoFenceController().onGeofenceEvent(intent);
    }
}
