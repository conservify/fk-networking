package org.conservify.networking;

public class JavaUdpMessage {
    private final String address;
    private final String data;

    public String getAddress() {
        return address;
    }

    public String getData() {
        return data;
    }

    public JavaUdpMessage(String address, String data) {
        this.address = address;
        this.data = data;
    }
}
