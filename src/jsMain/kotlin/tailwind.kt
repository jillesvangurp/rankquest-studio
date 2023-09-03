import dev.fritz2.core.HtmlTag
import dev.fritz2.core.RenderContext
import org.w3c.dom.HTMLDivElement


enum class DivStyle(val classes:String) {
    Primary("p-1.5 bg-white"),
    Secondary("p-1.5 bg-white")
}

fun tw(vararg values: Any) = values.joinToString(" ")


fun RenderContext.styledDiv(divStyle: DivStyle,vararg otherClasses: String, content: HtmlTag<HTMLDivElement>.() -> Unit) =
    div(tw(divStyle.classes,*otherClasses), content = content)
