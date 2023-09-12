import components.busyPopupMountPoint
import components.row
import dev.fritz2.core.RenderContext
import dev.fritz2.core.render
import dev.fritz2.core.src
import dev.fritz2.headless.components.toastContainer
import dev.fritz2.headless.foundation.portalRoot
import dev.fritz2.routing.MapRouter
import metrics.metrics
import ratedsearches.ratedSearches
import search.searchScreen
import searchpluginconfig.ActiveSearchPluginConfigurationStore
import searchpluginconfig.pluginConfiguration

suspend fun main() {
    koinInit()
    render("#target") { // using id selector here, leave blank to use document.body by default
        div("h-screen flex flex-col overflow-hidden") {
            div("bg-blueBright-50 p-1.5 flex flex-col md:flex-row w-full align-middle justify-between") {
                row {
                    rankQuestStudio()
                }
                div("flex flex-col md:flex-row gap-2 overflow-auto") {
                    menu()
                }
            }
            div("bg-blueBright-50 scroll-smooth overflow-auto grow-0 h-full w-full") {
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
    busyPopupMountPoint()
    cookiePopup()
    div {
        div("w-full") {

            router.select(key = "page").render { (selected, _) ->
                when (Page.resolve(selected)) {
                    Page.Search -> div {
                        searchScreen()
                    }

                    Page.Conf -> {
                        pluginConfiguration()
                    }

                    Page.TestCases -> ratedSearches()
                    Page.Metrics -> metrics()
                    Page.About -> about()
                    Page.Privacy -> privacy()
                    Page.License -> license()
                    Page.Root -> {
                        if (activeSearchPluginConfigurationStore.current == null) {
                            router.navTo(Page.Conf.route)
                        } else {
                            router.navTo(Page.Search.route)
                        }
                    }

                }
            }
        }
    }
    toastContainer(
        "messages", // name
        "absolute bottom-5 left-1/2 -translate-x-48 mx-auto flex flex-col gap-2 place-items-center w-96"
    )
    portalRoot()
}
