package org.conservify.networking;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.AsyncTask;
import android.support.annotation.RequiresApi;
import android.os.Build;
import android.util.Log;

import java.util.concurrent.Semaphore;

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
    private Semaphore lock = new Semaphore(1);
    private DispatchGroup dg = new DispatchGroup();
    private boolean registered = false;
    private boolean discovering = false;
    private boolean publishingError = false;
    private boolean registrationError = false;
    private boolean discoveringError = false;

    private void clearErrors() {
        publishingError = false;
        registrationError = false;
        discoveringError = false;
    }

    public ServiceDiscovery(Context context, final NetworkingListener networkingListener) {
        this.context = context;
        this.networkingListener = networkingListener;

        registrationListener =  new NsdManager.RegistrationListener() {
            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.d(TAG, "ServiceDiscovery service registration failed");
                registrationError = true;
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.d(TAG, "ServiceDiscovery service un-registration failed");
                registrationError = true;
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
                Log.d(TAG, "ServiceDiscovery started: " + regType);
                dg.leave();
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "ServiceDiscovery stopped: " + serviceType);
                dg.leave();
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
                    discoveringError = true;
                }
                catch (Exception e) {
                    Log.e(TAG, "ServiceDiscovery stopServiceDiscovery failed:", e);
                }
                finally {
                    dg.leave();
                }
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                try {
                    Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                    nsdManager.stopServiceDiscovery(this);
                    discoveringError = true;
                }
                catch (Exception e) {
                    Log.e(TAG, "ServiceDiscovery.stopServiceDiscovery failed: Error code:", e);
                }
                finally {
                    dg.leave();
                }
            }
        };

        nsdManager = (NsdManager)context.getSystemService(Context.NSD_SERVICE);
    }

    public void start(StartOptions options) {
        String serviceTypeSearch = options.getServiceTypeSearch();
        String serviceNameSelf = options.getServiceNameSelf();
        String serviceTypeSelf = options.getServiceTypeSelf();

        Log.d(TAG, "ServiceDiscovery starting (acquire)");

        try {
            lock.acquire();
        } catch (InterruptedException e) {
            Log.d(TAG, "ServiceDiscovery starting (acquire failed");
            return;
        }

        Log.d(TAG, "ServiceDiscovery starting");

        clearErrors();
        dg.reset();

        try {
            Log.d(TAG, "ServiceDiscovery.start called");

            if (!discovering && serviceTypeSearch != null && serviceTypeSearch.length() > 0) {
                Log.d(TAG, "ServiceDiscovery discovery starting");
                dg.enter();
                nsdManager.discoverServices(serviceTypeSearch, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
                discovering = true;
            }
            else {
                Log.d(TAG, String.format("ServiceDiscovery skip discovering: %s / %s", discovering, serviceTypeSearch));
            }

            if (listenGroupTask == null) {
                Log.d(TAG, "ServiceDiscovery listeners udp-g");
                dg.enter();
                listenGroupTask = new ListenForStationGroupTask(networkingListener, context, dg);
                listenGroupTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
            else {
                Log.d(TAG, "ServiceDiscovery listeners udp-g already running");
            }

            if (listenDirectTask == null) {
                Log.d(TAG, "ServiceDiscovery listeners udp-d");
                dg.enter();
                listenDirectTask = new ListenForStationDirectTask(this. networkingListener, dg);
                listenDirectTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
            else {
                Log.d(TAG, "ServiceDiscovery listeners udp-d already running");
            }

            if (serviceNameSelf != null && serviceNameSelf.length() > 0 && serviceTypeSelf != null && serviceTypeSelf.length() > 0) {
                if (!registered) {
                    Log.d(TAG, "ServiceDiscovery registering: " + serviceTypeSelf + " " + serviceNameSelf);
                    NsdServiceInfo info = new NsdServiceInfo();
                    info.setServiceName(serviceNameSelf);
                    info.setServiceType(serviceTypeSelf);
                    info.setPort(UdpGroupPort);
                    dg.enter();
                    nsdManager.registerService(info, NsdManager.PROTOCOL_DNS_SD, registrationListener);
                    registered = true;
                }
                else {
                    Log.d(TAG, "ServiceDiscovery already registered");
                }
            }
            else {
                Log.d(TAG, "ServiceDiscovery registering skipped");
            }

            dg.notify(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, String.format("ServiceDiscovery start notified"));
                    lock.release();
                    networkingListener.onStarted();
                    Log.i(TAG, String.format("ServiceDiscovery start finished"));
                }
            });
        }
        catch (Exception e) {
            Log.e(TAG, "ServiceDiscovery.start failed:", e);
            networkingListener.onDiscoveryFailed();
        }
        finally {
            Log.i(TAG, String.format("ServiceDiscovery start exiting"));
        }
    }

    public void stop(StopOptions options) {
        Log.d(TAG, "ServiceDiscovery stopping (acquire)");

        try {
            lock.acquire();
        } catch (InterruptedException e) {
            Log.d(TAG, "ServiceDiscovery starting (acquire failed");
            return;
        }

        Log.d(TAG, "ServiceDiscovery stopping");

        clearErrors();
        dg.reset();

        try {
            if (registered) {
                Log.d(TAG, "ServiceDiscovery.stop");
                dg.enter();
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
            if (listenDirectTask != null && listenDirectTask.isRunning()) {
                Log.d(TAG, "ServiceDiscovery.cancel udp-d");
                dg.enter();
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
            if (listenGroupTask != null && listenGroupTask.isRunning()) {
                Log.d(TAG, "ServiceDiscovery.cancel udp-g");
                dg.enter();
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
                Log.d(TAG, "ServiceDiscovery.stop finished");
                dg.enter();
                nsdManager.stopServiceDiscovery(discoveryListener);
            }
        }
        catch (Exception e) {
            Log.e(TAG, "ServiceDiscovery.stop stopServiceDiscovery failed:", e);
        }
        finally {
            discovering = false;
        }

        dg.notify(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "ServiceDiscovery stopped notified");
                lock.release();
                networkingListener.onStopped();
                Log.i(TAG, "ServiceDiscovery stopped finished");
            }
        });

        Log.i(TAG, "ServiceDiscovery stopped exiting");
    }

}
