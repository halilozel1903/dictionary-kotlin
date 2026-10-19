package io.github.halilozel1903.dictionary.sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlin.coroutines.cancellation.CancellationException

/** Where a load is: running, done with a value, or failed. */
sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>

    data class Success<out T>(val value: T) : LoadState<T>

    data class Failure(val error: Throwable) : LoadState<Nothing>
}

/** The value, or `null` while loading or after a failure. */
val <T> LoadState<T>.valueOrNull: T? get() = (this as? LoadState.Success<T>)?.value

/** A load started from composition. Read [state]; call [retry] to run it again. */
@Stable
class Load<out T>(private val current: State<LoadState<T>>, private val onRetry: () -> Unit) {
    val state: LoadState<T> get() = current.value

    fun retry() {
        onRetry()
    }
}

/** Runs [load] when this enters composition and again whenever one of [keys] changes. */
@Composable
fun <T> rememberLoad(vararg keys: Any?, load: suspend () -> T): Load<T> {
    val latestLoad by rememberUpdatedState(load)
    var attempt by remember(*keys) { mutableIntStateOf(0) }
    val state = produceState<LoadState<T>>(LoadState.Loading, *keys, attempt) {
        value = LoadState.Loading
        value = try {
            LoadState.Success(latestLoad())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LoadState.Failure(e)
        }
    }
    return remember(state) { Load(state) { attempt++ } }
}
