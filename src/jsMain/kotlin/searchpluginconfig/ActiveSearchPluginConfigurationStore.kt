package searchpluginconfig

import com.jilesvangurp.rankquest.core.SearchResults
import com.jilesvangurp.rankquest.core.pluginconfiguration.SearchPluginConfiguration
import com.jilesvangurp.rankquest.core.plugins.PluginFactoryRegistry
import components.LocalStoringStore
import components.busyResult
import handlerScope
import koin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.nullable
import search.SearchResultsStore
import kotlin.time.Duration.Companion.milliseconds

class ActiveSearchPluginConfigurationStore : LocalStoringStore<SearchPluginConfiguration?>(
    null, "active-search-plugin-configuration", SearchPluginConfiguration.serializer().nullable
) {
    // using get forces an early init ;-), fixes bug where first search is empty because it does not create the store until you use it
    private val searchResultsStore = koin.get<SearchResultsStore>()
    private val pluginFactoryRegistry = koin.get<PluginFactoryRegistry>()

    val search = handle<Map<String, String>> { config, query ->
        busyResult({
            var outcome: Result<SearchResults>? = null
            coroutineScope {
                launch {
                    if (config != null) {
                        val selectedPlugin = current
                        if (selectedPlugin != null) {
                            handlerScope.launch {
                                console.log("SEARCH $query")
                                val searchPlugin = pluginFactoryRegistry[config.pluginType]?.create(config)
                                outcome = searchPlugin?.fetch(query, query["size"]?.toInt() ?: 10)
                            }
                        } else {
                            outcome = Result.failure(IllegalArgumentException("no plugin selected"))
                        }
                    }
                }
                // whichever takes longer; make sure the spinner doesn't flash in and out
                launch {
                    delay(200.milliseconds)
                }
            }.join()
            searchResultsStore.update(outcome)
            Result.success(true)
        }, initialTitle = "Searching", initialMessage = "Query for $query")
        config
    }
}