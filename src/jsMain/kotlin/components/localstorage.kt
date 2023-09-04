package components

import com.jilesvangurp.rankquest.core.DEFAULT_JSON
import dev.fritz2.core.RootStore
import kotlinx.browser.window
import kotlinx.serialization.KSerializer

open class LocalStoringStore<T>(
    initialData: T?,
    val key: String,
    val serializer: KSerializer<T>
) :
    RootStore<T?>(initialData) {
    private var latest: T? = null
    private var loaded=false

//    val nonNullableStore by lazy {
//        val store = RootStore(emptyValue)
//        data.map { store } handledBy propagationHandler
//        store
//    }

    private val storeHandler = handle<T?> { _, v ->
        if (latest != v && loaded) {
            if (v == null || v == initialData) {
                console.log("DELETE $key")
                window.localStorage.removeItem(key)
            } else {
                console.log("OVERWRITE $key")
                val value = DEFAULT_JSON.encodeToString(serializer, v)
                window.localStorage.setItem(key, value)
            }
            latest = v
        }
        v
    }

//    private val propagationHandler = handle<RootStore<T>> {v,s->
//        if(v==null || v==emptyValue) {
//            s.update(emptyValue)
//        } else {
//            s.update(v)
//        }
//        v
//    }

//    fun asString(): String {
//        return current?.let { value ->
//            DEFAULT_JSON.encodeToString(serializer, value)
//        }?:""
//    }
//
//    val fromString = handle<String> { current, value ->
//        try {
//            DEFAULT_JSON.decodeFromString(serializer, value)
//        } catch (e: Exception) {
//            console.error(e)
//            current
//        }
//    }

    init {
        try {
            data handledBy storeHandler
            val item = window.localStorage.getItem(key)?.let {
                DEFAULT_JSON.decodeFromString(serializer, it).also { v ->
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
