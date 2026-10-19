package io.github.halilozel1903.dictionary.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import io.github.halilozel1903.dictionary.DictionaryClient
import io.github.halilozel1903.dictionary.DictionaryException
import io.github.halilozel1903.dictionary.model.LookupResult
import io.github.halilozel1903.dictionary.model.WordLookup

/** A spinner in the middle of the screen. */
@Composable
fun LoadingScreen() {
    ScreenScaffold {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(40.dp))
        }
    }
}

/** A title, a message and an optional button, kept inside the round screen's safe area. */
@Composable
fun MessageScreen(title: String, message: String, action: String? = null, onAction: () -> Unit = {}) {
    ScreenScaffold {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
            )
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            if (action != null) CompactButton(onClick = onAction, label = { Text(action) })
        }
    }
}

private fun errorText(error: Throwable): String = when (error) {
    is DictionaryException.Network -> "No connection to the dictionary. Check Wi-Fi or LTE."
    is DictionaryException.RateLimited -> "Too many lookups. Try again in a minute."
    is DictionaryException.Http -> "The dictionary is not answering (HTTP ${error.status})."
    else -> error.message ?: "Something went wrong."
}

/**
 * Shows [state]: a spinner while loading, an error with retry on failure, [content] with the value. In a
 * screenshot scene the state is reported too; [problem] can name a reason a loaded value is not good enough for
 * the scene (for example no synonyms), which reports the scene as failed.
 */
@Composable
fun <T> Loaded(
    state: LoadState<T>,
    onRetry: () -> Unit,
    detail: (T) -> String = { "" },
    problem: (T) -> String? = { null },
    content: @Composable (T) -> Unit,
) {
    val reported = state.valueOrNull?.let(problem)?.let { LoadState.Failure(IllegalStateException(it)) } ?: state
    ReportScene(reported) { state.valueOrNull?.let(detail) ?: "" }
    when (state) {
        LoadState.Loading -> LoadingScreen()
        is LoadState.Failure -> MessageScreen("No answer", errorText(state.error), "Retry", onRetry)
        is LoadState.Success -> content(state.value)
    }
}

/**
 * Looks [word] up and shows [content] with the entry, or the API's own "No Definitions Found" texts when the
 * dictionary does not know the word.
 */
@Composable
fun WordLoaded(
    client: DictionaryClient,
    word: String,
    onFound: (WordLookup) -> Unit = {},
    problem: (WordLookup) -> String? = { null },
    content: @Composable (WordLookup) -> Unit,
) {
    val load = rememberLoad(client, word) { client.lookup(word).also { (it as? LookupResult.Found)?.let { found -> onFound(found.lookup) } } }
    Loaded(
        load.state,
        onRetry = load::retry,
        detail = { result -> (result as? LookupResult.Found)?.lookup?.let { "${it.word}: ${it.definitions.size} definitions, ${it.synonyms.size} synonyms" } ?: "" },
        problem = { result ->
            when (result) {
                is LookupResult.NotFound -> "${result.title} for \"${result.query}\""
                is LookupResult.Found -> problem(result.lookup)
            }
        },
    ) { result ->
        when (result) {
            is LookupResult.Found -> content(result.lookup)
            is LookupResult.NotFound -> MessageScreen(result.title, result.message)
        }
    }
}

/** "1 definition", "3 definitions". */
fun plural(count: Int, one: String, many: String = "${one}s"): String = "$count ${if (count == 1) one else many}"

/** The host of a source url, for example `en.wiktionary.org`. */
fun hostOf(url: String): String = url.substringAfter("://").substringBefore('/').removePrefix("www.")
