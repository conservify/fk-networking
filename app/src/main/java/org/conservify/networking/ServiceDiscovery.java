package org.conservify.networking;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.support.annotation.RequiresApi;
import android.os.Build;
import android.util.Log;

import java.net.InetAddress;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class ServiceDiscovery {
    private static final String TAG = "JS";

    private final Context context;
    private final NetworkingListener networkingListener;
    private final NsdManager.DiscoveryListener discoveryListener;
    private final NsdManager.ResolveListener resolveListener;
    private final NsdManager nsdManager;

    public ServiceDiscovery(Context context, NetworkingListener networkingListener) {
        this.context = context;
        this.networkingListener = networkingListener;

        resolveListener = new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "Resolve failed: " + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.e(TAG, "Resolve Succeeded. " + serviceInfo);
                int port = serviceInfo.getPort();
                InetAddress host = serviceInfo.getHost();
            }
        };

        discoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                Log.d(TAG, "onServiceFound: " + service);
                nsdManager.resolveService(service, resolveListener);
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                Log.e(TAG, "onServiceLost: " + service);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                nsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                nsdManager.stopServiceDiscovery(this);
            }
        };

        nsdManager = (NsdManager)context.getSystemService(Context.NSD_SERVICE);
    }


    public void start(/*Callback*/) {
        Log.d(TAG, "ServiceDiscovery.start called");

        nsdManager.discoverServices("_fk._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }

    void stop() {
        Log.d(TAG, "ServiceDiscovery.stop");
        nsdManager.stopServiceDiscovery(discoveryListener);
    }
}
