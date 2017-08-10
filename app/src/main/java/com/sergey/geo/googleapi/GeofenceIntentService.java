package com.sergey.geo.googleapi;

import android.app.IntentService;
import android.content.Intent;

import com.sergey.geo.googleapi.GeofenceControllerImpl;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class GeofenceIntentService extends IntentService {

    public GeofenceIntentService() {
        super("GeofenceIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        GeofenceControllerImpl.getInstance().onGeofenceEvent(intent);
    }
}
