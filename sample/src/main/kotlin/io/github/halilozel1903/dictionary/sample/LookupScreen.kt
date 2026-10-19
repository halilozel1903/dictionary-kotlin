package io.github.halilozel1903.dictionary.sample

import android.app.Activity
import android.app.RemoteInput
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListSubHeader
import androidx.wear.compose.material3.LocalContentColor
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.input.RemoteInputIntentHelper
import io.github.halilozel1903.dictionary.DictionaryClient
import io.github.halilozel1903.dictionary.model.LookupResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/** Words offered on the lookup screen. */
val SuggestedWords: List<String> = listOf("serendipity", "ephemeral", "eloquent", "ubiquitous", "luminous", "nostalgia")

private const val INPUT_KEY = "word"

/**
 * The start screen: a search button that opens the watch's text input (keyboard, voice or handwriting), suggested
 * words with their phonetic and part of speech (looked up live in the background) and the recent lookups.
 */
@Composable
fun LookupScreen(
    client: DictionaryClient,
    recent: List<String>,
    onLookUp: (String) -> Unit,
    onClearRecent: () -> Unit,
) {
    val input = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val word = result.data?.let(RemoteInput::getResultsFromIntent)?.getCharSequence(INPUT_KEY)?.toString()?.trim()
        if (!word.isNullOrEmpty()) onLookUp(word)
    }
    // The suggestions render at once; their phonetics fill in when the lookups answer.
    val previews = rememberLoad(client) {
        coroutineScope { SuggestedWords.map { word -> async { client.lookup(word) } }.awaitAll() }
    }
    val previewState = previews.state
    ReportScene(previewState) {
        val found = previewState.valueOrNull.orEmpty().count { it is LookupResult.Found }
        "$found of ${SuggestedWords.size} suggestions found"
    }
    val results = previewState.valueOrNull.orEmpty().associateBy { it.query }

    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    ScreenScaffold(scrollState = listState) { padding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                ) {
                    Text("Dictionary", textAlign = TextAlign.Center)
                }
            }
            item {
                Button(
                    onClick = {
                        val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
                        val remoteInput = RemoteInput.Builder(INPUT_KEY).setLabel("Look up a word").build()
                        RemoteInputIntentHelper.putRemoteInputsExtra(intent, listOf(remoteInput))
                        input.launch(intent)
                    },
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                    icon = { SearchIcon(LocalContentColor.current, Modifier.size(20.dp)) },
                    label = { Text("Look up a word") },
                    secondaryLabel = { Text("Type, speak or write") },
                )
            }
            item {
                ListSubHeader(
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                    label = { Text("Suggested words") },
                )
            }
            items(SuggestedWords, key = { "suggested-$it" }) { word ->
                Button(
                    onClick = { onLookUp(word) },
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    icon = { BookIcon(LocalContentColor.current, Modifier.size(20.dp)) },
                    label = { Text(word, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    secondaryLabel = {
                        Text(previewLabel(results[word]) ?: "…", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                )
            }
            if (recent.isNotEmpty()) {
                item {
                    ListSubHeader(
                        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                        transformation = SurfaceTransformation(spec),
                        label = { Text("Recent lookups") },
                    )
                }
                items(recent, key = { "recent-$it" }) { word ->
                    Button(
                        onClick = { onLookUp(word) },
                        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                        transformation = SurfaceTransformation(spec),
                        colors = ButtonDefaults.filledTonalButtonColors(),
                        icon = { ClockIcon(LocalContentColor.current, Modifier.size(20.dp)) },
                        label = { Text(word, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
                item {
                    CompactButton(
                        onClick = onClearRecent,
                        modifier = Modifier.padding(top = 4.dp),
                        label = { Text("Clear recent") },
                    )
                }
            }
            item {
                Text(
                    "Free Dictionary API · Wiktionary",
                    style = MaterialTheme.typography.bodyExtraSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
            }
        }
    }
}

/** `/ˌsɛɹ.ənˈdɪp.ɪ.ti/ · noun` for a found word, a short note for a missing one, `null` while loading. */
private fun previewLabel(result: LookupResult?): String? = when (result) {
    null -> null
    is LookupResult.NotFound -> "Not in the dictionary"
    is LookupResult.Found -> listOfNotNull(result.lookup.phonetic, result.lookup.partsOfSpeech.firstOrNull()).joinToString(" · ")
}
