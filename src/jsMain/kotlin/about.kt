import components.markdownFile
import dev.fritz2.core.RenderContext
import dev.fritz2.routing.MapRouter
import kotlinx.coroutines.flow.map

fun RenderContext.about() {
    val router = koin.get<MapRouter>()

    div("mx-auto w-5/6 bg-blueBright-50 p-10") {


        div("flex flex-row gap-3") {
            a {
                +"License"
                clicks.map { Page.License.route } handledBy router.navTo
            }
            a {
                +"GDPR & Privacy"
                clicks.map { Page.Privacy.route } handledBy router.navTo
            }
        }
        markdownFile("about.md")
    }
}