package search

import Page
import com.jilesvangurp.rankquest.core.RatedSearch
import com.jilesvangurp.rankquest.core.SearchResultRating
import com.jilesvangurp.rankquest.core.SearchResults
import com.jilesvangurp.rankquest.core.pluginconfiguration.SearchContextField
import com.jilesvangurp.rankquest.core.pluginconfiguration.SearchPluginConfiguration
import com.jilesvangurp.rankquest.core.plugins.PluginFactoryRegistry
import components.*
import dev.fritz2.core.*
import handlerScope
import koin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.nullable
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.w3c.dom.HTMLHeadingElement
import pageLink
import ratedsearches.RatedSearchesStore
import utils.md5Hash
import kotlin.time.Duration.Companion.milliseconds

val searchModule = module {
    singleOf(::ActiveSearchPluginConfigurationStore)
    singleOf(::SearchResultsStore)
}

class SearchResultsStore : RootStore<Result<SearchResults>?>(null)

class ActiveSearchPluginConfigurationStore : LocalStoringStore<SearchPluginConfiguration?>(
    null,
    "active-search-plugin-configuration",
    SearchPluginConfiguration.serializer().nullable
) {
    // using get forces an early init ;-), fixes bug where first search is empty because it does not create the store until you use it
    private val searchResultsStore = koin.get<SearchResultsStore>()
    private val pluginFactoryRegistry = koin.get<PluginFactoryRegistry>()

    val search = handle<Map<String, String>> { config, query ->
        busy({
            var outcome: Result<SearchResults>? = null
            coroutineScope {
                launch {
                    if (config != null) {
                        val selectedPlugin = current
                        if (selectedPlugin != null) {
                            handlerScope.launch {
                                console.log("SEARCH $query")
                                val searchPlugin = pluginFactoryRegistry.get(config.pluginType)?.create(config)
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

fun RenderContext.searchScreen() {
    val activeSearchPluginConfigurationStore = koin.get<ActiveSearchPluginConfigurationStore>()
    val ratedSearchesStore = koin.get<RatedSearchesStore>()
    val searchResultsStore = koin.get<SearchResultsStore>()


    activeSearchPluginConfigurationStore.data.render { config ->
        if (config == null) {
            para {
                +"Configure a search plugin first. "
                pageLink(Page.Conf)
            }
        } else {
            val stores = config.fieldConfig.associate {
                it.name to when (it) {
                    is SearchContextField.BoolField -> storeOf("${it.defaultValue}")
                    is SearchContextField.IntField -> storeOf("${it.defaultValue}")
                    is SearchContextField.StringField -> storeOf("")
                }
            }
            div("flex flex-col items-left space-y-1 w-fit items-center m-auto") {
                h1(
                    content = fun HtmlTag<HTMLHeadingElement>.() {
                        +config.title
                    })
                div("") {
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
                                        stores.map { (f, s) -> f to s.current }.toMap()
                                    } handledBy activeSearchPluginConfigurationStore.search
                                }
                            }
                        }
                    }
                }
                div("flex flex-row") {
                    searchResultsStore.data.render { searchResults ->
                        ratedSearchesStore.data.render { ratedSearches ->
                            val rsId = md5Hash(*stores.map { it.value.current }.toTypedArray())
                            val alreadyAdded = ratedSearches != null && ratedSearches.firstOrNull { it.id == rsId } != null
                            secondaryButton {
                                +if (alreadyAdded) "Already a Testcase" else "Add Testcase"
                                disabled(searchResults?.getOrNull()?.searchResultList.isNullOrEmpty() || alreadyAdded)
                                clicks.map {
                                    val ratings = searchResultsStore.current?.let {
                                        it.getOrNull()?.let { searchResults ->
                                            var rate = searchResults.searchResultList.size
                                            searchResults.searchResultList.map {
                                                SearchResultRating(
                                                    it.id,
                                                    label = it.label,
                                                    rating = rate--
                                                )
                                            }
                                        }
                                    } ?: listOf()
                                    RatedSearch(
                                        // FIXME nicer id?
                                        id = rsId,
                                        searchContext = stores.map { (f, s) -> f to s.current }.toMap(),
                                        ratings = ratings
                                    )
                                } handledBy ratedSearchesStore.addOrReplace
                            }
                        }
                    }

                    primaryButton {
                        +"Search!"

                        clicks.map {
                            stores.map { (f, s) -> f to s.current }.toMap()
                        } handledBy activeSearchPluginConfigurationStore.search
                    }

                }
                searchResults()
            }
        }
    }
}

fun RenderContext.searchResults() {
    val searchResultsStore = koin.get<SearchResultsStore>()
    searchResultsStore.data.render { rs ->
        div("mt-10") {
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
                                li("ml-5") {
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



