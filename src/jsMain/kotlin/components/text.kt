package components

import dev.fritz2.core.HtmlTag
import dev.fritz2.core.RenderContext
import dev.fritz2.core.ScopeContext
import org.w3c.dom.HTMLHeadingElement

fun RenderContext.para(
    id: String? = null,
    scope: (ScopeContext.() -> Unit) = {},
    content: HtmlTag<HTMLHeadingElement>.() -> Unit
) = h2(
    "mb-2",
    id=id,
    scope = scope,
    content = content)