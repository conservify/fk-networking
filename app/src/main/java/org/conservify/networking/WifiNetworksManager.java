package org.conservify.networking;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 Android 8.0 and Android 8.1:

 Each background app can scan one time in a 30-minute period.

 Android 9:

 Each foreground app can scan four times in a 2-minute period. This allows for a burst of scans in a short time.

 All background apps combined can scan one time in a 30-minute period.

 Android 10 and higher:

 The same throttling limits from Android 9 apply. There is a new developer option to toggle the throttling
 off for local testing (under Developer Options > Networking > Wi-Fi scan throttling).
 */

public class WifiNetworksManager {
    private static final String TAG = "JS";

    private final Context context;
    private final NetworkingListener networkingListener;

    public WifiNetworksManager(Context context, NetworkingListener networkingListener) {
        this.context = context;
        this.networkingListener = networkingListener;
    }

    public void findConnectedNetwork() {
        final ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        final NetworkingStatus status = new NetworkingStatus();


        if (networkInfo.isConnected()) {
            final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            if (connectionInfo != null) {
                WifiNetwork network = new WifiNetwork(connectionInfo.getSSID());
                status.setConnectedWifi(network);
            }

            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            status.setConnected(activeNetworkInfo.isConnected());
        }

        this.networkingListener.onNetworkStatus(status);
    }

    public void scan() {
        final WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);

        BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) {
                final NetworkingStatus status = new NetworkingStatus();
                boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
                if (success) {
                    List<ScanResult> results = wifiManager.getScanResults();
                    List<WifiNetwork> list = new ArrayList<WifiNetwork>();
                    for (ScanResult sr : results) {
                        list.add(new WifiNetwork(sr.SSID));
                        Log.i(TAG, "network: " + sr.SSID);
                    }
                    status.setWifiNetworks(new WifiNetworks(list));
                } else {
                    status.setScanError(true);
                }

                networkingListener.onNetworkStatus(status);
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        context.registerReceiver(wifiScanReceiver, intentFilter);

        if (!wifiManager.startScan()) {
            final NetworkingStatus status = new NetworkingStatus();
            status.setScanError(true);
            this.networkingListener.onNetworkStatus(status);
        }
    }
}
