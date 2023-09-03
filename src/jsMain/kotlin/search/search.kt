package search

import com.jilesvangurp.rankquest.core.SearchPlugin
import com.jilesvangurp.rankquest.core.SearchResults
import components.primaryButton
import dev.fritz2.core.*
import dev.fritz2.headless.components.InputField
import dev.fritz2.headless.components.inputField
import examples.quotesearch.MovieQuotesStore
import handlerScope
import koin
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.w3c.dom.HTMLDivElement

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

    div("flex flex-row items-center space-x-1") {
        textField(
            placeHolder = "Type something to search for ..",
            inputLabelText = "q"
        ) {
            value(textStore)
            changes.map {
                mapOf(
                    "q" to textStore.current,
                    "size" to "5"
                )
            } handledBy activeSearchPlugin.search
        }
        primaryButton {
            +"Search!"

            clicks.map {
                mapOf(
                    "q" to textStore.current,
                    "size" to "5"
                )
            } handledBy activeSearchPlugin.search
        }
    }
    searchResultsStore.data.render { rs ->
        when(rs) {
            null -> {
                p { +"-"}
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
}


fun RenderContext.textField(
    placeHolder: String? = null,
    inputLabelText: String? = null,
    id: String? = null,
    scope: (ScopeContext.() -> Unit) = {},
    initialize: InputField<HTMLDivElement>.() -> Unit
) {
    inputField("flex flex-row border p-2 items-center", id=id, scope=scope) {
        inputLabelText?.let { l ->
            inputLabel("italic mr-5") {
                +l
            }
        }
        inputTextfield("bg-gray-100 border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 p-2.5") {
            placeHolder?.let { pl ->
                placeholder(pl)
            }
        }

        initialize(this)
    }
}

