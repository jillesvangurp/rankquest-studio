package search

import com.jilesvangurp.rankquest.core.RatedSearch
import com.jilesvangurp.rankquest.core.SearchPlugin
import com.jilesvangurp.rankquest.core.SearchResultRating
import com.jilesvangurp.rankquest.core.SearchResults
import components.*
import dev.fritz2.core.*
import examples.quotesearch.MovieQuotesStore
import examples.quotesearch.movieQuotesSearchPluginConfig
import examples.quotesearch.searchPlugin
import handlerScope
import koin
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.builtins.nullable
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.w3c.dom.HTMLHeadingElement
import pageLink
import ratedsearches.RatedSearchesStore
import searchpluginconfig.SearchContextField
import searchpluginconfig.SearchPluginConfiguration
import utils.md5Hash

val searchModule = module {
    singleOf(::ActiveSearchPluginConfiguration)
    singleOf(::SearchResultsStore)
}

class SearchResultsStore : RootStore<Result<SearchResults>?>(null)

class ActiveSearchPluginConfiguration : LocalStoringStore<SearchPluginConfiguration?>(
    null,
    "active-search-plugin-configuration",
    SearchPluginConfiguration.serializer().nullable
) {
    // using get forces an early init ;-), fixes bug where first search is empty because it does not create the store until you use it
    private val movieQuotesStore = koin.get<MovieQuotesStore>()
    private val searchResultsStore by koin.inject<SearchResultsStore>()

    val search = handle<Map<String, String>> { config, query ->
        if (config != null) {
            val selectedPlugin = current
            if (selectedPlugin != null) {
                handlerScope.launch {
                    console.log("SEARCH $query")
                    val result = searchPlugin?.fetch(query, query["size"]?.toInt() ?: 10)
                    searchResultsStore.update(result)
                }
            } else {
                searchResultsStore.update(Result.failure(IllegalArgumentException("no plugin selected")))
            }
        }
        config
    }

    val searchPlugin: SearchPlugin?
        get() = when (current?.pluginName) {
            movieQuotesSearchPluginConfig.pluginName -> {
                movieQuotesStore.current.searchPlugin()
            }

            else -> {
                console.log("unknown plugin ${current?.pluginName}")
                null
            }
        }
}

fun RenderContext.searchScreen() {
    val activeSearchPluginConfiguration by koin.inject<ActiveSearchPluginConfiguration>()
    val ratedSearchesStore by koin.inject<RatedSearchesStore>()
    val searchResultsStore by koin.inject<SearchResultsStore>()


    activeSearchPluginConfiguration.data.render { config ->
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
            div("flex flex-col items-left space-y-1 w-fit") {
                h1(
                    content = fun HtmlTag<HTMLHeadingElement>.() {
                        +config.pluginName
                    })
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
                                } handledBy activeSearchPluginConfiguration.search
                            }
                        }
                    }
                }
                div("flex flex-row") {
                    searchResultsStore.data.render {searchResults->
                        ratedSearchesStore.data.render { ratedSearches ->
                            val rsId = md5Hash(*stores.map { it.value.current }.toTypedArray())
                            val alreadyAdded = ratedSearches.firstOrNull {it.id == rsId } != null
                            secondaryButton {
                                +if(alreadyAdded) "Already a Testcase" else "Add Testcase"
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
                        } handledBy activeSearchPluginConfiguration.search
                    }
                }
            }
            searchResults()
        }
    }
}

fun RenderContext.searchResults() {
    val searchResultsStore by koin.inject<SearchResultsStore>()
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



