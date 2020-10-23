package org.conservify.networking;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.annotation.RequiresApi;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import org.conservify.data.FileInfo;
import org.conservify.data.PbFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class ServiceDiscovery {
    private static final String TAG = "JS";

    private final Context context;
    private final NetworkingListener networkingListener;
    private final NsdManager.DiscoveryListener discoveryListener;
    private final NsdManager.RegistrationListener registrationListener;
    private final NsdManager nsdManager;

    public ServiceDiscovery(Context context, final NetworkingListener networkingListener) {
        this.context = context;
        this.networkingListener = networkingListener;

        registrationListener =  new NsdManager.RegistrationListener() {
            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.d(TAG, "service registration failed");
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.d(TAG, "service un-registration failed");
            }

            @Override
            public void onServiceRegistered(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "service registered");
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "service un-registered");
            }
        };

        discoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
                networkingListener.onStarted();
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
                networkingListener.onStopped();
            }

            private void resolveService(final NsdServiceInfo service) {
                try {
                    nsdManager.resolveService(service, new NsdManager.ResolveListener() {
                        @Override
                        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                            switch (errorCode) {
                                case NsdManager.FAILURE_ALREADY_ACTIVE:
                                    Log.e(TAG, "Resolve failed: " + errorCode + ", trying again.");
                                    if (errorCode != NsdManager.FAILURE_ALREADY_ACTIVE) {
                                        resolveService(service);
                                    }
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

    private ListenForStationGroupTask listenGroupTask;
    private ListenForStationTask listenTask;
    private boolean initialized;

    public void start(String serviceType) {
        try {
            Log.d(TAG, "ServiceDiscovery.start called");
            if (!initialized) {
                Log.d(TAG, "ServiceDiscovery.registering");
                NsdServiceInfo info = new NsdServiceInfo();
                info.setServiceName("jacob-phone");
                info.setServiceType("_http._tcp");
                info.setPort(80);
                nsdManager.registerService(info, NsdManager.PROTOCOL_DNS_SD, registrationListener);

                Log.d(TAG, "ServiceDiscovery.listeners");
                // listenTask = new ListenForStationTask();
                // listenTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                listenGroupTask = new ListenForStationGroupTask();
                listenGroupTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                initialized = true;
            }
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

    public class ListenForStationTask extends AsyncTask<Void,Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                Log.i(TAG,"udp: listening: " + 11000);
                try {
                    byte[] buffer = new byte[2048];
                    MulticastSocket socket = new MulticastSocket(null);
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    SocketAddress socketAddress = new InetSocketAddress(11000);
                    socket.setReuseAddress(true);
                    socket.bind(socketAddress);

                    while (true) {
                        socket.receive(packet);
                        Log.i(TAG,"udp: received: " + packet.getLength());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "ServiceDiscovery.udp failed: Error code:", e);
                }

                Log.i(TAG,"udp: stopped");

                return true;
            } catch (Exception e) {
                Log.e(TAG, "ServiceDiscovery.udp failed: Error code:", e);
                return false;
            }
        }
    }

    public class ListenForStationGroupTask extends AsyncTask<Void,Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                Log.i(TAG, "udp: starting, acquiring wifi locks");

                WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
                WifiManager.WifiLock wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, TAG);
                WifiManager.MulticastLock multicastLock = wifiManager.createMulticastLock(TAG);
                multicastLock.setReferenceCounted(true);

                wifiLock.acquire();
                multicastLock.acquire();

                Log.i(TAG, "udp: listening: " + 22143);

                try {
                    MulticastSocket socket = new MulticastSocket(22143);

                    InetAddress group = InetAddress.getByName("224.1.2.3");
                    socket.joinGroup(group);

                    try {
                        DatagramPacket packet;
                        byte[] buf = new byte[256];
                        packet = new DatagramPacket(buf, buf.length);

                        while (true) {
                            socket.receive(packet);

                            if (packet.getLength() == 0) {
                                Log.w(TAG, "udp: unexpected empty packet");
                            }
                            else {
                                byte[] data = packet.getData();
                                InetAddress remote = packet.getAddress();
                                String remoteAddress = remote.getHostAddress();
                                String encoded = Base64.encodeToString(data, 0, packet.getLength(), Base64.NO_WRAP);
                                Log.i(TAG, "udp! " + packet.getLength() + " " + remoteAddress + " " + encoded + " len=" + encoded.length());
                                networkingListener.onSimpleDiscovery(new ServiceInfo(encoded, "udp", remoteAddress, 80));
                            }
                        }
                    }
                    finally {
                        socket.leaveGroup(group);
                        socket.close();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "ServiceDiscovery.udp failed: Error code:", e);
                }

                multicastLock.release();
                wifiLock.release();
            } catch (Exception e) {
                Log.e(TAG, "ServiceDiscovery.udp failed: Error code:", e);
            }

            return false;
        }
    }

}
