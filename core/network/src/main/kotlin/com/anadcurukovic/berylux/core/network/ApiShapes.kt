package com.anadcurukovic.berylux.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// The API's JSON, as Berylux-API's docs/openapi.yaml describes it. Nothing here leaves this
// module: AccountsDataSource maps every answer to the app's own models in core:model.

@Serializable
internal data class RegisterBody(val email: String, val password: String, val profile: ProfileBody)

@Serializable
internal data class ProfileBody(
    val name: String,
    val sigilSeed: Long,
    val pickSeed: Long,
    val timeZone: String,
    val dayStartHour: Int,
)

@Serializable
internal data class LoginBody(val email: String, val password: String)

@Serializable
internal data class RefreshBody(val refreshToken: String)

@Serializable
internal data class PasswordBody(val password: String)

@Serializable
internal data class TokenAnswer(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long = 0,
    val refreshExpiresIn: Long = 0,
)

/** An RFC 9457 problem; `problems` comes only with a 400 and names each wrong field. */
@Serializable
internal data class Problem(
    val title: String? = null,
    val status: Int? = null,
    val problems: Map<String, String> = emptyMap(),
)

/** Fields the API adds later are skipped, not an error. */
internal val ApiJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}
