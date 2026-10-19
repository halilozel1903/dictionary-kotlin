package io.github.halilozel1903.dictionary.sample

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.delay

/**
 * README screenshot scenes, opened with `--es scene <id>`. Each scene looks its words up LIVE in the Free
 * Dictionary API, exactly like a normal launch (no cache, no recorded data). [ReportScene] logs
 * `SCENE_READY <id>` once the live entry is on screen, or `SCENE_FAILED <id>: <reason>` when it could not be loaded
 * (offline, an HTTP error, "No Definitions Found"); `scripts/screenshots.sh` waits for the first and fails the CI
 * run on the second, so an error screen never ends up in the README.
 */
enum class Scene(val id: String) {
    /** The lookup screen: search, suggested words with their live phonetics. */
    Lookup("lookup"),

    /** "serendipity": phonetic, play button, first definition. */
    Word("word"),

    /** The meanings pager of "light", one page per part of speech. */
    Meanings("meanings"),

    /** The synonyms and antonyms of "happy". */
    Synonyms("synonyms"),
    ;

    /** The screen the scene opens on. */
    val screen: Screen
        get() = when (this) {
            Lookup -> Screen.Lookup
            Word -> Screen.Word("serendipity")
            Meanings -> Screen.Meanings("light")
            Synonyms -> Screen.Synonyms("happy")
        }

    companion object {
        const val TAG = "DictionaryScene"

        fun from(id: String?): Scene? = entries.firstOrNull { it.id == id }
    }
}

/** The scene on screen, or `null` in normal use and for screens behind the top one. */
val LocalScene = staticCompositionLocalOf<Scene?> { null }

/**
 * Reports the load [state] of the screen to `scripts/screenshots.sh` when a scene is open. [detail] is logged with
 * the ready line, so the CI log shows which live entry the screenshot holds.
 */
@Composable
fun ReportScene(state: LoadState<*>, detail: () -> String = { "" }) {
    val scene = LocalScene.current ?: return
    LaunchedEffect(scene, state) {
        when (state) {
            LoadState.Loading -> Unit
            is LoadState.Success -> {
                // Let layout and the list's first frame settle before the capture.
                delay(1_500)
                withFrameNanos { }
                Log.i(Scene.TAG, "SCENE_READY ${scene.id} ${detail()}".trim())
            }
            is LoadState.Failure -> Log.e(Scene.TAG, "SCENE_FAILED ${scene.id}: ${state.error.message ?: state.error::class.simpleName}")
        }
    }
}
