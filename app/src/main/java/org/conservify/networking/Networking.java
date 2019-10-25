package org.conservify.networking;

import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class Networking {
    private final ServiceDiscovery serviceDiscovery;
    private final Web web;
    private final Context context;

    public ServiceDiscovery getServiceDiscovery() {
        return serviceDiscovery;
    }

    public Web getWeb() {
        return this.web;
    }

    public Networking(Context context, NetworkingListener networkingListener, WebTransferListener uploadListener, WebTransferListener downloadListener) {
        this.context = context;
        this.serviceDiscovery = new ServiceDiscovery(context, networkingListener);
        this.web = new Web(context, uploadListener, downloadListener);
    }
}
