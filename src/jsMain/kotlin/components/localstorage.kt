package components

import com.jilesvangurp.rankquest.core.DEFAULT_JSON
import dev.fritz2.core.RootStore
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.serialization.KSerializer
import kotlinx.coroutines.Job
import kotlin.time.Duration.Companion.milliseconds

open class LocalStoringStore<T>(
    val initialData: T?,
    val key: String,
    val serializer: KSerializer<T>

) :
    RootStore<T?>(initialData, Job()) {
    private var latest: T? = null
    private var loaded=false

    suspend fun awaitLoaded() {
        while(!loaded) delay(20.milliseconds)
    }

    private val propagationHandler = handle<Pair<RootStore<T>,T>> {v,(s,defaultValue)->
        if(v==null || v==initialData) {
            s.update(defaultValue)
        } else {
            s.update(v)
        }
        v
    }

    private fun onStoreUpdated(v: T?) {
        console.log("store change! $current $v")
        if (latest != v && loaded) {
            if (v == null || v == initialData) {
                window.localStorage.removeItem(key)
            } else {
                val value = DEFAULT_JSON.encodeToString(serializer, v)
                window.localStorage.setItem(key, value)
            }
            latest = v
        }
    }

    init {
        try {
            data.distinctUntilChanged() handledBy {
                onStoreUpdated(it)
            }

            window.localStorage.getItem(key)?.let { content ->
                DEFAULT_JSON.decodeFromString(serializer, content).also { v ->
                    console.log("INIT $key")
                    latest = v
                }
            }?.let {item ->
                 update(item)
            }
            loaded = true
        } catch (e: Exception) {
            console.log(e)
            console.log(key)
        }
    }
}
