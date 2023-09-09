package components

import com.jilesvangurp.rankquest.core.DEFAULT_JSON
import dev.fritz2.core.RootStore
import kotlinx.browser.window
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.serialization.KSerializer

@Suppress("LeakingThis")
open class LocalStoringStore<T>(
    initialData: T?,
    val key: String,
    val serializer: KSerializer<T>
) :
    RootStore<T?>(initialData) {
    private var latest: T? = null
    private var loaded=false

    private val storeHandler = handle<T?> { old, v ->
        if (latest != v && loaded) {
            if (v == null || v == initialData) {
                console.log("DELETE $key")
                window.localStorage.removeItem(key)
            } else {
                console.log("Update $key")
                val value = DEFAULT_JSON.encodeToString(serializer, v)
                window.localStorage.setItem(key, value)
            }
            latest = v
        }
        v
    }
    init {
        try {
            data.distinctUntilChanged() handledBy storeHandler
            val item = window.localStorage.getItem(key)?.let { content ->
                DEFAULT_JSON.decodeFromString(serializer, content).also { v ->
                    console.log("INIT $key with stored value $v")
                    latest = v
                }
            }
            if (item != null) {
                update(item)
            }
            loaded = true
        } catch (e: Exception) {
            console.log(e)
            console.log(key)
        }
    }
}
