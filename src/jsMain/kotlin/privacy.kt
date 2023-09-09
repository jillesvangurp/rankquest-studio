import components.markdownFile
import components.para
import components.primaryButton
import dev.fritz2.core.RenderContext
import dev.fritz2.routing.MapRouter
import kotlinx.coroutines.flow.map

fun RenderContext.privacy() {
    val router = koin.get<MapRouter>()

    div("mx-auto w-5/6 bg-blueBright-50 p-10") {
        markdownFile("privacy.md")
        primaryButton {
            +"Back"
            clicks.map { Page.About.route } handledBy router.navTo

        }
    }
}