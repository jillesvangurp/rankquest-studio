import components.busyPopupModule
import examples.quotesearch.quoteSearchModule
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import search.searchModule

val koin get() = GlobalContext.get()

val handlerScope = CoroutineScope(CoroutineName("handler"))

suspend fun koinInit() {
    startKoin {
        modules(
            busyPopupModule,
            quoteSearchModule,
            searchModule,
            navigationModule
        )
    }
}