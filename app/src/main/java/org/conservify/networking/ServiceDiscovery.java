package org.conservify.networking;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.AsyncTask;
import android.support.annotation.RequiresApi;
import android.os.Build;
import android.util.Log;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class ServiceDiscovery {
    public static final String UdpMulticastGroup = "224.1.2.3";
    public static final short UdpGroupPort = 22143;
    public static final short UdpDirectPort = UdpGroupPort + 1;
    public static final int UdpMaximumPacketSize = 512;

    private static final String TAG = "JS";
    private final Context context;
    private final NetworkingListener networkingListener;
    private final NsdManager.DiscoveryListener discoveryListener;
    private final NsdManager.RegistrationListener registrationListener;
    private final NsdManager nsdManager;
    private ListenForStationDirectTask listenDirectTask;
    private ListenForStationGroupTask listenGroupTask;
    private boolean registered = false;
    private boolean discovering = false;

    public ServiceDiscovery(Context context, final NetworkingListener networkingListener) {
        this.context = context;
        this.networkingListener = networkingListener;

        registrationListener =  new NsdManager.RegistrationListener() {
            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.d(TAG, "ServiceDiscovery service registration failed");
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.d(TAG, "ServiceDiscovery service un-registration failed");
            }

            @Override
            public void onServiceRegistered(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "ServiceDiscovery service registered");
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "ServiceDiscovery service un-registered");
            }
        };

        discoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "ServiceDiscovery started");
                networkingListener.onStarted();
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "ServiceDiscovery stopped: " + serviceType);
                networkingListener.onStopped();
            }

            private void resolveService(final NsdServiceInfo service) {
                try {
                    nsdManager.resolveService(service, new NsdManager.ResolveListener() {
                        @Override
                        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                            switch (errorCode) {
                                case NsdManager.FAILURE_ALREADY_ACTIVE:
                                    Log.e(TAG, "ServiceDiscovery resolve failed: " + errorCode + ", trying again.");
                                    if (errorCode != NsdManager.FAILURE_ALREADY_ACTIVE) {
                                        resolveService(service);
                                    }
                                    break;
                                default:
                                    Log.e(TAG, "ServiceDiscovery resolve failed: " + errorCode);
                                    break;
                            }
                        }

                        @Override
                        public void onServiceResolved(NsdServiceInfo serviceInfo) {
                            Log.e(TAG, "ServiceDiscovery resolve succeeded. " + serviceInfo);
                            networkingListener.onFoundService(new ServiceInfo(serviceInfo.getServiceName(), serviceInfo.getServiceType(), serviceInfo.getHost().getHostAddress(), serviceInfo.getPort()));
                        }
                    });
                }
                catch (Exception e) {
                    Log.e(TAG, "ServiceDiscovery resolve failed:", e);
                }
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                Log.d(TAG, "ServiceDiscovery onServiceFound: " + service + ", resolving...");
                resolveService(service);
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                Log.e(TAG, "ServiceDiscovery onServiceLost: " + serviceInfo);
                if (serviceInfo.getHost() != null) {
                    networkingListener.onLostService(new ServiceInfo(serviceInfo.getServiceName(), serviceInfo.getServiceType(), serviceInfo.getHost().getHostAddress(), serviceInfo.getPort()));
                }
                else {
                    networkingListener.onLostService(new ServiceInfo(serviceInfo.getServiceName(), serviceInfo.getServiceType(), null, serviceInfo.getPort()));
                }
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                try {
                    Log.e(TAG, "ServiceDiscovery discovery failed:" + errorCode);
                    nsdManager.stopServiceDiscovery(this);
                }
                catch (Exception e) {
                    Log.e(TAG, "ServiceDiscovery stopServiceDiscovery failed:", e);
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
                networkingListener.onStopped();
            }
        };

        nsdManager = (NsdManager)context.getSystemService(Context.NSD_SERVICE);
    }

    public void start(String serviceTypeSearch, String serviceNameSelf, String serviceTypeSelf) {
        try {
            Log.d(TAG, "ServiceDiscovery.start called");

            if (!discovering && serviceTypeSearch != null && serviceTypeSearch.length() > 0) {
                nsdManager.discoverServices(serviceTypeSearch, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
                discovering = true;
            }
            else {
                Log.d(TAG, String.format("ServiceDiscovery skip discoveryServices: {}", discovering));
            }

            if (listenGroupTask == null) {
                Log.d(TAG, "ServiceDiscovery listeners udp-g");
                listenGroupTask = new ListenForStationGroupTask(networkingListener, context);
                listenGroupTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
            else {
                Log.d(TAG, "ServiceDiscovery listeners udp-g already running");
            }

            if (listenDirectTask == null) {
                Log.d(TAG, "ServiceDiscovery listeners udp-d");
                listenDirectTask = new ListenForStationDirectTask(this. networkingListener);
                listenDirectTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
            else {
                Log.d(TAG, "ServiceDiscovery listeners udp-d already running");
            }

            if (!registered && serviceNameSelf != null && serviceNameSelf.length() > 0 && serviceTypeSelf != null && serviceTypeSelf.length() > 0) {
                Log.d(TAG, "ServiceDiscovery registering: " + serviceTypeSelf + " " + serviceNameSelf);
                NsdServiceInfo info = new NsdServiceInfo();
                info.setServiceName(serviceNameSelf);
                info.setServiceType(serviceTypeSelf);
                info.setPort(UdpGroupPort);
                nsdManager.registerService(info, NsdManager.PROTOCOL_DNS_SD, registrationListener);
                registered = true;
            }
            else {
                Log.d(TAG, "ServiceDiscovery already registered");
            }
        }
        catch (Exception e) {
            Log.e(TAG, "ServiceDiscovery.start failed:", e);
            networkingListener.onDiscoveryFailed();
        }
    }

    public void stop() {
        try {
            if (registered) {
                Log.d(TAG, "ServiceDiscovery.stop");
                nsdManager.unregisterService(registrationListener);
            }
        }
        catch (Exception e) {
            Log.e(TAG, "ServiceDiscovery.stop unregisterService failed:", e);
        }
        finally {
            registered = false;
        }

        try {
            if (listenDirectTask != null) {
                Log.d(TAG, "ServiceDiscovery.cancel udp-d");
                listenDirectTask.cancel(true);
            }
        }
        catch (Exception e) {
            Log.e(TAG, "ServiceDiscovery.stop udp-d:", e);
        }
        finally {
            listenDirectTask = null;
        }

        try {
            if (listenGroupTask != null) {
                Log.d(TAG, "ServiceDiscovery.cancel udp-g");
                listenGroupTask.cancel(true);
            }
        }
        catch (Exception e) {
            Log.e(TAG, "ServiceDiscovery.stop udp-g:", e);
        }
        finally {
            listenGroupTask = null;
        }

        try {
            if (discovering) {
                Log.d(TAG, "ServiceDiscovery.stop");
                nsdManager.stopServiceDiscovery(discoveryListener);
            }
        }
        catch (Exception e) {
            Log.e(TAG, "ServiceDiscovery.stop stopServiceDiscovery failed:", e);
            networkingListener.onStopped();
        }
        finally {
            discovering = false;
        }
    }

}
