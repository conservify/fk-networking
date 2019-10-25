package org.conservify.networking;

public interface NetworkingListener {
    void onFoundService(ServiceInfo service);
    void onLostService(ServiceInfo service);

    void onNetworkChanged(WifiNetwork network);
    void onNetworksFound(WifiNetworks networks);
}
