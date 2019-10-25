package org.conservify.networking;

public class WifiNetwork {
    private final String ssid;

    public String getSsid() {
        return ssid;
    }

    public WifiNetwork(String ssid) {
        this.ssid = ssid;
    }
}
