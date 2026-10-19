package io.github.halilozel1903.dictionary

/**
 * Every failure of [DictionaryClient]. A word the dictionary does not know is not a failure: it is
 * [io.github.halilozel1903.dictionary.model.LookupResult.NotFound].
 */
public sealed class DictionaryException(
    /** The requested url. */
    public val url: String,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    /** The API answered 429: too many requests. Wait [retryAfterSeconds] (when it said) and try again. */
    public class RateLimited(url: String, public val retryAfterSeconds: Long?) :
        DictionaryException(url, "Too many requests for $url" + (retryAfterSeconds?.let { ", retry after ${it}s" } ?: ""))

    /** The API answered with another unsuccessful [status] (5xx, ...). */
    public class Http(url: String, public val status: Int) : DictionaryException(url, "HTTP $status for $url")

    /** The request failed before a response arrived (offline, timeout, DNS). */
    public class Network(url: String, cause: Throwable) :
        DictionaryException(url, "Request failed for $url: ${cause.message ?: cause::class.simpleName}", cause)

    /** The response was not the JSON this library expects. */
    public class Decoding(url: String, cause: Throwable) :
        DictionaryException(url, "Could not decode the response of $url: ${cause.message}", cause)
}
