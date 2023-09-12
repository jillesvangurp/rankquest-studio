package components

import dev.fritz2.core.HtmlTag
import dev.fritz2.core.RenderContext
import org.w3c.dom.HTMLDivElement

fun RenderContext.row(content: HtmlTag<HTMLDivElement>.() -> Unit) {
    div("flex flex-row gap-2 align-middle", content = content)
}

fun RenderContext.centeredMainPanel(content: HtmlTag<HTMLDivElement>.() -> Unit) =
    div("flex flex-col grow items-left space-y-1 w-5/6 m-auto bg-white px-10 pt-5 pb-32", content = content )