package search

import com.jilesvangurp.rankquest.core.DEFAULT_JSON
import com.jilesvangurp.rankquest.core.DEFAULT_PRETTY_JSON
import com.jilesvangurp.rankquest.core.RatedSearch
import com.jilesvangurp.rankquest.core.SearchResultRating
import com.jilesvangurp.rankquest.core.SearchResults
import com.jilesvangurp.rankquest.core.pluginconfiguration.SearchContextField
import com.jilesvangurp.rankquest.core.pluginconfiguration.SearchPluginConfiguration
import components.*
import dev.fritz2.core.*
import koin
import kotlinx.coroutines.flow.map
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.w3c.dom.HTMLHeadingElement
import testcases.RatedSearchesStore
import searchpluginconfig.ActiveSearchPluginConfigurationStore
import kotlinx.coroutines.Job
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import openai.OpenAiService
import org.w3c.dom.HTMLDivElement
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
        if (config == null) {
            noConfigYet()
        } else {
            val stores = config.fieldConfig.associate {
                it.name to when (it) {
                    is SearchContextField.BoolField -> storeOf("${it.defaultValue ?: ""}")
                    is SearchContextField.IntField -> storeOf("${it.defaultValue ?: ""}")
                    is SearchContextField.DoubleField -> storeOf("${it.defaultValue ?: ""}")
                    is SearchContextField.StringField -> storeOf(it.defaultValue ?: "")
                }
            }
            centeredMainPanel {
                flexRowReverse {
                    div("flex flex-col justify-start space-y-1 w-fit mx-auto") {
                        h1(content = fun HtmlTag<HTMLHeadingElement>.() {
                            +config.name
                        })
                        searchForm(
                            config,
                            ratedSearchesStore,
                            searchResultsStore,
                            stores,
                            activeSearchPluginConfigurationStore
                        )

                        ratedSearchesStore.data.render {
                            if (it.isNullOrEmpty()) {
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
                    aiSideBar(config, stores,activeSearchPluginConfigurationStore)
                }
            }
        }
    }
}

fun RenderContext.searchForm(
    config: SearchPluginConfiguration,
    ratedSearchesStore: RatedSearchesStore,
    searchResultsStore: SearchResultsStore,
    stores: Map<String, Store<String>>,
    activeSearchPluginConfigurationStore: ActiveSearchPluginConfigurationStore
) {
    div {
        for (field in config.fieldConfig) {
            val fieldStore = stores[field.name]!!
            //                                        changes.map {
//                                            stores.map { (f, s) -> f to s.current }.toMap()
//                                        } handledBy activeSearchPluginConfigurationStore.search
            div("flex flex-row flex-nowrap gap-2 align-middle place-items-center", content = fun HtmlTag<HTMLDivElement>.() {
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
                    is SearchContextField.DoubleField -> {
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

                if (field.help.isNotBlank()) {
                    infoPopup("Help", markdown = field.help)
                }
            })
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
                                searchResults.searchResultList.mapIndexed { index, searchResult ->
                                    SearchResultRating(
                                        searchResult.id,
                                        label = searchResult.label,
                                        rating = (if (index < 5) 5 - index else 1)
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

@Serializable
data class AiGeneratedQueries(val items: List<Map<String,String>>)

fun RenderContext.aiSideBar(
    config: SearchPluginConfiguration,
    stores: Map<String, Store<String>>,
    activeSearchPluginConfigurationStore: ActiveSearchPluginConfigurationStore
) {
    div("w-35%") {
        val showAiTools = storeOf(false)
        h1 {
            +"Search Tools"
        }
        a {
            showAiTools.data.render {show ->
                if(show) {
                    +"Close AI Query Generation"
                } else {
                    +"AI Query Generation"
                }
            }
            clicks handledBy {
                showAiTools.update(!showAiTools.current)
            }
        }
        showAiTools.data.render {show ->
            if(show) {
                val populatedFields = stores.filter { (key,store) -> store.current.isNotBlank() }.map {(key,store) ->
                    key to store.current
                }.toMap()
                val systemPromptStore = storeOf("""
Given a search context with these parameters:

{${DEFAULT_PRETTY_JSON.encodeToString(populatedFields)}}

Generate similar json in response to the questions. 

The response should be json object with a list property items matching this schema

{
  "\{'$'}schema": "http://json-schema.org/draft-07/schema#",
  "type": "array",
  "items": {
    "type": "object",
    "patternProperties": {
      ".*": {
        "type": "string"
      }
    },
    "additionalProperties": false
  }
}

It is important to use the same fields as the original parameters.
""".trimIndent())
                textAreaField("", "AI System Prompt") {
                    value(systemPromptStore)
                }
                val promptStore = storeOf("""
Generate 20 query contexts similar to this with spelling mistakes, synonyms, variations of word order, etc.
""".trimIndent())
                textAreaField("Add some details about what type of queries you want to generate.", "AI Prompt") {
                    value(promptStore)
                }
                val answersStore = storeOf<List<Map<String,String>>>(emptyList())
                primaryButton {
                    +"Generate Queries"
                    clicks handledBy {
                        runWithBusy({
                            val ai = koin.get<OpenAiService>()
                            ai.sendPrompt(systemPromptStore.current, promptStore.current)
                        }) { answer ->
                            answersStore.update(DEFAULT_JSON.decodeFromString<AiGeneratedQueries>(answer).items)
                        }
                    }
                }

                answersStore.data.render {items ->
                    ul {
                        items.forEach { item ->
                            li {
                                a {
                                    +"Use"
                                    clicks handledBy {
                                        item.forEach { (key, value) ->
                                            stores[key]?.update?.invoke(value)
                                        }
                                    }
                                }
                                +": "
                                +DEFAULT_JSON.encodeToString(item)
                            }

                        }
                    }
                }
            }
        }
    }
}



