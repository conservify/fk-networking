package org.conservify.networking;

public class StartOptions {
    private String serviceTypeSearch;
    private String serviceNameSelf;
    private String serviceTypeSelf;
    private boolean dns;

    public String getServiceTypeSearch() {
        return serviceTypeSearch;
    }

    public void setServiceTypeSearch(String serviceTypeSearch) {
        this.serviceTypeSearch = serviceTypeSearch;
    }

    public String getServiceNameSelf() {
        return serviceNameSelf;
    }

    public void setServiceNameSelf(String serviceNameSelf) {
        this.serviceNameSelf = serviceNameSelf;
    }

    public String getServiceTypeSelf() {
        return serviceTypeSelf;
    }

    public void setServiceTypeSelf(String serviceTypeSelf) {
        this.serviceTypeSelf = serviceTypeSelf;
    }

    public boolean getDns() {
        return dns;
    }

    public void setDns(boolean dns) {
        this.dns = dns;
    }
}
