package io.github.halilozel1903.dictionary.sample

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/** Plays pronunciation recordings (MP3 urls from the dictionary) with [MediaPlayer], one at a time. */
class Pronouncer {
    /** The url being prepared or played, or `null`. */
    var playing: String? by mutableStateOf(null)
        private set

    private var player: MediaPlayer? = null

    fun play(url: String) {
        release()
        playing = url
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            setOnPreparedListener { it.start() }
            setOnCompletionListener { release() }
            setOnErrorListener { _, _, _ ->
                release()
                true
            }
            try {
                setDataSource(url)
                prepareAsync()
            } catch (e: Exception) {
                release()
            }
        }
    }

    fun release() {
        player?.release()
        player = null
        playing = null
    }
}

/** A [Pronouncer] released when it leaves composition. */
@Composable
fun rememberPronouncer(): Pronouncer {
    val pronouncer = remember { Pronouncer() }
    DisposableEffect(pronouncer) { onDispose { pronouncer.release() } }
    return pronouncer
}
