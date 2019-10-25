package org.conservify.networking;

import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class Networking {
    private final ServiceDiscovery serviceDiscovery;
    private final Web web;
    private final WifiNetworksManager wifi;
    private final Context context;

    public ServiceDiscovery getServiceDiscovery() {
        return serviceDiscovery;
    }

    public Web getWeb() {
        return this.web;
    }

    public WifiNetworksManager getWifi() {
        return wifi;
    }

    public Networking(Context context, NetworkingListener networkingListener, WebTransferListener uploadListener, WebTransferListener downloadListener) {
        if (networkingListener == null) throw new IllegalArgumentException();
        if (uploadListener == null) throw new IllegalArgumentException();
        if (downloadListener == null) throw new IllegalArgumentException();

        this.context = context;
        this.serviceDiscovery = new ServiceDiscovery(context, networkingListener);
        this.web = new Web(context, uploadListener, downloadListener);
        this.wifi = new WifiNetworksManager(context, networkingListener);
    }
}
