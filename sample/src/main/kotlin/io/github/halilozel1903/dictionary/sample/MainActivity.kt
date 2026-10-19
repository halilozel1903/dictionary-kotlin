package io.github.halilozel1903.dictionary.sample

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import io.github.halilozel1903.dictionary.DictionaryClient
import io.github.halilozel1903.dictionary.DictionarySource
import io.github.halilozel1903.dictionary.cache.CachePolicy
import io.ktor.client.engine.okhttp.OkHttp

/**
 * A pocket dictionary for Wear OS on dictionary-kotlin, with live entries from api.dictionaryapi.dev.
 *
 * `--es source wiktionary` reads Wiktionary's REST API instead (see [DictionarySource]); the screenshot workflow
 * passes the source that answered its pre-check.
 *
 * `scripts/screenshots.sh` starts it with `--es scene <scene>` to open one screen (see [Scene]). Scenes look words
 * up live like a normal launch, without a cache, and the script fails when one cannot load:
 *
 * - `lookup`: search, suggested words with their phonetics
 * - `word`: "serendipity" with its phonetic, play button and first definition
 * - `meanings`: the meanings pager of "light"
 * - `synonyms`: the synonyms and antonyms of "happy"
 */
class MainActivity : ComponentActivity() {

    private val engine = OkHttp.create()
    private lateinit var client: DictionaryClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scene = Scene.from(intent.getStringExtra(EXTRA_SCENE))
        val source = when (intent.getStringExtra(EXTRA_SOURCE)?.lowercase()) {
            "wiktionary" -> DictionarySource.Wiktionary
            else -> DictionarySource.FreeDictionary
        }
        client = DictionaryClient {
            engine = this@MainActivity.engine
            this.source = source
            if (scene != null) {
                // Screenshots show what the dictionary answers right now: no cache, no stale fallback.
                cache = null
                cachePolicy = CachePolicy.NetworkOnly
                retries = 3
            }
        }
        val recent = RecentStore(this)
        if (scene != null) {
            // Keeps the watch from dimming into ambient mode while screenshots are taken.
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        setContent {
            MaterialTheme(colorScheme = DictionaryColors) {
                // The scaffold paints the theme background and shows the time at the top of every screen.
                AppScaffold {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        DictionaryApp(client, recent, scene)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        client.close()
        engine.close()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_SCENE = "scene"
        const val EXTRA_SOURCE = "source"
    }
}
