package components

import dev.fritz2.core.HtmlTag
import dev.fritz2.core.RenderContext
import dev.fritz2.core.SvgTag
import dev.fritz2.core.title
import org.w3c.dom.HTMLButtonElement

// icons sourced from https://www.svgrepo.com/
enum class SvgIconSource(val content:String,val viewBox: String="0 0 1024 1024") {
// icons sourced from https://www.svgrepo.com/
    Expand("""
<path d="M917.333333 364.8L851.2 298.666667 512 637.866667 172.8 298.666667 106.666667 364.8 512 768z"  />        
    """.trimIndent()),
    Collapse("""
        <path d="M106.666667 659.2L172.8 725.333333 512 386.133333 851.2 725.333333l66.133333-66.133333L512 256z" />
    """.trimIndent()),
    Delete("""
        <path d="M160 256H96a32 32 0 0 1 0-64h256V95.936a32 32 0 0 1 32-32h256a32 32 0 0 1 32 32V192h256a32 32 0 1 1 0 64h-64v672a32 32 0 0 1-32 32H192a32 32 0 0 1-32-32V256zm448-64v-64H416v64h192zM224 896h576V256H224v640zm192-128a32 32 0 0 1-32-32V416a32 32 0 0 1 64 0v320a32 32 0 0 1-32 32zm192 0a32 32 0 0 1-32-32V416a32 32 0 0 1 64 0v320a32 32 0 0 1-32 32z"/>
    """.trimIndent())
}

fun RenderContext.iconButton(svg: SvgIconSource, title:String="",baseClass:String?="w-7 h-7 fill-blueBright-500 hover:fill-blueBright-900",block: (HtmlTag<HTMLButtonElement>.() -> Unit)?=null) {
    button(baseClass) {
        svg {
            attr("viewBox",svg.viewBox)
            content(svg.content)
        }
        title(title)

        block?.invoke(this)
    }
}