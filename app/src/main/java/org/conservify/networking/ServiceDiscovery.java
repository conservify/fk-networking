package org.conservify.networking;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.support.annotation.RequiresApi;
import android.os.Build;
import android.util.Log;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class ServiceDiscovery {
    private static final String TAG = "JS";

    private final Context context;
    private final NetworkingListener networkingListener;
    private final NsdManager.DiscoveryListener discoveryListener;
    private final NsdManager nsdManager;

    public ServiceDiscovery(Context context, final NetworkingListener networkingListener) {
        this.context = context;
        this.networkingListener = networkingListener;

        discoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
                networkingListener.onStarted();
            }

            private void resolveService(final NsdServiceInfo service) {
                try {
                    nsdManager.resolveService(service, new NsdManager.ResolveListener() {
                        @Override
                        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                            switch (errorCode) {
                                case NsdManager.FAILURE_ALREADY_ACTIVE:
                                    Log.e(TAG, "Resolve failed: " + errorCode + ", trying again.");
                                    resolveService(service);
                                    break;
                                default:
                                    Log.e(TAG, "Resolve failed: " + errorCode);
                                    break;
                            }
                        }

                        @Override
                        public void onServiceResolved(NsdServiceInfo serviceInfo) {
                            Log.e(TAG, "Resolve Succeeded. " + serviceInfo);
                            networkingListener.onFoundService(new ServiceInfo(serviceInfo.getServiceName(), serviceInfo.getServiceType(), serviceInfo.getHost().getHostAddress(), serviceInfo.getPort()));
                        }
                    });
                }
                catch (Exception e) {
                    Log.e(TAG, "ServiceDiscovery.resolve failed: Error code:", e);
                }
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                Log.d(TAG, "onServiceFound: " + service + ", resolving...");
                resolveService(service);
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                Log.e(TAG, "onServiceLost: " + serviceInfo);
                if (serviceInfo.getHost() != null) {
                    networkingListener.onLostService(new ServiceInfo(serviceInfo.getServiceName(), serviceInfo.getServiceType(), serviceInfo.getHost().getHostAddress(), serviceInfo.getPort()));
                }
                else {
                    networkingListener.onLostService(new ServiceInfo(serviceInfo.getServiceName(), serviceInfo.getServiceType(), null, serviceInfo.getPort()));
                }
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                try {
                    Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                    nsdManager.stopServiceDiscovery(this);
                }
                catch (Exception e) {
                    Log.e(TAG, "ServiceDiscovery.stopServiceDiscovery failed: Error code:", e);
                }
                networkingListener.onDiscoveryFailed();
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                try {
                    Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                    nsdManager.stopServiceDiscovery(this);
                }
                catch (Exception e) {
                    Log.e(TAG, "ServiceDiscovery.stopServiceDiscovery failed: Error code:", e);
                }
            }
        };

        nsdManager = (NsdManager)context.getSystemService(Context.NSD_SERVICE);
    }


    public void start(String serviceType) {
        try {
            Log.d(TAG, "ServiceDiscovery.start called");
            nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
        }
        catch (Exception e) {
            Log.e(TAG, "ServiceDiscovery.start failed: Error code:", e);
            networkingListener.onDiscoveryFailed();
        }
    }

    public void stop() {
        try {
            Log.d(TAG, "ServiceDiscovery.stop");
            nsdManager.stopServiceDiscovery(discoveryListener);
        }
        catch (Exception e) {
            Log.e(TAG, "ServiceDiscovery.stop failed: Error code:", e);
        }
    }
}
