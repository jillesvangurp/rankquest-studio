package search

import com.jilesvangurp.rankquest.core.DEFAULT_PRETTY_JSON
import com.jilesvangurp.rankquest.core.SearchPlugin
import com.jilesvangurp.rankquest.core.SearchResults
import components.busy
import components.primaryButton
import components.row
import dev.fritz2.core.RenderContext
import dev.fritz2.core.RootStore
import dev.fritz2.core.placeholder
import dev.fritz2.core.storeOf
import dev.fritz2.headless.components.inputField
import examples.quotesearch.MovieQuotesStore
import handlerScope
import koin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import kotlin.time.Duration.Companion.seconds

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

    div("flex items-center") {
        inputField("flex flex-col border md-6") {
            value(textStore)
            inputLabel {
                +"Search"
            }
            inputTextfield("mb-6 bg-gray-100 border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 p-2.5") {
                placeholder("Type something to search")
            }
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