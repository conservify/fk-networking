package org.conservify.networking;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Enumeration;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class ListenForStationGroupTask extends AsyncTask<Void, Void, Boolean> {
    private static final String TAG = "JS";
    private final NetworkingListener networkingListener;
    private final Context context;
    private final DispatchGroup sync;
    private boolean running;

    public ListenForStationGroupTask(NetworkingListener networkingListener, Context context, DispatchGroup sync) {
        this.networkingListener = networkingListener;
        this.context = context;
        this.sync = sync;
    }

    public boolean isRunning() {
        return running;
    }

    public NetworkInterface getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && (inetAddress.getAddress().length == 4)) {
                        Log.i(TAG, "ServiceDiscovery.udp-g: selected interface with ip: "+ inetAddress.getHostAddress());
                        return intf;
                    }
                }
            }
        } catch (SocketException e) {
            Log.w(TAG, "ServiceDiscovery.udp-g: unable to find interface: " + e.toString());
        }
        return null;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        try {
            Log.i(TAG, "ServiceDiscovery.udp-g: starting, acquiring wifi locks");

            running = true;

            WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
            WifiManager.WifiLock wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, TAG);
            WifiManager.MulticastLock multicastLock = wifiManager.createMulticastLock(TAG);
            multicastLock.setReferenceCounted(true);

            wifiLock.acquire();
            multicastLock.acquire();

            Log.i(TAG, "ServiceDiscovery.udp-g: listening " + ServiceDiscovery.UdpMulticastGroup + ":" + ServiceDiscovery.UdpGroupPort);

            try {
                MulticastSocket socket = new MulticastSocket(ServiceDiscovery.UdpGroupPort);
                InetAddress group = InetAddress.getByName(ServiceDiscovery.UdpMulticastGroup);

                socket.setSoTimeout(1000);
                socket.setReuseAddress(true);

                NetworkInterface hostInterface = getLocalIpAddress();
                if (hostInterface != null) {
                    try {
                        socket.setNetworkInterface(hostInterface);
                    } catch (SocketException e) {
                        Log.i(TAG, "ServiceDiscovery.udp-g: open() setNetworkInterface exception: " + e.getMessage());
                    }
                    final SocketAddress multicastAddr = new InetSocketAddress(group, ServiceDiscovery.UdpGroupPort);

                    Log.i(TAG, String.format("ServiceDiscovery.udp-g: trying to joinGroup(%s, %s)", multicastAddr, hostInterface));

                    socket.joinGroup(multicastAddr, hostInterface);
                } else {
                    Log.i(TAG, String.format("ServiceDiscovery.udp-g: trying to joinGroup(%s)", group));
                    socket.joinGroup(group);
                }

                sync.leave();

                try {
                    byte[] buffer = new byte[ServiceDiscovery.UdpMaximumPacketSize];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                    while (!isCancelled()) {
                        try {
                            socket.receive(packet);

                            if (packet.getLength() == 0) {
                                Log.w(TAG, "ServiceDiscovery.udp-g: unexpected empty packet");
                                continue;
                            }

                            if (packet.getLength() == ServiceDiscovery.UdpMaximumPacketSize) {
                                Log.w(TAG, "ServiceDiscovery.udp-g: unexpected huge packet");
                                continue;
                            }

                            byte[] data = packet.getData();
                            InetAddress remote = packet.getAddress();
                            String remoteAddress = remote.getHostAddress();
                            String encoded = Base64.encodeToString(data, 0, packet.getLength(), Base64.NO_WRAP);
                            Log.i(TAG, "ServiceDiscovery.udp-g: length=" + packet.getLength() + " " + remoteAddress +
                                    " b64=" + encoded + " encoded-length=" + encoded.length());
                            networkingListener.onUdpMessage(new JavaUdpMessage(remoteAddress, encoded));
                        }
                        catch (SocketTimeoutException e){
                            Log.d(TAG, "ServiceDiscovery.udp-g: to");
                        }
                    }

                    running = false;
                    sync.leave();
                }
                finally {
                    Log.i(TAG, "ServiceDiscovery.udp-g: shutting down");
                    try {
                        socket.leaveGroup(group);
                    }
                    catch (Exception e) {
                        Log.e(TAG, "ServiceDiscovery.udp-g leaveGroup failed:", e);
                    }
                    try {
                        socket.close();
                    }
                    catch (Exception e) {
                        Log.e(TAG, "ServiceDiscovery.udp-g close failed:", e);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "ServiceDiscovery.udp-g failed: Error code:", e);
            }

            Log.i(TAG, "ServiceDiscovery.udp-g: releasing");
            multicastLock.release();
            wifiLock.release();
        } catch (Exception e) {
            Log.e(TAG, "ServiceDiscovery.udp-g: failed: Error code:", e);
            running = false;
        }

        Log.i(TAG,"ServiceDiscovery.udp-g: stopped");

        return false;
    }
}
