package com.example.mapserialization.controller.dto;

import java.util.Map;

public class TestSerializationMapRequest {

    private String value;
    private Map<String, Object> additionalData;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Map<String, Object> getAdditionalData() {
        return additionalData;
    }

    public void setAdditionalData(Map<String, Object> additionalData) {
        this.additionalData = additionalData;
    }
}
