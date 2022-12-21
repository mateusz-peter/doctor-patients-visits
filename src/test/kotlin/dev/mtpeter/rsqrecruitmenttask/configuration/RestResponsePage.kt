package dev.mtpeter.rsqrecruitmenttask.configuration

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

@Suppress("UNUSED_PARAMETER")
class RestResponsePage<T>(
    @JsonProperty("content") content: List<T>,
    @JsonProperty("number") number: Int,
    @JsonProperty("size") size: Int,
    @JsonProperty("totalElements") totalElements: Long,
    @JsonProperty("pageable") pageable: JsonNode,
    @JsonProperty("last") last: Boolean,
    @JsonProperty("totalPages") totalPages: Int,
    @JsonProperty("sort") sort: JsonNode?,
    @JsonProperty("first") first: Boolean,
    @JsonProperty("numberOfElements") numberOfElements: Int
) : PageImpl<T>(content, PageRequest.of(number, size), totalElements)