package searchpluginconfig

import com.jilesvangurp.rankquest.core.DEFAULT_PRETTY_JSON
import com.jilesvangurp.rankquest.core.pluginconfiguration.SearchPluginConfiguration
import com.jilesvangurp.rankquest.core.plugins.PluginFactoryRegistry
import components.LocalStoringStore
import components.busyResult
import koin
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.encodeToString
import search.SearchResultsStore

class ActiveSearchPluginConfigurationStore : LocalStoringStore<SearchPluginConfiguration?>(
    null, "active-search-plugin-configuration", SearchPluginConfiguration.serializer().nullable
) {
    // using get forces an early init ;-), fixes bug where first search is empty because it does not create the store until you use it
    private val searchResultsStore = koin.get<SearchResultsStore>()
    private val pluginFactoryRegistry = koin.get<PluginFactoryRegistry>()

    val search = handle<Map<String, String>> { config, query ->
        busyResult({
            val outcome = if (config != null) {
                val selectedPlugin = current
                if (selectedPlugin != null) {
                    console.log("SEARCH $query")
                    val searchPlugin = pluginFactoryRegistry[config.pluginType]?.create(config)
                    searchPlugin?.fetch(query, query["size"]?.toIntOrNull() ?: 10)
                        ?: Result.failure(IllegalArgumentException("search plugin not found"))
                } else {
                    Result.failure(IllegalArgumentException("no plugin selected"))
                }
            } else {
                Result.failure(IllegalArgumentException("no plugin selected"))
            }
            console.log("success: " + outcome.isSuccess)
            console.log(outcome.getOrNull()?.let {
                DEFAULT_PRETTY_JSON.encodeToString(it)
            })
            searchResultsStore.update(outcome)
            Result.success(true)
        }, initialTitle = "Searching", initialMessage = "Query for $query")
        config
    }
}