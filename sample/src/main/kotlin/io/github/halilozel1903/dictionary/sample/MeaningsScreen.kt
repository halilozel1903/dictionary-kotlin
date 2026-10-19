package io.github.halilozel1903.dictionary.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import io.github.halilozel1903.dictionary.DictionaryClient
import io.github.halilozel1903.dictionary.model.Meaning
import io.github.halilozel1903.dictionary.model.WordLookup

/** Every meaning of [word], one page per part of speech: swipe left for the next one. */
@Composable
fun MeaningsScreen(client: DictionaryClient, word: String) {
    WordLoaded(client, word, problem = { if (it.meanings.isEmpty()) "No meanings for \"${it.word}\"" else null }) { lookup ->
        MeaningsPager(lookup)
    }
}

@Composable
private fun MeaningsPager(lookup: WordLookup) {
    val meanings = lookup.meanings
    val pager = rememberPagerState { meanings.size }
    ScreenScaffold { padding ->
        Box(Modifier.fillMaxSize()) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                MeaningPage(lookup.word, meanings[page], page, meanings.size, padding)
            }
            if (meanings.size > 1) {
                PageDots(
                    count = meanings.size,
                    current = pager.currentPage,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun MeaningPage(
    word: String,
    meaning: Meaning,
    index: Int,
    count: Int,
    padding: PaddingValues,
) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
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
                Text(meaning.partOfSpeech, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.secondary)
            }
        }
        item {
            Text(
                "$word · ${index + 1} of $count",
                style = MaterialTheme.typography.bodyExtraSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
            )
        }
        items(meaning.definitions.withIndex().toList()) { (number, definition) ->
            Card(
                onClick = {},
                modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                transformation = SurfaceTransformation(spec),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "${number + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Column {
                        Text(definition.definition, style = MaterialTheme.typography.bodySmall)
                        definition.example?.let { example ->
                            Text(
                                "“$example”",
                                style = MaterialTheme.typography.bodyExtraSmall.copy(fontStyle = FontStyle.Italic),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 3.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** One dot per page, the current one in the accent color. */
@Composable
private fun PageDots(count: Int, current: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { page ->
            val selected = page == current
            Box(
                Modifier
                    .size(if (selected) 7.dp else 5.dp)
                    .clip(CircleShape)
                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
            )
        }
    }
}
