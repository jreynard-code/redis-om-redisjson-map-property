package com.example.mapserialization

import com.redis.om.spring.annotations.EnableRedisDocumentRepositories
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableRedisDocumentRepositories
class MapserializationApplication

fun main(args: Array<String>) {
    runApplication<MapserializationApplication>(*args)
}
