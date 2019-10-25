package org.conservify.networking;

public class ServiceInfo {
    private final String name;
    private final String type;
    private final String address;
    private final int port;

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public ServiceInfo(String name, String type, String address, int port) {
        this.name = name;
        this.type = type;
        this.address = address;
        this.port = port;
    }
}
