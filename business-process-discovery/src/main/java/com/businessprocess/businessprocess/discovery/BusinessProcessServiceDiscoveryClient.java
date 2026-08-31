package com.businessprocess.businessprocess.discovery;

public interface BusinessProcessServiceDiscoveryClient {
    BusinessProcessServiceEndpoint resolve(String providerName);
}
