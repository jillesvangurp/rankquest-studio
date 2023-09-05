package components

import dev.fritz2.core.HtmlTag
import dev.fritz2.core.RenderContext
import dev.fritz2.core.title
import org.w3c.dom.HTMLButtonElement

// icons sourced from https://www.svgrepo.com/
enum class SvgIconSource(val content:String,val viewBox: String="0 0 1024 1024") {
// icons sourced from https://www.svgrepo.com/
    Down("""
<path d="M917.333333 364.8L851.2 298.666667 512 637.866667 172.8 298.666667 106.666667 364.8 512 768z"  />        
    """.trimIndent()),
    Up("""
        <path d="M106.666667 659.2L172.8 725.333333 512 386.133333 851.2 725.333333l66.133333-66.133333L512 256z" />
    """.trimIndent()),
    Delete("""
        <path d="M160 256H96a32 32 0 0 1 0-64h256V95.936a32 32 0 0 1 32-32h256a32 32 0 0 1 32 32V192h256a32 32 0 1 1 0 64h-64v672a32 32 0 0 1-32 32H192a32 32 0 0 1-32-32V256zm448-64v-64H416v64h192zM224 896h576V256H224v640zm192-128a32 32 0 0 1-32-32V416a32 32 0 0 1 64 0v320a32 32 0 0 1-32 32zm192 0a32 32 0 0 1-32-32V416a32 32 0 0 1 64 0v320a32 32 0 0 1-32 32z"/>
    """.trimIndent()),
    Plus("""
 <g id="Page-1" stroke="none" stroke-width="1" fill-rule="evenodd" sketch:type="MSPage">
        <g id="Icon-Set" sketch:type="MSLayerGroup" transform="translate(-464.000000, -1087.000000)">
            <path d="M480,1117 C472.268,1117 466,1110.73 466,1103 C466,1095.27 472.268,1089 480,1089 C487.732,1089 494,1095.27 494,1103 C494,1110.73 487.732,1117 480,1117 L480,1117 Z M480,1087 C471.163,1087 464,1094.16 464,1103 C464,1111.84 471.163,1119 480,1119 C488.837,1119 496,1111.84 496,1103 C496,1094.16 488.837,1087 480,1087 L480,1087 Z M486,1102 L481,1102 L481,1097 C481,1096.45 480.553,1096 480,1096 C479.447,1096 479,1096.45 479,1097 L479,1102 L474,1102 C473.447,1102 473,1102.45 473,1103 C473,1103.55 473.447,1104 474,1104 L479,1104 L479,1109 C479,1109.55 479.447,1110 480,1110 C480.553,1110 481,1109.55 481,1109 L481,1104 L486,1104 C486.553,1104 487,1103.55 487,1103 C487,1102.45 486.553,1102 486,1102 L486,1102 Z" id="plus-circle" sketch:type="MSShapeGroup">

</path>
        </g>
    </g>        
    """.trimIndent(), viewBox = "0 0 32 32"),
    Minus("""
<g id="Page-1" stroke="none" stroke-width="1" fill-rule="evenodd" sketch:type="MSPage">
        <g id="Icon-Set" sketch:type="MSLayerGroup" transform="translate(-516.000000, -1087.000000)">
            <path d="M532,1117 C524.268,1117 518,1110.73 518,1103 C518,1095.27 524.268,1089 532,1089 C539.732,1089 546,1095.27 546,1103 C546,1110.73 539.732,1117 532,1117 L532,1117 Z M532,1087 C523.163,1087 516,1094.16 516,1103 C516,1111.84 523.163,1119 532,1119 C540.837,1119 548,1111.84 548,1103 C548,1094.16 540.837,1087 532,1087 L532,1087 Z M538,1102 L526,1102 C525.447,1102 525,1102.45 525,1103 C525,1103.55 525.447,1104 526,1104 L538,1104 C538.553,1104 539,1103.55 539,1103 C539,1102.45 538.553,1102 538,1102 L538,1102 Z" id="minus-circle" sketch:type="MSShapeGroup">

</path>
        </g>
    </g>        
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