import components.busyPopupModule
import examples.quotesearch.quoteSearchModule
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import metrics.metricsModule
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import search.searchModule
import searchpluginconfig.configurationModule
import testcases.ratedSearchesModule

val koin get() = GlobalContext.get()

val handlerScope = CoroutineScope(CoroutineName("handler"))

suspend fun koinInit() {
    startKoin {
        modules(
            busyPopupModule,
            cookiePermissionModule,
            quoteSearchModule,
            configurationModule,
            searchModule,
            ratedSearchesModule,
            metricsModule,
            navigationModule,
        )
    }
}