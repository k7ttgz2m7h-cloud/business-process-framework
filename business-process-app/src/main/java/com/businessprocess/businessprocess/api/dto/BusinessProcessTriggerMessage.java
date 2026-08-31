package com.businessprocess.businessprocess.api.dto;

import tools.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class BusinessProcessTriggerMessage {
    private String businessProcessName;
    private JsonNode inputPayload;
}
