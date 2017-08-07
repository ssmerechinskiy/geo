package com.sergey.geo;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

/**
 * Created by sober on 31.07.2017.
 */

public class NetworkUtil {

    public static Network updateNetworkInfo() {
        Network network = null;
        ConnectivityManager connectivityManager = (ConnectivityManager) GeoApp.getInstance().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetInfo == null) {
            return network;
        }
        switch (activeNetInfo.getType()) {
            case ConnectivityManager.TYPE_WIFI: {
                network = Network.WIFI;
                WifiManager wifiManager = (WifiManager) GeoApp.getInstance().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                WifiInfo connectionInfo = wifiManager.getConnectionInfo();
                if (connectionInfo != null && !connectionInfo.getSSID().equals("")) {
                    network.setName(connectionInfo.getSSID());
                }
                break;
            }
            case ConnectivityManager.TYPE_MOBILE: {
                network = Network.MOBILE_DATA;
                break;
            }
            default:
                break;
        }
        return network;
    }

}
