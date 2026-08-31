package com.businessprocess.businessprocessserviceregistry.model;

public class BusinessProcessServiceEndpoint {
    private String name;
    private String host;
    private int port;
    private String healthPath;
    private String protocol = "http";
    private long timeoutMs = 2000;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getHealthPath() {
        return healthPath;
    }

    public void setHealthPath(String healthPath) {
        this.healthPath = healthPath;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public String url() {
        String path = healthPath != null && healthPath.startsWith("/") ? healthPath : "/" + healthPath;
        return protocol + "://" + host + ":" + port + path;
    }
}
