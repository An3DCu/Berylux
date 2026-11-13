package com.anadcurukovic.berylux.core.network

import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.POST

internal interface AccountsApi {
    @POST(REGISTER)
    suspend fun register(@Body body: RegisterBody): Response<TokenAnswer>

    @POST(LOGIN)
    suspend fun login(@Body body: LoginBody): Response<TokenAnswer>

    @POST("auth/logout")
    suspend fun logout(@Body body: RefreshBody): Response<Unit>

    @HTTP(method = "DELETE", path = "account", hasBody = true)
    suspend fun deleteAccount(@Body body: PasswordBody): Response<Unit>

    companion object {
        const val REGISTER = "auth/register"
        const val LOGIN = "auth/login"
        const val REFRESH = "auth/refresh"

        /** The routes that take no access token. Every other call carries one. */
        val OPEN = setOf("/$REGISTER", "/$LOGIN", "/$REFRESH")
    }
}

/** Refresh alone, on a client of its own: the refresh is what runs when a token is turned away. */
internal interface RefreshApi {
    @POST(AccountsApi.REFRESH)
    fun refresh(@Body body: RefreshBody): Call<TokenAnswer>
}
