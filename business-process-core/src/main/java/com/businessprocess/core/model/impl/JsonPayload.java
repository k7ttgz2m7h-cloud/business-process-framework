package com.businessprocess.core.model.impl;

import com.businessprocess.core.model.base.BasePayload;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class JsonPayload extends BasePayload<Map<String, Object>> {

    private String store; // FULL,METADATA_ONLY,NONE
    private String saveAs; // variable name to save the response payload for easier access to extract data from the response payload

    public JsonPayload(Map<String, Object> payload) {
        super(null, payload);
    }

    public JsonPayload(Map<String, Object> metadata, Map<String, Object> payload) {
        super(metadata, payload);
    }
}
