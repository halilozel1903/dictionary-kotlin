package io.github.halilozel1903.dictionary.sample

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList

/** The screens of the app. [key] identifies a screen in the saved back stack. */
sealed interface Screen {
    val key: String

    data object Lookup : Screen {
        override val key: String = "lookup"
    }

    data class Word(val word: String) : Screen {
        override val key: String = "word/${Uri.encode(word)}"
    }

    data class Meanings(val word: String) : Screen {
        override val key: String = "meanings/${Uri.encode(word)}"
    }

    data class Synonyms(val word: String) : Screen {
        override val key: String = "synonyms/${Uri.encode(word)}"
    }

    companion object {
        fun fromKey(key: String): Screen? {
            val name = key.substringBefore('/')
            val word = key.substringAfter('/', "").let(Uri::decode).takeIf { it.isNotBlank() }
            return when (name) {
                "lookup" -> Lookup
                "word" -> word?.let(::Word)
                "meanings" -> word?.let(::Meanings)
                "synonyms" -> word?.let(::Synonyms)
                else -> null
            }
        }
    }
}

/** A back stack that survives configuration changes and process death. */
@Composable
fun rememberBackStack(start: Screen): SnapshotStateList<Screen> =
    rememberSaveable(
        saver = listSaver<SnapshotStateList<Screen>, String>(
            save = { stack -> stack.map { it.key } },
            restore = { keys -> keys.mapNotNull(Screen::fromKey).ifEmpty { listOf(start) }.toMutableStateList() },
        ),
    ) {
        mutableStateListOf<Screen>(start)
    }

fun SnapshotStateList<Screen>.pop() {
    if (size > 1) removeAt(lastIndex)
}
