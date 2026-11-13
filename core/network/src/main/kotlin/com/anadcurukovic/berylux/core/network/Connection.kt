package com.anadcurukovic.berylux.core.network

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/** The API, if this build has one. Release builds have no address until the API is hosted. */
internal class Connection(val accounts: AccountsApi?) {
    companion object {
        private const val CONNECT_SECONDS = 10L
        private const val READ_SECONDS = 20L
        private const val UNAUTHORIZED = 401

        fun to(baseUrl: String, tokens: TokenStore): Connection {
            if (baseUrl.isBlank()) return Connection(null)
            val json = ApiJson.asConverterFactory("application/json".toMediaType())
            // The refresh has a client of its own, with no authenticator: it can't loop on itself.
            val plain = OkHttpClient.Builder()
                .connectTimeout(CONNECT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(READ_SECONDS, TimeUnit.SECONDS)
                .build()
            val refreshApi = Retrofit.Builder().baseUrl(baseUrl).client(plain).addConverterFactory(json).build()
                .create(RefreshApi::class.java)
            val signedIn = plain.newBuilder()
                .addInterceptor(BearerToken(tokens))
                .authenticator(TokenRefresher(tokens, refreshApi))
                .build()
            val accounts = Retrofit.Builder().baseUrl(baseUrl).client(signedIn).addConverterFactory(json).build()
                .create(AccountsApi::class.java)
            return Connection(accounts)
        }

        fun Request.withBearer(access: String): Request =
            newBuilder().header("Authorization", "Bearer $access").build()

        fun isUnauthorized(code: Int) = code == UNAUTHORIZED
    }
}

/** Puts the access token on every call but register, log in and refresh. */
internal class BearerToken(private val tokens: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val access = if (request.url.encodedPath in AccountsApi.OPEN) null else runBlocking { tokens.current() }?.access
        return chain.proceed(if (access == null) request else with(Connection) { request.withBearer(access) })
    }
}

/**
 * When the API turns an access token away, trades the refresh token for a new pair once and
 * tries again. One refresh at a time: two calls turned away together must not both send the
 * same refresh token, or the API takes the second as a stolen copy and ends the chain. A refresh
 * the API refuses signs the phone out.
 */
internal class TokenRefresher(
    private val tokens: TokenStore,
    private val refreshApi: RefreshApi,
) : Authenticator {
    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        val sent = response.request.header("Authorization")?.removePrefix("Bearer ")
        // Only one try with a new token; if that's turned away too, the answer stands.
        if (sent == null || response.priorResponse != null) return null
        val access = synchronized(lock) { accessAfter(sent) }
        return access?.let { with(Connection) { response.request.withBearer(it) } }
    }

    /** The access token to try again with, or null to let the 401 stand. */
    private fun accessAfter(sent: String): String? {
        val now = runBlocking { tokens.current() }
        return when {
            now == null -> null
            // Another call refreshed while this one waited: use what it got.
            now.access != sent -> now.access
            else -> trade(now.refresh)
        }
    }

    private fun trade(refresh: String): String? {
        val answer = try {
            refreshApi.refresh(RefreshBody(refresh)).execute()
        } catch (_: IOException) {
            null
        }
        val body = answer?.body()
        if (answer != null && answer.isSuccessful && body != null) {
            runBlocking { tokens.save(Tokens(body.accessToken, body.refreshToken)) }
            return body.accessToken
        }
        if (answer != null && Connection.isUnauthorized(answer.code())) runBlocking { tokens.clear() }
        return null
    }
}
