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

    private ListenForStationDirectTask listenDirectTask;
    private ListenForStationGroupTask listenGroupTask;
    private boolean registered = false;

    public void start(String serviceTypeSearch, String serviceNameSelf, String serviceTypeSelf) {
        try {
            Log.d(TAG, "ServiceDiscovery.start called");

            if (listenGroupTask == null) {
                Log.d(TAG, "ServiceDiscovery.listeners udp-g");
                listenGroupTask = new ListenForStationGroupTask();
                listenGroupTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }

            if (listenDirectTask == null) {
                Log.d(TAG, "ServiceDiscovery.listeners udp-d");
                listenDirectTask = new ListenForStationDirectTask();
                listenDirectTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }

            if (!registered && serviceNameSelf != null && serviceNameSelf.length() > 0 && serviceTypeSelf != null && serviceTypeSelf.length() > 0) {
                Log.d(TAG, "ServiceDiscovery.registering: " + serviceTypeSelf + " " + serviceNameSelf);
                NsdServiceInfo info = new NsdServiceInfo();
                info.setServiceName(serviceNameSelf);
                info.setServiceType(serviceTypeSelf);
                info.setPort(UdpGroupPort);
                nsdManager.registerService(info, NsdManager.PROTOCOL_DNS_SD, registrationListener);
                registered = true;
            }

            if (serviceTypeSearch != null && serviceTypeSearch.length() > 0) {
                nsdManager.discoverServices(serviceTypeSearch, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
            }
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

	private static final String UdpMulticastGroup = "224.1.2.3";
	private static final short UdpGroupPort = 22143;
    private static final short UdpDirectPort = UdpGroupPort + 1;
	private static final int UdpMaximumPacketSize = 512;

    public class ListenForStationDirectTask extends AsyncTask<Void,Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                Log.i(TAG, "udp: listening " + "local:" + UdpDirectPort);
                try {
                    byte[] buffer = new byte[UdpMaximumPacketSize];
                    MulticastSocket socket = new MulticastSocket(null);
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    SocketAddress socketAddress = new InetSocketAddress(UdpDirectPort);
                    socket.setReuseAddress(true);
                    socket.bind(socketAddress);

                    while (true) {
                        socket.receive(packet);

                        if (packet.getLength() == 0) {
                            Log.w(TAG, "udp: unexpected empty packet");
                            continue;
                        }

                        if (packet.getLength() == UdpMaximumPacketSize) {
                            Log.w(TAG, "udp: unexpected huge packet");
                            continue;
                        }

                        byte[] data = packet.getData();
                        InetAddress remote = packet.getAddress();
                        String remoteAddress = remote.getHostAddress();
                        String encoded = Base64.encodeToString(data, 0, packet.getLength(), Base64.NO_WRAP);
                        Log.i(TAG, "udp: length=" + packet.getLength() + " " + remoteAddress +
                                " b64=" + encoded + " encoded-length=" + encoded.length());
                        networkingListener.onUdpMessage(new JavaUdpMessage(remoteAddress, encoded));
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

                Log.i(TAG, "udp: listening " + UdpMulticastGroup + ":" + UdpGroupPort);

                try {
                    MulticastSocket socket = new MulticastSocket(UdpGroupPort);
                    InetAddress group = InetAddress.getByName(UdpMulticastGroup);
                    socket.joinGroup(group);

                    try {
                        byte[] buffer = new byte[UdpMaximumPacketSize];
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                        while (true) {
                            socket.receive(packet);

                            if (packet.getLength() == 0) {
                                Log.w(TAG, "udp: unexpected empty packet");
								continue;
                            }

                            if (packet.getLength() == UdpMaximumPacketSize) {
                                Log.w(TAG, "udp: unexpected huge packet");
								continue;
                            }

							byte[] data = packet.getData();
							InetAddress remote = packet.getAddress();
							String remoteAddress = remote.getHostAddress();
							String encoded = Base64.encodeToString(data, 0, packet.getLength(), Base64.NO_WRAP);
							Log.i(TAG, "udp: length=" + packet.getLength() + " " + remoteAddress +
								  " b64=" + encoded + " encoded-length=" + encoded.length());
							networkingListener.onUdpMessage(new JavaUdpMessage(remoteAddress, encoded));
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
