package io.github.halilozel1903.dictionary.transport

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders

/** The default transport: GET requests with Ktor. Closes the client only when it created it. */
internal class KtorTransport(
    httpClient: HttpClient?,
    engine: HttpClientEngine?,
    private val userAgent: String,
    requestTimeoutMillis: Long,
    retries: Int,
) : DictionaryTransport, AutoCloseable {

    private val ownsClient = httpClient == null
    private val client: HttpClient = httpClient ?: run {
        val setup: HttpClientConfig<*>.() -> Unit = {
            expectSuccess = false
            followRedirects = true
            install(HttpTimeout) {
                this.requestTimeoutMillis = requestTimeoutMillis
            }
            if (retries > 0) {
                install(HttpRequestRetry) {
                    // 404 is an answer ("No Definitions Found"), never retried. 5xx and network errors are.
                    retryOnServerErrors(maxRetries = retries)
                    retryOnException(maxRetries = retries, retryOnTimeout = true)
                    exponentialDelay()
                }
            }
        }
        if (engine != null) HttpClient(engine, setup) else HttpClient(CIO, setup)
    }

    override suspend fun get(url: String): DictionaryResponse {
        val response = client.get(url) {
            header(HttpHeaders.UserAgent, userAgent)
            accept(ContentType.Application.Json)
        }
        return DictionaryResponse(
            status = response.status.value,
            body = runCatching { response.bodyAsText() }.getOrDefault(""),
            retryAfterSeconds = response.headers[HttpHeaders.RetryAfter]?.trim()?.toLongOrNull(),
        )
    }

    override fun close() {
        if (ownsClient) client.close()
    }
}
