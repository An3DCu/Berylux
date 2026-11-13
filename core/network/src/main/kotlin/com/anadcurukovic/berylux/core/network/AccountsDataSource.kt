package com.anadcurukovic.berylux.core.network

import com.anadcurukovic.berylux.core.model.AccountResult
import com.anadcurukovic.berylux.core.model.NewAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerializationException
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The account routes of Berylux-API, in the app's words. The tokens are kept here and nowhere
 * else: callers see whether the phone is signed in and what each call came to.
 */
@Singleton
class AccountsDataSource @Inject internal constructor(
    private val connection: Connection,
    private val tokens: TokenStore,
) {
    val signedIn: Flow<Boolean> = tokens.signedIn

    /** False in a build without a server: release builds until the API is hosted. */
    val available: Boolean = connection.accounts != null

    suspend fun register(account: NewAccount): AccountResult = signIn {
        register(
            RegisterBody(
                email = account.email,
                password = account.password,
                profile = ProfileBody(
                    name = account.name,
                    sigilSeed = account.sigilSeed,
                    pickSeed = account.pickSeed,
                    timeZone = account.timeZone,
                    dayStartHour = account.dayStartHour,
                ),
            ),
        )
    }

    suspend fun logIn(email: String, password: String): AccountResult = signIn { login(LoginBody(email, password)) }

    /**
     * Ends this phone's chain on the server, then forgets the tokens. The tokens go even when the
     * server can't be reached: signing out never waits on the network.
     */
    suspend fun logOut(): AccountResult {
        val api = connection.accounts ?: return AccountResult.NoServer
        val held = tokens.current()
        if (held != null) {
            try {
                api.logout(RefreshBody(held.refresh))
            } catch (_: IOException) {
                // The chain runs out on its own within 30 days; this phone has forgotten it either way.
            } finally {
                tokens.clear()
            }
        }
        return AccountResult.Done
    }

    /** Deletes the account. The phone then plays on as a guest, with everything it had. */
    suspend fun deleteAccount(password: String): AccountResult = call(
        send = { deleteAccount(PasswordBody(password)) },
        onDone = {
            tokens.clear()
            AccountResult.Done
        },
        onUnauthorized = ::wrongPasswordOrSignedOut,
    )

    private suspend fun signIn(send: suspend AccountsApi.() -> Response<TokenAnswer>): AccountResult = call(
        send = send,
        onDone = { answer ->
            if (answer == null) {
                AccountResult.ServerTrouble
            } else {
                tokens.save(Tokens(answer.accessToken, answer.refreshToken))
                AccountResult.Done
            }
        },
        onUnauthorized = { AccountResult.WrongCredentials },
    )

    /** A 401 on a signed-in call: the password was wrong, unless the refresh just signed the phone out. */
    private suspend fun wrongPasswordOrSignedOut(): AccountResult =
        if (tokens.current() == null) AccountResult.SignedOut else AccountResult.WrongCredentials

    /** [onDone] gets the body, which is null for an answer without one, like a 204. */
    private suspend fun <T : Any> call(
        send: suspend AccountsApi.() -> Response<T>,
        onDone: suspend (T?) -> AccountResult,
        onUnauthorized: suspend () -> AccountResult,
    ): AccountResult {
        val api = connection.accounts ?: return AccountResult.NoServer
        return try {
            val response = api.send()
            when {
                response.isSuccessful -> onDone(response.body())
                Connection.isUnauthorized(response.code()) -> onUnauthorized()
                else -> Answers.failure(response)
            }
        } catch (_: IOException) {
            AccountResult.NoConnection
        } catch (_: SerializationException) {
            AccountResult.ServerTrouble
        }
    }
}
