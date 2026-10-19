package io.github.halilozel1903.dictionary.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListSubHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import io.github.halilozel1903.dictionary.DictionaryClient
import io.github.halilozel1903.dictionary.model.WordLookup

/** At most this many chips per group, so the list stays short on a watch. */
private const val MAX_CHIPS = 24

/** The synonyms and antonyms of [word] as chips; tapping one looks it up. */
@Composable
fun SynonymsScreen(client: DictionaryClient, word: String, onLookUp: (String) -> Unit) {
    WordLoaded(client, word, problem = { if (it.synonyms.isEmpty()) "The dictionary lists no synonyms of \"${it.word}\"" else null }) { lookup ->
        SynonymList(lookup, onLookUp)
    }
}

@Composable
private fun SynonymList(lookup: WordLookup, onLookUp: (String) -> Unit) {
    val synonyms = lookup.synonyms
    val antonyms = lookup.antonyms
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
                    Text("Synonyms", textAlign = TextAlign.Center)
                }
            }
            item {
                Text(
                    "of ${lookup.word}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                )
            }
            item {
                if (synonyms.isEmpty()) {
                    Text("None listed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Chips(
                        words = synonyms.take(MAX_CHIPS),
                        container = MaterialTheme.colorScheme.primaryContainer,
                        content = MaterialTheme.colorScheme.onPrimaryContainer,
                        onClick = onLookUp,
                    )
                }
            }
            if (antonyms.isNotEmpty()) {
                item {
                    ListSubHeader(
                        modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                        transformation = SurfaceTransformation(spec),
                        label = { Text("Antonyms") },
                    )
                }
                item {
                    Chips(
                        words = antonyms.take(MAX_CHIPS),
                        container = MaterialTheme.colorScheme.surfaceContainerHigh,
                        content = AntonymColor,
                        onClick = onLookUp,
                    )
                }
            }
        }
    }
}

/** Words as rounded chips that wrap onto as many lines as they need. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Chips(words: List<String>, container: Color, content: Color, onClick: (String) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        words.forEach { word ->
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(container)
                    .clickable(role = Role.Button, onClickLabel = "Look up $word") { onClick(word) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    word,
                    style = MaterialTheme.typography.labelSmall,
                    color = content,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
