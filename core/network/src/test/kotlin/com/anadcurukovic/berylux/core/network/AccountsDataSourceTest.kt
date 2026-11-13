package com.anadcurukovic.berylux.core.network

import androidx.datastore.preferences.core.stringPreferencesKey
import com.anadcurukovic.berylux.core.model.AccountField
import com.anadcurukovic.berylux.core.model.AccountResult
import com.anadcurukovic.berylux.core.model.FieldProblem
import com.anadcurukovic.berylux.core.model.NewAccount
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.After
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class AccountsDataSourceTest {
    private val server = MockWebServer().apply { start() }
    private val file = InMemoryPreferencesDataStore()
    private val tokens = TokenStore(file, InMemoryTokenCipher())
    private val accounts = AccountsDataSource(Connection.to(server.url("/").toString(), tokens), tokens)

    private val guest = NewAccount(
        email = "player@example.com",
        password = "correct horse battery",
        name = "Marlowe",
        sigilSeed = 7,
        pickSeed = 11,
        timeZone = "Europe/Sarajevo",
        dayStartHour = 4,
    )

    @After
    fun stop() = server.close()

    private fun answer(code: Int, body: String = "", vararg headers: Pair<String, String>): MockResponse {
        val builder = MockResponse.Builder().code(code).body(body)
        for ((name, value) in headers) builder.addHeader(name, value)
        return builder.build()
    }

    private fun pair(access: String, refresh: String) = answer(
        200,
        """{"accessToken":"$access","tokenType":"Bearer","expiresIn":900,""" +
            """"refreshToken":"$refresh","refreshExpiresIn":2592000}""",
    )

    private fun problem(status: Int, title: String) = answer(status, """{"status":$status,"title":"$title"}""")

    private fun RecordedRequest.json(): JsonObject = Json.parseToJsonElement(body!!.utf8()).jsonObject

    private fun RecordedRequest.bearer() = headers["Authorization"]

    @Test
    fun `register sends the guest's profile, and keeps the tokens sealed`() = runBlocking {
        server.enqueue(pair("access-1", "refresh-1"))

        assertThat(accounts.register(guest)).isEqualTo(AccountResult.Done)

        val sent = server.takeRequest()
        assertThat(sent.url.encodedPath).isEqualTo("/auth/register")
        assertThat(sent.bearer()).isNull()
        val body = sent.json()
        assertThat(body["email"]!!.jsonPrimitive.content).isEqualTo("player@example.com")
        assertThat(body["password"]!!.jsonPrimitive.content).isEqualTo("correct horse battery")
        val profile = body["profile"]!!.jsonObject
        assertThat(profile.mapValues { it.value.jsonPrimitive.content }).isEqualTo(
            mapOf(
                "name" to "Marlowe",
                "sigilSeed" to "7",
                "pickSeed" to "11",
                "timeZone" to "Europe/Sarajevo",
                "dayStartHour" to "4",
            ),
        )
        assertThat(tokens.current()).isEqualTo(Tokens("access-1", "refresh-1"))
        assertThat(accounts.signedIn.first()).isTrue()
        // What's on disk is sealed: neither token appears in it.
        val stored = file.state.value.asMap().values.joinToString()
        assertThat(stored).doesNotContain("access-1")
        assertThat(stored).doesNotContain("refresh-1")
    }

    @Test
    fun `each wrong field comes back as the app's own field and problem`() = runBlocking {
        val body = """{"status":400,"title":"Some fields are wrong","problems":{"email":"invalid",
            "password":"too_short","profile.name":"empty","profile.dayStartHour":"out_of_range",
            "profile.pickSeed":"missing"}}"""
        server.enqueue(answer(400, body))

        assertThat(accounts.register(guest)).isEqualTo(
            AccountResult.Invalid(
                mapOf(
                    AccountField.EMAIL to FieldProblem.INVALID,
                    AccountField.PASSWORD to FieldProblem.TOO_SHORT,
                    AccountField.NAME to FieldProblem.EMPTY,
                    // A code the app has no word for reads as invalid.
                    AccountField.DAY_START_HOUR to FieldProblem.INVALID,
                    AccountField.OTHER to FieldProblem.MISSING,
                ),
            ),
        )
        assertThat(accounts.signedIn.first()).isFalse()
    }

    @Test
    fun `a taken email can't be used`() = runBlocking {
        server.enqueue(problem(409, "This email can't be used"))
        assertThat(accounts.register(guest)).isEqualTo(AccountResult.EmailUnavailable)
    }

    @Test
    fun `log in turned away leaves the phone signed out`() = runBlocking {
        server.enqueue(problem(401, "Wrong email or password"))
        assertThat(accounts.logIn("player@example.com", "not it")).isEqualTo(AccountResult.WrongCredentials)
        assertThat(server.takeRequest().bearer()).isNull()
        assertThat(accounts.signedIn.first()).isFalse()
    }

    @Test
    fun `too many tries say how long to wait`() = runBlocking {
        server.enqueue(answer(429, """{"status":429,"title":"Too many tries"}""", "Retry-After" to "3600"))
        server.enqueue(answer(429, """{"status":429,"title":"Too many tries"}"""))
        assertThat(accounts.logIn("player@example.com", "x")).isEqualTo(AccountResult.TooManyTries(3_600))
        // Without a Retry-After, the shortest window the API has.
        assertThat(accounts.logIn("player@example.com", "x")).isEqualTo(AccountResult.TooManyTries(60))
    }

    @Test
    fun `no answer from the server is no connection`() = runBlocking {
        server.close()
        assertThat(accounts.logIn("player@example.com", "x")).isEqualTo(AccountResult.NoConnection)
    }

    @Test
    fun `an answer the app can't use is server trouble`() = runBlocking {
        server.enqueue(answer(500, "oops"))
        server.enqueue(answer(200, "not json"))
        server.enqueue(answer(400, "{"))
        assertThat(accounts.logIn("player@example.com", "x")).isEqualTo(AccountResult.ServerTrouble)
        assertThat(accounts.logIn("player@example.com", "x")).isEqualTo(AccountResult.ServerTrouble)
        assertThat(accounts.register(guest)).isEqualTo(AccountResult.ServerTrouble)
        assertThat(accounts.signedIn.first()).isFalse()
    }

    @Test
    fun `a build without a server says so and sends nothing`() = runBlocking {
        val none = AccountsDataSource(Connection(null), tokens)
        assertThat(none.available).isFalse()
        assertThat(none.register(guest)).isEqualTo(AccountResult.NoServer)
        assertThat(none.logIn("player@example.com", "x")).isEqualTo(AccountResult.NoServer)
        assertThat(none.logOut()).isEqualTo(AccountResult.NoServer)
        assertThat(none.deleteAccount("x")).isEqualTo(AccountResult.NoServer)
        assertThat(server.requestCount).isEqualTo(0)
    }

    @Test
    fun `a signed-in call carries the access token`() = runBlocking {
        tokens.save(Tokens("access-1", "refresh-1"))
        server.enqueue(answer(204))

        assertThat(accounts.deleteAccount("correct horse battery")).isEqualTo(AccountResult.Done)

        val sent = server.takeRequest()
        assertThat(sent.method).isEqualTo("DELETE")
        assertThat(sent.url.encodedPath).isEqualTo("/account")
        assertThat(sent.bearer()).isEqualTo("Bearer access-1")
        assertThat(sent.json()["password"]!!.jsonPrimitive.content).isEqualTo("correct horse battery")
    }

    @Test
    fun `a turned-away access token is refreshed once and the call tried again`() = runBlocking {
        tokens.save(Tokens("access-1", "refresh-1"))
        server.enqueue(answer(401))
        server.enqueue(pair("access-2", "refresh-2"))
        server.enqueue(problem(401, "Wrong password"))

        assertThat(accounts.deleteAccount("not it")).isEqualTo(AccountResult.WrongCredentials)

        assertThat(server.takeRequest().bearer()).isEqualTo("Bearer access-1")
        val refresh = server.takeRequest()
        assertThat(refresh.url.encodedPath).isEqualTo("/auth/refresh")
        assertThat(refresh.bearer()).isNull()
        assertThat(refresh.json()["refreshToken"]!!.jsonPrimitive.content).isEqualTo("refresh-1")
        assertThat(server.takeRequest().bearer()).isEqualTo("Bearer access-2")
        // The new pair is kept; the old refresh token is never sent again.
        assertThat(tokens.current()).isEqualTo(Tokens("access-2", "refresh-2"))
        assertThat(server.requestCount).isEqualTo(3)
    }

    @Test
    fun `two calls turned away together refresh only once`() = runBlocking {
        tokens.save(Tokens("access-1", "refresh-1"))
        val refreshes = AtomicInteger()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.url.encodedPath == "/auth/refresh" -> {
                    refreshes.incrementAndGet()
                    Thread.sleep(200)
                    pair("access-2", "refresh-2")
                }
                request.headers["Authorization"] == "Bearer access-1" -> answer(401)
                else -> problem(401, "Wrong password")
            }
        }

        val results = (1..2).map { async(Dispatchers.IO) { accounts.deleteAccount("not it") } }.awaitAll()

        assertThat(results).containsExactly(AccountResult.WrongCredentials, AccountResult.WrongCredentials)
        // A second refresh with refresh-1 would look like a stolen copy, and end the chain.
        assertThat(refreshes.get()).isEqualTo(1)
        assertThat(tokens.current()).isEqualTo(Tokens("access-2", "refresh-2"))
    }

    @Test
    fun `a refused refresh signs the phone out`() = runBlocking {
        tokens.save(Tokens("access-1", "refresh-1"))
        server.enqueue(answer(401))
        server.enqueue(problem(401, "Sign in again"))

        assertThat(accounts.deleteAccount("correct horse battery")).isEqualTo(AccountResult.SignedOut)

        assertThat(tokens.current()).isNull()
        assertThat(accounts.signedIn.first()).isFalse()
        assertThat(server.requestCount).isEqualTo(2)
    }

    @Test
    fun `log out ends this phone's chain and forgets the tokens`() = runBlocking {
        tokens.save(Tokens("access-1", "refresh-1"))
        server.enqueue(answer(204))

        assertThat(accounts.logOut()).isEqualTo(AccountResult.Done)

        val sent = server.takeRequest()
        assertThat(sent.url.encodedPath).isEqualTo("/auth/logout")
        assertThat(sent.bearer()).isEqualTo("Bearer access-1")
        assertThat(sent.json()["refreshToken"]!!.jsonPrimitive.content).isEqualTo("refresh-1")
        assertThat(tokens.current()).isNull()
    }

    @Test
    fun `log out forgets the tokens even with no connection`() = runBlocking {
        tokens.save(Tokens("access-1", "refresh-1"))
        server.close()
        assertThat(accounts.logOut()).isEqualTo(AccountResult.Done)
        assertThat(tokens.current()).isNull()
    }

    @Test
    fun `a deleted account's tokens are forgotten`() = runBlocking {
        tokens.save(Tokens("access-1", "refresh-1"))
        server.enqueue(answer(204))
        assertThat(accounts.deleteAccount("correct horse battery")).isEqualTo(AccountResult.Done)
        assertThat(tokens.current()).isNull()
        assertThat(accounts.signedIn.first()).isFalse()
    }

    @Test
    fun `a stored token that won't open signs the phone out`() = runBlocking {
        file.updateData {
            it.toMutablePreferences().apply {
                set(stringPreferencesKey("access"), "bm90IHNlYWxlZA==")
                set(stringPreferencesKey("refresh"), "bm90IHNlYWxlZA==")
            }
        }
        assertThat(accounts.signedIn.first()).isTrue()
        assertThat(tokens.current()).isNull()
        assertThat(accounts.signedIn.first()).isFalse()
    }
}
