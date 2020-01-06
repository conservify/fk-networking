package org.conservify.networking;

public interface NetworkingListener {
    void onStarted();

    void onDiscoveryFailed();
    void onFoundService(ServiceInfo service);
    void onLostService(ServiceInfo service);

    void onNetworkStatus(NetworkingStatus status);
}
