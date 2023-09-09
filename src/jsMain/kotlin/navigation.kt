import components.activeNavButton
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
    TestCases("Test Cases"),
    Metrics("Metrics"),
    Conf("Configuration"),
    About("About"),
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

fun RenderContext.pageLink(page: Page) {
    val router = koin.get<MapRouter>()

    a {
        +page.title
        clicks.map { page.route } handledBy router.navTo
    }
}


fun RenderContext.menu() {
    val router = koin.get<MapRouter>()
    router.select(key = "page").render { (selected, _) ->
        Page.entries.filter { it.showInMenu }.forEach { page ->
            menuButton(page, page.name.lowercase() == selected)
        }
    }
}



private fun RenderContext.menuButton(page: Page, active: Boolean = false) {
    val router = koin.get<MapRouter>()

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

