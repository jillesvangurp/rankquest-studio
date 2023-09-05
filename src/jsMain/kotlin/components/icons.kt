package components

import dev.fritz2.core.RenderContext

enum class SvgIconSource(val viewBox: String, val content:String) {
    Expand("0 0 1024 1024", """
<path d="M917.333333 364.8L851.2 298.666667 512 637.866667 172.8 298.666667 106.666667 364.8 512 768z" />        
    """.trimIndent()),
    Collapse("0 0 1024 1024","""
        <path d="M106.666667 659.2L172.8 725.333333 512 386.133333 851.2 725.333333l66.133333-66.133333L512 256z" fill="#2196F3" />
    """.trimIndent())
}

fun RenderContext.icon(svg: SvgIconSource, baseClass:String?="w-8 h-8 fill-blueBright-600 hover:fill-blueBright-300") {
    svg(baseClass) {
        attr("viewBox",svg.viewBox)
        content(svg.content)
    }
}