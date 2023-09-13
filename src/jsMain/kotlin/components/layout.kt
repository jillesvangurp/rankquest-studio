package components

import dev.fritz2.core.HtmlTag
import dev.fritz2.core.RenderContext
import dev.fritz2.core.transition
import org.w3c.dom.HTMLDivElement

fun RenderContext.row(content: HtmlTag<HTMLDivElement>.() -> Unit) {
    div("flex flex-row gap-2 align-middle", content = content)
}
fun RenderContext.rowCemtered(content: HtmlTag<HTMLDivElement>.() -> Unit) {
    div("flex flex-row gap-2 align-middle place-items-center mx-auto w-fit", content = content)
}
fun RenderContext.leftRightRow(content: HtmlTag<HTMLDivElement>.() -> Unit) {
    div("flex flex-row w-full place-items-center justify-between", content = content)
}

fun RenderContext.border(content: HtmlTag<HTMLDivElement>.() -> Unit) {
    div("rounded-lg border-2 border-blueBright-100 my-2 p-2 w-full", content = content)
}

fun RenderContext.centeredMainPanel(content: HtmlTag<HTMLDivElement>.() -> Unit) =
    div("""flex flex-col grow items-left space-y-1 w-5/6 m-auto 
        |bg-white px-10 pt-5 pb-32 transition-opacity
        |drop-shadow-md border-bg-blueBright-200
        |rounded-lg border-2 border-blueBright-100""".trimMargin()) {
        transition(
            "ease-out duration-100",
            "opacity-0",
            "opacity-100",
            "ease-in duration-100",
            "opacity-100",
            "opacity-0"
        )
        content.invoke(this)
    }