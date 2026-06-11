# Redis OM Spring + spring-data-commons 4.x: Map serialization bug reproducer

## Bug

`MappingRedisOMConverter.write()` throws:

```
MappingException: Couldn't find PersistentEntity for type class java.util.LinkedHashMap
```

when a `@Document` entity has a `Map<String, Any>` field containing **nested Map values** (e.g. from Jackson deserializing `{"key": {"nested": "value"}}`).

## Environment

| Dependency | Version |
|---|---|
| Spring Boot | 4.0.6 |
| spring-data-commons | 4.0.5 |
| spring-data-redis | 4.0.5 |
| Redis OM Spring | 2.0.4 |
| Kotlin | 2.2.0 |

## Root Cause

spring-data-commons 4.x no longer creates `PersistentEntity` for JDK Map/Collection types.

In `MappingRedisOMConverter`:
1. `writeMap()` iterates map entry values
2. For values that are themselves Maps (e.g. `LinkedHashMap`), it calls `writeInternal()`
3. `writeInternal()` checks `customConversions.hasCustomWriteTarget(LinkedHashMap.class)` → returns **false**
4. Falls through to `getRequiredPersistentEntity(LinkedHashMap.class)` → **throws MappingException**

`RedisOMCustomConversions` does not register converters for Map or Collection types.

## Reproduce

### Prerequisites

- JDK 21+
- Docker (for Redis Stack)

### Run the tests (no Docker needed)

```bash
./gradlew test
```

The test `MappingExceptionReproducerTest` proves the bug:
- `write entity with nested map in additionalData throws MappingException` ← **proves the bug**
- `write() works fine when map values are simple types` ← shows flat maps work fine
- `hasCustomWriteTarget returns false for LinkedHashMap` ← confirms root cause

### Run the REST endpoint (requires Docker)

```bash
docker compose up -d
./gradlew bootRun
```

Then call:

```bash
curl -X POST http://localhost:8080/reproduce \
  -H "Content-Type: application/json" \
  -d '{"name": "test", "additionalData": {"simple": "value", "nested": {"key": "deep"}}}'
```

Returns **HTTP 500** with:
```
MappingException: Couldn't find PersistentEntity for type class java.util.LinkedHashMap
```

## Proposed Fix

Add `MapToBytesConverter` and `CollectionToBytesConverter` to `RedisOMCustomConversions.omConverters`:

```java
// In RedisOMCustomConversions static block:
omConverters.add(new MapToBytesConverter());
omConverters.add(new CollectionToBytesConverter());
```

```java
@WritingConverter
public class MapToBytesConverter implements Converter<Map<?, ?>, byte[]> {
  private static final Gson GSON = new Gson();

  @Override
  public byte[] convert(Map<?, ?> source) {
    return GSON.toJson(source).getBytes(StandardCharsets.UTF_8);
  }
}

@WritingConverter
public class CollectionToBytesConverter implements Converter<Collection<?>, byte[]> {
  private static final Gson GSON = new Gson();

  @Override
  public byte[] convert(Collection<?> source) {
    return GSON.toJson(source).getBytes(StandardCharsets.UTF_8);
  }
}
```

This makes `hasCustomWriteTarget(LinkedHashMap.class)` return `true`, routing through `writeToBucket()` instead of `writeInternal()`.

Also, the `RedisOMCustomConversions` constructor ignores user-provided converters:
```java
public RedisOMCustomConversions(List<?> converters) {
    super(omConverters); // ← 'converters' parameter is ignored!
}
```

See [redis-om-spring-pr.patch](redis-om-spring-pr.patch) for the complete fix.

## Workaround

Until the fix is merged upstream, use a `BeanFactoryPostProcessor` to patch `omConverters` via reflection:

```kotlin
@Configuration
class RedisOMCompatConfig {
    @Bean
    fun redisOMMapConverterRegistrar() = BeanFactoryPostProcessor { _ ->
        val field = RedisOMCustomConversions::class.java.getDeclaredField("omConverters")
        field.isAccessible = true
        val converters = field.get(null) as MutableList<Any>
        converters.add(MapToJsonBytesConverter())
        converters.add(CollectionToJsonBytesConverter())
    }
}

@WritingConverter
class MapToJsonBytesConverter : Converter<Map<*, *>, ByteArray> {
    override fun convert(source: Map<*, *>) = Gson().toJson(source).toByteArray(Charsets.UTF_8)
}

@WritingConverter
class CollectionToJsonBytesConverter : Converter<Collection<*>, ByteArray> {
    override fun convert(source: Collection<*>) = Gson().toJson(source).toByteArray(Charsets.UTF_8)
}
```
