package org.conservify.networking;

import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Base64;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class ListenForStationDirectTask extends AsyncTask<Void,Void, Boolean> {
    private static final String TAG = "JS";
    private final NetworkingListener networkingListener;

    public ListenForStationDirectTask(NetworkingListener networkingListener) {
        this.networkingListener = networkingListener;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        try {
            Log.i(TAG, "ServiceDiscovery.udp-d: listening " + "local:" + ServiceDiscovery.UdpDirectPort);
            try {
                byte[] buffer = new byte[ServiceDiscovery.UdpMaximumPacketSize];
                MulticastSocket socket = new MulticastSocket(null);
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                SocketAddress socketAddress = new InetSocketAddress(ServiceDiscovery.UdpDirectPort);
                socket.setReuseAddress(true);
                socket.bind(socketAddress);
                socket.setSoTimeout(1000);

                while (!isCancelled()) {
                    try {
                        socket.receive(packet);

                        if (packet.getLength() == 0) {
                            Log.w(TAG, "ServiceDiscovery.udp-d: empty packet");
                            continue;
                        }

                        if (packet.getLength() >= ServiceDiscovery.UdpMaximumPacketSize) {
                            Log.w(TAG, "ServiceDiscovery.udp-d: huge packet");
                            continue;
                        }

                        byte[] data = packet.getData();
                        InetAddress remote = packet.getAddress();
                        String remoteAddress = remote.getHostAddress();
                        String encoded = Base64.encodeToString(data, 0, packet.getLength(), Base64.NO_WRAP);
                        Log.i(TAG, "ServiceDiscovery.udp-d: length=" + packet.getLength() + " " + remoteAddress +
                                " b64=" + encoded + " encoded-length=" + encoded.length());

                        networkingListener.onUdpMessage(new JavaUdpMessage(remoteAddress, encoded));
                    }
                    catch (SocketTimeoutException e){
                        Log.d(TAG, "ServiceDiscovery.udp-d: to");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "ServiceDiscovery.udp-d: failed: Error code:", e);
            }

            Log.i(TAG,"ServiceDiscovery.udp-d: stopped");

            return true;
        } catch (Exception e) {
            Log.e(TAG, "ServiceDiscovery.udp-d: failed: Error code:", e);
            return false;
        }
    }
}
