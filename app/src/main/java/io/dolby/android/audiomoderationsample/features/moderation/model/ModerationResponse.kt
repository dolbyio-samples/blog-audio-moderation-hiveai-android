package io.dolby.android.audiomoderationsample.features.moderation.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ModerationResponse(
    val id: String? = null,
    val code: Int? = 0,
    @Json(name = "project_id") val projectId: Int? = 0,
    @Json(name = "user_id") val userId: Int? = 0,
    @Json(name = "created_on") val createdOn: String? = null,
    val status : List<StatusResponse>? = null
)

@JsonClass(generateAdapter = true)
data class StatusResponse(
    val status: Status? = null,
    val response: Response? = null
)

@JsonClass(generateAdapter = true)
data class Status(
    val code: String? = null,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class Response(
    val output: List<Output>? = null,
    val language: String? = null
)

@JsonClass(generateAdapter = true)
data class Output(
    val transcript: String? = null,
    val classifications: List<Clasification>? = null
)

@JsonClass(generateAdapter = true)
data class Clasification(
    val classes: List<ClasificationClass>? = null,
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class ClasificationClass(
    @Json(name = "class") val klass: String? = null,
    val score: Double? = 0.0
)
