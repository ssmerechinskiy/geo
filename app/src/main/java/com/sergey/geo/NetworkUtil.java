package com.sergey.geo;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import static android.content.Context.CONNECTIVITY_SERVICE;

/**
 * Created by sober on 31.07.2017.
 */

public class NetworkUtil {

    public static NetworkType updateNetworkInfo() {
        NetworkType networkType = null;
        ConnectivityManager connectivityManager = (ConnectivityManager) GeoApp.getInstance().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetInfo == null) {
            return networkType;
        }
        switch (activeNetInfo.getType()) {
            case ConnectivityManager.TYPE_WIFI: {
                networkType = NetworkType.WIFI;
                WifiManager wifiManager = (WifiManager) GeoApp.getInstance().getSystemService(Context.WIFI_SERVICE);
                WifiInfo connectionInfo = wifiManager.getConnectionInfo();
                if (connectionInfo != null && !connectionInfo.getSSID().equals("")) {
                    networkType.setName(connectionInfo.getSSID());
                }
                break;
            }
            case ConnectivityManager.TYPE_MOBILE: {
                networkType = NetworkType.MOBILE_DATA;
                break;
            }
            default:
                break;
        }
        return networkType;
    }

}
