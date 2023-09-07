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
    """.trimIndent(), viewBox = "0 0 32 32"),
    Download("""
    <path d="M23.845 8.125c-1.395-3.701-4.392-6.045-8.92-6.045-5.762 0-9.793 4.279-10.14 9.861-2.779 0.889-4.784 3.723-4.784 6.933 0 3.93 3.089 7.249 6.744 7.249h0.889c0.552 0 1-0.448 1-1s-0.448-1-1-1h-0.889c-2.572 0-4.776-2.404-4.776-5.249 0-2.515 1.763-4.783 3.974-5.163l0.907-0.156-0.081-0.917-0.008-0.011c0-4.871 3.206-8.545 8.162-8.545 3.972 0 6.204 1.957 7.236 5.295l0.213 0.688 0.721 0.015c3.715 0.078 6.971 3.092 6.971 6.837 0 3.408-2.259 7.206-5.679 7.206h-0.285c-0.552 0-1 0.448-1 1s0.448 1 1 1v-0.003c5-0.132 7.883-4.909 7.883-9.203-0.001-4.617-3.619-8.304-8.141-8.791zM20.198 24.233c-0.279-0.292-0.731-0.292-1.010-0l-2.2 2.427v-10.067c0-0.552-0.448-1-1-1s-1 0.448-1 1v10.076l-2.128-2.373c-0.28-0.292-0.732-0.355-1.011-0.063l-0.252 0.138c-0.28 0.293-0.28 0.765 0 1.057l3.61 3.992c0.005 0.005 0.006 0.012 0.011 0.017l0.253 0.265c0.14 0.146 0.324 0.219 0.509 0.218 0.183 0.001 0.368-0.072 0.507-0.218l0.253-0.265c0.005-0.005 0.008-0.011 0.012-0.017l3.701-4.055c0.279-0.292 0.279-0.639 0-0.932z"></path>
    """.trimIndent(),"0 0 32 32")
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

fun RenderContext.iconImage(svg: SvgIconSource, title: String="", baseClass: String?="w-30 h-30 m-2") {
    div {
        title(title)
        svg(baseClass) {
            attr("viewBox",svg.viewBox)
            content(svg.content)
        }
    }
}