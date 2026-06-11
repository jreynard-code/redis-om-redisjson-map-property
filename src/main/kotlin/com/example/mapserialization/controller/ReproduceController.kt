package com.example.mapserialization.controller

import com.example.mapserialization.model.MyEntity
import com.redis.om.spring.convert.MappingRedisOMConverter
import org.springframework.data.redis.core.convert.RedisData
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/**
 * REST controller that demonstrates the MappingException bug.
 *
 * The endpoint reproduces exactly what SimpleRedisDocumentRepository.saveAll() does internally:
 * it calls MappingRedisOMConverter.write(entity, redisData).
 *
 * With spring-data-commons 4.x, this throws:
 *   MappingException: Couldn't find PersistentEntity for type class java.util.LinkedHashMap
 *
 * when the entity's Map field contains nested Map values (which is normal for
 * OpenAPI "additionalProperties: true" fields deserialized by Jackson).
 */
@RestController
class ReproduceController {

    private val converter = MappingRedisOMConverter()

    @PostMapping("/reproduce")
    fun reproduce(@RequestBody request: Map<String, Any>): ResponseEntity<Map<String, Any>> {
        val entity = MyEntity(
            name = request["name"] as? String ?: "test",
            organizationId = "org-1",
            additionalData = (request["additionalData"] as? MutableMap<String, Any>)
        )

        // This is what SimpleRedisDocumentRepository.saveAll() does internally
        // (see redis-om-spring source: SimpleRedisDocumentRepository.java line ~180)
        val redisData = RedisData()
        converter.write(entity, redisData)

        return ResponseEntity.ok(mapOf(
            "status" to "success",
            "bucket_size" to redisData.bucket.size()
        ))
    }
}
