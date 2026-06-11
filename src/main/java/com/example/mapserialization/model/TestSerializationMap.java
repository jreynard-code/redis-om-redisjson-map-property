package com.example.mapserialization.model;

import com.redis.om.spring.annotations.Document;
import org.springframework.data.annotation.Id;

import java.util.Map;

@Document
public class TestSerializationMap {

    @Id
    private String id;

    private String value;

    private Map<String, Object> additionalData;

    public TestSerializationMap() {
    }

    public TestSerializationMap(String value, Map<String, Object> additionalData) {
        this.value = value;
        this.additionalData = additionalData;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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
