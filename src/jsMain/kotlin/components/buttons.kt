package components

import dev.fritz2.core.HtmlTag
import dev.fritz2.core.RenderContext
import dev.fritz2.core.ScopeContext
import org.w3c.dom.HTMLButtonElement

fun RenderContext.primaryButton(
    id: String? = null,
    scope: (ScopeContext.() -> Unit) = {},
    content: HtmlTag<HTMLButtonElement>.() -> Unit
) = button(
    baseClass = "m-2 w-fit text-white bg-blueBright-600 hover:bg-blueBright-700 disabled:bg-gray-300 focus:ring-button-300 focus:ring-4 font-medium rounded-lg text-sm px-5 py-2.5 focus:outline-none",
    id=id,
    scope = scope,
    content = content
)

fun RenderContext.secondaryButton(
    id: String? = null,
    scope: (ScopeContext.() -> Unit) = {},
    content: HtmlTag<HTMLButtonElement>.() -> Unit
) = button(
    baseClass = "m-2 w-fit text-white bg-blueMuted-600 hover:bg-blueMuted-700 disabled:bg-gray-300 focus:ring-buttonSecondary-300 focus:ring-4 font-medium rounded-lg text-sm px-5 py-2.5 focus:outline-none",
    id=id,
    scope = scope,
    content = content
)

fun RenderContext.navButton(
    id: String? = null,
    scope: (ScopeContext.() -> Unit) = {},
    content: HtmlTag<HTMLButtonElement>.() -> Unit
) = button(
    baseClass = "mb-2 w-11/12 text-white bg-blueGrayMuted-600 hover:bg-blueGrayMuted-700 focus:ring-buttonNav-300 focus:ring-4 font-medium rounded-lg text-sm px-5 py-2.5 focus:outline-none",
    id=id,
    scope = scope,
    content = content
)

fun RenderContext.activeNavButton(
    id: String? = null,
    scope: (ScopeContext.() -> Unit) = {},
    content: HtmlTag<HTMLButtonElement>.() -> Unit
) = button(
    baseClass = "mb-2 w-11/12 text-white bg-blueGray-600 hover:bg-blueGray-700 focus:ring-buttonNavAct-300 focus:ring-4 font-medium rounded-lg text-sm px-5 py-2.5 focus:outline-none",
    id=id,
    scope = scope,
    content = content
)