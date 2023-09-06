package components

import dev.fritz2.core.HtmlTag
import dev.fritz2.core.RenderContext
import dev.fritz2.core.title
import org.w3c.dom.HTMLButtonElement

// icons sourced from https://www.svgrepo.com/
enum class SvgIconSource(val content:String,val viewBox: String) {
    // icons sourced from https://www.svgrepo.com/
    // orchid line icons collection, MIT Licensed: https://www.svgrepo.com/collection/orchid-line-interface-icons/
    Delete("""
        <path d="M16 0c-8.836 0-16 7.163-16 16s7.163 16 16 16c8.837 0 16-7.163 16-16s-7.163-16-16-16zM16 30.032c-7.72 0-14-6.312-14-14.032s6.28-14 14-14 14 6.28 14 14-6.28 14.032-14 14.032zM21.657 10.344c-0.39-0.39-1.023-0.39-1.414 0l-4.242 4.242-4.242-4.242c-0.39-0.39-1.024-0.39-1.415 0s-0.39 1.024 0 1.414l4.242 4.242-4.242 4.242c-0.39 0.39-0.39 1.024 0 1.414s1.024 0.39 1.415 0l4.242-4.242 4.242 4.242c0.39 0.39 1.023 0.39 1.414 0s0.39-1.024 0-1.414l-4.242-4.242 4.242-4.242c0.391-0.391 0.391-1.024 0-1.414z"></path>
    """.trimIndent(),"0 0 32 32"),
    Plus("""
 <path d="M16 0c-8.836 0-16 7.163-16 16s7.163 16 16 16c8.837 0 16-7.163 16-16s-7.163-16-16-16zM16 30.032c-7.72 0-14-6.312-14-14.032s6.28-14 14-14 14 6.28 14 14-6.28 14.032-14 14.032zM23 15h-6v-6c0-0.552-0.448-1-1-1s-1 0.448-1 1v6h-6c-0.552 0-1 0.448-1 1s0.448 1 1 1h6v6c0 0.552 0.448 1 1 1s1-0.448 1-1v-6h6c0.552 0 1-0.448 1-1s-0.448-1-1-1z"></path>
    """.trimIndent(), viewBox = "0 0 32 32"),
    Minus("""
<path d="M16 0c8.844 0 16 7.156 16 16s-7.156 16-16 16-16-7.156-16-16 7.156-16 16-16zM16 30.031c7.719 0 14-6.313 14-14.031s-6.281-14-14-14-14 6.281-14 14 6.281 14.031 14 14.031zM14.906 17h-5.906c-0.563 0-1-0.438-1-1s0.438-1 1-1h14c0.563 0 1 0.438 1 1s-0.438 1-1 1h-8.094z"></path>
    """.trimIndent(), viewBox = "0 0 32 32")
}

fun RenderContext.iconButton(svg: SvgIconSource, title:String="",baseClass:String?="w-5 h-5 fill-blueBright-500 hover:fill-blueBright-900",block: (HtmlTag<HTMLButtonElement>.() -> Unit)?=null) {
    button(baseClass) {
        svg {
            attr("viewBox",svg.viewBox)
            content(svg.content)
        }
        title(title)

        block?.invoke(this)
    }
}