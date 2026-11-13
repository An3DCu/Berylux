package com.anadcurukovic.berylux.core.network

import com.anadcurukovic.berylux.core.model.AccountField
import com.anadcurukovic.berylux.core.model.AccountResult
import com.anadcurukovic.berylux.core.model.FieldProblem
import kotlinx.serialization.SerializationException
import retrofit2.Response

/** The API's answers in the app's words. */
internal object Answers {
    private const val BAD_REQUEST = 400
    private const val CONFLICT = 409
    private const val TOO_MANY = 429

    /** Used when a 429 comes without a readable `Retry-After`: the shortest window the API has. */
    const val DEFAULT_WAIT_SECONDS = 60L

    /** Any answer that is neither a success nor a 401: the caller decides a 401. */
    fun failure(response: Response<*>): AccountResult = when (response.code()) {
        BAD_REQUEST -> problems(response.errorBody()?.string())
            ?.let { AccountResult.Invalid(it) }
            ?: AccountResult.ServerTrouble
        CONFLICT -> AccountResult.EmailUnavailable
        TOO_MANY -> AccountResult.TooManyTries(
            response.headers()["Retry-After"]?.toLongOrNull()?.takeIf { it > 0 } ?: DEFAULT_WAIT_SECONDS,
        )
        else -> AccountResult.ServerTrouble
    }

    /** The wrong fields of a 400, or null when the body names none. The first word for a field wins. */
    fun problems(body: String?): Map<AccountField, FieldProblem>? =
        body?.let(::decode)?.problems?.takeIf { it.isNotEmpty() }?.entries?.fold(mapOf()) { found, (name, code) ->
            val field = field(name)
            if (field in found) found else found + (field to problemOf(code))
        }

    private fun decode(body: String): Problem? = try {
        ApiJson.decodeFromString(Problem.serializer(), body)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    fun field(name: String): AccountField = when (name) {
        "email" -> AccountField.EMAIL
        "password" -> AccountField.PASSWORD
        "profile.name" -> AccountField.NAME
        "profile.timeZone" -> AccountField.TIME_ZONE
        "profile.dayStartHour" -> AccountField.DAY_START_HOUR
        else -> AccountField.OTHER
    }

    fun problemOf(code: String): FieldProblem = when (code) {
        "missing" -> FieldProblem.MISSING
        "too_short" -> FieldProblem.TOO_SHORT
        "too_long" -> FieldProblem.TOO_LONG
        "empty" -> FieldProblem.EMPTY
        else -> FieldProblem.INVALID
    }
}
