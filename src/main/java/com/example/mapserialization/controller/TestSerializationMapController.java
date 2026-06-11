package com.example.mapserialization.controller;

import com.example.mapserialization.controller.dto.TestSerializationMapRequest;
import com.example.mapserialization.model.TestSerializationMap;
import com.example.mapserialization.service.TestSerializationMapService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "working", description = "Working endpoints")
public class TestSerializationMapController {

    private final TestSerializationMapService service;

    public TestSerializationMapController(TestSerializationMapService service) {
        this.service = service;
    }

    @PostMapping("/test-serialization-map")
    public ResponseEntity<TestSerializationMap> testSerializationMap(@RequestBody TestSerializationMapRequest request) {
        TestSerializationMap result = service.save(request.getValue(), request.getAdditionalData());
        return ResponseEntity.ok(result);
    }
}
