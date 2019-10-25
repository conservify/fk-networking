package org.conservify.networking;

import java.util.List;

public class WifiNetworks {
    private final List<WifiNetwork> networks;

    public List<WifiNetwork> getNetworks() {
        return networks;
    }

    public WifiNetworks(List<WifiNetwork> networks) {
        this.networks = networks;
    }

}
