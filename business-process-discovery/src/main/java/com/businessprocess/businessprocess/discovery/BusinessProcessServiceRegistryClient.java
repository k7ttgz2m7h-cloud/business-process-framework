package com.businessprocess.businessprocess.discovery;

public interface BusinessProcessServiceRegistryClient {
    BusinessProcessServiceEndpoint fetch(String providerName);
}
