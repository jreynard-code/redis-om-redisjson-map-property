package com.example.mapserialization.model

import com.redis.om.spring.annotations.Document
import com.redis.om.spring.annotations.Indexed
import com.redis.om.spring.annotations.Searchable
import org.springframework.data.annotation.Id

/**
 * A simple @Document entity with a Map<String, Any> field.
 * This reproduces a typical OpenAPI-generated model with "additionalProperties: true".
 */
@Document
data class MyEntity(
    @Id var id: String? = null,
    @Searchable var name: String = "",
    @Indexed var organizationId: String = "",
    var additionalData: MutableMap<String, Any>? = null,
)
