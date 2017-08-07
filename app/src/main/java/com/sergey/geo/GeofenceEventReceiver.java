package com.sergey.geo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class GeofenceEventReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        GeofenceControllerImpl.getInstance().onGeofenceEvent(intent);
    }
}
