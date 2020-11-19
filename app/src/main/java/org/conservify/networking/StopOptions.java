package org.conservify.networking;

public class StopOptions {
    private boolean suspending;
    private boolean dns;
    private boolean mdns;

    public boolean isSuspending() {
        return suspending;
    }

    public void setSuspending(boolean suspending) {
        this.suspending = suspending;
    }

    public boolean getDns() {
        return dns;
    }

    public void setDns(boolean dns) {
        this.dns = dns;
    }

    public boolean getMdns() {
        return mdns;
    }

    public void setMdns(boolean mdns) {
        this.mdns = mdns;
    }
}
