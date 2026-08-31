package com.businessprocess.engine.template.identity;

import java.util.Map;

public interface BusinessProcessIdentityResolver {
    void resolveIdentity(Map<String, Object> businessProcessTemplate);
}
