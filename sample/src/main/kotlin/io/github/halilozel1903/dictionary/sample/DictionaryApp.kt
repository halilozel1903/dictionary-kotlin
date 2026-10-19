package io.github.halilozel1903.dictionary.sample

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.wear.compose.material3.SwipeToDismissBox
import io.github.halilozel1903.dictionary.DictionaryClient

/**
 * The app: a back stack of [Screen]s starting at the lookup screen (or at the [scene]'s screen). Swiping right or
 * the back button goes back; the screen behind stays visible during the swipe.
 */
@Composable
fun DictionaryApp(client: DictionaryClient, recent: RecentStore, scene: Scene?) {
    val stack = rememberBackStack(scene?.screen ?: Screen.Lookup)
    val top = stack.last()
    val behind = stack.getOrNull(stack.lastIndex - 1)
    val pronouncer = rememberPronouncer()
    BackHandler(enabled = stack.size > 1) { stack.pop() }
    SwipeToDismissBox(
        onDismissed = { stack.pop() },
        backgroundKey = behind?.key ?: "none",
        contentKey = top.key,
        userSwipeEnabled = stack.size > 1,
    ) { isBackground ->
        if (isBackground) {
            // Only the top screen reports to the screenshot script.
            if (behind != null) {
                CompositionLocalProvider(LocalScene provides null) {
                    ScreenContent(behind, client, recent, pronouncer, stack)
                }
            }
        } else {
            CompositionLocalProvider(LocalScene provides scene?.takeIf { it.screen == top }) {
                ScreenContent(top, client, recent, pronouncer, stack)
            }
        }
    }
}

@Composable
private fun ScreenContent(
    screen: Screen,
    client: DictionaryClient,
    recent: RecentStore,
    pronouncer: Pronouncer,
    stack: SnapshotStateList<Screen>,
) {
    when (screen) {
        Screen.Lookup -> LookupScreen(
            client = client,
            recent = recent.words,
            onLookUp = { stack.add(Screen.Word(it)) },
            onClearRecent = recent::clear,
        )
        is Screen.Word -> WordScreen(
            client = client,
            word = screen.word,
            pronouncer = pronouncer,
            onFound = { recent.add(it.word) },
            onOpen = { stack.add(it) },
        )
        is Screen.Meanings -> MeaningsScreen(client, screen.word)
        is Screen.Synonyms -> SynonymsScreen(client, screen.word, onLookUp = { stack.add(Screen.Word(it)) })
    }
}
