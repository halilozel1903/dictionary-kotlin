package io.github.halilozel1903.dictionary

import io.github.halilozel1903.dictionary.model.Definition
import io.github.halilozel1903.dictionary.model.License
import io.github.halilozel1903.dictionary.model.LookupResult
import io.github.halilozel1903.dictionary.model.Meaning
import io.github.halilozel1903.dictionary.model.Phonetic
import io.github.halilozel1903.dictionary.model.WordEntry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Parses the Free Dictionary API's JSON. Parsing is lenient: missing, `null` or empty fields become `null` or empty
 * lists, empty audio urls become `null`, and protocol-relative urls (`//ssl.gstatic.com/...`, used by older entries)
 * get `https:`. Only a body that is not a JSON array of entries fails.
 */
public object DictionaryJson {
    private val json = Json { ignoreUnknownKeys = true }

    /** Parses a successful answer: a JSON array of entries. */
    public fun parseEntries(body: String): List<WordEntry> {
        val root = json.parseToJsonElement(body) as? JsonArray ?: error("The response is not a JSON array")
        return root.mapIndexed { index, element ->
            val entry = element as? JsonObject ?: error("Entry $index is not an object")
            parseEntry(entry, index)
        }
    }

    /**
     * Parses a "no definitions" answer, `{"title":"No Definitions Found","message":"...","resolution":"..."}`, for
     * [query]. Unknown or empty bodies get the API's usual title.
     */
    public fun parseNotFound(query: String, body: String?): LookupResult.NotFound {
        val root = body?.takeIf { it.isNotBlank() }?.let { runCatching { json.parseToJsonElement(it) as? JsonObject }.getOrNull() }
        return LookupResult.NotFound(
            query = query,
            title = root?.text("title") ?: LookupResult.NO_DEFINITIONS_FOUND,
            message = root?.text("message") ?: "No definitions were found for \"$query\".",
            resolution = root?.text("resolution"),
        )
    }

    private fun parseEntry(entry: JsonObject, index: Int): WordEntry = WordEntry(
        word = entry.text("word") ?: error("Entry $index has no word"),
        phonetic = entry.text("phonetic"),
        phonetics = entry.objects("phonetics").map { phonetic ->
            Phonetic(
                text = phonetic.text("text"),
                audioUrl = phonetic.text("audio")?.let(::absoluteUrl),
                sourceUrl = phonetic.text("sourceUrl")?.let(::absoluteUrl),
                license = phonetic.license(),
            )
        }.filter { it.text != null || it.audioUrl != null },
        origin = entry.text("origin"),
        meanings = entry.objects("meanings").mapNotNull { meaning ->
            val partOfSpeech = meaning.text("partOfSpeech") ?: return@mapNotNull null
            Meaning(
                partOfSpeech = partOfSpeech,
                definitions = meaning.objects("definitions").mapNotNull { definition ->
                    Definition(
                        definition = definition.text("definition") ?: return@mapNotNull null,
                        example = definition.text("example"),
                        synonyms = definition.strings("synonyms"),
                        antonyms = definition.strings("antonyms"),
                    )
                },
                synonyms = meaning.strings("synonyms"),
                antonyms = meaning.strings("antonyms"),
            )
        },
        license = entry.license(),
        sourceUrls = entry.strings("sourceUrls").map(::absoluteUrl),
    )

    /** `//host/path` becomes `https://host/path`; other urls stay as they are. */
    internal fun absoluteUrl(url: String): String = if (url.startsWith("//")) "https:$url" else url

    private fun JsonObject.text(key: String): String? =
        (get(key) as? JsonPrimitive)?.takeIf { it !is JsonNull && it.isString }?.content?.trim()?.takeIf { it.isNotEmpty() }

    private fun JsonObject.objects(key: String): List<JsonObject> =
        (get(key) as? JsonArray)?.filterIsInstance<JsonObject>().orEmpty()

    private fun JsonObject.strings(key: String): List<String> =
        (get(key) as? JsonArray)?.mapNotNull { element: JsonElement ->
            (element as? JsonPrimitive)?.takeIf { it !is JsonNull && it.isString }?.content?.trim()?.takeIf { it.isNotEmpty() }
        }.orEmpty()

    private fun JsonObject.license(): License? {
        val license = get("license") as? JsonObject ?: return null
        val name = license.text("name") ?: return null
        return License(name, license.text("url"))
    }
}
