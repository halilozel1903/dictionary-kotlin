package io.github.halilozel1903.dictionary.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.LocalContentColor
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import io.github.halilozel1903.dictionary.DictionaryClient
import io.github.halilozel1903.dictionary.model.WordLookup

/**
 * A word: its spelling, phonetic and a button that plays the recording (US accent first), its parts of speech,
 * the first definition with an example, and links to every meaning and to the synonyms.
 */
@Composable
fun WordScreen(
    client: DictionaryClient,
    word: String,
    pronouncer: Pronouncer,
    onFound: (WordLookup) -> Unit,
    onOpen: (Screen) -> Unit,
) {
    WordLoaded(client, word, onFound = onFound) { lookup ->
        WordDetail(lookup, pronouncer, onOpen)
    }
}

@Composable
private fun WordDetail(lookup: WordLookup, pronouncer: Pronouncer, onOpen: (Screen) -> Unit) {
    val pronunciation = lookup.pronunciation()
    val phonetic = lookup.phoneticFor(pronunciation)
    val first = lookup.firstDefinition
    val synonyms = lookup.synonyms.size
    val antonyms = lookup.antonyms.size
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
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        lookup.word,
                        style = if (lookup.word.length > 11) MaterialTheme.typography.titleLarge else MaterialTheme.typography.displaySmall,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        Modifier.padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (phonetic != null) {
                            Text(
                                phonetic,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                        }
                        val url = pronunciation?.audioUrl
                        if (url != null) {
                            FilledIconButton(
                                onClick = { if (pronouncer.playing == url) pronouncer.release() else pronouncer.play(url) },
                                modifier = Modifier.size(36.dp).semantics {
                                    contentDescription = "Play pronunciation" + (pronunciation?.accent?.let { " ($it)" } ?: "")
                                },
                            ) {
                                SpeakerIcon(LocalContentColor.current, Modifier.size(18.dp))
                            }
                        }
                    }
                    Text(
                        lookup.partsOfSpeech.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                    )
                }
            }
            if (first != null) {
                item {
                    Card(
                        onClick = { onOpen(Screen.Meanings(lookup.word)) },
                        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                        transformation = SurfaceTransformation(spec),
                    ) {
                        Text(first.definition, style = MaterialTheme.typography.bodySmall, maxLines = 5, overflow = TextOverflow.Ellipsis)
                        first.example?.let { example ->
                            Text(
                                "“$example”",
                                style = MaterialTheme.typography.bodyExtraSmall.copy(fontStyle = FontStyle.Italic),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = { onOpen(Screen.Meanings(lookup.word)) },
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                    icon = { BookIcon(LocalContentColor.current, Modifier.size(20.dp)) },
                    label = { Text("Meanings") },
                    secondaryLabel = { Text("${plural(lookup.meanings.size, "part")} of speech · ${plural(lookup.definitions.size, "definition")}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
            }
            item {
                Button(
                    onClick = { onOpen(Screen.Synonyms(lookup.word)) },
                    enabled = synonyms + antonyms > 0,
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    label = { Text("Synonyms") },
                    secondaryLabel = { Text("${plural(synonyms, "synonym")} · ${plural(antonyms, "antonym")}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
            }
            item {
                Text(
                    listOfNotNull(
                        lookup.sourceUrls.firstOrNull()?.let(::hostOf),
                        lookup.licenses.firstOrNull()?.name,
                    ).joinToString(" · ").ifEmpty { "Free Dictionary API" },
                    style = MaterialTheme.typography.bodyExtraSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
            }
        }
    }
}
