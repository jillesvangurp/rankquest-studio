import components.markdownFile
import dev.fritz2.core.RenderContext

fun RenderContext.about() {
    markdownFile("about.md", "mx-auto w-5/6 bg-blueBright-50 p-10")
}