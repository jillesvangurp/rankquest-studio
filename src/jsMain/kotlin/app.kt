import components.busyPopup
import dev.fritz2.core.RenderContext
import dev.fritz2.core.render
import dev.fritz2.core.src
import dev.fritz2.headless.foundation.portalRoot
import dev.fritz2.routing.MapRouter
import metrics.metrics
import ratedsearches.ratedSearches
import search.ActiveSearchPluginConfigurationStore
import search.searchScreen
import searchpluginconfig.pluginConfiguration

suspend fun main() {
    koinInit()
    render("#target") { // using id selector here, leave blank to use document.body by default
        div("h-screen flex flex-col overflow-hidden") {
            div("bg-blueBright-50 p-1.5 flex flex-col md:flex-row w-full align-middle justify-between") {
                div("flex flex-row gap-x-3") {
                    rankQuestStudio()
                }
                div("flex flex-col md:flex-row gap-x-3 overflow-auto") {
                    menu()
                }
            }
            div("bg-white scroll-smooth overflow-auto grow-0 h-full w-full") {
                mainView()
            }
        }
    }
}


private fun RenderContext.rankQuestStudio() {
    img("w-8 h-8") {
        src("rankquest_logo.png")
    }
    h1("text-blueBright-700 font-bold m-0") { +"Rankquest Studio" }
}

private fun RenderContext.mainView() {
    val router = koin.get<MapRouter>()
    val activeSearchPluginConfigurationStore = koin.get<ActiveSearchPluginConfigurationStore>()
    busyPopup()
    div {
        router.select(key = "page").render { (selected, _) ->
//            console.log("selected $selected")
            when (Page.resolve(selected)) {
                Page.Search -> div {
                    searchScreen()
                }
                Page.Conf -> {
                    pluginConfiguration()
                }
                Page.RatedSearches -> ratedSearches()
                Page.Metrics -> metrics()
                Page.Root -> {
                    if(activeSearchPluginConfigurationStore.current == null) {
                        router.navTo(Page.Conf.route)
                    } else {
                        router.navTo(Page.Search.route)
                    }
                }

            }
        }
    }
    portalRoot()
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