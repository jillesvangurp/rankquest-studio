package search

import Page
import com.jilesvangurp.rankquest.core.RatedSearch
import com.jilesvangurp.rankquest.core.SearchResultRating
import com.jilesvangurp.rankquest.core.SearchResults
import com.jilesvangurp.rankquest.core.pluginconfiguration.SearchContextField
import components.*
import dev.fritz2.core.*
import koin
import kotlinx.coroutines.flow.map
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.w3c.dom.HTMLHeadingElement
import pageLink
import testcases.RatedSearchesStore
import searchpluginconfig.ActiveSearchPluginConfigurationStore
import kotlinx.coroutines.Job
import searchpluginconfig.noConfigYet
import utils.md5Hash

val searchModule = module {
    singleOf(::SearchResultsStore)
}

class SearchResultsStore() : RootStore<Result<SearchResults>?>(null, Job())


fun RenderContext.searchScreen() {
    val activeSearchPluginConfigurationStore = koin.get<ActiveSearchPluginConfigurationStore>()
    val ratedSearchesStore = koin.get<RatedSearchesStore>()
    val searchResultsStore = koin.get<SearchResultsStore>()

    activeSearchPluginConfigurationStore.data.render { config ->
        centeredMainPanel {

            if (config == null) {
                noConfigYet()
            } else {
                div("flex flex-col items-left space-y-1 w-fit m-auto") {
                    h1(content = fun HtmlTag<HTMLHeadingElement>.() {
                        +config.name
                    })
                    val stores = config.fieldConfig.associate {
                        it.name to when (it) {
                            is SearchContextField.BoolField -> storeOf("${it.defaultValue?:""}")
                            is SearchContextField.IntField -> storeOf("${it.defaultValue?:""}")
                            is SearchContextField.StringField -> storeOf(it.defaultValue?:"")
                        }
                    }
                    div {
                        for (field in config.fieldConfig) {
                            val fieldStore = stores[field.name]!!
                            when (field) {
                                is SearchContextField.BoolField -> {

                                    textField(
                                        placeHolder = "true", label = field.name
                                    ) {
                                        value(fieldStore)
//                                        changes.map {
//                                            stores.map { (f, s) -> f to s.current }.toMap()
//                                        } handledBy activeSearchPluginConfigurationStore.search
                                    }
                                }

                                is SearchContextField.IntField -> {
                                    textField(
                                        placeHolder = field.placeHolder, label = field.name
                                    ) {
                                        value(fieldStore)
                                    }
                                }

                                is SearchContextField.StringField -> {
                                    textField(
                                        placeHolder = field.placeHolder, label = field.name
                                    ) {
                                        value(fieldStore)
                                    }
                                }

                            }
                        }
                    }
                    flexRow {
                        searchResultsStore.data.render { searchResults ->
                            ratedSearchesStore.data.render { ratedSearches ->
                                // use a content hash to avoid duplicates
                                val rsId = md5Hash(*stores.map { it.value.current }.toTypedArray())
                                val alreadyAdded =
                                    ratedSearches != null && ratedSearches.firstOrNull { it.id == rsId } != null
                                secondaryButton(
                                    text = if (alreadyAdded) "Already a Testcase" else "Add Testcase",
                                    iconSource = SvgIconSource.Plus
                                ) {
                                    disabled(searchResults?.getOrNull()?.searchResultList.isNullOrEmpty() || alreadyAdded)
                                    clicks.map {
                                        val ratings = searchResultsStore.current?.let {
                                            it.getOrNull()?.let { searchResults ->
                                                searchResults.searchResultList.map {
                                                    SearchResultRating(
                                                        it.id, label = it.label, rating = 5
                                                    )
                                                }
                                            }
                                        } ?: listOf()
                                        RatedSearch(
                                            id = rsId,
                                            searchContext = stores.map { (f, s) -> f to s.current }.toMap(),
                                            ratings = ratings
                                        )
                                    } handledBy ratedSearchesStore.addOrReplace
                                }
                            }
                        }

                        primaryButton(text = "Search!", iconSource = SvgIconSource.Magnifier) {

                            clicks.map {
                                stores.map { (f, s) -> f to s.current }.toMap()
                            } handledBy activeSearchPluginConfigurationStore.search
                        }
                        infoPopup(
                            "The Search Tool", """
                            You can use the search tool to explore your search service and convert 
                            the searches you do into test cases.
                            
                            ## The search context
                            
                            The search form allows you to fill in values for each of the parameters in your
                            search context. You can customize the parameters and their default values
                            in the configuration screen.
                            
                            ## Creating test cases from search
                             
                            To create a new test case, simply click the "Add Test Case" button. If it is greyed out,
                            that means you already have a test case with the same search context. If so, you can
                            modify it in the test cases screen. The id of each test case is a content hash of the 
                            search context.
                        """.trimIndent()
                        )

                    }
                    ratedSearchesStore.data.render {
                        if(it.isNullOrEmpty()) {
                            para {
                                +"""
                                    You have no test cases yet. Use Add Testcase button for queries
                                    that you want to turn into a Test Case. This copies the query and results
                                    into a new test case.
                                """.trimIndent()
                            }
                        }
                    }
                    searchResults()
                }
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



