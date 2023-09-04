package search

import com.jilesvangurp.rankquest.core.SearchPlugin
import com.jilesvangurp.rankquest.core.SearchResults
import components.header1
import components.para
import components.primaryButton
import components.textField
import dev.fritz2.core.*
import dev.fritz2.headless.components.InputField
import dev.fritz2.headless.components.inputField
import examples.quotesearch.MovieQuotesStore
import examples.quotesearch.moviequotesSearchPluginConfig
import examples.quotesearch.searchPlugin
import handlerScope
import koin
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.w3c.dom.HTMLDivElement
import searchpluginconfig.SearchContextField
import searchpluginconfig.SearchPluginConfiguration

val searchModule = module {
    singleOf(::ActiveSearchPlugin)
    singleOf(::SearchResultsStore)
}

class SearchResultsStore : RootStore<Result<SearchResults>?>(null)

class ActiveSearchPlugin : RootStore<Pair<SearchPluginConfiguration, SearchPlugin>?>(null) {
    val searchResultsStore by koin.inject<SearchResultsStore>()

    val search = handle<Map<String, String>> { p, query ->
        if (p != null) {
            val (config, searchPlugin) = p
            val selectedPlugin = current
            if (selectedPlugin != null) {
                handlerScope.launch {
                    console.log("SEARCH $query")
                    val result = searchPlugin.fetch(query, query["size"]?.toInt() ?: 10)
                    console.log("success: ${result.isFailure}")
                    searchResultsStore.update(result)
                }
            } else {
                searchResultsStore.update(Result.failure(IllegalArgumentException("no plugin selected")))
            }
        }
        p
    }

    val loadPlugin = handle<SearchPluginConfiguration> { existing, c ->
        when (c.pluginName) {
            moviequotesSearchPluginConfig.pluginName -> {
                val movieQuotesStore by koin.inject<MovieQuotesStore>()
                movieQuotesStore.current.searchPlugin()
            }

            else -> error("unknown plugin")
        }
    }

}

fun RenderContext.searchScreen() {
    val activeSearchPlugin by koin.inject<ActiveSearchPlugin>()
    val searchResultsStore by koin.inject<SearchResultsStore>()


    activeSearchPlugin.data.render { configPair ->
        if (configPair == null) {
            para { +"Configure a search plugin first" }
        } else {
            val (config, plugin) = configPair
            val stores = config.fieldConfig.associate {
                it.name to when(it) {
                    is SearchContextField.BoolField -> storeOf("${it.defaultValue}")
                    is SearchContextField.IntField -> storeOf("${it.defaultValue}")
                    is SearchContextField.StringField -> storeOf("")
                }
            }
            div("flex flex-col items-left space-y-1 w-fit") {
                header1 { +config.pluginName }
                for (field in config.fieldConfig) {
                    val fieldStore = stores[field.name]!!
                    when (field) {
                       else -> {
                            textField(
                                placeHolder = "Type something to search for ..",
                                inputLabelText = field.name
                            ) {
                                value(fieldStore)
                                changes.map {
                                    stores.map { (f,s) -> f to s.current}.toMap()
                                } handledBy activeSearchPlugin.search
                            }
                        }
                    }
                }
                primaryButton {
                    +"Search!"

                    clicks.map {
                        stores.map { (f,s) -> f to s.current}.toMap()
                    } handledBy activeSearchPlugin.search
                }
            }
            searchResultsStore.data.render { rs ->
                when (rs) {
                    null -> {
                        para { +"-" }
                    }

                    else -> {
                        if (rs.isFailure) {
                            para { +"Oopsie ${rs.exceptionOrNull()}" }
                        } else {
                            val results = rs.getOrThrow()
                            p("mb-2") { +"Found ${results.total} results in ${results.responseTime}" }

                            ul("list-disc") {
                                results.searchResultList.forEach { result ->
                                    li {
                                        +"${result.id}${result.label?.let { l -> ": $l" } ?: ""}"
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}




