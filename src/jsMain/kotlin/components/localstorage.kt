package components

import com.jilesvangurp.rankquest.core.DEFAULT_JSON
import dev.fritz2.core.RootStore
import dev.fritz2.core.Store
import kotlinx.browser.window
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer

@Suppress("LeakingThis")
open class LocalStoringStore<T>(
    val initialData: T?,
    val key: String,
    val serializer: KSerializer<T>
) :
    RootStore<T?>(initialData) {
    private var latest: T? = null
    private var loaded=false

    fun nonNullableStore(defaultValue: T): RootStore<T> {
        val store = RootStore(defaultValue)
        data handledBy {v->
            if(v==null || v==initialData) {
                store.update(defaultValue)
            } else {
                store.update(v)
            }
        }
        return store
    }

    private val propagationHandler = handle<Pair<RootStore<T>,T>> {v,(s,defaultValue)->
        if(v==null || v==initialData) {
            s.update(defaultValue)
        } else {
            s.update(v)
        }
        v
    }

    private val storeHandler = handle<T?> { old, v ->
        if (latest != v && loaded) {
            if (v == null || v == initialData) {
//                console.log("DELETE $key")
                window.localStorage.removeItem(key)
            } else {
//                console.log("Update $key")
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
                    console.log("INIT $key")
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
