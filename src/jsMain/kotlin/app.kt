import components.primaryButton
import dev.fritz2.core.RenderContext
import dev.fritz2.core.render
import dev.fritz2.routing.MapRouter
import examples.quotesearch.MovieQuotesStore
import kotlinx.coroutines.flow.map
import search.searchScreen

suspend fun main() {
    koinInit()
    render("#target") { // using id selector here, leave blank to use document.body by default
        div("h-screen flex flex-col overflow-hidden") {
            div("bg-gray-100 p-1.5") {
                statusBar()
            }
            div("flex flex-row grow h-full w-full") {
                div("bg-gray-100 shrink-0 w-2/12") {
                    menu()
                }
                div("bg-white overflow-auto grow-0 h-full w-full") {
                    mainView()
                }
            }
        }
    }
}


private fun RenderContext.statusBar() {
    p { +"Rankquest Studio" }
}

private fun RenderContext.mainView() {
    val router by koin.inject<MapRouter>()
    val movieQuotesStore by koin.inject<MovieQuotesStore>()

    div {
        router.select(key = "page").render { (selected, _) ->
            when (Page.resolve(selected)) {
                Page.Search -> div {
                    searchScreen()
                }

                Page.Conf -> {
                    div {
                        h1 { +"Configure" }

                        primaryButton {
                            +"Use Movie Quotes"
                            clicks.map { "moviequotes.json" } handledBy movieQuotesStore.load
                        }
                    }
                }
            }
        }
    }
}


/**

manage search APIs

Search plugin configurations:

- templated json POST
- extract template variables from json string
- add headers
- json path for items, id, and label
- export configuration

- search explore UI
- populate query context
- review results
- add to test cases

- test case browser
- list test cases
- open test case
- edit query context
- result rater (star rating)
- benchmark configuration
- sane defaults
- add/remove metric configuration
- configure metric configuration
- run benchmark
- review results
- detail for each test case
- export/import
- diff
- export/import

- cli run benchmark
- cli search
- cli add test case
 */