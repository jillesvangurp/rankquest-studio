package search

import com.jilesvangurp.rankquest.core.SearchPlugin
import com.jilesvangurp.rankquest.core.SearchResults
import components.primaryButton
import dev.fritz2.core.RenderContext
import dev.fritz2.core.RootStore
import dev.fritz2.core.placeholder
import dev.fritz2.core.storeOf
import dev.fritz2.headless.components.inputField
import examples.quotesearch.MovieQuotesStore
import handlerScope
import koin
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val searchModule = module {
    singleOf(::ActiveSearchPlugin)
    singleOf(::SearchResultsStore)
}

class SearchResultsStore: RootStore<Result<SearchResults>?>(null)

class ActiveSearchPlugin: RootStore<SearchPlugin?>(null) {
    val searchResultsStore by koin.inject<SearchResultsStore>()

    val search = handle<Map<String,String>>{plugin, query ->
        val selectedPlugin = current
        if(selectedPlugin != null) {
            handlerScope.launch {
                console.log("SEARCH $query")
                val result = selectedPlugin.fetch(query, query["size"]?.toInt()?:10)
                console.log("success: ${result.isFailure}")
                searchResultsStore.update(result)
            }
        } else {
            searchResultsStore.update(Result.failure(IllegalArgumentException("no plugin selected")))
        }
        plugin
    }

}

fun RenderContext.searchScreen() {
    val movieQuotesStore by koin.inject<MovieQuotesStore>()
    val activeSearchPlugin by koin.inject<ActiveSearchPlugin>()
    val searchResultsStore by koin.inject<SearchResultsStore>()

    val textStore = storeOf("")

    div("flex items-center align-middle") {
        inputField("flex flex-col border md-6") {
            value(textStore)
            inputLabel {
                +"q"
            }
            inputTextfield("bg-gray-100 border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 p-2.5") {
                placeholder("Type something to search")
            }
            changes.map { mapOf(
                "q" to textStore.current,
                "size" to "5"
            ) } handledBy activeSearchPlugin.search
        }
        primaryButton {
            +"Search!"

            clicks.map { mapOf(
                "q" to textStore.current,
                "size" to "5"
            ) } handledBy activeSearchPlugin.search
        }
    }
    searchResultsStore.data.render { rs ->
        when(rs) {
            null -> {
                p { +"Execute a search"}
            }
            else -> {
                if(rs.isFailure) {
                    p {+"Oopsie ${rs.exceptionOrNull()}"}
                } else {
                    val results = rs.getOrThrow()
                    p {"Found ${results.total} results in ${results.responseTime}"}
                    ul {
                        results.searchResultList.forEach { result ->
                            li {
                                +"${result.id}${result.label?.let { l -> ": $l" }?:""}"
                            }
                        }
                    }
                }
            }
        }
    }
//    movieQuotesStore.data.render { content ->
//        pre {
//            +content.let { DEFAULT_PRETTY_JSON.encodeToString(it) }
//        }
//    }

}