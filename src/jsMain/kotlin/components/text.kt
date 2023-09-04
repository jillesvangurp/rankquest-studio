package components

import dev.fritz2.core.HtmlTag
import dev.fritz2.core.RenderContext
import dev.fritz2.core.ScopeContext
import org.w3c.dom.HTMLHeadingElement

fun RenderContext.header1(
    id: String? = null,
    scope: (ScopeContext.() -> Unit) = {},
    content: HtmlTag<HTMLHeadingElement>.() -> Unit
) = h1(
    "text-3xl mb-4",
    id=id,
    scope = scope,
    content = content)

fun RenderContext.header2(
    id: String? = null,
    scope: (ScopeContext.() -> Unit) = {},
    content: HtmlTag<HTMLHeadingElement>.() -> Unit
) = h2(
    "text-3xl",
    id=id,
    scope = scope,
    content = content)

fun RenderContext.para(
    id: String? = null,
    scope: (ScopeContext.() -> Unit) = {},
    content: HtmlTag<HTMLHeadingElement>.() -> Unit
) = h2(
    "mb-2",
    id=id,
    scope = scope,
    content = content)