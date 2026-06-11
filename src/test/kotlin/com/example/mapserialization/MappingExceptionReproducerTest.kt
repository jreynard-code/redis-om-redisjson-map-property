package com.example.mapserialization

import com.example.mapserialization.model.MyEntity
import com.redis.om.spring.convert.MappingRedisOMConverter
import com.redis.om.spring.convert.RedisOMCustomConversions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.data.mapping.MappingException
import org.springframework.data.redis.core.convert.RedisData
import java.util.LinkedHashMap

/**
 * Reproduces: "Couldn't find PersistentEntity for type class java.util.LinkedHashMap"
 *
 * Environment:
 *   - Spring Boot 4.0.6 (spring-data-commons 4.0.5, spring-data-redis 4.0.5)
 *   - Redis OM Spring 2.0.4
 *
 * Root cause:
 *   spring-data-commons 4.x no longer creates PersistentEntity for JDK Map types.
 *   MappingRedisOMConverter.writeMap() iterates map entry values and for nested maps
 *   calls writeInternal() → getRequiredPersistentEntity(LinkedHashMap.class) → throws.
 *
 * Code path:
 *   SimpleRedisDocumentRepository.saveAll()
 *     → mappingConverter.write(entity, redisData)
 *       → writeEntity() → writeInternal() → detects isMap()
 *         → writeMap() → for each entry value that is a Map:
 *           → hasCustomWriteTarget(LinkedHashMap.class) = false
 *           → writeInternal(LinkedHashMap instance)
 *             → getRequiredPersistentEntity(LinkedHashMap.class) → THROWS
 */
@DisplayName("Redis OM + spring-data-commons 4.x: Map serialization bug")
class MappingExceptionReproducerTest {

    @Test
    @DisplayName("RedisOMCustomConversions has no write converter for LinkedHashMap")
    fun `hasCustomWriteTarget returns false for LinkedHashMap`() {
        val conversions = RedisOMCustomConversions()
        val result = conversions.hasCustomWriteTarget(LinkedHashMap::class.java)
        assert(!result) {
            "Expected hasCustomWriteTarget(LinkedHashMap) = false. " +
                "RedisOMCustomConversions does not register a converter for Map types."
        }
    }

    @Test
    @DisplayName("BUG: write() throws MappingException for entity with nested Map values")
    fun `write entity with nested map in additionalData throws MappingException`() {
        val converter = MappingRedisOMConverter()

        // Simulate what Jackson produces when deserializing {"key": {"nested": "value"}}
        val nestedMap = LinkedHashMap<String, Any>()
        nestedMap["nested_key"] = "nested_value"
        nestedMap["nested_number"] = 42

        val entity = MyEntity(
            name = "test-entity",
            organizationId = "org-1",
            additionalData = mutableMapOf(
                "simple_string" to "hello",
                "nested_object" to nestedMap  // ← This triggers the bug
            )
        )

        val exception = assertThrows<MappingException> {
            converter.write(entity, RedisData())
        }

        assert(exception.message!!.contains("Couldn't find PersistentEntity")) {
            "Expected 'Couldn't find PersistentEntity' but got: ${exception.message}"
        }
        assert(exception.message!!.contains("LinkedHashMap")) {
            "Expected error to mention LinkedHashMap but got: ${exception.message}"
        }
    }

    @Test
    @DisplayName("write() works fine when map values are simple types (no nested maps)")
    fun `write entity with flat map values does not throw`() {
        val converter = MappingRedisOMConverter()

        val entity = MyEntity(
            name = "test-entity",
            organizationId = "org-1",
            additionalData = mutableMapOf(
                "key1" to "string_value",
                "key2" to 123,
                "key3" to true
            )
        )

        // No nested maps → no PersistentEntity lookup → no crash
        assertDoesNotThrow {
            converter.write(entity, RedisData())
        }
    }
}
