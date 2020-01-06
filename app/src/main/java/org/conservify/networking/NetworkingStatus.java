package org.conservify.networking;

public class NetworkingStatus {
    private boolean scanError;
    private boolean connected;
    private WifiNetwork connectedWifi;
    private WifiNetworks wifiNetworks;

    public boolean getScanError() {
        return scanError;
    }

    public void setScanError(boolean scanError) {
        this.scanError = scanError;
    }

    public boolean getConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public WifiNetwork getConnectedWifi() {
        return connectedWifi;
    }

    public void setConnectedWifi(WifiNetwork connectedWifi) {
        this.connectedWifi = connectedWifi;
    }

    public WifiNetworks getWifiNetworks() {
        return wifiNetworks;
    }

    public void setWifiNetworks(WifiNetworks wifiNetworks) {
        this.wifiNetworks = wifiNetworks;
    }
}
