package components

import dev.fritz2.core.HtmlTag
import dev.fritz2.core.RenderContext
import org.w3c.dom.HTMLDivElement

fun RenderContext.row(content: HtmlTag<HTMLDivElement>.() -> Unit) {
    div("flex flex-row", content = content)
}