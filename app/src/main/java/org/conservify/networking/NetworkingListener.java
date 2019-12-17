package org.conservify.networking;

public interface NetworkingListener {
    void onStarted();

    void onDiscoveryFailed();
    void onFoundService(ServiceInfo service);
    void onLostService(ServiceInfo service);

    void onConnectionInfo(boolean connected);
    void onConnectedNetwork(WifiNetwork network);
    void onNetworksFound(WifiNetworks networks);
    void onNetworkScanError();
}
