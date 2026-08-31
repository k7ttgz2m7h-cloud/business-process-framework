package com.businessprocess.businessprocess.discovery;

public class BusinessProcessServiceEndpoint {
    private String name;
    private String url;
    private String status;

    public BusinessProcessServiceEndpoint() {
    }

    public BusinessProcessServiceEndpoint(String name, String url, String status) {
        this.name = name;
        this.url = url;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
