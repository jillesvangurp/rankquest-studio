import components.busyPopupMountPoint
import components.flexRow
import dev.fritz2.core.RenderContext
import dev.fritz2.core.render
import dev.fritz2.core.src
import dev.fritz2.headless.components.toastContainer
import dev.fritz2.headless.foundation.portalRoot
import dev.fritz2.routing.MapRouter
import metrics.metrics
import testcases.testCases
import search.searchScreen
import searchpluginconfig.ActiveSearchPluginConfigurationStore
import searchpluginconfig.SettingsStore
import searchpluginconfig.pluginConfiguration
import utils.JsLogLevel
import utils.setJsLogLevel


suspend fun main() {
    setJsLogLevel(JsLogLevel.INFO)
    koinInit()
    val cookiePermissionStore = koin.get<CookiePermissionStore>()
    cookiePermissionStore.awaitLoaded() // prevents flashing the cookie screen before we load the settings from local storage
    // force initialization on start
    val settingsStore = koin.get<SettingsStore>()
    render("#target") { // using id selector here, leave blank to use document.body by default
        busyPopupMountPoint()

        cookiePopup()
        div("h-screen flex flex-col overflow-hidden") {
            div("bg-blueBright-50 p-2 flex flex-col md:flex-row w-full align-middle justify-between") {
                flexRow {
                    rankQuestStudioLogo()
                }
                div("flex flex-col md:flex-row gap-2 overflow-auto") {
                    menu()
                }
            }
            div("bg-blueBright-50 scroll-smooth overflow-auto grow-0 h-full w-full") {
                mainView()
            }
        }
        // FIXME there's a .portal-container style rule that interferes with this working correctly
        // https://github.com/jwstegemann/fritz2/issues/802
        toastContainer(
            "messages", // name
            "absolute bottom-5 left-1/2 -translate-x-48 mx-auto flex flex-col gap-2 place-items-center w-96"
        )
        portalRoot()
    }
}


private fun RenderContext.rankQuestStudioLogo() {
    img("w-8 h-8") {
        src("rankquest_logo.png")
    }
    h1("text-blueBright-700 font-bold m-0") { +"Rankquest Studio" }
}

private fun RenderContext.mainView() {
    val router = koin.get<MapRouter>()
    val activeSearchPluginConfigurationStore = koin.get<ActiveSearchPluginConfigurationStore>()
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

                    Page.TestCases -> testCases()
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
}
