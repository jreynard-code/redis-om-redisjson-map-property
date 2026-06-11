package com.example.mapserialization.repository;

import com.example.mapserialization.model.TestSerializationMap;
import com.redis.om.spring.repository.RedisDocumentRepository;

public interface TestSerializationMapRepository extends RedisDocumentRepository<TestSerializationMap, String> {
}
