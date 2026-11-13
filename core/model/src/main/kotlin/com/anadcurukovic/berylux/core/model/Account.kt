package com.anadcurukovic.berylux.core.model

/** A new account: the email and password, and the guest's profile it starts from. */
data class NewAccount(
    val email: String,
    val password: String,
    val name: String,
    val sigilSeed: Long,
    val pickSeed: Long,
    val timeZone: String,
    val dayStartHour: Int,
)

/** A field the server can find wrong. */
enum class AccountField {
    EMAIL,
    PASSWORD,
    NAME,
    TIME_ZONE,
    DAY_START_HOUR,
    OTHER,
}

/** What's wrong with a field. Anything the app doesn't know yet reads as [INVALID]. */
enum class FieldProblem {
    MISSING,
    INVALID,
    TOO_SHORT,
    TOO_LONG,
    EMPTY,
}

/** What the server answered to a sign-in, a register, a sign-out or a delete. */
sealed interface AccountResult {
    data object Done : AccountResult

    data class Invalid(val problems: Map<AccountField, FieldProblem>) : AccountResult

    /** Register only: an account has this email. */
    data object EmailUnavailable : AccountResult

    /** Log in: the same for a wrong password and an unknown email. Delete: the password is wrong. */
    data object WrongCredentials : AccountResult

    data class TooManyTries(val waitSeconds: Long) : AccountResult

    /** The refresh token stopped working. The phone is signed out and plays on as a guest. */
    data object SignedOut : AccountResult

    data object NoConnection : AccountResult

    /** The server answered with something the app can't use. */
    data object ServerTrouble : AccountResult

    /** This build has no server to talk to. */
    data object NoServer : AccountResult
}
