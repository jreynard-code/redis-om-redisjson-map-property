package com.example.mapserialization.service;

import com.example.mapserialization.model.TestSerializationMap;
import com.example.mapserialization.repository.TestSerializationMapRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TestSerializationMapService {

    private final TestSerializationMapRepository repository;

    public TestSerializationMapService(TestSerializationMapRepository repository) {
        this.repository = repository;
    }

    public TestSerializationMap save(String value, Map<String, Object> additionalData) {
        TestSerializationMap entity = new TestSerializationMap(value, additionalData);
        return repository.save(entity);
    }
}
