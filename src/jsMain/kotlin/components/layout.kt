package components

import dev.fritz2.core.*
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement

fun RenderContext.flexCol(content: HtmlTag<HTMLDivElement>.() -> Unit) {
    div("flex flex-col flex-wrap", content = content)
}

fun RenderContext.flexRow(content: HtmlTag<HTMLDivElement>.() -> Unit) {
    div("flex flex-row flex-wrap gap-2 align-middle place-items-center", content = content)
}

fun RenderContext.flexRowReverse(content: HtmlTag<HTMLDivElement>.() -> Unit) {
    div("flex flex-row-reverse flex-nowrap gap-2 items-start", content = content)
}

fun RenderContext.flexRowCentered(content: HtmlTag<HTMLDivElement>.() -> Unit) {
    div("flex flex-row flex-wrap gap-2 align-middle place-items-center mx-auto w-fit", content = content)
}

fun RenderContext.leftRightRow(content: HtmlTag<HTMLDivElement>.() -> Unit) {
    div("flex flex-row w-full place-items-center justify-between", content = content)
}

fun RenderContext.border(content: HtmlTag<HTMLDivElement>.() -> Unit) {
    div("rounded-lg border-2 border-blueBright-100 my-2 p-2 w-[80%]", content = content)
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



fun Tag<HTMLElement>.showTooltip(
    text: String
) {
    title(text)
    // weird issue with resize Loop
//    tooltip("bg-blueBright-900 text-white p-2") {
//        +text
//    }
}