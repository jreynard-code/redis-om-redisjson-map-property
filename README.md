# Map Serialization with Redis OM Spring

## Problem

When using `Map<String, Object>` (or `Map<String, Any>`) fields in entities annotated with `@Document` in Redis OM Spring, a persistence error occurs:

```
Couldn't find PersistentEntity for type class java.util.LinkedHashMap
```

### Root Cause

This issue is caused by a change in **Spring Data Commons** (starting from v4.0.x, included in Spring Boot 4.x):

Spring Data Commons no longer creates `PersistentEntity` instances for standard JDK types such as `Map`, `LinkedHashMap`, `List`, etc.

The problematic execution flow in Redis OM Spring is as follows:

1. `MappingRedisOMConverter.writeMap()` is called to serialize a `Map` field
2. For each map entry value, `writeInternal()` is invoked
3. `writeInternal()` calls `getRequiredPersistentEntity(LinkedHashMap.class)`
4. Spring Data Commons v4+ cannot find a `PersistentEntity` for `LinkedHashMap` and throws an exception

### Affected Versions

| Redis OM Spring | Spring Boot | Spring Data Commons | Affected |
|-----------------|-------------|--------------------:|----------|
| 1.1.x           | 3.5.x      | 3.x                | **No** |
| 2.0.x           | 4.0.x      | 4.x                | **Yes** |

## Workaround

The solution is to register **custom converters** (`Map -> byte[]` and `Collection -> byte[]`) in `RedisOMCustomConversions` via reflection, **before** the repository beans are instantiated.

When `customConversions.hasCustomWriteTarget(LinkedHashMap.class)` returns `true`, Redis OM calls `writeToBucket()` instead of `writeInternal()`, thus avoiding the call to `getRequiredPersistentEntity()`.

### Implementation

The fix uses a `BeanFactoryPostProcessor` that runs before repository beans are instantiated:

```java
package com.example.mapserialization.config;

import com.google.gson.Gson;
import com.redis.om.spring.convert.RedisOMCustomConversions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Configuration
public class RedisOMMapConverterConfig {

    private static final Logger logger = LoggerFactory.getLogger(RedisOMMapConverterConfig.class);

    @Bean
    public BeanFactoryPostProcessor redisOMMapConverterRegistrar() {
        return (ConfigurableListableBeanFactory beanFactory) -> registerMapConverters();
    }

    @SuppressWarnings("unchecked")
    private static void registerMapConverters() {
        try {
            var field = RedisOMCustomConversions.class.getDeclaredField("omConverters");
            field.setAccessible(true);
            var converters = (List<Object>) field.get(null);
            converters.add(new MapToBytesConverter());
            converters.add(new CollectionToBytesConverter());
            logger.info("Registered Map/Collection -> byte[] converters in RedisOMCustomConversions");
        } catch (Exception e) {
            logger.error("Failed to register custom converters in RedisOMCustomConversions: {}", e.getMessage(), e);
        }
    }

    @WritingConverter
    static class MapToBytesConverter implements Converter<Map<?, ?>, byte[]> {
        private final Gson gson = new Gson();

        @Override
        public byte[] convert(Map<?, ?> source) {
            return gson.toJson(source).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    @WritingConverter
    static class CollectionToBytesConverter implements Converter<Collection<?>, byte[]> {
        private final Gson gson = new Gson();

        @Override
        public byte[] convert(Collection<?> source) {
            return gson.toJson(source).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
```

### Why a `BeanFactoryPostProcessor`?

The `BeanFactoryPostProcessor` runs **before** Spring beans are created. This is essential because `SimpleRedisDocumentRepository` creates its internal `MappingRedisOMConverter` during instantiation. The converters must therefore be registered before Redis OM repositories are created.

## Reference

- [PR Cosmo-Tech/cosmotech-api#1163](https://github.com/Cosmo-Tech/cosmotech-api/pull/1163) — Spring Boot 4 migration including this fix
- Source file: `RedisOMMapConverterConfig.kt`

