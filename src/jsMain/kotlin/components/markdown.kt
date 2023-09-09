package components

import dev.fritz2.core.RenderContext
import dev.fritz2.core.RootStore
import dev.fritz2.remote.http
import org.intellij.markdown.IElementType
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser

fun renderMarkdown(md: String): String {
    val src = md
    val flavour = GFMFlavourDescriptor()
    val parsedTree = MarkdownParser(flavour).parse(IElementType("ROOT"),src)
    return HtmlGenerator(src, parsedTree, flavour).generateHtml()
}

private class MarkdownStore(file: String): RootStore<String>("") {
    val load = handle<String> { _, path ->
        http(path).get().body()
    }

    init {
        load(file)
    }
}

fun RenderContext.markdownFile(file:String, baseClass: String?=null) {
    val mdStore =MarkdownStore(file)
    mdStore.data.render {
        div(baseClass) {  }.domNode.innerHTML = renderMarkdown(it)
    }
}