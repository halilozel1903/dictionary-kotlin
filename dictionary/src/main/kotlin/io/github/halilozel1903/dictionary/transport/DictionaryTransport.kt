package io.github.halilozel1903.dictionary.transport

/**
 * Fetches a Free Dictionary API url. The client uses a Ktor based transport by default; supply your own through
 * `DictionaryConfig.transport` to serve recorded responses or use another HTTP stack.
 *
 * Return every HTTP answer, successful or not, as a [DictionaryResponse]: the client maps 404 to a typed
 * not-found result and other statuses to exceptions. Exceptions thrown here are reported as
 * [io.github.halilozel1903.dictionary.DictionaryException.Network].
 */
public fun interface DictionaryTransport {
    public suspend fun get(url: String): DictionaryResponse
}

/** An HTTP answer: its [status], [body] and, for 429 answers, the `Retry-After` header in seconds. */
public data class DictionaryResponse(
    val status: Int,
    val body: String,
    val retryAfterSeconds: Long? = null,
)
