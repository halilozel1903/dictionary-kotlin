package io.github.halilozel1903.dictionary

import io.github.halilozel1903.dictionary.model.Definition
import io.github.halilozel1903.dictionary.model.License
import io.github.halilozel1903.dictionary.model.Meaning
import io.github.halilozel1903.dictionary.model.WordEntry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.net.URLEncoder
import java.util.Locale

/**
 * Parses Wiktionary's REST definition answer (`GET /page/definition/{word}`):
 *
 * ```json
 * {"en": [{"partOfSpeech": "Adjective", "language": "English",
 *          "definitions": [{"definition": "<html>", "parsedExamples": [{"example": "<html>"}], "examples": ["<html>"]}]}],
 *  "fr": [...]}
 * ```
 *
 * The top-level keys are language codes. Definitions and examples are HTML; they become plain text, and the
 * synonyms and antonyms Wiktionary lists inline under a definition (`<span class="nyms synonym">`) become
 * [Definition.synonyms] and [Definition.antonyms]. Wiktionary has no phonetics or recordings here.
 */
public object WiktionaryJson {
    private val json = Json { ignoreUnknownKeys = true }

    /** The license of Wiktionary's text. */
    public val LICENSE: License = License("CC BY-SA 4.0", "https://creativecommons.org/licenses/by-sa/4.0/")

    /**
     * Parses an answer for [word] and keeps the [language] section (`en`). Returns no entries when the page has no
     * section in that language or no definitions in it; fails only when the body is not a JSON object.
     */
    public fun parseEntries(word: String, language: String, body: String): List<WordEntry> {
        val root = json.parseToJsonElement(body) as? JsonObject ?: error("The response is not a JSON object")
        val usages = (root[language] as? JsonArray)?.filterIsInstance<JsonObject>().orEmpty()
        val meanings = usages.mapNotNull { usage ->
            val partOfSpeech = usage.text("partOfSpeech")?.lowercase(Locale.ROOT) ?: return@mapNotNull null
            val definitions = (usage["definitions"] as? JsonArray)?.filterIsInstance<JsonObject>().orEmpty().mapNotNull { definition ->
                val parsed = WiktionaryHtml.parse(definition.text("definition") ?: return@mapNotNull null)
                if (parsed.text.isEmpty()) return@mapNotNull null
                Definition(
                    definition = parsed.text,
                    example = definition.example(),
                    synonyms = parsed.synonyms,
                    antonyms = parsed.antonyms,
                )
            }
            if (definitions.isEmpty()) null else Meaning(partOfSpeech, definitions, synonyms = emptyList(), antonyms = emptyList())
        }
        if (meanings.isEmpty()) return emptyList()
        return listOf(
            WordEntry(
                word = word,
                phonetic = null,
                phonetics = emptyList(),
                origin = null,
                meanings = meanings,
                license = LICENSE,
                sourceUrls = listOf(pageUrl(word)),
            ),
        )
    }

    /** The Wiktionary page of [word], for example `https://en.wiktionary.org/wiki/ice_cream`. */
    public fun pageUrl(word: String): String = "https://en.wiktionary.org/wiki/" + title(word)

    /** [word] as a Wiktionary title in a url: spaces become underscores, the rest is percent-encoded. */
    internal fun title(word: String): String =
        URLEncoder.encode(word.replace(' ', '_'), Charsets.UTF_8).replace("+", "%20")

    /** The first example of a definition: `parsedExamples[].example`, else `examples[]`, as plain text. */
    private fun JsonObject.example(): String? {
        val parsed = (get("parsedExamples") as? JsonArray)?.filterIsInstance<JsonObject>().orEmpty().mapNotNull { it.text("example") }
        val plain = (get("examples") as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content }.orEmpty()
        return (parsed + plain).asSequence().map { WiktionaryHtml.parse(it).text }.firstOrNull { it.isNotEmpty() }
    }

    private fun JsonObject.text(key: String): String? =
        (get(key) as? JsonPrimitive)?.takeIf { it !is JsonNull && it.isString }?.content?.trim()?.takeIf { it.isNotEmpty() }
}

/** Turns Wiktionary's definition HTML into text, and pulls out the inline synonyms and antonyms. */
internal object WiktionaryHtml {

    data class Parsed(val text: String, val synonyms: List<String>, val antonyms: List<String>)

    private val TAG = Regex("""<(/?)([a-zA-Z][a-zA-Z0-9]*)([^>]*?)(/?)>""")
    private val CLASS = Regex("""class\s*=\s*"([^"]*)"""")
    private val ENTITY = Regex("""&(#[0-9]+|#[xX][0-9a-fA-F]+|[a-zA-Z]+);""")
    private val VOID = setOf("br", "img", "hr", "wbr", "link", "meta", "input")

    /** Elements whose content is not part of the definition text: nested lists, references, styles. */
    private val SKIPPED_ELEMENTS = setOf("ul", "ol", "dl", "sup", "style", "script", "table")

    /** Classes of spans that are left out of the text: dates of first use and the like. */
    private val SKIPPED_CLASSES = setOf("defdate", "HQToggle", "mw-ref", "reference")

    private enum class Kind { Synonym, Antonym, OtherNym, Skip }

    fun parse(html: String): Parsed {
        val text = StringBuilder()
        val synonyms = ArrayList<String>()
        val antonyms = ArrayList<String>()
        // One frame per open element: what it is, so its content goes to the right place.
        val stack = ArrayList<Pair<String, Kind?>>()
        var link: StringBuilder? = null
        var position = 0

        fun currentKind(): Kind? = stack.lastOrNull { it.second != null }?.second

        fun emit(raw: String) {
            if (raw.isEmpty()) return
            val decoded = decode(raw)
            when (currentKind()) {
                null -> text.append(decoded)
                Kind.Synonym, Kind.Antonym -> link?.append(decoded)
                Kind.OtherNym, Kind.Skip -> Unit
            }
        }

        for (match in TAG.findAll(html)) {
            emit(html.substring(position, match.range.first))
            position = match.range.last + 1
            val closing = match.groupValues[1] == "/"
            val name = match.groupValues[2].lowercase(Locale.ROOT)
            val selfClosing = match.groupValues[4] == "/" || name in VOID
            if (closing) {
                val index = stack.indexOfLast { it.first == name }
                if (index < 0) continue
                if (name == "a" && link != null) {
                    val kind = currentKind()
                    val word = link.toString().replace(Regex("\\s+"), " ").trim()
                    if (word.isNotEmpty() && ':' !in word) {
                        if (kind == Kind.Synonym) synonyms += word else if (kind == Kind.Antonym) antonyms += word
                    }
                    link = null
                }
                while (stack.size > index) stack.removeAt(stack.lastIndex)
                continue
            }
            if (name == "br") text.append(' ')
            if (selfClosing) continue
            val classes = CLASS.find(match.groupValues[3])?.groupValues?.get(1)?.split(' ')?.filter { it.isNotBlank() }.orEmpty()
            val kind = when {
                "nyms" in classes && "synonym" in classes -> Kind.Synonym
                "nyms" in classes && "antonym" in classes -> Kind.Antonym
                "nyms" in classes -> Kind.OtherNym
                name in SKIPPED_ELEMENTS || classes.any { it in SKIPPED_CLASSES } -> Kind.Skip
                else -> null
            }
            // Inside a nym list the kind of the list wins. Skipped content (such as the `<dl>` under a definition)
            // stays out of the text, but a nym list inside it is still read.
            val current = currentKind()
            val effective = when {
                current == null -> kind
                current == Kind.Skip && kind != null && kind != Kind.Skip -> kind
                else -> null
            }
            stack += name to effective
            if (name == "a" && (currentKind() == Kind.Synonym || currentKind() == Kind.Antonym)) link = StringBuilder()
        }
        emit(html.substring(position))
        return Parsed(
            text = text.toString().replace(Regex("\\s+"), " ").replace(Regex(" ([,.;:!?)])"), "$1").replace("( ", "(").trim(),
            synonyms = synonyms.distinct(),
            antonyms = antonyms.distinct(),
        )
    }

    private fun decode(raw: String): String = ENTITY.replace(raw) { match ->
        val name = match.groupValues[1]
        when {
            name.startsWith("#x") || name.startsWith("#X") -> name.substring(2).toIntOrNull(16)?.let { String(Character.toChars(it)) }
            name.startsWith("#") -> name.substring(1).toIntOrNull()?.let { String(Character.toChars(it)) }
            else -> NAMED[name]
        } ?: match.value
    }

    private val NAMED = mapOf(
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to " ",
        "ndash" to "–", "mdash" to "—", "hellip" to "…", "lsquo" to "‘", "rsquo" to "’", "ldquo" to "“", "rdquo" to "”",
    )
}
