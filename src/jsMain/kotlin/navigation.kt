import components.activeNavButton
import components.secondaryButton
import components.navButton
import dev.fritz2.core.RenderContext
import dev.fritz2.routing.MapRouter
import dev.fritz2.routing.routerOf
import kotlinx.coroutines.flow.map
import org.koin.dsl.module

val navigationModule = module {
    single { routerOf(
        mapOf("page" to "main")
    ) }
}
enum class Page(val title: String, val showInMenu: Boolean = true) {
    Search("Search Tool"),
    Conf("Plugin Configuration"),
    Root("Not Found)",false)
    ;
    companion object {
        fun resolve(value: String?, defaultPage: Page = Root): Page {
            return value.takeIf { !it.isNullOrBlank() }.let {
                Page.entries.firstOrNull { it.name.equals(value,true) }
            } ?: defaultPage
        }
    }
}

val Page.route get() = mapOf("page" to name.lowercase())

fun RenderContext.menu() {
    val router by koin.inject<MapRouter>()
    router.select(key = "page").render { (selected, _) ->
        div("flex flex-col items-center") {
            Page.entries.filter { it.showInMenu }.forEach { page ->
                menuButton(page, page.name.lowercase() == selected)
            }
        }
    }
}



private fun RenderContext.menuButton(page: Page, active: Boolean = false) {
    val router by koin.inject<MapRouter>()

    if(active) {
        activeNavButton {
            +(page.title )
            clicks.map { page.route } handledBy router.navTo
        }
    } else {
        navButton {
            +(page.title )
            clicks.map { page.route } handledBy router.navTo
        }
    }
}

